package com.kasiguru.ui.screens.games.sentenceorder

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.local.entity.VocabularyEntity
import com.kasiguru.data.repository.GameLevelRepository
import com.kasiguru.data.repository.GameRepository
import com.kasiguru.data.repository.SavedGameRepository
import com.kasiguru.data.repository.UserProgressRepository
import com.kasiguru.data.repository.VocabularyRepository
import com.kasiguru.domain.games.SavedRound
import com.kasiguru.ui.screens.games.shared.toReviewItem
import com.kasiguru.ui.screens.games.shared.toSavedAnswer
import com.kasiguru.util.Constants
import com.kasiguru.util.srs.ReviewRating
import com.kasiguru.util.srs.ReviewRatingMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.kasiguru.domain.games.sentenceHint
import com.kasiguru.domain.lesson.SentenceBank
import com.kasiguru.ui.components.GameReviewItem

data class SentenceQuestion(
    val englishSentence: String,
    val correctKasiguraninWords: List<String>,
    val shuffledWords: List<String>,
    /** The dictionary word whose example this is, for My words; null for an authored bank sentence. */
    val wordId: Int? = null,
    /** What each of the sentence's words means ([sentenceHint]); null when none is in the dictionary. */
    val hint: String? = null
)

data class SentenceOrderUiState(
    val questions: List<SentenceQuestion> = emptyList(),
    val currentQuestionIndex: Int = 0,
    val availableWords: List<String> = emptyList(),
    val constructedWords: List<String> = emptyList(),
    val isCorrect: Boolean? = null,
    val score: Int = 0,
    /** The current sentence's word meanings are showing; costs the round's perfect bonus. */
    val hintRevealed: Boolean = false,
    val isGameFinished: Boolean = false,
    val finalXp: Int = 0,
    val starsEarned: Int = 0,
    val totalQuestions: Int = 5,
    val reviewItems: List<GameReviewItem> = emptyList(),
    val nextLevel: Int? = null
)

