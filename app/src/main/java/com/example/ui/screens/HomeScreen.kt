package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.GameRepository
import com.example.data.model.PlayerProgress
import com.example.data.model.Screen
import com.example.ui.GameViewModel
import com.example.ui.components.GemIcon
import com.example.data.model.GemType
import com.example.ui.theme.*

@Composable
fun HomeScreen(
    viewModel: GameViewModel,
    progress: PlayerProgress,
    onNavigate: (Screen) -> Unit
) {
    val scrollState = rememberScrollState()
    val frontierLevelId = remember(progress) { viewModel.repository.getFrontierLevelId(progress) }
    val isFirstTime = remember(progress) { progress.completedLevelsCount == 0 && !progress.isStorySeen(1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberVoid)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hero Card with Generated Image
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CyberSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, RaGold.copy(alpha = 0.35f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("hero_banner_card")
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.hero_cairo_2077),
                        contentDescription = "قاهرة ٢٠٧٧ السايبربانكية",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, CyberSurface.copy(alpha = 0.95f)),
                                    startY = 100f
                                )
                            )
                    )

                    // Floating Glowing Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CyberVoid.copy(alpha = 0.85f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AnkhCyan.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "مصر القديمة × سايبربانك",
                                color = AnkhCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "أسرار النيل: نهضة سايبر",
                        color = RaGold,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "وادي النيون • أحجار رع",
                        color = AnkhCyan,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = "طابِق أحجار رع، حلّ ألغاز الذكاء بين المراحل، وأعد الضوء إلى شبكة قاهرة ٢٠٧٧ قبل أن يبتلعها الصمت.",
                        color = SandMuted,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 10.dp)
                    )

                    // Mini Gem Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        GemType.entries.take(4).forEach { gem ->
                            GemIcon(gem = gem, size = 32.dp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Progress Overview Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                title = "النجوم",
                value = GameRepository.toArabicNumerals(progress.totalStars),
                icon = Icons.Default.Star,
                accentColor = RaGold,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "المراحل المنجزة",
                value = GameRepository.toArabicNumerals(progress.completedLevelsCount),
                icon = Icons.Default.CheckCircle,
                accentColor = ScarabEmerald,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "ألغاز محلولة",
                value = GameRepository.toArabicNumerals(progress.solvedPuzzles.size),
                icon = Icons.Default.Lightbulb,
                accentColor = AnkhCyan,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Primary Action: Continue or Start Adventure
        Button(
            onClick = {
                if (isFirstTime) {
                    onNavigate(Screen.Story(1))
                } else {
                    viewModel.startLevel(frontierLevelId)
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = RaGold,
                contentColor = Color(0xFF1E1300)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("continue_play_button")
        ) {
            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isFirstTime) "ابدأ المغامرة (الفصل الأول)" else "متابعة اللعب (المرحلة $frontierLevelId)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Secondary Action: Campaign Map
        OutlinedButton(
            onClick = { onNavigate(Screen.Map(1)) },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AnkhCyan),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, AnkhCyan.copy(alpha = 0.7f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("open_map_button")
        ) {
            Icon(imageVector = Icons.Default.Map, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "خريطة المراحل (٥,٢٠٠ فصل • ٣١,٢٠٠ مرحلة)",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // High Scores & Leaderboard Action
        OutlinedButton(
            onClick = { onNavigate(Screen.Leaderboard) },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = RaGold),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, RaGold.copy(alpha = 0.7f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("open_leaderboard_button")
        ) {
            Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = null, tint = RaGold, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "سجل الأرقام القياسية للألغاز (${GameRepository.toArabicNumerals(progress.totalPuzzleScore)} نقطة)",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tertiary Action: Pharaonic Codex & Archive
        TextButton(
            onClick = { onNavigate(Screen.Codex) },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.textButtonColors(contentColor = HorusPurple),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("open_codex_button")
        ) {
            Icon(imageVector = Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "الأرشيف الفرعوني وسجل الرموز",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // AdMob Banner
        com.example.ui.components.AdMobBanner()
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = CyberSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = value,
                color = RaGold,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = title,
                color = SandMuted,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
