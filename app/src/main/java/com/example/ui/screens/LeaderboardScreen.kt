package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GameRepository
import com.example.data.PuzzleScoringEngine
import com.example.data.model.PuzzleScoreRecord
import com.example.data.model.Screen
import com.example.ui.GameViewModel
import com.example.ui.components.AdMobBanner
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LeaderboardScreen(
    viewModel: GameViewModel,
    onNavigate: (Screen) -> Unit
) {
    BackHandler {
        viewModel.popBack()
    }

    val progress by viewModel.progress.collectAsState()
    val puzzleRecords = remember(progress) {
        progress.puzzleScores.values.sortedByDescending { it.highScore }
    }

    val totalScore = remember(puzzleRecords) { puzzleRecords.sumOf { it.highScore } }
    val threeStarsCount = remember(puzzleRecords) { puzzleRecords.count { it.stars >= 3 } }
    val bestTime = remember(puzzleRecords) {
        puzzleRecords.filter { it.bestTimeSeconds > 0 }.minOfOrNull { it.bestTimeSeconds }
    }
    val fewestMoves = remember(puzzleRecords) {
        puzzleRecords.filter { it.fewestMoves > 0 }.minOfOrNull { it.fewestMoves }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberVoid)
            .padding(16.dp)
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
                    .testTag("leaderboard_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "رجوع",
                    tint = RaGold
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "سجل الأرقام القياسية",
                    color = RaGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "أعلى النتائج المخزنة محلياً للألغاز",
                    color = SandMuted,
                    fontSize = 11.sp
                )
            }

            IconButton(
                onClick = { onNavigate(Screen.Home) },
                modifier = Modifier
                    .size(46.dp)
                    .background(CyberSurface, CircleShape)
                    .border(1.dp, CyberBorder, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "الرئيسية",
                    tint = AnkhCyan
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Hero Stats Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CyberSurface),
            border = androidx.compose.foundation.BorderStroke(1.2.dp, RaGold.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = RaGold,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "مجموع نقاط ألغاز الذكاء",
                        color = SandText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = "${GameRepository.toArabicNumerals(totalScore)} نقطة",
                    color = RaGold,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MiniStatItem(
                        icon = Icons.Default.Lightbulb,
                        title = "ألغاز محلولة",
                        value = GameRepository.toArabicNumerals(puzzleRecords.size),
                        accentColor = AnkhCyan
                    )

                    MiniStatItem(
                        icon = Icons.Default.Star,
                        title = "إتقان ٣ نجوم",
                        value = GameRepository.toArabicNumerals(threeStarsCount),
                        accentColor = RaGold
                    )

                    MiniStatItem(
                        icon = Icons.Default.Timer,
                        title = "أسرع حل",
                        value = bestTime?.let { PuzzleScoringEngine.formatDurationArabic(it) } ?: "—",
                        accentColor = ScarabEmerald
                    )

                    MiniStatItem(
                        icon = Icons.Default.TouchApp,
                        title = "أقل حركات",
                        value = fewestMoves?.let { GameRepository.toArabicNumerals(it) } ?: "—",
                        accentColor = HorusPurple
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Records List Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "قائمة الألغاز المحلولة (${GameRepository.toArabicNumerals(puzzleRecords.size)})",
                color = SandText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "مرتبة حسب أعلى نتيجة",
                color = SandMuted,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (puzzleRecords.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberSurfaceVariant.copy(alpha = 0.5f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Extension,
                            contentDescription = null,
                            tint = SandMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "لم تسجل أي نتيجة بعد!",
                            color = RaGold,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "تحدّ ألغاز الذكاء في نهاية كل فصل، حلها بأسرع وقت وأقل حركات لتسجيل رقمك القياسي هنا محلياً.",
                            color = SandMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                viewModel.loadPuzzle("1-6")
                                onNavigate(Screen.Puzzle("1-6"))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RaGold, contentColor = Color(0xFF1E1300)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("العب أول لغز (١-٦)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(puzzleRecords, key = { it.levelId }) { record ->
                    PuzzleScoreItemCard(
                        record = record,
                        viewModel = viewModel,
                        onReplay = {
                            viewModel.loadPuzzle(record.levelId, forceReplay = true)
                            onNavigate(Screen.Puzzle(record.levelId))
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        AdMobBanner()
    }
}

@Composable
private fun MiniStatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    accentColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
        Text(text = value, color = SandText, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
        Text(text = title, color = SandMuted, fontSize = 9.sp)
    }
}

@Composable
private fun PuzzleScoreItemCard(
    record: PuzzleScoreRecord,
    viewModel: GameViewModel,
    onReplay: () -> Unit
) {
    val level = remember(record.levelId) { viewModel.repository.getLevel(record.levelId) }
    val levelName = level?.name ?: "المرحلة ${record.levelId}"
    val kindLabel = level?.puzzleKind?.label ?: "لغز"

    val dateFormatted = remember(record.timestamp) {
        try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            sdf.format(Date(record.timestamp))
        } catch (_: Exception) {
            ""
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = AnkhCyan.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(0.6.dp, AnkhCyan.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = kindLabel,
                            color = AnkhCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "المرحلة ${record.levelId}: $levelName",
                        color = SandText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Timer, contentDescription = null, tint = ScarabEmerald, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = PuzzleScoringEngine.formatDurationArabic(record.bestTimeSeconds),
                            color = SandText,
                            fontSize = 12.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.TouchApp, contentDescription = null, tint = HorusPurple, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${GameRepository.toArabicNumerals(record.fewestMoves)} حركة",
                            color = SandText,
                            fontSize = 12.sp
                        )
                    }

                    // Stars
                    Row {
                        repeat(3) { starIdx ->
                            val filled = starIdx < record.stars
                            Icon(
                                imageVector = if (filled) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = if (filled) RaGold else CyberBorder,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${GameRepository.toArabicNumerals(record.highScore)}",
                    color = RaGold,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "نقطة",
                    color = SandMuted,
                    fontSize = 10.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedButton(
                    onClick = onReplay,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RaGold),
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, RaGold),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(imageVector = Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إعادة التحدي", fontSize = 10.sp)
                }
            }
        }
    }
}
