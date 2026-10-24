package com.example.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.GameViewModel
import com.example.ui.components.AdMobBanner
import com.example.ui.theme.*

@Composable
fun PuzzleScreen(
    viewModel: GameViewModel,
    levelId: String,
    onNavigate: (Screen) -> Unit
) {
    BackHandler {
        viewModel.popBack()
    }

    val context = LocalContext.current
    val activity = remember(context) { context as? Activity }

    LaunchedEffect(levelId) {
        viewModel.loadPuzzle(levelId)
    }

    val activePuzzle by viewModel.activePuzzle.collectAsState()
    val isSolved by viewModel.isPuzzleSolved.collectAsState()
    val wrongAttempts by viewModel.puzzleWrongAttempts.collectAsState()
    val eliminatedOptions by viewModel.eliminatedRiddleOptions.collectAsState()
    val tileBoard by viewModel.tileBoardState.collectAsState()
    val tileMoves by viewModel.tileMovesCount.collectAsState()

    val timerSeconds by viewModel.puzzleTimerSeconds.collectAsState()
    val puzzleMoves by viewModel.puzzleMovesCount.collectAsState()
    val puzzleResult by viewModel.latestPuzzleResult.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val currentRecord = remember(progress, levelId) { progress.getPuzzleRecord(levelId) }

    val level = remember(levelId) { viewModel.repository.getLevel(levelId) }
    val nextLevelId = remember(levelId) { viewModel.repository.getNextLevelId(levelId) }
    val nextLevel = remember(nextLevelId) { nextLevelId?.let { viewModel.repository.getLevel(it) } }

    if (activePuzzle == null || level == null) {
        Box(modifier = Modifier.fillMaxSize().background(CyberVoid), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = RaGold)
        }
        return
    }

    val puzzle = activePuzzle!!

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberVoid)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top App Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { viewModel.popBack() },
                modifier = Modifier
                    .size(46.dp)
                    .background(CyberSurface, CircleShape)
                    .border(1.dp, CyberBorder, CircleShape)
                    .testTag("puzzle_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "رجوع",
                    tint = RaGold
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "لغز الذكاء • ${puzzle.kind.label}",
                    color = AnkhCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = level.name,
                    color = RaGold,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Leaderboard Shortcut
            IconButton(
                onClick = { onNavigate(Screen.Leaderboard) },
                modifier = Modifier
                    .size(46.dp)
                    .background(CyberSurface, CircleShape)
                    .border(1.dp, CyberBorder, CircleShape)
                    .testTag("puzzle_leaderboard_button")
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "سجل الأرقام القياسية",
                    tint = RaGold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Live Real-Time Score & Moves HUD
        PuzzleScoreHUD(
            timerSeconds = timerSeconds,
            moves = puzzleMoves,
            highScore = currentRecord?.highScore ?: 0,
            onLeaderboardClick = { onNavigate(Screen.Leaderboard) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // Success Card with Score Breakdown if solved
            if (isSolved) {
                PuzzleVictoryCard(
                    result = puzzleResult,
                    record = currentRecord,
                    nextLevelName = nextLevel?.name,
                    onNextLevel = if (nextLevelId != null) {
                        {
                            if (activity != null) {
                                viewModel.showInterstitial(activity) {
                                    viewModel.startLevel(nextLevelId)
                                }
                            } else {
                                viewModel.startLevel(nextLevelId)
                            }
                        }
                    } else null,
                    onReplay = {
                        viewModel.replayCurrentPuzzle()
                    },
                    onLeaderboard = {
                        onNavigate(Screen.Leaderboard)
                    },
                    onBackToMap = {
                        onNavigate(Screen.Map(level.chapterId))
                    }
                )
            } else {
                // Interactive Puzzle View based on kind
                when (puzzle) {
                    is RiddlePuzzle -> {
                        RiddleView(
                            puzzle = puzzle,
                            wrongAttempts = wrongAttempts,
                            eliminatedOptions = eliminatedOptions,
                            onSelectOption = { viewModel.answerMultipleChoice(it) },
                            onHelpFiftyFifty = {
                                activity?.let { viewModel.showRewardedForRiddleFiftyFifty(it) }
                            }
                        )
                    }
                    is SequencePuzzle -> {
                        SequenceView(
                            puzzle = puzzle,
                            wrongAttempts = wrongAttempts,
                            eliminatedOptions = eliminatedOptions,
                            onSelectOption = { viewModel.answerMultipleChoice(it) },
                            onHelpFiftyFifty = {
                                activity?.let { viewModel.showRewardedForRiddleFiftyFifty(it) }
                            }
                        )
                    }
                    is TilePuzzle -> {
                        TileSlidingView(
                            puzzle = puzzle,
                            board = tileBoard,
                            moves = tileMoves,
                            onTileClick = { viewModel.onTilePuzzleMove(it) },
                            onSkip = {
                                if (activity != null) {
                                    viewModel.showRewardedForTileSkip(activity)
                                } else {
                                    viewModel.skipTilePuzzle()
                                }
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // AdMob Banner
        AdMobBanner()
    }
}

@Composable
private fun RiddleView(
    puzzle: RiddlePuzzle,
    wrongAttempts: Set<Int>,
    eliminatedOptions: Set<Int>,
    onSelectOption: (Int) -> Unit,
    onHelpFiftyFifty: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, AnkhCyan.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AnkhCyan.copy(alpha = 0.1f),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, AnkhCyan.copy(alpha = 0.5f))
            ) {
                Text(
                    text = "أحجية فرعونية قديمة",
                    color = AnkhCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = puzzle.prompt,
                color = SandText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 25.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            puzzle.options.forEachIndexed { index, option ->
                val isWrong = wrongAttempts.contains(index)
                val isEliminated = eliminatedOptions.contains(index)

                if (!isEliminated) {
                    OutlinedButton(
                        onClick = { onSelectOption(index) },
                        enabled = !isWrong,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isWrong) Color(0xFF3B1515) else CyberSurfaceVariant.copy(alpha = 0.6f),
                            contentColor = if (isWrong) Color(0xFFFCA5A5) else RaGold
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.2.dp,
                            if (isWrong) Color(0xFFEF4444) else CyberBorder
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .height(50.dp)
                            .testTag("riddle_option_$index")
                    ) {
                        Text(
                            text = option,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (eliminatedOptions.isEmpty()) {
                FilledTonalButton(
                    onClick = onHelpFiftyFifty,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = HorusPurple.copy(alpha = 0.15f),
                        contentColor = HorusPurple
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.FilterAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("شاهد إعلاناً لإزالة خيارين (٥٠:٥٠)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SequenceView(
    puzzle: SequencePuzzle,
    wrongAttempts: Set<Int>,
    eliminatedOptions: Set<Int>,
    onSelectOption: (Int) -> Unit,
    onHelpFiftyFifty: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, HorusPurple.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = HorusPurple.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, HorusPurple)
            ) {
                Text(
                    text = "نمط التردد الرقمي",
                    color = HorusPurple,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "اكتشف الرقم التالي لإكمال الشفرة:",
                color = SandMuted,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Sequence Cards Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                puzzle.terms.forEach { term ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CyberSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AnkhCyan.copy(alpha = 0.4f)),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = term,
                                color = AnkhCyan,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Mystery slot
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = RaGold.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, RaGold),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "؟",
                            color = RaGold,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            puzzle.options.forEachIndexed { index, option ->
                val isWrong = wrongAttempts.contains(index)
                val isEliminated = eliminatedOptions.contains(index)

                if (!isEliminated) {
                    OutlinedButton(
                        onClick = { onSelectOption(index) },
                        enabled = !isWrong,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isWrong) Color(0xFF3B1515) else CyberSurfaceVariant.copy(alpha = 0.6f),
                            contentColor = if (isWrong) Color(0xFFFCA5A5) else RaGold
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.2.dp,
                            if (isWrong) Color(0xFFEF4444) else CyberBorder
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .height(48.dp)
                            .testTag("seq_option_$index")
                    ) {
                        Text(
                            text = option,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (eliminatedOptions.isEmpty()) {
                FilledTonalButton(
                    onClick = onHelpFiftyFifty,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = HorusPurple.copy(alpha = 0.15f),
                        contentColor = HorusPurple
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.FilterAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("شاهد إعلاناً لإزالة خيارين (٥٠:٥٠)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TileSlidingView(
    puzzle: TilePuzzle,
    board: List<Int>,
    moves: Int,
    onTileClick: (Int) -> Unit,
    onSkip: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, RaPink.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = RaPink.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, RaPink)
            ) {
                Text(
                    text = puzzle.name,
                    color = RaPink,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "رتّب الأحجار — اضغط الحجر المجاور للفراغ لنقله",
                color = SandMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3x3 Tile Grid
            Box(
                modifier = Modifier
                    .size(230.dp)
                    .background(CyberVoid, RoundedCornerShape(16.dp))
                    .border(1.5.dp, CyberBorder, RoundedCornerShape(16.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (row in 0..2) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            for (col in 0..2) {
                                val index = row * 3 + col
                                val tileValue = board.getOrNull(index) ?: 0

                                if (tileValue == 0) {
                                    // Empty Slot
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .padding(3.dp)
                                            .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(10.dp))
                                            .border(1.dp, CyberBorder.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                    )
                                } else {
                                    val label = puzzle.labels.getOrNull(tileValue - 1) ?: tileValue.toString()
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .padding(3.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(CyberSurfaceVariant)
                                            .border(1.dp, RaGold.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                            .clickable { onTileClick(index) }
                                            .testTag("tile_item_$index"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            color = SandText,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "الحركات: ${moves}",
                color = SandMuted,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            FilledTonalButton(
                onClick = onSkip,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = CyberSurfaceVariant,
                    contentColor = AnkhCyan
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.SmartDisplay, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("شاهد إعلاناً لتخطّي اللوح", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PuzzleScoreHUD(
    timerSeconds: Long,
    moves: Int,
    highScore: Int,
    onLeaderboardClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Live Timer
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = "الوقت",
                    tint = ScarabEmerald,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "الوقت",
                        color = SandMuted,
                        fontSize = 10.sp
                    )
                    Text(
                        text = com.example.data.PuzzleScoringEngine.formatDurationArabic(timerSeconds),
                        color = ScarabEmerald,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Live Moves
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = "الحركات",
                    tint = AnkhCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "الحركات",
                        color = SandMuted,
                        fontSize = 10.sp
                    )
                    Text(
                        text = com.example.data.GameRepository.toArabicNumerals(moves),
                        color = AnkhCyan,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Local High Score
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onLeaderboardClick() }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "أعلى رقم",
                    tint = RaGold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "أعلى رقم",
                        color = SandMuted,
                        fontSize = 10.sp
                    )
                    Text(
                        text = if (highScore > 0) com.example.data.GameRepository.toArabicNumerals(highScore) else "جديد",
                        color = RaGold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun PuzzleVictoryCard(
    result: com.example.data.model.PuzzleScoreResult?,
    record: com.example.data.model.PuzzleScoreRecord?,
    nextLevelName: String?,
    onNextLevel: (() -> Unit)?,
    onReplay: () -> Unit,
    onLeaderboard: () -> Unit,
    onBackToMap: () -> Unit
) {
    val score = result?.score ?: record?.highScore ?: 0
    val stars = result?.stars ?: record?.stars ?: 3
    val isNewHigh = result?.isNewHighScore == true
    val rankTitle = result?.rankTitle ?: when {
        score >= 3300 -> "أسطوري • حكيم تحوت 👑"
        score >= 2600 -> "ممتاز • عين حورس ⚡"
        score >= 1900 -> "بارع • جعران السرعة 🏺"
        else -> "مستكشف نيون 🛡️"
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isNewHigh) RaGold else ScarabEmerald),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // New High Score Ribbon
            if (isNewHigh) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = RaGold.copy(alpha = 0.18f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RaGold),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Whatshot,
                            contentDescription = null,
                            tint = RaGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "رقم قياسي جديد على جهازك! 🎉",
                            color = RaGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Star Rating
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                repeat(3) { index ->
                    val filled = index < stars
                    Icon(
                        imageVector = if (filled) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = null,
                        tint = if (filled) RaGold else CyberBorder,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Text(
                text = "تم حلّ اللغز بنجاح!",
                color = RaGold,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(top = 4.dp)
            )

            Text(
                text = rankTitle,
                color = AnkhCyan,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 2.dp)
            )

            // Total Score
            Text(
                text = "+${com.example.data.GameRepository.toArabicNumerals(score)} نقطة",
                color = RaGold,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Performance Breakdown Box
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = CyberSurfaceVariant.copy(alpha = 0.6f),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val timeSecs = result?.timeSeconds ?: record?.bestTimeSeconds ?: 0
                    val movesMade = result?.moves ?: record?.fewestMoves ?: 0

                    ScoreBreakdownRow(
                        title = "الوقت المستغرق",
                        detail = com.example.data.PuzzleScoringEngine.formatDurationArabic(timeSecs),
                        points = if (result != null) "+${com.example.data.GameRepository.toArabicNumerals(result.timeBonus)}" else null,
                        accentColor = ScarabEmerald
                    )

                    ScoreBreakdownRow(
                        title = "عدد الحركات",
                        detail = "${com.example.data.GameRepository.toArabicNumerals(movesMade)} حركة",
                        points = if (result != null) "+${com.example.data.GameRepository.toArabicNumerals(result.movesBonus)}" else null,
                        accentColor = AnkhCyan
                    )

                    ScoreBreakdownRow(
                        title = "نقاط حل اللغز",
                        detail = "أساس التحدي",
                        points = if (result != null) "+${com.example.data.GameRepository.toArabicNumerals(result.basePoints)}" else null,
                        accentColor = RaGold
                    )

                    if (record != null) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 2.dp),
                            color = CyberBorder.copy(alpha = 0.5f),
                            thickness = 0.5.dp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "أفضل نتيجة مسجلة محلياً",
                                color = SandMuted,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "${com.example.data.GameRepository.toArabicNumerals(record.highScore)} نقطة",
                                color = RaGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            if (onNextLevel != null) {
                Button(
                    onClick = onNextLevel,
                    colors = ButtonDefaults.buttonColors(containerColor = RaGold, contentColor = Color(0xFF1E1300)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("puzzle_next_level_button")
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (nextLevelName != null) "المرحلة التالية ($nextLevelName)" else "المرحلة التالية",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onReplay,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RaGold),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, RaGold),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).testTag("puzzle_replay_button")
                ) {
                    Icon(imageVector = Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تحطيم الرقم 🔄", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                FilledTonalButton(
                    onClick = onLeaderboard,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = CyberSurfaceVariant,
                        contentColor = AnkhCyan
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).testTag("puzzle_view_leaderboard_button")
                ) {
                    Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("الأرقام القياسية 🏆", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            TextButton(
                onClick = onBackToMap,
                colors = ButtonDefaults.textButtonColors(contentColor = SandMuted),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("عودة إلى الخريطة")
            }
        }
    }
}

@Composable
private fun ScoreBreakdownRow(
    title: String,
    detail: String,
    points: String?,
    accentColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(accentColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                color = SandText,
                fontSize = 12.sp
            )
            Text(
                text = " ($detail)",
                color = SandMuted,
                fontSize = 11.sp
            )
        }

        if (points != null) {
            Text(
                text = points,
                color = accentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

