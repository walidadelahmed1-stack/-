package com.example.data

import com.example.data.model.PuzzleKind
import com.example.data.model.PuzzleScoreResult

object PuzzleScoringEngine {

    fun calculateScore(
        kind: PuzzleKind,
        moves: Int,
        timeSeconds: Long,
        usedFiftyFifty: Boolean = false,
        isSkipped: Boolean = false,
        previousHighScore: Int = 0
    ): PuzzleScoreResult {
        if (isSkipped) {
            val score = 150
            return PuzzleScoreResult(
                score = score,
                moves = moves.coerceAtLeast(1),
                timeSeconds = timeSeconds,
                stars = 1,
                basePoints = 150,
                timeBonus = 0,
                movesBonus = 0,
                isNewHighScore = score > previousHighScore,
                previousHighScore = previousHighScore,
                rankTitle = "تخطّي بمساعدة الآلهة ⏩"
            )
        }

        val basePoints = when (kind) {
            PuzzleKind.TILE -> 2000
            PuzzleKind.RIDDLE -> 1500
            PuzzleKind.SEQUENCE -> 1800
        }

        val timeBonus = when (kind) {
            PuzzleKind.TILE -> {
                // Decay after 35 seconds, floor at 100
                val decay = (timeSeconds * 12).toInt()
                maxOf(100, 1000 - decay)
            }
            PuzzleKind.RIDDLE, PuzzleKind.SEQUENCE -> {
                // Quick answer within 25 seconds gets highest bonus
                val decay = (timeSeconds * 18).toInt()
                maxOf(100, 850 - decay)
            }
        }

        val movesBonus = when (kind) {
            PuzzleKind.TILE -> {
                // 15-20 moves is optimal
                val extraMoves = (moves - 12).coerceAtLeast(0)
                maxOf(100, 1300 - extraMoves * 35)
            }
            PuzzleKind.RIDDLE, PuzzleKind.SEQUENCE -> {
                // 1 move = solved on first attempt without mistake
                when (moves) {
                    0, 1 -> 1200
                    2 -> 650
                    3 -> 350
                    else -> 100
                }
            }
        }

        val hintPenalty = if (usedFiftyFifty) 200 else 0
        val finalScore = maxOf(300, basePoints + timeBonus + movesBonus - hintPenalty)

        val stars = when (kind) {
            PuzzleKind.TILE -> when {
                finalScore >= 3100 -> 3
                finalScore >= 2200 -> 2
                else -> 1
            }
            PuzzleKind.RIDDLE -> when {
                finalScore >= 2500 -> 3
                finalScore >= 1800 -> 2
                else -> 1
            }
            PuzzleKind.SEQUENCE -> when {
                finalScore >= 2800 -> 3
                finalScore >= 2000 -> 2
                else -> 1
            }
        }

        val rankTitle = when {
            finalScore >= 3300 -> "أسطوري • حكيم تحوت 👑"
            finalScore >= 2600 -> "ممتاز • عين حورس ⚡"
            finalScore >= 1900 -> "بارع • جعران السرعة 🏺"
            else -> "مستكشف نيون 🛡️"
        }

        val isNewHighScore = finalScore > previousHighScore

        return PuzzleScoreResult(
            score = finalScore,
            moves = moves.coerceAtLeast(1),
            timeSeconds = timeSeconds.coerceAtLeast(1),
            stars = stars,
            basePoints = basePoints,
            timeBonus = timeBonus,
            movesBonus = movesBonus,
            isNewHighScore = isNewHighScore,
            previousHighScore = previousHighScore,
            rankTitle = rankTitle
        )
    }

    fun formatDuration(seconds: Long): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return "%02d:%02d".format(mins, secs)
    }

    fun formatDurationArabic(seconds: Long): String {
        val standard = formatDuration(seconds)
        return GameRepository.toArabicNumerals(standard)
    }
}
