package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.GemType
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GemIcon(
    gem: GemType,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    isSelected: Boolean = false,
    isPulsing: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gem_pulse")
    val pulseScale by if (isPulsing || isSelected) {
        infiniteTransition.animateFloat(
            initialValue = 0.92f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_scale"
        )
    } else {
        rememberUpdatedState(1f)
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.size(size * pulseScale)
        ) {
            val w = this.size.width
            val h = this.size.height
            val strokeW = (w * 0.08f).coerceAtLeast(2.5f)
            val stroke = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
            val glowColor = gem.color.copy(alpha = if (isSelected) 0.6f else 0.25f)

            // Background soft cyber glow
            drawCircle(
                color = glowColor,
                radius = w * 0.45f
            )

            when (gem) {
                GemType.SUN -> {
                    // Sun Disc with radiating rays
                    val center = Offset(w * 0.5f, h * 0.5f)
                    val r = w * 0.22f
                    drawCircle(color = gem.color, radius = r, style = stroke)

                    val angles = listOf(0.0, 45.0, 90.0, 135.0, 180.0, 225.0, 270.0, 315.0)
                    for (deg in angles) {
                        val rad = Math.toRadians(deg)
                        val start = Offset(
                            center.x + (cos(rad) * w * 0.30f).toFloat(),
                            center.y + (sin(rad) * h * 0.30f).toFloat()
                        )
                        val end = Offset(
                            center.x + (cos(rad) * w * 0.44f).toFloat(),
                            center.y + (sin(rad) * h * 0.44f).toFloat()
                        )
                        drawLine(color = gem.color, start = start, end = end, strokeWidth = strokeW, cap = StrokeCap.Round)
                    }
                }
                GemType.ANKH -> {
                    // Ankh: Loop at top, crossbar, stem
                    val headCenter = Offset(w * 0.5f, h * 0.30f)
                    val headR = w * 0.16f
                    drawCircle(color = gem.color, center = headCenter, radius = headR, style = stroke)

                    // Vertical stem
                    drawLine(
                        color = gem.color,
                        start = Offset(w * 0.5f, h * 0.46f),
                        end = Offset(w * 0.5f, h * 0.88f),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                    // Horizontal crossbar
                    drawLine(
                        color = gem.color,
                        start = Offset(w * 0.24f, h * 0.58f),
                        end = Offset(w * 0.76f, h * 0.58f),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                }
                GemType.SCARAB -> {
                    // Scarab: Body oval, head circle, front horns/feelers, side legs
                    // Oval body
                    drawOval(
                        color = gem.color,
                        topLeft = Offset(w * 0.30f, h * 0.36f),
                        size = Size(w * 0.40f, h * 0.48f),
                        style = stroke
                    )
                    // Head
                    drawCircle(
                        color = gem.color,
                        center = Offset(w * 0.5f, h * 0.24f),
                        radius = w * 0.11f,
                        style = stroke
                    )
                    // Spine line
                    drawLine(
                        color = gem.color,
                        start = Offset(w * 0.5f, h * 0.36f),
                        end = Offset(w * 0.5f, h * 0.82f),
                        strokeWidth = strokeW * 0.8f
                    )
                    // Feelers
                    drawLine(color = gem.color, start = Offset(w * 0.42f, h * 0.18f), end = Offset(w * 0.30f, h * 0.08f), strokeWidth = strokeW, cap = StrokeCap.Round)
                    drawLine(color = gem.color, start = Offset(w * 0.58f, h * 0.18f), end = Offset(w * 0.70f, h * 0.08f), strokeWidth = strokeW, cap = StrokeCap.Round)
                }
                GemType.EYE -> {
                    // Eye of Horus: Almond contour, pupil, cosmetic tear lines
                    val path = Path().apply {
                        moveTo(w * 0.14f, h * 0.50f)
                        cubicTo(w * 0.30f, h * 0.28f, w * 0.70f, h * 0.28f, w * 0.86f, h * 0.50f)
                        cubicTo(w * 0.70f, h * 0.72f, w * 0.30f, h * 0.72f, w * 0.14f, h * 0.50f)
                        close()
                    }
                    drawPath(path = path, color = gem.color, style = stroke)

                    // Pupil
                    drawCircle(color = gem.color, center = Offset(w * 0.5f, h * 0.50f), radius = w * 0.09f)

                    // Tear markings
                    val tearPath = Path().apply {
                        moveTo(w * 0.68f, h * 0.64f)
                        lineTo(w * 0.82f, h * 0.84f)
                    }
                    drawPath(path = tearPath, color = gem.color, style = stroke)
                    drawLine(
                        color = gem.color,
                        start = Offset(w * 0.28f, h * 0.38f),
                        end = Offset(w * 0.38f, h * 0.26f),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                }
                GemType.PYRAMID -> {
                    // Stepped triangular pyramid
                    val pyrPath = Path().apply {
                        moveTo(w * 0.50f, h * 0.16f)
                        lineTo(w * 0.86f, h * 0.84f)
                        lineTo(w * 0.14f, h * 0.84f)
                        close()
                    }
                    drawPath(path = pyrPath, color = gem.color, style = stroke)
                    // Inner tier line
                    drawLine(
                        color = gem.color,
                        start = Offset(w * 0.28f, h * 0.62f),
                        end = Offset(w * 0.72f, h * 0.62f),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                }
                GemType.LOTUS -> {
                    // Lotus flower petals
                    val centerPetal = Path().apply {
                        moveTo(w * 0.50f, h * 0.14f)
                        cubicTo(w * 0.62f, h * 0.34f, w * 0.62f, h * 0.58f, w * 0.50f, h * 0.76f)
                        cubicTo(w * 0.38f, h * 0.58f, w * 0.38f, h * 0.34f, w * 0.50f, h * 0.14f)
                        close()
                    }
                    drawPath(path = centerPetal, color = gem.color, style = stroke)

                    // Left petal
                    val leftPetal = Path().apply {
                        moveTo(w * 0.44f, h * 0.48f)
                        cubicTo(w * 0.24f, h * 0.42f, w * 0.18f, h * 0.64f, w * 0.44f, h * 0.76f)
                    }
                    drawPath(path = leftPetal, color = gem.color, style = stroke)

                    // Right petal
                    val rightPetal = Path().apply {
                        moveTo(w * 0.56f, h * 0.48f)
                        cubicTo(w * 0.76f, h * 0.42f, w * 0.82f, h * 0.64f, w * 0.56f, h * 0.76f)
                    }
                    drawPath(path = rightPetal, color = gem.color, style = stroke)

                    // Stem base curve
                    drawLine(
                        color = gem.color,
                        start = Offset(w * 0.32f, h * 0.82f),
                        end = Offset(w * 0.68f, h * 0.82f),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}
