package com.kasiguru.data.repository

import com.kasiguru.data.local.dao.VocabularyDao
import com.kasiguru.data.local.entity.VocabularyEntity
import com.kasiguru.domain.lesson.DistractorSelector
import com.kasiguru.util.AnswerLabel
import com.kasiguru.util.Constants
import com.kasiguru.util.srs.ReviewRating
import com.kasiguru.util.srs.Sm2Algorithm
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class VocabularyRepository @Inject constructor(
    private val vocabularyDao: VocabularyDao,
    private val userProgressRepository: UserProgressRepository
) {
    fun getAllVocabulary(): Flow<List<VocabularyEntity>> =
        vocabularyDao.getAllVocabulary()

    /** One-shot read of the whole corpus, for callers that must not hold a subscription. */
    suspend fun getAllVocabularyOnce(): List<VocabularyEntity> =
        vocabularyDao.getAllVocabularyOnce()

    fun getVocabularyByCategory(category: String): Flow<List<VocabularyEntity>> =
        vocabularyDao.getVocabularyByCategory(category)

    suspend fun getVocabularyById(id: Int): VocabularyEntity? =
        vocabularyDao.getVocabularyById(id)

    fun getLearnedVocabulary(): Flow<List<VocabularyEntity>> =
        vocabularyDao.getLearnedVocabulary()

    suspend fun getRandomWords(count: Int): List<VocabularyEntity> =
        vocabularyDao.getRandomWords(count)

    suspend fun getFreshWords(count: Int): List<VocabularyEntity> =
        vocabularyDao.getFreshWords(count)

    suspend fun getUnlearnedWords(count: Int): List<VocabularyEntity> =
        vocabularyDao.getUnlearnedWords(count)

    suspend fun getDueReviewWords(count: Int = 10): List<VocabularyEntity> {
        val today = LocalDate.now().toString()
        val due = vocabularyDao.getDueReviewWords(today, count)
        return if (due.isNotEmpty()) due else vocabularyDao.getRandomWords(count)
    }

    /**
     * Words genuinely due for review today, with no fallback.
     *
     * [getDueReviewWords] substitutes random words when nothing is due, which is right for filling a
     * practice deck but wrong for any count shown to the learner — it would report words due every
     * single day. Anything that displays a number must use this.
     */
    suspend fun getDueReviewWordsStrict(limit: Int = 20): List<VocabularyEntity> =
        vocabularyDao.getScheduledDueWords(LocalDate.now().toString(), limit)

    /**
     * Words for one round of practice: everything genuinely due first, then unseen material.
     *
     * The games write an SM-2 schedule on every answer and then ignored it when choosing what to
     * ask next — selection was `ORDER BY timesReviewed ASC` (VocabularyDao.getFreshWords), so a
     * word due today was no likelier to appear than any other word with the same review count.
     * That is a novelty treadmill: it spends the learner's session on whatever they have seen
     * least rather than on what they are about to forget, which is the entire mechanism spaced
     * repetition relies on.
     *
     * Rounds are not made *purely* of due words on purpose — see [buildPracticeRound].
     */
    suspend fun getPracticeWords(count: Int): List<VocabularyEntity> {
        val today = LocalDate.now().toString()
        // Ask for the full round's worth: buildPracticeRound decides how many actually get used.
        val due = vocabularyDao.getScheduledDueWords(today, count)
        val fresh = vocabularyDao.getFreshWords(count * 2)
        return buildPracticeRound(due, fresh, count)
    }

    fun getLearnedCount(): Flow<Int> =
        vocabularyDao.getLearnedCount()

    fun getTotalCount(): Flow<Int> =
        vocabularyDao.getTotalCount()

    suspend fun getTotalCountDirect(): Int =
        vocabularyDao.getTotalCountDirect()

    suspend fun insertAll(words: List<VocabularyEntity>) =
        vocabularyDao.insertAll(words)

    suspend fun updateVocabulary(word: VocabularyEntity) =
        vocabularyDao.updateVocabulary(word)

    fun getCategories(): Flow<List<String>> =
        vocabularyDao.getCategories()

    fun searchVocabulary(query: String): Flow<List<VocabularyEntity>> =
        vocabularyDao.searchVocabulary(query)

    /**
     * Processes a spaced repetition review for a word using SM-2.
     * Updates SM-2 fields and increments user progress stats if learned threshold is passed.
     */
    suspend fun processWordReview(word: VocabularyEntity, rating: ReviewRating): VocabularyEntity {
        val sm2Result = Sm2Algorithm.calculateNextReview(word, rating)
        val updatedWord = word.copy(
            easinessFactor = sm2Result.easinessFactor,
            intervalDays = sm2Result.intervalDays,
            nextReviewDate = sm2Result.nextReviewDate,
            timesReviewed = sm2Result.timesReviewed,
            isLearned = sm2Result.isLearned,
            lapses = sm2Result.lapses,
            relearningStep = sm2Result.relearningStep
        )
        vocabularyDao.updateVocabulary(updatedWord)

        // A retrieval is the smallest unit of real learning in the app, so it is what keeps the
        // streak alive. Same-day repeats are cheap: the streak write returns early once set.
        userProgressRepository.recordLearningActivity()

        if (!word.isLearned && sm2Result.isLearned) {
            userProgressRepository.incrementWordsLearned()
            userProgressRepository.addXp(Constants.XP_PER_WORD_LEARNED)
            checkCategoryMastery(word.category)
        }
        return updatedWord
    }

    /**
     * Backs the "Category Master" badge. Lives here rather than in UserProgressRepository because
     * that would need VocabularyRepository injected there, which already depends on
     * UserProgressRepository - a cycle Hilt can't build.
     */
    private suspend fun checkCategoryMastery(category: String) {
        val words = vocabularyDao.getVocabularyByCategory(category).first()
        if (words.isNotEmpty() && words.all { it.isLearned }) {
            userProgressRepository.checkAchievements(
                com.kasiguru.data.local.entity.MetricType.CATEGORY_MASTERED,
                currentValue = 1
            )
        }
    }

    /**
     * Manually mark a word as learned from the dictionary — "I already know this one".
     *
     * Seeds SM-2 to a state that genuinely satisfies the mastery bar rather than setting the flag
     * and hoping. The previous version pinned `timesReviewed` to 2 with the comment "ensure SM-2
     * treats it as learned", which was true only while the threshold happened to be 2; once the bar
     * moved, the next real review would recompute `isLearned` as false and the learner's manual
     * mark would silently evaporate. Reading the thresholds from the algorithm keeps the two in
     * step by construction.
     */
    suspend fun markAsLearned(id: Int) {
        val word = vocabularyDao.getVocabularyById(id) ?: return
        if (word.isLearned) return

        val sm2Result = Sm2Algorithm.calculateNextReview(word, ReviewRating.GOOD)
        val seededInterval = maxOf(Sm2Algorithm.MIN_LEARNED_INTERVAL_DAYS, sm2Result.intervalDays)
        val updatedWord = word.copy(
            easinessFactor = sm2Result.easinessFactor,
            intervalDays = seededInterval,
            // Next review follows the seeded interval, not SM-2's shorter one — claiming to know a
            // word should not put it at the front of tomorrow's queue.
            nextReviewDate = LocalDate.now().plusDays(seededInterval.toLong()).toString(),
            timesReviewed = maxOf(Sm2Algorithm.MIN_LEARNED_REVIEWS, sm2Result.timesReviewed),
            isLearned = true
        )
        vocabularyDao.updateVocabulary(updatedWord)

        userProgressRepository.incrementWordsLearned()
        userProgressRepository.addXp(Constants.XP_PER_WORD_LEARNED)
        checkCategoryMastery(word.category)
    }

    suspend fun unmarkAsLearned(id: Int) {
        val word = vocabularyDao.getVocabularyById(id) ?: return
        val resetWord = word.copy(
            isLearned = false,
            timesReviewed = 0,
            intervalDays = 0,
            nextReviewDate = "",
            // The schedule is cleared, so the ladder position it referred to is meaningless. The
            // lapse count stays: it is the honest record that this word has been forgotten before,
            // and unmarking a word is itself an admission of that rather than a reason to forget it.
            relearningStep = 0
        )
        vocabularyDao.updateVocabulary(resetWord)
    }

    suspend fun incrementReviewCount(id: Int) =
        vocabularyDao.incrementReviewCount(id)

    /**
     * Fetches the wrong answers to show beside [targetWord].
     *
     * The old rule was fixed: same category if possible, otherwise similar length or first letter,
     * otherwise anything. Fixed is wrong in both directions — a word met for the first time got
     * three near-identical alternatives from its own category, and a word the learner has recalled
     * twenty times got the same. Difficulty now follows the word's own SM-2 state, so options
     * tighten as it becomes better known. See [DistractorSelector].
     */
    suspend fun getDistractorsForWord(targetWord: VocabularyEntity, count: Int = 3): List<VocabularyEntity> {
        val difficulty = DistractorSelector.difficultyFor(targetWord)
        // Over-fetch: the selector ranks candidates rather than taking whatever arrives first, and
        // a category can be smaller than one exercise needs.
        val pool = count * 4
        val sameCategory = vocabularyDao.getRandomWordsInCategory(targetWord.category, targetWord.id, pool)
        val otherCategory = vocabularyDao.getRandomWordsOutsideCategory(targetWord.category, targetWord.id, pool)

        return DistractorSelector.choose(
            target = targetWord,
            sameCategory = sameCategory,
            otherCategory = otherCategory,
            difficulty = difficulty,
            count = count
        )
    }

    /**
     * Words the learner keeps forgetting.
     *
     * These are the ones the review deck alone will not fix: five lapses means the word has been
     * known and lost five times, and asking it a sixth time in the same four-option shape is how a
     * deck grinds. They are re-taught inside lessons instead -- see
     * [com.kasiguru.domain.lesson.Interleaving].
     */
    suspend fun getLeechWords(limit: Int): List<VocabularyEntity> =
        vocabularyDao.getLeechWords(Sm2Algorithm.LEECH_LAPSES, limit)

    /**
     * Resolves the answer a learner actually chose back to the word it belongs to.
     *
     * Options are rendered as text, and a gloss button carries both languages ("daras · adze"), so
     * each part is tried in turn. Returns null when the text matches nothing — a typed answer that
     * is not a word at all is the normal case, and there is nothing to teach about it.
     */
    suspend fun findByWrittenForm(text: String): VocabularyEntity? {
        for (candidate in AnswerLabel.candidates(text)) {
            vocabularyDao.getVocabularyByAnyForm(candidate)?.let { return it }
        }
        return null
    }
}

