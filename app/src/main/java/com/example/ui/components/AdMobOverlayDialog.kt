package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ads.AdSimulationState
import com.example.data.GameRepository
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun AdMobOverlayDialog(
    state: AdSimulationState,
    onDismiss: () -> Unit
) {
    if (!state.isShowing) return

    val totalDurationSec = 5
    var secondsLeft by remember(state) { mutableIntStateOf(totalDurationSec) }
    val isFinished = secondsLeft <= 0

    LaunchedEffect(state) {
        secondsLeft = totalDurationSec
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft--
        }
    }

    val progress by animateFloatAsState(
        targetValue = (totalDurationSec - secondsLeft).toFloat() / totalDurationSec.toFloat(),
        label = "ad_progress"
    )

    AlertDialog(
        onDismissRequest = {
            if (isFinished) {
                onDismiss()
            }
        },
        containerColor = CyberSurface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = RaGold.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RaGold.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = if (state.isRewarded) "AdMob بمكافأة" else "AdMob بيني",
                        color = RaGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                if (isFinished) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = Color.Gray
                        )
                    }
                } else {
                    Text(
                        text = "${GameRepository.toArabicNumerals(secondsLeft)} ث",
                        color = AnkhCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Eye of Horus Glowing Emblem
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(CyberSurfaceVariant, CircleShape)
                        .border(1.5.dp, RaGold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "𓂀", fontSize = 28.sp, color = RaGold)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = state.title,
                    color = RaGold,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = state.description,
                    color = SandMuted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )

                if (state.rewardLabel.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = AnkhCyan.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, AnkhCyan),
                        modifier = Modifier.padding(top = 10.dp)
                    ) {
                        Text(
                            text = "المكافأة: ${state.rewardLabel}",
                            color = AnkhCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = { progress },
                    color = RaGold,
                    trackColor = CyberVoid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isFinished) "اكتمل الإعلان بنجاح!" else "يرجى الانتظار ${GameRepository.toArabicNumerals(secondsLeft)} ثانية...",
                    color = if (isFinished) ScarabEmerald else SandMuted,
                    fontSize = 11.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isFinished) {
                        if (state.isRewarded) {
                            state.onRewarded()
                        } else {
                            state.onDismissed()
                        }
                    }
                },
                enabled = isFinished,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isRewarded) ScarabEmerald else RaGold,
                    contentColor = Color(0xFF001F29),
                    disabledContainerColor = CyberSurfaceVariant,
                    disabledContentColor = Color.Gray
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = if (isFinished) {
                        if (state.isRewarded) "استلام المكافأة (${state.rewardLabel})" else "متابعة اللعب"
                    } else {
                        "جارٍ عرض الإعلان..."
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    )
}
