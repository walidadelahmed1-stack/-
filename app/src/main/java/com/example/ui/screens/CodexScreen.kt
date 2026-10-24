package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GemType
import com.example.data.model.Screen
import com.example.ui.GameViewModel
import com.example.ui.components.GemIcon
import com.example.ui.theme.*

@Composable
fun CodexScreen(
    viewModel: GameViewModel,
    onNavigate: (Screen) -> Unit
) {
    BackHandler {
        viewModel.popBack()
    }

    val scrollState = rememberScrollState()
    var showResetDialog by remember { mutableStateOf(false) }

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
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "رجوع",
                    tint = RaGold
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "الأرشيف الفرعوني",
                    color = AnkhCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "سجل أحجار رع وشفرات النيون",
                    color = RaGold,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.size(46.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Story & Rules Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "قواعد وادي النيون",
                        color = RaGold,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• طابق ٣ أحجار متطابقة أو أكثر أفقياً أو عمودياً لشحن بوابة الطاقة.\n" +
                               "• كل تسلسل من التطابقات المتتالية (Cascade) يضاعف نقاط الكومبو (x2, x3).\n" +
                               "• لإتمام المرحلة، حقق النقاط المستهدفة قبل نفاد رصيد الحركات.\n" +
                               "• بعد الفوز بالمرحلة، يجب حل «لغز الذكاء» الفرعوني لفتح الباب للمرحلة التالية.",
                        color = SandText,
                        fontSize = 13.sp,
                        lineHeight = 22.sp
                    )
                }
            }

            // Sacred Stones Guide
            Text(
                text = "أحجار رع الستة المقدسة",
                color = AnkhCyan,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )

            val stoneLore = listOf(
                Pair(GemType.SUN, "يمثل إله الشمس رع وطاقة الضوء الأبدي التي تحمي قاهرة ٢٠٧٧ من الظلام الرقمي."),
                Pair(GemType.ANKH, "مفتاح الحياة الخالدة، يربط السيرفرات القديمة بقلب النيل النيونى."),
                Pair(GemType.SCARAB, "رمز البعث والتجدد المستمر، ينظف شفرات الفضاء السيبراني من الفيروسات."),
                Pair(GemType.EYE, "عين حورس الواقية من الأخطار، تكشف الأنماط الخفية في ألغاز الذكاء."),
                Pair(GemType.PYRAMID, "رمز الثبات والحكمة الهندسية، حجر الصمود أمام انهيار الشبكة."),
                Pair(GemType.LOTUS, "رمز الولادة والجمال والصفاء، تتفتح أوراقه مع كل انتصار تحققه.")
            )

            stoneLore.forEach { (gem, lore) ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, gem.color.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GemIcon(gem = gem, size = 48.dp)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = gem.displayName,
                                color = gem.color,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = lore,
                                color = SandMuted,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // High Scores Leaderboard Entry
            Button(
                onClick = { onNavigate(Screen.Leaderboard) },
                colors = ButtonDefaults.buttonColors(containerColor = RaGold, contentColor = Color(0xFF1E1300)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("codex_leaderboard_button")
            ) {
                Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("سجل الأرقام القياسية للألغاز", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Reset Game Progress
            OutlinedButton(
                onClick = { showResetDialog = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF87171)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF87171).copy(alpha = 0.5f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("reset_progress_button")
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("إعادة ضبط تقدم اللعبة بالكامل")
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = CyberSurface,
            title = {
                Text(text = "إعادة ضبط اللعبة؟", color = Color(0xFFF87171), fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "سيتم مسح جميع النجوم والنقاط والمراحل المفتوحة والبدء من البداية.",
                    color = SandText,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.repository.resetProgress()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("مسح وإعادة الضبط", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("إلغاء", color = SandMuted)
                }
            }
        )
    }
}
