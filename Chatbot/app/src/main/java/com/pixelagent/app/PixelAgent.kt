package com.pixelagent.app

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Animated Pixel Art Agent
 * Bouncy, glow pulse, blinking eyes, floating particles
 * Style: White/silver hair, cyan eyes, futuristic pixel aesthetic
 */

@Composable
fun PixelAgent(
    modifier: Modifier = Modifier,
    isTalking: Boolean = false,
    size: Int = 120
) {
    val pixelSize = size / 16f

    // Bounce animation
    val bounceAnim = rememberInfiniteTransition(label = "bounce")
    val bounceY by bounceAnim.animateFloat(
        initialValue = 0f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    // Glow pulse
    val glowAnim = rememberInfiniteTransition(label = "glow")
    val glowAlpha by glowAnim.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    // Talking scale pulse
    val talkAnim = rememberInfiniteTransition(label = "talk")
    val talkScale by talkAnim.animateFloat(
        initialValue = 1f,
        targetValue = if (isTalking) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "talk"
    )

    // Eye blink
    var isBlinking by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(2000, 5000))
            isBlinking = true
            delay(150)
            isBlinking = false
        }
    }

    // Floating particles
    val particles = remember { List(8) { ParticleData() } }
    particles.forEach { particle ->
        val particleAnim = rememberInfiniteTransition(label = "particle_${particle.id}")
        particle.offsetY by particleAnim.animateFloat(
            initialValue = 0f,
            targetValue = -30f,
            animationSpec = infiniteRepeatable(
                animation = tween(particle.duration, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "particle_y_${particle.id}"
        )
        particle.alpha by particleAnim.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = particle.duration
                    0f at 0
                    1f at (particle.duration * 0.2).toInt()
                    1f at (particle.duration * 0.7).toInt()
                    0f at particle.duration
                },
                repeatMode = RepeatMode.Restart
            ),
            label = "particle_a_${particle.id}"
        )
    }

    Box(modifier = modifier.size(size.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size / 2f
            val centerY = size / 2f + bounceY
            val scale = if (isTalking) talkScale else 1f

            // Glow background
            drawCircle(
                color = Color(0xFF00E5FF).copy(alpha = glowAlpha),
                radius = (size / 2f) * scale * 1.1f,
                center = Offset(centerX, centerY)
            )

            // Outer glow ring
            drawCircle(
                color = Color(0xFF00E5FF).copy(alpha = glowAlpha * 0.5f),
                radius = (size / 2f) * scale * 1.3f,
                center = Offset(centerX, centerY),
                style = Stroke(width = 2f)
            )

            // Draw pixel agent
            drawPixelAgent(
                centerX = centerX,
                centerY = centerY,
                pixelSize = pixelSize,
                scale = scale,
                isBlinking = isBlinking
            )

            // Floating particles
            particles.forEach { p ->
                val px = centerX + cos(p.angle) * p.radius
                val py = centerY + sin(p.angle) * p.radius + p.offsetY
                drawRect(
                    color = p.color.copy(alpha = p.alpha * glowAlpha * 2f),
                    topLeft = Offset(px - pixelSize/2, py - pixelSize/2),
                    size = Size(pixelSize * p.size, pixelSize * p.size)
                )
            }
        }
    }
}

private class ParticleData {
    val id = Random.nextInt()
    val angle = Random.nextFloat() * 2f * kotlin.math.PI.toFloat()
    val radius = Random.nextFloat() * 40f + 30f
    val duration = Random.nextInt(2000, 4000)
    val color = listOf(
        Color(0xFF00E5FF),  // Cyan
        Color(0xFFFF00E5),  // Magenta
        Color(0xFFFFFF00),  // Yellow
        Color(0xFF00FF88),  // Green
    ).random()
    val size = Random.nextFloat() * 1.5f + 0.5f
    var offsetY by mutableFloatStateOf(0f)
    var alpha by mutableFloatStateOf(0f)
}

