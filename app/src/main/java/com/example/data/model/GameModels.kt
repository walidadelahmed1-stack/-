package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*

enum class GemType(val displayName: String, val color: Color, val shape: String) {
    SUN("قرص الشمس", RaGold, "sun"),
    ANKH("مفتاح الحياة", AnkhCyan, "ankh"),
    SCARAB("الجعران", ScarabEmerald, "scarab"),
    EYE("عين حورس", HorusPurple, "eye"),
    PYRAMID("الهرم", RaPink, "pyramid"),
    LOTUS("زهرة اللوتس", LotusBlue, "lotus");

    companion object {
        fun fromIndex(index: Int): GemType {
            val values = entries
            return values[index % values.size]
        }
    }
}

enum class PuzzleKind(val label: String) {
    RIDDLE("أحجية"),
    SEQUENCE("نمط رقمي"),
    TILE("لوح حجري منزلق")
}

sealed interface GamePuzzle {
    val id: String
    val kind: PuzzleKind
}

data class RiddlePuzzle(
    override val id: String,
    val prompt: String,
    val options: List<String>,
    val answerIndex: Int,
    val explanation: String
) : GamePuzzle {
    override val kind: PuzzleKind = PuzzleKind.RIDDLE
}

data class SequencePuzzle(
    override val id: String,
    val terms: List<String>,
    val options: List<String>,
    val answerIndex: Int,
    val explanation: String
) : GamePuzzle {
    override val kind: PuzzleKind = PuzzleKind.SEQUENCE
}

data class TilePuzzle(
    override val id: String,
    val name: String,
    val labels: List<String>,
    val shuffles: Int,
    val initialBoard: List<Int>
) : GamePuzzle {
    override val kind: PuzzleKind = PuzzleKind.TILE
}

data class GameLevel(
    val id: String, // e.g. "1-1"
    val chapterId: Int,
    val index: Int, // 1 to 6
    val name: String,
    val rows: Int,
    val cols: Int,
    val gemTypeCount: Int,
    val moves: Int,
    val targetScore: Int,
    val isBoss: Boolean,
    val puzzleKind: PuzzleKind
)

data class Chapter(
    val id: Int,
    val tag: String,
    val title: String,
    val hook: String,
    val lines: List<String>,
    val levels: List<GameLevel>
)

data class PuzzleScoreRecord(
    val levelId: String,
    val highScore: Int,
    val bestTimeSeconds: Long,
    val fewestMoves: Int,
    val stars: Int = 3,
    val timestamp: Long = System.currentTimeMillis()
)

data class PuzzleScoreResult(
    val score: Int,
    val moves: Int,
    val timeSeconds: Long,
    val stars: Int,
    val basePoints: Int,
    val timeBonus: Int,
    val movesBonus: Int,
    val isNewHighScore: Boolean,
    val previousHighScore: Int,
    val rankTitle: String
)

data class PlayerProgress(
    val stars: Map<String, Int> = emptyMap(),
    val scores: Map<String, Int> = emptyMap(),
    val solvedPuzzles: Set<String> = emptySet(),
    val storiesSeen: Set<Int> = emptySet(),
    val puzzleScores: Map<String, PuzzleScoreRecord> = emptyMap()
) {
    val totalStars: Int get() = stars.values.sum()
    val completedLevelsCount: Int get() = stars.count { it.value > 0 }
    val totalPuzzleScore: Int get() = puzzleScores.values.sumOf { it.highScore }
    val threeStarPuzzlesCount: Int get() = puzzleScores.count { it.value.stars >= 3 }

    fun getStarsFor(levelId: String): Int = stars[levelId] ?: 0
    fun getScoreFor(levelId: String): Int = scores[levelId] ?: 0
    fun isPuzzleSolved(levelId: String): Boolean = solvedPuzzles.contains(levelId)
    fun isStorySeen(chapterId: Int): Boolean = storiesSeen.contains(chapterId)
    fun getPuzzleRecord(levelId: String): PuzzleScoreRecord? = puzzleScores[levelId]
    fun getPuzzleHighScore(levelId: String): Int = puzzleScores[levelId]?.highScore ?: 0
}

sealed interface Screen {
    data object Home : Screen
    data class Map(val initialChapterId: Int = 1) : Screen
    data class Play(val levelId: String) : Screen
    data class Puzzle(val levelId: String) : Screen
    data class Story(val chapterId: Int) : Screen
    data object Codex : Screen
    data object Leaderboard : Screen
}