/**
 * Highest share of a practice round that may be words already due for review.
 *
 * Not 1.0. A learner who has let a backlog build would otherwise spend every round re-treading
 * old words and never meet new ones, which stalls the dictionary and makes practice feel like
 * punishment for having missed a few days. Reserving roughly a third for new material keeps a
 * session moving forward even mid-backlog.
 */
private const val MAX_DUE_SHARE = 0.7

/**
 * Assembles one practice round from due and fresh candidates.
 *
 * Due words lead because they are the ones close to being forgotten — that is the whole point of
 * keeping a schedule. But the round degrades sensibly in both directions: with nothing due it is
 * all fresh material, and with no fresh material left it is all review. Callers over-fetch and let
 * this decide the mix.
 *
 * Pure so the mixing rules can be tested without a database, the same reason [mergeProgress] and
 * [com.kasiguru.data.remote.isDueForSync] live outside their classes.
 */
internal fun buildPracticeRound(
    due: List<VocabularyEntity>,
    fresh: List<VocabularyEntity>,
    count: Int
): List<VocabularyEntity> {
    if (count <= 0) return emptyList()

    val dueBudget = Math.ceil(count * MAX_DUE_SHARE).toInt()
    val chosen = LinkedHashMap<Int, VocabularyEntity>()

    due.take(dueBudget).forEach { chosen[it.id] = it }

    // Fresh fills the remainder. Deduped by id because getFreshWords orders by timesReviewed and
    // will happily return a word that is also due.
    for (word in fresh) {
        if (chosen.size >= count) break
        chosen.putIfAbsent(word.id, word)
    }

    // Still short — the corpus is smaller than the round, or everything left is already due.
    // Spend the remaining slots on review rather than returning a stunted round.
    for (word in due) {
        if (chosen.size >= count) break
        chosen.putIfAbsent(word.id, word)
    }

    // Shuffled so the due words are not always the first questions, which would let a learner
    // pattern-match "the early ones are the hard ones" instead of engaging with each prompt.
    return chosen.values.toList().shuffled()
}

/**
 * The same review-first mix as [buildPracticeRound], for games that must filter the corpus
 * themselves before choosing.
 *
 * Fill in the Blank and Aspect Builder cannot use a plain query — they need words that actually
 * carry aspect forms or an example sentence, so they pull the whole corpus and filter in memory.
 * They then sorted by `timesReviewed` alone, which reintroduced the novelty treadmill this change
 * removes everywhere else. Splitting their pool here keeps one definition of the mix.
 *
 * @param today ISO date; a word is due when it has a real review date that has arrived.
 */
internal fun buildPracticeRoundFromPool(
    pool: List<VocabularyEntity>,
    today: String,
    count: Int
): List<VocabularyEntity> {
    val (due, rest) = pool.partition { it.nextReviewDate.isNotBlank() && it.nextReviewDate <= today }
    return buildPracticeRound(
        due = due.sortedBy { it.nextReviewDate },
        fresh = rest.sortedBy { it.timesReviewed },
        count = count
    )
}
