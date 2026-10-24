package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GameRepository
import com.example.data.model.GameLevel
import com.example.data.model.PlayerProgress
import com.example.data.model.Screen
import com.example.ui.GameViewModel
import com.example.ui.components.AdMobBanner
import com.example.ui.theme.*

@Composable
fun MapScreen(
    viewModel: GameViewModel,
    progress: PlayerProgress,
    initialChapterId: Int = 1,
    onNavigate: (Screen) -> Unit
) {
    BackHandler {
        viewModel.popBack()
    }

    var currentChapterId by remember { mutableIntStateOf(initialChapterId.coerceIn(1, GameRepository.MAX_CHAPTERS)) }
    val chapter = remember(currentChapterId) { viewModel.repository.getChapter(currentChapterId) }
    var lockedMessage by remember { mutableStateOf<String?>(null) }
    var showChapterJumpDialog by remember { mutableStateOf(false) }
    var jumpChapterInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberVoid)
            .padding(horizontal = 14.dp, vertical = 10.dp)
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
                    .testTag("map_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "رجوع",
                    tint = RaGold
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "خريطة المغامرة (٥,٢٠٠ فصل)",
                    color = AnkhCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "وادي النيون",
                    color = RaGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Leaderboard Shortcut
                IconButton(
                    onClick = { onNavigate(Screen.Leaderboard) },
                    modifier = Modifier
                        .size(38.dp)
                        .background(CyberSurface, CircleShape)
                        .border(1.dp, RaGold.copy(alpha = 0.5f), CircleShape)
                        .testTag("map_leaderboard_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "سجل الأرقام القياسية",
                        tint = RaGold,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Total Stars Badge
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CyberSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, RaGold.copy(alpha = 0.5f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = RaGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = GameRepository.toArabicNumerals(progress.totalStars),
                            color = RaGold,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Chapter Pager Switcher & Quick Navigation
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = CyberSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Previous buttons: -10 and -1
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (currentChapterId > 10) {
                            TextButton(
                                onClick = { currentChapterId = (currentChapterId - 10).coerceAtLeast(1) },
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("-١٠", color = SandMuted, fontSize = 11.sp)
                            }
                        }
                        IconButton(
                            onClick = { if (currentChapterId > 1) currentChapterId-- },
                            enabled = currentChapterId > 1,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "الفصل السابق",
                                tint = if (currentChapterId > 1) AnkhCyan else Color.Gray.copy(alpha = 0.4f)
                            )
                        }
                    }

                    // Chapter Title (Clickable to jump)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                jumpChapterInput = currentChapterId.toString()
                                showChapterJumpDialog = true
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${chapter.tag} من ٥,٢٠٠",
                                color = AnkhCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = AnkhCyan, modifier = Modifier.size(12.dp))
                        }
                        Text(
                            text = chapter.title,
                            color = RaGold,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Next buttons: +1 and +10
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { if (currentChapterId < GameRepository.MAX_CHAPTERS) currentChapterId++ },
                            enabled = currentChapterId < GameRepository.MAX_CHAPTERS,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "الفصل التالي",
                                tint = if (currentChapterId < GameRepository.MAX_CHAPTERS) AnkhCyan else Color.Gray.copy(alpha = 0.4f)
                            )
                        }
                        if (currentChapterId <= GameRepository.MAX_CHAPTERS - 10) {
                            TextButton(
                                onClick = { currentChapterId = (currentChapterId + 10).coerceAtMost(GameRepository.MAX_CHAPTERS) },
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("+١٠", color = SandMuted, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Chapter Lore Header Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CyberSurfaceVariant.copy(alpha = 0.5f),
            border = androidx.compose.foundation.BorderStroke(1.dp, AnkhCyan.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = chapter.hook,
                        color = SandText,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                FilledTonalButton(
                    onClick = { onNavigate(Screen.Story(chapter.id)) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = AnkhCyan.copy(alpha = 0.15f),
                        contentColor = AnkhCyan
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("chapter_story_button")
                ) {
                    Icon(imageVector = Icons.Default.AutoStories, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("القصة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Locked Message Alert
        AnimatedVisibility(visible = lockedMessage != null) {
            lockedMessage?.let { msg ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF3B1515),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF87171)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color(0xFFF87171), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = msg, color = Color(0xFFFCA5A5), fontSize = 12.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { lockedMessage = null }, modifier = Modifier.size(24.dp)) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Color(0xFFF87171), modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        // 6 Level Cards Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(chapter.levels, key = { it.id }) { level ->
                val isUnlocked = viewModel.repository.isLevelUnlocked(level.id, progress)
                val stars = progress.getStarsFor(level.id)
                val isPuzzleSolved = progress.isPuzzleSolved(level.id)
                val puzzleHighScore = progress.getPuzzleHighScore(level.id)

                LevelCard(
                    level = level,
                    isUnlocked = isUnlocked,
                    stars = stars,
                    isPuzzleSolved = isPuzzleSolved,
                    puzzleHighScore = puzzleHighScore,
                    onClick = {
                        if (isUnlocked) {
                            viewModel.startLevel(level.id)
                        } else {
                            lockedMessage = "المرحلة مقفلة! يجب إكمال المرحلة السابقة وحل لغز الذكاء الخاص بها أولاً."
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // AdMob Banner Ad at Bottom of Map
        AdMobBanner(modifier = Modifier.padding(bottom = 4.dp))
    }

    // Fast Chapter Jump Dialog
    if (showChapterJumpDialog) {
        AlertDialog(
            onDismissRequest = { showChapterJumpDialog = false },
            containerColor = CyberSurface,
            title = {
                Text(
                    text = "انتقال سريع إلى فصل (١ — ٥,٢٠٠)",
                    color = RaGold,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = jumpChapterInput,
                        onValueChange = { jumpChapterInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("رقم الفصل (مثال: 50, 100, 5000)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RaGold,
                            unfocusedBorderColor = CyberBorder,
                            focusedTextColor = SandText,
                            unfocusedTextColor = SandText
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "محطات رئيسية:",
                        color = SandMuted,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    val milestones = listOf(1, 50, 100, 500, 1000, 2500, 5000, 5200)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        milestones.take(4).forEach { num ->
                            AssistChip(
                                onClick = {
                                    currentChapterId = num
                                    showChapterJumpDialog = false
                                },
                                label = { Text("${num}", fontSize = 10.sp) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        milestones.drop(4).forEach { num ->
                            AssistChip(
                                onClick = {
                                    currentChapterId = num
                                    showChapterJumpDialog = false
                                },
                                label = { Text("${num}", fontSize = 10.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val entered = jumpChapterInput.toIntOrNull()
                        if (entered != null) {
                            currentChapterId = entered.coerceIn(1, GameRepository.MAX_CHAPTERS)
                        }
                        showChapterJumpDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RaGold, contentColor = Color(0xFF1E1300))
                ) {
                    Text("انتقال", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChapterJumpDialog = false }) {
                    Text("إلغاء", color = SandMuted)
                }
            }
        )
    }
}

@Composable
private fun LevelCard(
    level: GameLevel,
    isUnlocked: Boolean,
    stars: Int,
    isPuzzleSolved: Boolean,
    puzzleHighScore: Int = 0,
    onClick: () -> Unit
) {
    val borderColor = when {
        !isUnlocked -> CyberBorder.copy(alpha = 0.5f)
        level.isBoss -> RaPink.copy(alpha = 0.8f)
        stars > 0 -> RaGold.copy(alpha = 0.7f)
        else -> AnkhCyan.copy(alpha = 0.6f)
    }

    val containerColor = when {
        !isUnlocked -> CyberSurface.copy(alpha = 0.4f)
        level.isBoss -> CyberSurfaceVariant.copy(alpha = 0.9f)
        else -> CyberSurface
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .height(124.dp)
            .clickable(onClick = onClick)
            .testTag("level_item_${level.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Row: Level number & Boss/Lock badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${level.chapterId}-${level.index}",
                    color = if (isUnlocked) AnkhCyan else Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                if (!isUnlocked) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "مقفلة",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                } else if (level.isBoss) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = RaPink.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, RaPink)
                    ) {
                        Text(
                            text = "تحدي اللوح",
                            color = RaPink,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                } else if (isPuzzleSolved) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "اللغز محلول",
                        tint = ScarabEmerald,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Level Name and Puzzle Score
            Column {
                Text(
                    text = level.name,
                    color = if (isUnlocked) RaGold else Color.Gray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (puzzleHighScore > 0) {
                    Text(
                        text = "🏆 ${GameRepository.toArabicNumerals(puzzleHighScore)} نقطة",
                        color = RaGold.copy(alpha = 0.9f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Star Rating Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (s in 1..3) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = if (s <= stars) RaGold else Color.White.copy(alpha = 0.15f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