@HiltViewModel
class SentenceOrderViewModel @Inject constructor(
    private val vocabularyRepository: VocabularyRepository,
    private val userProgressRepository: UserProgressRepository,
    private val gameRepository: GameRepository,
    private val gameLevelRepository: GameLevelRepository,
    private val savedGames: SavedGameRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    /** The level being played; Start over replays it. */
    val levelNumber = savedStateHandle.get<Int>("level") ?: 1

    private var finishing = false
    private var usedHint = false
    private val _uiState = MutableStateFlow(SentenceOrderUiState())
    val uiState: StateFlow<SentenceOrderUiState> = _uiState.asStateFlow()

    private val questionQueue = mutableListOf<SentenceQuestion>()
    private var questionStartTimeMs: Long = 0L
    private val reviewItems = mutableListOf<GameReviewItem>()

    init {
        loadQuestions()
    }

    private fun loadQuestions() {
        reviewItems.clear()
        viewModelScope.launch {
            var questionsCount = 5
            val levelInfo = gameLevelRepository.getLevel("sentence_order", levelNumber)
            if (levelInfo != null) {
                questionsCount = levelInfo.questionsCount
            }

            val allVocabRaw = vocabularyRepository.getAllVocabulary().first()
            val allVocab = if (allVocabRaw.isNotEmpty()) allVocabRaw else emptyList<VocabularyEntity>()

            // Two sources of running Kasiguranin, both authored by people.
            //
            // The bank is the project's own fifteen sentences, shared with the lesson system -- they
            // used to be a literal here, which meant the lessons could not reach them. The corpus
            // sentences are the ones recorded on a word in the admin portal, and they are the half
            // that grows: every sentence collected from a speaker widens this game with no app
            // release, which is the whole point of collecting them.
            //
            // A corpus sentence without a gloss is skipped, because the gloss is the prompt the
            // learner reads. Nothing here can tell which *language* a gloss is in, though, and the
            // admin field was labelled "Its Tagalog or English translation" until recently, so the
            // handful recorded so far hold Tagalog and will read as such. That is repaired in the
            // admin, not here -- see its "Sentence needs an English translation" filter.
            val bankSentences = SentenceBank.sentences.map { authored ->
                SentenceQuestion(
                    englishSentence = authored.english,
                    correctKasiguraninWords = authored.kasiguranin,
                    shuffledWords = authored.kasiguranin.shuffled()
                )
            }
            val corpusSentences = allVocab.mapNotNull { word ->
                val sentence = word.exampleSentence.trim()
                val gloss = word.exampleTranslation.trim()
                if (sentence.isBlank() || gloss.isBlank()) return@mapNotNull null
                val parts = sentence.split(" ").filter { it.isNotBlank() }
                // The same floor the lesson's sentence-building exercise uses. One definition, two readers.
                if (parts.size < SentenceBank.MIN_WORDS) return@mapNotNull null
                SentenceQuestion(
                    englishSentence = gloss,
                    correctKasiguraninWords = parts,
                    shuffledWords = parts.shuffled(),
                    wordId = word.id
                )
            }
            // Distinct, because one sentence can be recorded on more than one of the words it uses.
            val rawSentences = (bankSentences + corpusSentences)
                .distinctBy { it.correctKasiguraninWords.joinToString(" ").lowercase() }
            val vocabMap = allVocab.associateBy { it.kasiguranin.lowercase() }
            val vocabNeutralMap = allVocab.associateBy { it.neutralForm.lowercase() }

            fun sentenceScore(sentence: SentenceQuestion): Int {
                var totalReviews = 0
                var matchedWords = 0
                for (rawToken in sentence.correctKasiguraninWords) {
                    val cleanToken = rawToken.replace(Regex("[^a-zA-ZáéíóúəÁÉÍÓÚƏ\\-]"), "").lowercase()
                    val matched = vocabMap[cleanToken] ?: vocabNeutralMap[cleanToken]
                    if (matched != null) {
                        totalReviews += matched.timesReviewed
                        matchedWords++
                    }
                }
                return if (matchedWords > 0) totalReviews / matchedWords else 0
            }

            // A saved round is dealt its own sentences again, unless one has since left the corpus.
            val saved = savedGames.load("sentence_order", levelNumber) as? SavedRound
            val sentencesByKey = rawSentences.associateBy { keyOf(it) }
            val savedSentences = saved?.questionKeys
                ?.let { keys -> keys.mapNotNull { sentencesByKey[it] }.takeIf { it.size == keys.size } }
            val resumed = saved?.takeIf { savedSentences != null }

            val sampleSentences = (savedSentences ?: rawSentences.sortedBy { sentenceScore(it) }.take(questionsCount).shuffled())
                .map { q -> q.copy(hint = sentenceHint(q.correctKasiguraninWords) { vocabMap[it] ?: vocabNeutralMap[it] }) }

            questionQueue.clear()
            questionQueue.addAll(sampleSentences)
            questionStartTimeMs = System.currentTimeMillis()
            if (resumed != null) {
                reviewItems.addAll(resumed.answers.map { it.toReviewItem() })
                usedHint = resumed.usedHint
            }
            val startIndex = resumed?.nextIndex ?: 0

            _uiState.update {
                it.copy(
                    questions = questionQueue.toList(),
                    currentQuestionIndex = startIndex,
                    availableWords = questionQueue.getOrNull(startIndex)?.shuffledWords ?: emptyList(),
                    constructedWords = emptyList(),
                    isCorrect = null,
                    score = resumed?.score ?: 0,
                    hintRevealed = resumed?.hintRevealed ?: false,
                    totalQuestions = resumed?.totalQuestions ?: questionsCount
                )
            }
            // Every sentence answered before leaving: straight to the results.
            if (resumed != null && startIndex >= questionQueue.size) finishRound()
        }
    }

    /** How a sentence is known in a saved round: its Kasiguranin words, as the corpus is deduplicated. */
    private fun keyOf(sentence: SentenceQuestion): String =
        sentence.correctKasiguraninWords.joinToString(" ").lowercase()

    /** Keeps the round so far, so that leaving the game does not lose it. */
    private fun saveProgress(hintRevealed: Boolean = false) {
        savedGames.save(
            "sentence_order", levelNumber,
            SavedRound(
                questionKeys = questionQueue.map { keyOf(it) },
                answers = reviewItems.map { it.toSavedAnswer() },
                score = _uiState.value.score,
                totalQuestions = _uiState.value.totalQuestions,
                usedHint = usedHint,
                hintRevealed = hintRevealed
            )
        )
    }

    fun selectWord(word: String) {
        val currentState = _uiState.value
        val newAvailable = currentState.availableWords.toMutableList()
        newAvailable.remove(word)
        val newConstructed = currentState.constructedWords + word

        _uiState.update {
            it.copy(
                availableWords = newAvailable,
                constructedWords = newConstructed
            )
        }
    }

    fun deselectWord(word: String) {
        val currentState = _uiState.value
        val newConstructed = currentState.constructedWords.toMutableList()
        newConstructed.remove(word)
        val newAvailable = currentState.availableWords + word

        _uiState.update {
            it.copy(
                availableWords = newAvailable,
                constructedWords = newConstructed
            )
        }
    }

    /** Shows what the sentence's words mean. The answer still counts; the perfect bonus does not. */
    fun revealHint() {
        val state = _uiState.value
        if (state.hintRevealed || state.isCorrect != null) return
        usedHint = true
        _uiState.update { it.copy(hintRevealed = true) }
        saveProgress(hintRevealed = true)
    }

    fun checkAnswer() {
        val currentState = _uiState.value
        val currentQuestion = questionQueue.getOrNull(currentState.currentQuestionIndex) ?: return
        if (currentState.isCorrect != null) return

        val userSentence = currentState.constructedWords.joinToString(" ")
        val correctSentence = currentQuestion.correctKasiguraninWords.joinToString(" ")
        val isCorrect = userSentence.trim() == correctSentence.trim()
        val responseTimeMs = System.currentTimeMillis() - questionStartTimeMs

        val rating = ReviewRatingMapper.ratingForAnswer(isCorrect, responseTimeMs)

        val newScore = currentState.score + if (isCorrect) 1 else 0

        reviewItems.add(
            GameReviewItem(
                prompt = currentQuestion.englishSentence,
                userAnswer = if (userSentence.isBlank()) "(Empty)" else userSentence,
                correctAnswer = correctSentence,
                isCorrect = isCorrect
            )
        )

        _uiState.update {
            it.copy(
                isCorrect = isCorrect,
                score = newScore,
                questions = questionQueue.toList()
            )
        }
        saveProgress()

        // Feeds SM-2 only on success, and only ever as a mild positive.
        //
        // This game tests word *order*, not word *meaning*, so the evidence it produces about any
        // individual word is asymmetric. Ordering the sentence correctly does show the learner
        // recognised each word in context — weak but real positive evidence. Getting the order
        // wrong shows nothing about vocabulary at all: someone can know every word perfectly and
        // still misplace the enclitic.
        //
        // Previously every token in the sentence was written with the shared rating, so one fast
        // correct sentence stamped EASY on five or six words at once — inflating their intervals
        // far beyond what a single ordering task earns — and one wrong sentence stamped AGAIN on
        // all of them, wiping the schedule of words the learner may well have known. GOOD rather
        // than the latency-derived rating for the same reason: how fast a sentence is assembled
        // measures syntax fluency, not how fast any one word was recalled.
        if (isCorrect) {
            viewModelScope.launch {
                val allVocab = vocabularyRepository.getAllVocabulary().first()
                for (rawToken in currentQuestion.correctKasiguraninWords) {
                    val cleanToken = rawToken.replace(Regex("[^a-zA-ZáéíóúəÁÉÍÓÚƏ\\-]"), "")
                    val matched = allVocab.firstOrNull {
                        it.kasiguranin.equals(cleanToken, ignoreCase = true) ||
                                it.neutralForm.equals(cleanToken, ignoreCase = true)
                    }
                    if (matched != null) {
                        vocabularyRepository.processWordReview(matched, ReviewRating.GOOD)
                    }
                }
            }
        }
    }

    fun nextQuestion() {
        val currentState = _uiState.value
        val nextIndex = currentState.currentQuestionIndex + 1

        if (nextIndex < questionQueue.size) {
            val nextQuestion = questionQueue[nextIndex]
            questionStartTimeMs = System.currentTimeMillis()
            _uiState.update {
                it.copy(
                    currentQuestionIndex = nextIndex,
                    availableWords = nextQuestion.shuffledWords,
                    constructedWords = emptyList(),
                    isCorrect = null,
                    hintRevealed = false
                )
            }
        } else {
            finishRound()
        }
    }

    private fun finishRound() {
        if (finishing) return
        finishing = true
        // Before the reward, so the round can never be picked up and rewarded again.
        savedGames.clear("sentence_order", levelNumber)
        val currentState = _uiState.value
        viewModelScope.launch {
            val totalQs = questionQueue.size
            val successRate = currentState.score.toFloat() / totalQs.coerceAtLeast(1)
            val starsEarned = when {
                successRate >= 1.0f -> 3
                successRate >= 0.7f -> 2
                successRate >= 0.4f -> 1
                else -> 0
            }
            val earned = userProgressRepository.awardGame("sentence_order","sentence_order",levelNumber,currentState.score,totalQs,starsEarned,currentState.score == totalQs && !usedHint,
                currentState.questions.mapNotNull { it.wordId })
            gameLevelRepository.saveLevelResult("sentence_order", levelNumber, starsEarned)

            gameRepository.saveGameScore(
                gameType = Constants.Games.SENTENCE_ORDER,
                score = currentState.score,
                totalQuestions = totalQs,
                xpEarned = earned
            )
            userProgressRepository.incrementGamesPlayed()
            userProgressRepository.updateGameStats(currentState.score, totalQs)
            val nextLevel = if (levelNumber < 30 && (starsEarned >= 1 || gameLevelRepository.getLevel("sentence_order", levelNumber + 1)?.isUnlocked == true)) {
                levelNumber + 1
            } else null

            _uiState.update {
                it.copy(
                    isGameFinished = true,
                    finalXp = earned,
                    starsEarned = starsEarned,
                    totalQuestions = totalQs,
                    reviewItems = reviewItems.toList(),
                    nextLevel = nextLevel
                )
            }
        }
    }

    fun resetGame() {
        finishing = false
        usedHint = false
        reviewItems.clear()
        _uiState.update {
            SentenceOrderUiState(
                questions = emptyList(),
                currentQuestionIndex = 0,
                availableWords = emptyList(),
                constructedWords = emptyList(),
                isCorrect = null,
                score = 0,
                isGameFinished = false
            )
        }
        loadQuestions()
    }
}
