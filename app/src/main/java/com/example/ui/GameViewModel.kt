package com.example.ui

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ads.AdMobManager
import com.example.data.GameRepository
import com.example.data.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class Match3Status {
    PLAYING,
    WON,
    LOST
}

class GameViewModel(application: Application) : AndroidViewModel(application) {

    val repository = GameRepository(application)
    val progress: StateFlow<PlayerProgress> = repository.progress

    val firestoreSync = com.example.data.firebase.FirestoreSyncRepository(application)
    val currentUser = MutableStateFlow<com.google.firebase.auth.FirebaseUser?>(com.google.firebase.auth.FirebaseAuth.getInstance().currentUser)

    init {
        com.google.firebase.auth.FirebaseAuth.getInstance().addAuthStateListener { auth ->
            currentUser.value = auth.currentUser
            if (auth.currentUser != null) {
                syncWithCloud()
            }
        }
    }

    fun syncWithCloud() {
        viewModelScope.launch {
            try {
                val remote = firestoreSync.fetchProgress()
                if (remote != null) {
                    val merged = repository.mergeWithRemote(remote)
                    firestoreSync.saveProgress(merged)
                } else {
                    firestoreSync.saveProgress(repository.progress.value)
                }
            } catch (_: Exception) {}
        }
    }

    val adMobManager = AdMobManager(application)
    val adSimulationState = adMobManager.simulationState

    val eliminatedRiddleOptions = MutableStateFlow<Set<Int>>(emptySet())

    // Navigation Stack
    private val _backStack = MutableStateFlow<List<Screen>>(listOf(Screen.Home))
    val currentScreen: StateFlow<Screen> = MutableStateFlow(Screen.Home)

    private val _currentScreenState = MutableStateFlow<Screen>(Screen.Home)
    val screen: StateFlow<Screen> = _currentScreenState.asStateFlow()

    fun navigateTo(destination: Screen) {
        val stack = _backStack.value.toMutableList()
        stack.add(destination)
        _backStack.value = stack
        _currentScreenState.value = destination
    }

    fun popBack(): Boolean {
        val stack = _backStack.value.toMutableList()
        if (stack.size > 1) {
            stack.removeAt(stack.size - 1)
            _backStack.value = stack
            _currentScreenState.value = stack.last()
            return true
        }
        return false
    }

    // Match-3 Game State
    val currentLevel = MutableStateFlow<GameLevel?>(null)
    val board = MutableStateFlow<List<List<GemType>>>(emptyList())
    val selectedTile = MutableStateFlow<Pair<Int, Int>?>(null)
    val clearingTiles = MutableStateFlow<Set<Pair<Int, Int>>>(emptySet())
    val shakingTiles = MutableStateFlow<Set<Pair<Int, Int>>>(emptySet())
    val hintTiles = MutableStateFlow<List<Pair<Int, Int>>?>(null)

    val movesLeft = MutableStateFlow(0)
    val currentScore = MutableStateFlow(0)
    val comboMultiplier = MutableStateFlow(1)
    val gameStatus = MutableStateFlow(Match3Status.PLAYING)
    val starsEarned = MutableStateFlow(0)
    val isResolving = MutableStateFlow(false)

    fun startLevel(levelId: String) {
        val level = repository.getLevel(levelId) ?: return
        currentLevel.value = level
        movesLeft.value = level.moves
        currentScore.value = 0
        comboMultiplier.value = 1
        gameStatus.value = Match3Status.PLAYING
        starsEarned.value = 0
        selectedTile.value = null
        clearingTiles.value = emptySet()
        shakingTiles.value = emptySet()
        hintTiles.value = null
        isResolving.value = false

        board.value = createInitialBoard(level.rows, level.cols, level.gemTypeCount)
        navigateTo(Screen.Play(levelId))
    }

    fun restartCurrentLevel() {
        val level = currentLevel.value ?: return
        startLevel(level.id)
    }

    fun addBonusMoves(bonus: Int = 5) {
        movesLeft.value += bonus
        if (gameStatus.value == Match3Status.LOST) {
            gameStatus.value = Match3Status.PLAYING
        }
    }

    private fun createInitialBoard(rows: Int, cols: Int, gemTypesCount: Int): List<List<GemType>> {
        var grid: List<List<GemType>>
        var attempts = 0
        do {
            val candidate = mutableListOf<MutableList<GemType>>()
            for (r in 0 until rows) {
                val row = mutableListOf<GemType>()
                for (c in 0 until cols) {
                    var gemIdx: Int
                    var loop = 0
                    do {
                        gemIdx = Random.nextInt(gemTypesCount)
                        loop++
                    } while (loop < 40 && (
                            (c >= 2 && row[c - 1] == GemType.fromIndex(gemIdx) && row[c - 2] == GemType.fromIndex(gemIdx)) ||
                            (r >= 2 && candidate[r - 1][c] == GemType.fromIndex(gemIdx) && candidate[r - 2][c] == GemType.fromIndex(gemIdx))
                    ))
                    row.add(GemType.fromIndex(gemIdx))
                }
                candidate.add(row)
            }
            grid = candidate
            attempts++
        } while (attempts < 50 && (findMatches(grid).isNotEmpty() || findPossibleSwap(grid) == null))

        return grid
    }

