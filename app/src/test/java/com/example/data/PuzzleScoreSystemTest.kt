package com.example.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.PuzzleKind
import com.example.data.model.PuzzleScoreRecord
import com.example.data.model.PuzzleScoreResult
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PuzzleScoreSystemTest {

    private lateinit var context: Context
    private lateinit var repository: GameRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        repository = GameRepository(context)
        repository.resetProgress()
    }

    @Test
    fun testScoringEngine_TilePuzzle_HighPerformance() {
        // Fast solve with 14 moves in 15 seconds
        val result = PuzzleScoringEngine.calculateScore(
            kind = PuzzleKind.TILE,
            moves = 14,
            timeSeconds = 15,
            usedFiftyFifty = false,
            isSkipped = false,
            previousHighScore = 0
        )

        assertTrue("Score should be high for 14 moves and 15s", result.score >= 3000)
        assertEquals("Should earn 3 stars for top performance", 3, result.stars)
        assertTrue("Should be recognized as new high score", result.isNewHighScore)
        assertEquals(2000, result.basePoints)
        assertTrue(result.timeBonus > 500)
        assertTrue(result.movesBonus > 1000)
    }

    @Test
    fun testScoringEngine_RiddlePuzzle_FirstAttempt() {
        // 1 attempt in 5 seconds
        val result = PuzzleScoringEngine.calculateScore(
            kind = PuzzleKind.RIDDLE,
            moves = 1,
            timeSeconds = 5,
            usedFiftyFifty = false,
            isSkipped = false,
            previousHighScore = 0
        )

        assertEquals("Base points for riddle is 1500", 1500, result.basePoints)
        assertEquals("Perfect 1st attempt bonus is 1200", 1200, result.movesBonus)
        assertTrue("Total score should exceed 2700", result.score >= 2700)
        assertEquals(3, result.stars)
    }

    @Test
    fun testScoringEngine_Skip() {
        val result = PuzzleScoringEngine.calculateScore(
            kind = PuzzleKind.TILE,
            moves = 2,
            timeSeconds = 10,
            usedFiftyFifty = false,
            isSkipped = true,
            previousHighScore = 0
        )

        assertEquals(150, result.score)
        assertEquals(1, result.stars)
    }

    @Test
    fun testRepository_LocalHighScorePersistence() {
        val levelId = "1-6"
        val result1 = PuzzleScoreResult(
            score = 2500,
            moves = 18,
            timeSeconds = 30,
            stars = 2,
            basePoints = 2000,
            timeBonus = 300,
            movesBonus = 200,
            isNewHighScore = true,
            previousHighScore = 0,
            rankTitle = "ممتاز"
        )

        val record1 = repository.recordPuzzleScore(levelId, result1)
        assertEquals(2500, record1.highScore)
        assertEquals(18, record1.fewestMoves)
        assertEquals(30L, record1.bestTimeSeconds)
        assertEquals(2, record1.stars)

        // Verify loaded from repository
        val progress1 = repository.progress.value
        assertTrue("Puzzle should be marked as solved", progress1.isPuzzleSolved(levelId))
        assertEquals(2500, progress1.getPuzzleHighScore(levelId))
        assertEquals(2500, progress1.totalPuzzleScore)

        // Second attempt with BETTER score, time, and moves
        val result2 = PuzzleScoreResult(
            score = 3200,
            moves = 12,
            timeSeconds = 18,
            stars = 3,
            basePoints = 2000,
            timeBonus = 700,
            movesBonus = 500,
            isNewHighScore = true,
            previousHighScore = 2500,
            rankTitle = "أسطوري"
        )

        val record2 = repository.recordPuzzleScore(levelId, result2)
        assertEquals(3200, record2.highScore)
        assertEquals(12, record2.fewestMoves)
        assertEquals(18L, record2.bestTimeSeconds)
        assertEquals(3, record2.stars)

        // Verify persistent reload from new repo instance
        val freshRepo = GameRepository(context)
        val loaded = freshRepo.progress.value.getPuzzleRecord(levelId)
        assertNotNull(loaded)
        assertEquals(3200, loaded!!.highScore)
        assertEquals(12, loaded.fewestMoves)
        assertEquals(18L, loaded.bestTimeSeconds)
        assertEquals(3, loaded.stars)
    }

    @Test
    fun testFormatDurationArabic() {
        val arabicTime = PuzzleScoringEngine.formatDurationArabic(75) // 01:15
        assertEquals("٠١:١٥", arabicTime)
    }
}
