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
import com.example.data.model.Screen
import com.example.ui.GameViewModel
import com.example.ui.theme.*

@Composable
fun StoryScreen(
    viewModel: GameViewModel,
    chapterId: Int,
    onNavigate: (Screen) -> Unit
) {
    BackHandler {
        viewModel.popBack()
    }

    val chapter = remember(chapterId) { viewModel.repository.getChapter(chapterId) }
    var currentLineIndex by remember { mutableStateOf(0) }
    val isLastLine = currentLineIndex >= chapter.lines.size - 1
    val scrollState = rememberScrollState()

    LaunchedEffect(chapterId) {
        viewModel.repository.markStorySeen(chapterId)
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
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "رجوع",
                    tint = RaGold
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = chapter.tag,
                    color = AnkhCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = chapter.title,
                    color = RaGold,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.size(46.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dialogue Narrative Card
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = CyberSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, AnkhCyan.copy(alpha = 0.4f)),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = RaGold.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RaGold),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = "𓂀", fontSize = 16.sp, color = RaGold)
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "نور — حارسة الأرشيف الفرعوني",
                            color = RaGold,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "شبكة قاهرة ٢٠٧٧ السايبربانكية",
                            color = AnkhCyan,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Display revealed lines
                chapter.lines.take(currentLineIndex + 1).forEachIndexed { index, line ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (index == currentLineIndex) CyberSurfaceVariant else Color.Transparent,
                        border = if (index == currentLineIndex) androidx.compose.foundation.BorderStroke(0.8.dp, RaGold.copy(alpha = 0.3f)) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Text(
                            text = line,
                            color = if (index == currentLineIndex) SandText else SandMuted,
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!isLastLine) {
                Button(
                    onClick = { currentLineIndex++ },
                    colors = ButtonDefaults.buttonColors(containerColor = RaGold, contentColor = Color(0xFF1E1300)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("story_next_button")
                ) {
                    Text("التالي", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = {
                        val firstLevel = chapter.levels.firstOrNull()?.id ?: "$chapterId-1"
                        viewModel.startLevel(firstLevel)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RaGold, contentColor = Color(0xFF1E1300)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("story_start_levels_button")
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ابدأ مراحل الفصل", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }

            OutlinedButton(
                onClick = { onNavigate(Screen.Map(chapterId)) },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SandMuted),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(0.7f)
                    .height(50.dp)
                    .testTag("story_skip_button")
            ) {
                Text("تخطّي", fontSize = 14.sp)
            }
        }
    }
}
