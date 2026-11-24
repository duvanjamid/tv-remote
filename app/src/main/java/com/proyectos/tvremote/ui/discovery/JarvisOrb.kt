package com.proyectos.tvremote.ui.discovery

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Jarvis-style holographic orb (Age of Ultron Cradle look): amber/gold wireframe
 * sphere built from tilted rotating rings, radial circuit spokes and scattered
 * short segments, a pulsing core glow and a radar "ping" while scanning.
 * Pure Canvas 2D (no 3D libs) - tilt/rotation is faked with per-ring X-scale.
 */

private data class CircuitSeg(val cx: Float, val cy: Float, val dx: Float, val dy: Float)

@Composable
private fun InfiniteTransition.rememberSpin(baseMillis: Int, speed: Float, label: String): State<Float> =
    animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = max(200, (baseMillis / speed).toInt()), easing = LinearEasing)
        ),
        label = label
    )

@Composable
fun JarvisOrb(
    modifier: Modifier = Modifier,
    scanning: Boolean,
    size: Dp = 200.dp,
    // Defaults match the reference (amber/gold), but the caller passes the live
    // Material You palette so the orb reads as part of the theme, not a fixed color.
    primaryColor: Color = Color(0xFFFFC107),
    secondaryColor: Color = Color(0xFFFFB300),
    tertiaryColor: Color = Color(0xFFFF8F00)
) {
    val orbGold = primaryColor
    val orbAmber = secondaryColor
    val orbDeepAmber = tertiaryColor
    val orbCore = androidx.compose.ui.graphics.lerp(primaryColor, Color.White, 0.75f)

    val speed = if (scanning) 1f else 0.4f
    val alphaMul = if (scanning) 1f else 0.6f

    val infinite = rememberInfiniteTransition(label = "jarvisOrb")

    val ringA by infinite.rememberSpin(3200, speed, "ringA")
    val ringB by infinite.rememberSpin(5200, speed, "ringB")
    val ringC by infinite.rememberSpin(7400, speed, "ringC")
    val spokesRot by infinite.rememberSpin(26000, speed, "spokes")
    val circuitRot by infinite.rememberSpin(19000, speed, "circuit")

    val pulse by infinite.animateFloat(
        initialValue = 0.75f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = max(200, (1400 / speed).toInt()), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val pingProgress by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing)
        ),
        label = "ping"
    )

    val spokeAngles = remember { (0 until 36).map { it * 10f + Random.nextFloat() * 4f } }
    val circuitSegs = remember {
        val rnd = Random(42)
        (0 until 46).map {
            val angle = rnd.nextFloat() * 360f
            val radiusFrac = 0.25f + rnd.nextFloat() * 0.6f
            val len = 0.05f + rnd.nextFloat() * 0.09f
            val rad = Math.toRadians(angle.toDouble())
            val cx = (cos(rad) * radiusFrac).toFloat()
            val cy = (sin(rad) * radiusFrac).toFloat()
            val dx = (cos(rad) * len).toFloat()
            val dy = (sin(rad) * len).toFloat()
            CircuitSeg(cx, cy, dx, dy)
        }
    }

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val center = Offset(w / 2f, h / 2f)
        val r = min(w, h) / 2f

        withTransform({ rotate(degrees = spokesRot, pivot = center) }) {
            spokeAngles.forEach { angle ->
                val rad = Math.toRadians(angle.toDouble())
                val innerR = r * 0.22f
                val outerR = r * (0.85f + (angle % 7f) / 70f)
                val start = Offset(
                    center.x + (cos(rad) * innerR).toFloat(),
                    center.y + (sin(rad) * innerR).toFloat()
                )
                val end = Offset(
                    center.x + (cos(rad) * outerR).toFloat(),
                    center.y + (sin(rad) * outerR).toFloat()
                )
                drawLine(
                    color = orbAmber.copy(alpha = 0.35f * alphaMul),
                    start = start,
                    end = end,
                    strokeWidth = 1f
                )
            }
        }

        withTransform({ rotate(degrees = circuitRot, pivot = center) }) {
            circuitSegs.forEach { seg ->
                val start = Offset(center.x + seg.cx * r, center.y + seg.cy * r)
                val end = Offset(center.x + (seg.cx + seg.dx) * r, center.y + (seg.cy + seg.dy) * r)
                drawLine(
                    color = orbGold.copy(alpha = 0.45f * alphaMul),
                    start = start,
                    end = end,
                    strokeWidth = 1.4f
                )
            }
        }

        drawTiltedRing(center, r * 0.92f, tiltDeg = 0f, spin = ringA, color = orbGold, alphaMul = alphaMul)
        drawTiltedRing(center, r * 0.78f, tiltDeg = 55f, spin = ringB, color = orbAmber, alphaMul = alphaMul)
        drawTiltedRing(center, r * 0.78f, tiltDeg = 125f, spin = ringC, color = orbAmber, alphaMul = alphaMul)
        drawTiltedRing(center, r * 0.55f, tiltDeg = 90f, spin = -ringB, color = orbDeepAmber, alphaMul = alphaMul)

        if (scanning) {
            val pingR = r * (0.3f + pingProgress * 0.9f)
            drawCircle(
                color = orbGold.copy(alpha = (1f - pingProgress) * 0.5f),
                radius = pingR,
                center = center,
                style = Stroke(width = 2f)
            )
        }

        val coreR = r * 0.18f * pulse
        drawCircle(orbDeepAmber.copy(alpha = 0.10f * alphaMul), radius = coreR * 3.2f, center = center)
        drawCircle(orbAmber.copy(alpha = 0.18f * alphaMul), radius = coreR * 2.2f, center = center)
        drawCircle(orbGold.copy(alpha = 0.35f * alphaMul), radius = coreR * 1.4f, center = center)
        drawCircle(orbCore.copy(alpha = 0.9f * alphaMul), radius = coreR * 0.7f, center = center)
    }
}

private fun DrawScope.drawTiltedRing(
    center: Offset,
    radius: Float,
    tiltDeg: Float,
    spin: Float,
    color: Color,
    alphaMul: Float
) {
    val spinRad = Math.toRadians(spin.toDouble())
    val scaleX = max(0.12f, abs(cos(spinRad)).toFloat())
    withTransform({
        rotate(degrees = tiltDeg, pivot = center)
        scale(scaleX = scaleX, scaleY = 1f, pivot = center)
    }) {
        drawCircle(
            color = color.copy(alpha = 0.55f * alphaMul),
            radius = radius,
            center = center,
            style = Stroke(width = 1.6f)
        )
    }
}
