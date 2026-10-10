package com.kasiguru.data.repository

import com.kasiguru.domain.games.SavedAnswer
import com.kasiguru.domain.games.SavedGame
import com.kasiguru.domain.games.SavedRound
import com.kasiguru.domain.games.SavedWordSearch
import com.kasiguru.domain.games.SavedWordWheel
import org.json.JSONArray
import org.json.JSONObject

/**
 * [SavedGame] to and from the JSON text [SavedGameRepository] stores.
 *
 * [decode] returns null for anything it cannot read in full, including a save that contradicts
 * itself, and the game then deals a fresh round: losing a round in progress is better than resuming
 * one that is wrong.
 */
internal object SavedGameCodec {

    private const val FORMAT = 1

    fun encode(game: SavedGame): String {
        val json = JSONObject().put("format", FORMAT)
        when (game) {
            is SavedRound -> json
                .put("kind", "round")
                .put("questions", JSONArray(game.questionKeys))
                .put("answers", JSONArray(game.answers.map { answerJson(it) }))
                .put("score", game.score)
                .put("total", game.totalQuestions)
                .put("usedHint", game.usedHint)
                .put("hintRevealed", game.hintRevealed)
            is SavedWordSearch -> json
                .put("kind", "word_search")
                .put("words", JSONArray(game.puzzleWordIds))
                .put("found", JSONArray(game.foundIds))
                .put("misses", game.misses)
                .put("hintRevealed", game.hintRevealed)
            is SavedWordWheel -> json
                .put("kind", "word_wheel")
                .put("board", JSONArray(game.boardWords))
                .put("found", JSONArray(game.foundSlots))
                .put("bonus", JSONArray(game.bonusWords))
                .put("hintsUsed", game.hintsUsed)
                .apply { game.cluedSlot?.let { put("clued", it) } }
        }
        return json.toString()
    }

    fun decode(text: String): SavedGame? = runCatching {
        val json = JSONObject(text)
        if (json.getInt("format") != FORMAT) return null
        when (json.getString("kind")) {
            "round" -> SavedRound(
                questionKeys = json.getJSONArray("questions").strings(),
                answers = json.getJSONArray("answers").objects().map { answerOf(it) },
                score = json.getInt("score"),
                totalQuestions = json.getInt("total"),
                usedHint = json.getBoolean("usedHint"),
                hintRevealed = json.getBoolean("hintRevealed")
            ).takeIf {
                it.answers.size <= it.questionKeys.size &&
                    it.score in 0..it.answers.size &&
                    it.totalQuestions >= it.questionKeys.size
            }
            "word_search" -> SavedWordSearch(
                puzzleWordIds = json.getJSONArray("words").ints(),
                foundIds = json.getJSONArray("found").ints(),
                misses = json.getInt("misses"),
                hintRevealed = json.getBoolean("hintRevealed")
            ).takeIf { it.misses >= 0 }
            "word_wheel" -> SavedWordWheel(
                boardWords = json.getJSONArray("board").strings(),
                foundSlots = json.getJSONArray("found").ints(),
                bonusWords = json.getJSONArray("bonus").strings(),
                hintsUsed = json.getInt("hintsUsed"),
                cluedSlot = if (json.has("clued")) json.getInt("clued") else null
            ).takeIf { it.hintsUsed >= 0 }
            else -> null
        }
    }.getOrNull()

    private fun answerJson(answer: SavedAnswer): JSONObject = JSONObject()
        .put("prompt", answer.prompt)
        .put("answer", answer.userAnswer)
        .put("correct", answer.correctAnswer)
        .put("isCorrect", answer.isCorrect)
        .apply { answer.subPrompt?.let { put("subPrompt", it) } }

    private fun answerOf(json: JSONObject) = SavedAnswer(
        prompt = json.getString("prompt"),
        userAnswer = json.getString("answer"),
        correctAnswer = json.getString("correct"),
        isCorrect = json.getBoolean("isCorrect"),
        subPrompt = if (json.has("subPrompt")) json.getString("subPrompt") else null
    )

    private fun JSONArray.strings(): List<String> = (0 until length()).map { getString(it) }

    private fun JSONArray.ints(): List<Int> = (0 until length()).map { getInt(it) }

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
}
