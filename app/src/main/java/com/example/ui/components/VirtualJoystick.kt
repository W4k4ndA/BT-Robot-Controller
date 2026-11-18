package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AlloyHighlight
import com.example.ui.theme.BrushedTitanium
import com.example.ui.theme.ChromeEdge
import com.example.ui.theme.GunmetalChassis
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.SteelBevel
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/**
 * Futuristic metallic & neon precision virtual joystick for ESP32 differential-drive rover control.
 *
 * Output coordinates:
 * - x in [-1f, 1f]: -1 = Left, +1 = Right
 * - y in [-1f, 1f]: +1 = Up (Forward), -1 = Down (Reverse)
 */
@Composable
fun RoboticsVirtualJoystick(
    onVectorChange: (x: Float, y: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val currentOnVectorChange by rememberUpdatedState(onVectorChange)

    val animX = remember { Animatable(0f) }
    val animY = remember { Animatable(0f) }
    var isDragging by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .semantics {
                contentDescription = "Joystick virtual futurista de dirección y velocidad del rover"
            }
            .testTag("virtual_joystick"),
        contentAlignment = Alignment.Center
    ) {
        val diameterDp = min(maxWidth.value, maxHeight.value).dp.coerceIn(190.dp, 340.dp)

        Canvas(
            modifier = Modifier
                .size(diameterDp)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        isDragging = true
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val maxTravel = (min(size.width, size.height) / 2f) * 0.66f

                        fun pushOffset(px: Float, py: Float) {
                            val dx = px - centerX
                            val dy = py - centerY
                            val dist = hypot(dx, dy)
                            val clampedDist = dist.coerceAtMost(maxTravel)
                            val angle = atan2(dy, dx)
                            val nx = if (maxTravel > 0f) (cos(angle) * clampedDist) / maxTravel else 0f
                            val nyScreen = if (maxTravel > 0f) (sin(angle) * clampedDist) / maxTravel else 0f

                            scope.launch {
                                animX.snapTo(nx)
                                animY.snapTo(nyScreen)
                            }
                            // Invert screen Y so UP = +1 (forward), DOWN = -1 (reverse)
                            currentOnVectorChange(nx.coerceIn(-1f, 1f), (-nyScreen).coerceIn(-1f, 1f))
                        }

                        pushOffset(down.position.x, down.position.y)

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change == null || !change.pressed) {
                                break
                            }
                            if (change.positionChange() != Offset.Zero) {
                                change.consume()
                                pushOffset(change.position.x, change.position.y)
                            }
                        }

                        isDragging = false
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        currentOnVectorChange(0f, 0f)
                        scope.launch {
                            launch {
                                animX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                )
                            }
                            launch {
                                animY.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                )
                            }
                        }
                    }
                }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = min(size.width, size.height) / 2f - 4.dp.toPx()
            val maxTravel = (min(size.width, size.height) / 2f) * 0.66f
            val puckRadius = outerRadius * 0.27f

            val normX = animX.value
            val normYScreen = animY.value
            val normForward = -normYScreen
            val stickMag = hypot(normX, normYScreen).coerceIn(0f, 1f)

            val activeNeon = when {
                stickMag < 0.08f -> NeonCyan
                normForward < -0.15f -> NeonMagenta
                normForward > 0.15f -> NeonCyan
                else -> NeonAmber
            }

            // 1. Outer Metallic Bezel Ring
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(ChromeEdge.copy(alpha = 0.7f), GunmetalChassis, AlloyHighlight),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height)
                ),
                radius = outerRadius,
                center = center
            )

            // 2. Inner Deep Well
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(BrushedTitanium, ObsidianVoid),
                    center = center,
                    radius = outerRadius * 0.90f
                ),
                radius = outerRadius * 0.90f,
                center = center
            )

            // 3. Outer Neon Telemetry Halo
            drawCircle(
                color = activeNeon.copy(alpha = if (isDragging) 0.85f else 0.45f),
                radius = outerRadius * 0.90f,
                center = center,
                style = Stroke(width = 2.2.dp.toPx())
            )

            // 4. 8 Metallic Perimeter Hex Rivets
            for (i in 0 until 8) {
                val angleRad = (i * 45.0) * (PI / 180.0)
                val r = outerRadius * 0.945f
                val boltPos = Offset(
                    x = center.x + (cos(angleRad) * r).toFloat(),
                    y = center.y + (sin(angleRad) * r).toFloat()
                )
                drawCircle(
                    color = AlloyHighlight,
                    radius = 3.2.dp.toPx(),
                    center = boltPos
                )
                drawCircle(
                    color = NeonCyan.copy(alpha = 0.4f),
                    radius = 1.2.dp.toPx(),
                    center = boltPos
                )
            }

            // 5. Azimuth Degree Ticks & Concentric Radar Rings
            drawCircle(
                color = SteelBevel.copy(alpha = 0.4f),
                radius = maxTravel,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                color = SteelBevel.copy(alpha = 0.3f),
                radius = maxTravel * 0.5f,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            for (deg in 0 until 360 step 15) {
                val isMajor = deg % 45 == 0
                val rad = deg * (PI / 180.0)
                val innerR = outerRadius * (if (isMajor) 0.78f else 0.83f)
                val outerR = outerRadius * 0.88f
                drawLine(
                    color = if (isMajor) activeNeon.copy(alpha = 0.75f) else SteelBevel.copy(alpha = 0.5f),
                    start = Offset(
                        center.x + (cos(rad) * innerR).toFloat(),
                        center.y + (sin(rad) * innerR).toFloat()
                    ),
                    end = Offset(
                        center.x + (cos(rad) * outerR).toFloat(),
                        center.y + (sin(rad) * outerR).toFloat()
                    ),
                    strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Crosshair axis lines
            drawLine(
                color = SteelBevel.copy(alpha = 0.35f),
                start = Offset(center.x - maxTravel, center.y),
                end = Offset(center.x + maxTravel, center.y),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = SteelBevel.copy(alpha = 0.35f),
                start = Offset(center.x, center.y - maxTravel),
                end = Offset(center.x, center.y + maxTravel),
                strokeWidth = 1.dp.toPx()
            )

            // 6. Cardinal Directional Chevrons (Forward/Reverse/Left/Right)
            val chevronDist = outerRadius * 0.73f
            drawDirectionChevron(Offset(center.x, center.y - chevronDist), 0f, if (normForward > 0.15f) NeonCyan else SteelBevel)
            drawDirectionChevron(Offset(center.x, center.y + chevronDist), 180f, if (normForward < -0.15f) NeonMagenta else SteelBevel)
            drawDirectionChevron(Offset(center.x - chevronDist, center.y), -90f, if (normX < -0.15f) NeonAmber else SteelBevel)
            drawDirectionChevron(Offset(center.x + chevronDist, center.y), 90f, if (normX > 0.15f) NeonAmber else SteelBevel)

            // 7. Dynamic Neon Thrust Vector Beam
            val puckCenter = Offset(
                x = center.x + normX * maxTravel,
                y = center.y + normYScreen * maxTravel
            )

            if (stickMag > 0.05f) {
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(activeNeon.copy(alpha = 0.15f), activeNeon),
                        start = center,
                        end = puckCenter
                    ),
                    start = center,
                    end = puckCenter,
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(activeNeon.copy(alpha = 0.38f), Color.Transparent),
                        center = puckCenter,
                        radius = puckRadius * 1.9f
                    ),
                    radius = puckRadius * 1.9f,
                    center = puckCenter
                )
            }

            // 8. Brushed Metallic Control Puck
            drawCircle(
                color = Color.Black.copy(alpha = 0.65f),
                radius = puckRadius * 1.05f,
                center = Offset(puckCenter.x, puckCenter.y + 4.dp.toPx())
            )
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(ChromeEdge, BrushedTitanium, GunmetalChassis),
                    start = Offset(puckCenter.x - puckRadius, puckCenter.y - puckRadius),
                    end = Offset(puckCenter.x + puckRadius, puckCenter.y + puckRadius)
                ),
                radius = puckRadius,
                center = puckCenter
            )
            drawCircle(
                color = activeNeon,
                radius = puckRadius,
                center = puckCenter,
                style = Stroke(width = 2.5.dp.toPx())
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(ObsidianVoid, BrushedTitanium),
                    center = puckCenter,
                    radius = puckRadius * 0.72f
                ),
                radius = puckRadius * 0.72f,
                center = puckCenter
            )
            // Inner Neon Core Dot & Reticle
            drawCircle(
                color = activeNeon.copy(alpha = 0.65f),
                radius = puckRadius * 0.42f,
                center = puckCenter,
                style = Stroke(width = 1.5.dp.toPx())
            )
            drawCircle(
                color = if (isDragging) NeonEmerald else activeNeon,
                radius = puckRadius * 0.16f,
                center = puckCenter
            )
        }
    }
}

private fun DrawScope.drawDirectionChevron(
    center: Offset,
    rotationDeg: Float,
    color: Color
) {
    val sizePx = 6.dp.toPx()
    val rad = rotationDeg * (PI / 180.0)
    val cosA = cos(rad).toFloat()
    val sinA = sin(rad).toFloat()

    fun rotate(px: Float, py: Float): Offset {
        return Offset(
            x = center.x + px * cosA - py * sinA,
            y = center.y + px * sinA + py * cosA
        )
    }

    val p1 = rotate(-sizePx, sizePx * 0.6f)
    val p2 = rotate(0f, -sizePx * 0.7f)
    val p3 = rotate(sizePx, sizePx * 0.6f)

    val path = Path().apply {
        moveTo(p1.x, p1.y)
        lineTo(p2.x, p2.y)
        lineTo(p3.x, p3.y)
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
    )
}