private fun DrawScope.drawPixelAgent(
    centerX: Float,
    centerY: Float,
    pixelSize: Float,
    scale: Float,
    isBlinking: Boolean
) {
    val ps = pixelSize * scale
    val baseX = centerX - (8 * ps)
    val baseY = centerY - (10 * ps)

    fun px(x: Int, y: Int, color: Color) {
        drawRect(
            color = color,
            topLeft = Offset(baseX + x * ps, baseY + y * ps),
            size = Size(ps + 0.5f, ps + 0.5f)
        )
    }

    // Colors
    val hairMain = Color(0xFFE8E8F0)      // White/silver
    val hairShadow = Color(0xFFB8B8C8)   // Silver shadow
    val hairDark = Color(0xFF808090)      // Dark silver
    val skin = Color(0xFFFFF0E8)          // Pale skin
    val skinShadow = Color(0xFFE8D8D0)    // Skin shadow
    val eyeCyan = Color(0xFF00E5FF)      // Cyan eye
    val eyeDark = Color(0xFF006080)      // Dark cyan
    val eyeWhite = Color(0xFFFFFFFF)      // Eye white
    val pink = Color(0xFFFFA0B8)          // Pink accent
    val darkPink = Color(0xFFFF6090)      // Dark pink
    val outline = Color(0xFF202030)       // Dark outline
    val collar = Color(0xFF00A0C0)        // Cyan collar
    val collarDark = Color(0xFF006080)    // Dark cyan

    // === HAIR (back layer) ===
    // Row 0
    for (x in 2..13) px(x, 0, hairMain)
    // Row 1
    for (x in 1..14) px(x, 1, hairMain)
    px(0, 1, hairShadow); px(15, 1, hairShadow)
    // Row 2
    for (x in 0..15) px(x, 2, hairMain)
    // Row 3
    for (x in 0..15) px(x, 3, if (x in 4..11) hairMain else hairShadow)

    // === FACE ===
    // Row 4
    for (x in 2..13) px(x, 4, if (x in 4..11) skin else hairMain)
    px(2, 4, hairShadow); px(13, 4, hairShadow)

    // Row 5 - eyes row
    for (x in 3..12) {
        when (x) {
            in 5..6 -> { /* left eye area */ }
            in 9..10 -> { /* right eye area */ }
            else -> px(x, 5, skin)
        }
    }
    px(3, 5, skinShadow); px(12, 5, skinShadow)

    // Left eye
    if (isBlinking) {
        px(5, 5, outline); px(6, 5, outline)
    } else {
        px(5, 5, eyeWhite); px(6, 5, eyeWhite)
        px(5, 5, eyeCyan); px(6, 5, eyeDark)  // Iris
        px(5, 5, Color.White.copy(alpha = 0.7f))  // Highlight
    }

    // Right eye
    if (isBlinking) {
        px(9, 5, outline); px(10, 5, outline)
    } else {
        px(9, 5, eyeWhite); px(10, 5, eyeWhite)
        px(9, 5, eyeCyan); px(10, 5, eyeDark)
        px(9, 5, Color.White.copy(alpha = 0.7f))
    }

    // Row 6
    for (x in 4..11) px(x, 6, skin)
    px(4, 6, skinShadow); px(11, 6, skinShadow)

    // Row 7 - nose/mouth
    for (x in 5..10) px(x, 7, skin)
    px(7, 7, skinShadow)  // Nose
    px(6, 7, pink); px(7, 7, darkPink); px(8, 7, pink)  // Mouth

    // Row 8
    for (x in 4..11) px(x, 8, skin)
    px(4, 8, skinShadow); px(11, 8, skinShadow)

    // Row 9 - chin
    for (x in 5..10) px(x, 9, skin)
    px(5, 9, skinShadow); px(10, 9, skinShadow)

    // === HAIR (front/bangs) ===
    // Bangs
    px(2, 4, hairMain); px(3, 4, hairMain)
    px(12, 4, hairMain); px(13, 4, hairMain)
    px(1, 5, hairShadow); px(14, 5, hairShadow)
    px(0, 6, hairDark); px(15, 6, hairDark)
    px(1, 6, hairShadow); px(14, 6, hairShadow)
    px(2, 6, hairMain); px(13, 6, hairMain)

    // Side hair strands
    px(0, 7, hairDark); px(15, 7, hairDark)
    px(1, 7, hairShadow); px(14, 7, hairShadow)
    px(0, 8, hairShadow); px(15, 8, hairShadow)
    px(1, 8, hairMain); px(14, 8, hairMain)
    px(0, 9, hairMain); px(15, 9, hairMain)
    px(1, 9, hairShadow); px(14, 9, hairShadow)

    // === BODY / COLLAR ===
    // Row 10
    for (x in 4..11) px(x, 10, collar)
    px(4, 10, collarDark); px(11, 10, collarDark)

    // Row 11 - collar detail
    for (x in 5..10) px(x, 11, collar)
    px(6, 11, Color.White.copy(alpha = 0.5f))  // Highlight
    px(7, 11, collarDark); px(8, 11, collarDark)  // Center shadow

    // Row 12
    for (x in 5..10) px(x, 12, collarDark)

    // Row 13 - shoulders
    for (x in 3..12) px(x, 13, Color(0xFF1A1A2E))
    px(3, 13, Color(0xFF2A2A3E)); px(12, 13, Color(0xFF2A2A3E))

    // Row 14
    for (x in 2..13) px(x, 14, Color(0xFF1A1A2E))

    // === OUTLINE ===
    // Subtle outline around face
    val outlinePixels = listOf(
        4 to 4, 11 to 4,
        3 to 5, 12 to 5,
        3 to 6, 12 to 6,
        4 to 7, 11 to 7,
        4 to 8, 11 to 8,
        5 to 9, 10 to 9
    )
    // (Outline drawn implicitly by pixel placement above)

    // === ACCESSORIES ===
    // Cyan hair clip/tech on left side
    px(1, 3, eyeCyan); px(2, 3, eyeCyan)
    px(1, 4, eyeDark)

    // Small pixel star/sparkle near head
    px(14, 2, Color(0xFFFFFF00).copy(alpha = 0.8f))
    px(15, 2, Color(0xFFFFFF00).copy(alpha = 0.4f))
    px(14, 3, Color(0xFFFFFF00).copy(alpha = 0.4f))
}