    fun onTileClicked(row: Int, col: Int) {
        if (isResolving.value || gameStatus.value != Match3Status.PLAYING) return
        val currentSelected = selectedTile.value

        if (currentSelected == null) {
            selectedTile.value = Pair(row, col)
            hintTiles.value = null
            return
        }

        if (currentSelected.first == row && currentSelected.second == col) {
            selectedTile.value = null
            return
        }

        // Check if adjacent
        val isAdjacent = (Math.abs(currentSelected.first - row) + Math.abs(currentSelected.second - col)) == 1
        if (!isAdjacent) {
            selectedTile.value = Pair(row, col)
            return
        }

        // Attempt swap
        val p1 = currentSelected
        val p2 = Pair(row, col)
        selectedTile.value = null

        viewModelScope.launch {
            isResolving.value = true
            val swapped = swapGems(board.value, p1, p2)
            val matches = findMatches(swapped)

            if (matches.isEmpty()) {
                // Invalid move - shake tiles
                shakingTiles.value = setOf(p1, p2)
                delay(300)
                shakingTiles.value = emptySet()
                isResolving.value = false
            } else {
                // Valid move
                board.value = swapped
                movesLeft.value = (movesLeft.value - 1).coerceAtLeast(0)
                resolveCascadeMatches()
            }
        }
    }

    private suspend fun resolveCascadeMatches() {
        var currentGrid = board.value
        val level = currentLevel.value ?: return
        var combo = 0
        var totalPointsEarned = 0

        while (true) {
            val matches = findMatches(currentGrid)
            if (matches.isEmpty()) break

            combo++
            comboMultiplier.value = combo
            clearingTiles.value = matches

            val pointsThisStep = matches.size * 10 * combo
            totalPointsEarned += pointsThisStep
            currentScore.value += pointsThisStep

            delay(240)

            // Drop tiles
            currentGrid = dropAndFillBoard(currentGrid, matches, level.gemTypeCount)
            board.value = currentGrid
            clearingTiles.value = emptySet()

            delay(160)
        }

        // Check if board has at least 1 move available, if not reshuffle
        if (findPossibleSwap(currentGrid) == null) {
            board.value = createInitialBoard(level.rows, level.cols, level.gemTypeCount)
        }

        isResolving.value = false

        // Check victory or defeat condition
        if (currentScore.value >= level.targetScore) {
            val stars = calculateStars(level, currentScore.value, movesLeft.value)
            starsEarned.value = stars
            gameStatus.value = Match3Status.WON
            repository.recordLevelCompletion(level.id, stars, currentScore.value)
        } else if (movesLeft.value <= 0) {
            gameStatus.value = Match3Status.LOST
        }
    }

    private fun calculateStars(level: GameLevel, score: Int, movesRemaining: Int): Int {
        var stars = 1
        if (score >= level.targetScore * 1.3) stars++
        if (movesRemaining >= Math.ceil(level.moves * 0.2).toInt()) stars++
        return stars.coerceIn(1, 3)
    }

    private fun swapGems(
        grid: List<List<GemType>>,
        p1: Pair<Int, Int>,
        p2: Pair<Int, Int>
    ): List<List<GemType>> {
        val result = grid.map { it.toMutableList() }
        val temp = result[p1.first][p1.second]
        result[p1.first][p1.second] = result[p2.first][p2.second]
        result[p2.first][p2.second] = temp
        return result
    }

    private fun findMatches(grid: List<List<GemType>>): Set<Pair<Int, Int>> {
        val matches = mutableSetOf<Pair<Int, Int>>()
        val rows = grid.size
        if (rows == 0) return matches
        val cols = grid[0].size

        // Horizontal matches
        for (r in 0 until rows) {
            var streak = 1
            for (c in 1..cols) {
                if (c < cols && grid[r][c] == grid[r][c - 1]) {
                    streak++
                } else {
                    if (streak >= 3) {
                        for (i in (c - streak) until c) {
                            matches.add(Pair(r, i))
                        }
                    }
                    streak = 1
                }
            }
        }

        // Vertical matches
        for (c in 0 until cols) {
            var streak = 1
            for (r in 1..rows) {
                if (r < rows && grid[r][c] == grid[r - 1][c]) {
                    streak++
                } else {
                    if (streak >= 3) {
                        for (i in (r - streak) until r) {
                            matches.add(Pair(i, c))
                        }
                    }
                    streak = 1
                }
            }
        }

        return matches
    }

