package com.example.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GameRepository
import com.example.data.model.PlayerProgress
import com.example.data.model.Screen
import com.example.ui.GameViewModel
import com.example.ui.Match3Status
import com.example.ui.components.AdMobBanner
import com.example.ui.components.GemIcon
import com.example.ui.theme.*

@Composable
fun PlayScreen(
    viewModel: GameViewModel,
    progress: PlayerProgress,
    onNavigate: (Screen) -> Unit
) {
    BackHandler {
        viewModel.popBack()
    }

    val context = LocalContext.current
    val activity = remember(context) { context as? Activity }

    val level by viewModel.currentLevel.collectAsState()
    val board by viewModel.board.collectAsState()
    val selectedTile by viewModel.selectedTile.collectAsState()
    val clearingTiles by viewModel.clearingTiles.collectAsState()
    val shakingTiles by viewModel.shakingTiles.collectAsState()
    val hintTiles by viewModel.hintTiles.collectAsState()

    val movesLeft by viewModel.movesLeft.collectAsState()
    val score by viewModel.currentScore.collectAsState()
    val combo by viewModel.comboMultiplier.collectAsState()
    val status by viewModel.gameStatus.collectAsState()
    val starsEarned by viewModel.starsEarned.collectAsState()
    val isResolving by viewModel.isResolving.collectAsState()

    if (level == null) {
        Box(modifier = Modifier.fillMaxSize().background(CyberVoid), contentAlignment = Alignment.Center) {
            Text("جارٍ تحميل المرحلة...", color = RaGold)
        }
        return
    }

    val currentLevel = level!!
    val isPuzzleSolved = remember(progress, currentLevel.id) { progress.isPuzzleSolved(currentLevel.id) }
    val progressPercent = (score.toFloat() / currentLevel.targetScore.toFloat()).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberVoid)
            .padding(horizontal = 14.dp, vertical = 10.dp),
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
                    .testTag("play_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "رجوع",
                    tint = RaGold
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "الفصل ${GameRepository.toArabicNumerals(currentLevel.chapterId)} • مرحلة ${currentLevel.chapterId}-${currentLevel.index}",
                    color = AnkhCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = currentLevel.name,
                    color = RaGold,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            // Moves Indicator
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (movesLeft <= 5) Color(0xFF451A1A) else CyberSurface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (movesLeft <= 5) Color(0xFFEF4444) else AnkhCyan.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HourglassBottom,
                        contentDescription = null,
                        tint = if (movesLeft <= 5) Color(0xFFF87171) else AnkhCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = GameRepository.toArabicNumerals(movesLeft),
                        color = if (movesLeft <= 5) Color(0xFFF87171) else AnkhCyan,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Score Progress Header Card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = CyberSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "النقاط: ",
                            color = SandMuted,
                            fontSize = 12.sp
                        )
                        Text(
                            text = GameRepository.toArabicNumerals(score),
                            color = RaGold,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (combo > 1) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = RaPink.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, RaPink)
                        ) {
                            Text(
                                text = "كومبو x$combo!",
                                color = RaPink,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "الهدف: ",
                            color = SandMuted,
                            fontSize = 12.sp
                        )
                        Text(
                            text = GameRepository.toArabicNumerals(currentLevel.targetScore),
                            color = AnkhCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = { progressPercent },
                    color = RaGold,
                    trackColor = CyberVoid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Match-3 Game Board
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val rows = currentLevel.rows
            val cols = currentLevel.cols

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface.copy(alpha = 0.95f)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, CyberBorder),
                modifier = Modifier
                    .aspectRatio(1f)
                    .fillMaxWidth()
                    .padding(4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (r in 0 until rows) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            for (c in 0 until cols) {
                                val gem = board.getOrNull(r)?.getOrNull(c)
                                val isSelected = selectedTile?.first == r && selectedTile?.second == c
                                val isClearing = clearingTiles.contains(Pair(r, c))
                                val isShaking = shakingTiles.contains(Pair(r, c))
                                val isHint = hintTiles?.contains(Pair(r, c)) == true

                                val borderModifier = when {
                                    isSelected -> Modifier.border(2.dp, RaGold, RoundedCornerShape(10.dp))
                                    isHint -> Modifier.border(2.dp, AnkhCyan, RoundedCornerShape(10.dp))
                                    else -> Modifier.border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                                }

                                val cellScale = if (isClearing) 0.3f else if (isSelected) 1.08f else 1f

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .padding(2.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) RaGold.copy(alpha = 0.2f)
                                            else if (isHint) AnkhCyan.copy(alpha = 0.15f)
                                            else Color.White.copy(alpha = 0.04f)
                                        )
                                        .then(borderModifier)
                                        .clickable(enabled = !isResolving && status == Match3Status.PLAYING) {
                                            viewModel.onTileClicked(r, c)
                                        }
                                        .scale(cellScale),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (gem != null) {
                                        GemIcon(
                                            gem = gem,
                                            size = 36.dp,
                                            isSelected = isSelected,
                                            isPulsing = isHint
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Power-ups and Hints toolbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilledTonalButton(
                onClick = {
                    if (activity != null) {
                        viewModel.showRewardedForHint(activity)
                    } else {
                        viewModel.showHint()
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = AnkhCyan.copy(alpha = 0.15f),
                    contentColor = AnkhCyan
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .testTag("hint_button")
            ) {
                Icon(imageVector = Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("إعلان: تلميح", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            FilledTonalButton(
                onClick = {
                    if (activity != null) {
                        viewModel.showRewardedForBonusMoves(activity)
                    } else {
                        viewModel.addBonusMoves(5)
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = RaGold.copy(alpha = 0.15f),
                    contentColor = RaGold
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .testTag("bonus_moves_button")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("إعلان: +٥ حركات", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        AdMobBanner()
    }

    // Victory Dialog
    if (status == Match3Status.WON) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = CyberSurface,
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "أحجار رع تشتعل!",
                        color = RaGold,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = currentLevel.name,
                        color = AnkhCyan,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 3 Stars Display
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        for (s in 1..3) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (s <= starsEarned) RaGold else Color.White.copy(alpha = 0.2f),
                                modifier = Modifier
                                    .size(36.dp)
                                    .padding(horizontal = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "حققت ${GameRepository.toArabicNumerals(score)} نقطة من أصل ${GameRepository.toArabicNumerals(currentLevel.targetScore)}",
                        color = SandText,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = if (isPuzzleSolved) "لغز الذكاء محلول مسبقاً!" else "لفتح المرحلة التالية، يجب فك شفرة لغز الذكاء الخاص بهذه المرحلة.",
                        color = SandMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!isPuzzleSolved) {
                        Button(
                            onClick = {
                                viewModel.loadPuzzle(currentLevel.id)
                                onNavigate(Screen.Puzzle(currentLevel.id))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RaGold, contentColor = Color(0xFF1E1300)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("solve_puzzle_button")
                        ) {
                            Icon(imageVector = Icons.Default.Psychology, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("حلّ لغز الذكاء", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        val nextLevelId = viewModel.repository.getNextLevelId(currentLevel.id)
                        if (nextLevelId != null) {
                            Button(
                                onClick = {
                                    if (activity != null) {
                                        viewModel.showInterstitial(activity) {
                                            viewModel.startLevel(nextLevelId)
                                        }
                                    } else {
                                        viewModel.startLevel(nextLevelId)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AnkhCyan, contentColor = Color(0xFF001F29)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("next_level_button")
                            ) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("المرحلة التالية", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { onNavigate(Screen.Map(currentLevel.chapterId)) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SandText),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("map_button")
                    ) {
                        Text("عودة إلى الخريطة")
                    }
                }
            }
        )
    }

    // Defeat Dialog
    if (status == Match3Status.LOST) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = CyberSurface,
            title = {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "انتهت الحركات",
                        color = Color(0xFFF87171),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = currentLevel.name,
                        color = SandMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "جمعت ${GameRepository.toArabicNumerals(score)} من أصل ${GameRepository.toArabicNumerals(currentLevel.targetScore)} نقطة مطلوبة.",
                        color = SandText,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "يمكنك مشاهدة إعلان والحصول على ٥ حركات إضافية لمواصلة المحاولة، أو إعادة المرحلة.",
                        color = SandMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (activity != null) {
                                viewModel.showRewardedForBonusMoves(activity)
                            } else {
                                viewModel.addBonusMoves(5)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AnkhCyan, contentColor = Color(0xFF001F29)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("continue_moves_button")
                    ) {
                        Icon(imageVector = Icons.Default.AddCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("شاهد إعلاناً وواصل (+٥ حركات)", fontWeight = FontWeight.Bold)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.restartCurrentLevel() },
                            colors = ButtonDefaults.buttonColors(containerColor = RaGold, contentColor = Color(0xFF1E1300)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("retry_level_button")
                        ) {
                            Text("إعادة")
                        }

                        OutlinedButton(
                            onClick = { onNavigate(Screen.Map(currentLevel.chapterId)) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SandText),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("map_from_lost_button")
                        ) {
                            Text("الخريطة")
                        }
                    }
                }
            }
        )
    }
}