    private fun dropAndFillBoard(
        grid: List<List<GemType>>,
        cleared: Set<Pair<Int, Int>>,
        gemTypeCount: Int
    ): List<List<GemType>> {
        val rows = grid.size
        val cols = grid[0].size
        val result = grid.map { it.toMutableList() }

        for (c in 0 until cols) {
            var writeRow = rows - 1
            for (r in rows - 1 downTo 0) {
                if (!cleared.contains(Pair(r, c))) {
                    result[writeRow][c] = grid[r][c]
                    writeRow--
                }
            }
            // Fill remaining top spaces with new gems
            while (writeRow >= 0) {
                result[writeRow][c] = GemType.fromIndex(Random.nextInt(gemTypeCount))
                writeRow--
            }
        }

        return result
    }

    private fun findPossibleSwap(grid: List<List<GemType>>): List<Pair<Int, Int>>? {
        val rows = grid.size
        val cols = grid[0].size
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (c + 1 < cols) {
                    val p1 = Pair(r, c)
                    val p2 = Pair(r, c + 1)
                    val swapped = swapGems(grid, p1, p2)
                    if (findMatches(swapped).isNotEmpty()) return listOf(p1, p2)
                }
                if (r + 1 < rows) {
                    val p1 = Pair(r, c)
                    val p2 = Pair(r + 1, c)
                    val swapped = swapGems(grid, p1, p2)
                    if (findMatches(swapped).isNotEmpty()) return listOf(p1, p2)
                }
            }
        }
        return null
    }

    fun showHint() {
        val hint = findPossibleSwap(board.value)
        hintTiles.value = hint
    }

    // Intelligence Puzzles State & Score Tracking
    val activePuzzle = MutableStateFlow<GamePuzzle?>(null)
    val puzzleWrongAttempts = MutableStateFlow<Set<Int>>(emptySet())
    val isPuzzleSolved = MutableStateFlow(false)
    val tileBoardState = MutableStateFlow<List<Int>>(emptyList())
    val tileMovesCount = MutableStateFlow(0)

    val puzzleTimerSeconds = MutableStateFlow(0L)
    val isPuzzleTimerRunning = MutableStateFlow(false)
    val puzzleMovesCount = MutableStateFlow(0)
    val latestPuzzleResult = MutableStateFlow<PuzzleScoreResult?>(null)
    val usedFiftyFiftyInCurrentPuzzle = MutableStateFlow(false)
    private var puzzleTimerJob: kotlinx.coroutines.Job? = null

    private fun startPuzzleTimer() {
        stopPuzzleTimer()
        isPuzzleTimerRunning.value = true
        puzzleTimerJob = viewModelScope.launch {
            while (isPuzzleTimerRunning.value) {
                delay(1000)
                if (isPuzzleTimerRunning.value) {
                    puzzleTimerSeconds.value += 1
                }
            }
        }
    }

    private fun stopPuzzleTimer() {
        isPuzzleTimerRunning.value = false
        puzzleTimerJob?.cancel()
        puzzleTimerJob = null
    }

    fun loadPuzzle(levelId: String, forceReplay: Boolean = false) {
        val level = repository.getLevel(levelId) ?: return
        val puzzle = repository.getPuzzleForLevel(level)
        activePuzzle.value = puzzle
        puzzleWrongAttempts.value = emptySet()
        eliminatedRiddleOptions.value = emptySet()
        usedFiftyFiftyInCurrentPuzzle.value = false

        val alreadySolved = progress.value.isPuzzleSolved(levelId)

        if (alreadySolved && !forceReplay) {
            isPuzzleSolved.value = true
            stopPuzzleTimer()
            val existingRecord = progress.value.getPuzzleRecord(levelId)
            if (existingRecord != null) {
                puzzleMovesCount.value = existingRecord.fewestMoves
                puzzleTimerSeconds.value = existingRecord.bestTimeSeconds
                latestPuzzleResult.value = PuzzleScoreResult(
                    score = existingRecord.highScore,
                    moves = existingRecord.fewestMoves,
                    timeSeconds = existingRecord.bestTimeSeconds,
                    stars = existingRecord.stars,
                    basePoints = existingRecord.highScore,
                    timeBonus = 0,
                    movesBonus = 0,
                    isNewHighScore = false,
                    previousHighScore = existingRecord.highScore,
                    rankTitle = when {
                        existingRecord.highScore >= 3300 -> "أسطوري • حكيم تحوت 👑"
                        existingRecord.highScore >= 2600 -> "ممتاز • عين حورس ⚡"
                        existingRecord.highScore >= 1900 -> "بارع • جعران السرعة 🏺"
                        else -> "مستكشف نيون 🛡️"
                    }
                )
            } else {
                latestPuzzleResult.value = null
            }
        } else {
            isPuzzleSolved.value = false
            latestPuzzleResult.value = null
            puzzleMovesCount.value = 0
            puzzleTimerSeconds.value = 0L

            if (puzzle is TilePuzzle) {
                tileBoardState.value = puzzle.initialBoard
                tileMovesCount.value = 0
            }

            startPuzzleTimer()
        }
    }

    fun replayCurrentPuzzle() {
        val puzzle = activePuzzle.value ?: return
        loadPuzzle(puzzle.id, forceReplay = true)
    }

    private fun onPuzzleCompleted(isSkipped: Boolean = false) {
        val puzzle = activePuzzle.value ?: return
        stopPuzzleTimer()
        isPuzzleSolved.value = true

        val moves = puzzleMovesCount.value
        val timeSeconds = puzzleTimerSeconds.value
        val previousRecord = progress.value.getPuzzleRecord(puzzle.id)
        val previousHighScore = previousRecord?.highScore ?: 0

        val result = com.example.data.PuzzleScoringEngine.calculateScore(
            kind = puzzle.kind,
            moves = moves,
            timeSeconds = timeSeconds,
            usedFiftyFifty = usedFiftyFiftyInCurrentPuzzle.value,
            isSkipped = isSkipped,
            previousHighScore = previousHighScore
        )

        repository.recordPuzzleScore(puzzle.id, result)
        latestPuzzleResult.value = result

        // Background cloud sync if signed in
        if (currentUser.value != null) {
            syncWithCloud()
        }
    }

    fun answerMultipleChoice(choiceIndex: Int) {
        val puzzle = activePuzzle.value ?: return
        if (isPuzzleSolved.value) return

        val expected = when (puzzle) {
            is RiddlePuzzle -> puzzle.answerIndex
            is SequencePuzzle -> puzzle.answerIndex
            else -> return
        }

        puzzleMovesCount.value += 1

        if (choiceIndex == expected) {
            onPuzzleCompleted(isSkipped = false)
        } else {
            puzzleWrongAttempts.value += choiceIndex
        }
    }

    fun onTilePuzzleMove(clickedIndex: Int) {
        if (isPuzzleSolved.value) return
        val currentBoard = tileBoardState.value
        val emptyIndex = currentBoard.indexOf(0)
        if (emptyIndex == -1) return

        val neighbors = repository.getTileNeighbors(clickedIndex)
        if (neighbors.contains(emptyIndex)) {
            val newBoard = currentBoard.toMutableList()
            newBoard[emptyIndex] = newBoard[clickedIndex]
            newBoard[clickedIndex] = 0
            tileBoardState.value = newBoard
            tileMovesCount.value += 1
            puzzleMovesCount.value += 1

            // Check if solved
            val target = listOf(1, 2, 3, 4, 5, 6, 7, 8, 0)
            if (newBoard == target) {
                onPuzzleCompleted(isSkipped = false)
            }
        }
    }

    fun skipTilePuzzle() {
        onPuzzleCompleted(isSkipped = true)
    }


    // AdMob rewarded and interstitial flows
    fun showRewardedForBonusMoves(activity: Activity) {
        adMobManager.showRewarded(activity, "+٥ حركات إضافية") {
            addBonusMoves(5)
        }
    }

    fun showRewardedForHint(activity: Activity) {
        adMobManager.showRewarded(activity, "كشف تلميح النمط") {
            showHint()
        }
    }

    fun showRewardedForTileSkip(activity: Activity) {
        adMobManager.showRewarded(activity, "تخطّي اللوح الحجري") {
            skipTilePuzzle()
        }
    }

    fun showRewardedForRiddleFiftyFifty(activity: Activity) {
        val puzzle = activePuzzle.value ?: return
        val correctIndex = when (puzzle) {
            is RiddlePuzzle -> puzzle.answerIndex
            is SequencePuzzle -> puzzle.answerIndex
            else -> return
        }
        val optionsCount = when (puzzle) {
            is RiddlePuzzle -> puzzle.options.size
            is SequencePuzzle -> puzzle.options.size
            else -> 3
        }

        adMobManager.showRewarded(activity, "إزالة خيارين خاطئين") {
            val wrongIndices = (0 until optionsCount).filter { it != correctIndex }
            eliminatedRiddleOptions.value = wrongIndices.take(2).toSet()
            usedFiftyFiftyInCurrentPuzzle.value = true
        }
    }

    fun showInterstitial(activity: Activity, onFinished: () -> Unit) {
        adMobManager.showInterstitial(activity, onFinished)
    }
}
