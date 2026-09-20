package ru.finny.pet.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.finny.pet.PetSprites
import ru.finny.pet.domain.Face
import kotlin.math.sin

/**
 * The pet is a pre-rendered 3D sprite (tools/art/pet.py → PetSprites): species × colour × stage × face.
 * Motion is done here: idle breathing, a blink every few seconds (swaps to the "blink" frame),
 * a happy hop when [bounceKey] changes, and a ground shadow that shrinks while the pet is in the air.
 */
@Composable
fun PetView(
    speciesId: String,
    colorId: String,
    stage: Int,
    face: Face,
    animate: Boolean,
    size: Dp = 200.dp,
    bounceKey: Int = 0,
    description: String = "",
) {
    val t = rememberInfiniteTransition(label = "pet")
    val bob = if (animate) t.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart), label = "bob").value else 0f
    val blinkPhase = if (animate) t.animateFloat(0f, 1f, infiniteRepeatable(tween(3800, easing = LinearEasing), RepeatMode.Restart), label = "blink").value else 0f
    val hop = remember { Animatable(0f) }
    LaunchedEffect(bounceKey) {
        if (bounceKey > 0 && animate) {
            hop.snapTo(0f)
            hop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
            hop.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium))
        }
    }
    val blinking = blinkPhase in 0.92f..0.96f && face != Face.SAD
    val frame = if (blinking) "blink" else face.name.lowercase()
    val stageScale = when (stage) { 0 -> 0.86f; 1 -> 0.93f; else -> 1f }
    val breathe = 1f + sin(bob * 2 * Math.PI).toFloat() * 0.015f
    val lift = hop.value

    Box(Modifier.size(size).semantics { contentDescription = description }, contentAlignment = Alignment.BottomCenter) {
        Canvas(Modifier.fillMaxSize()) {
            val s = this.size.minDimension
            // feet sit at ~85% of the sprite frame (one camera for all renders)
            val w = s * (0.48f - lift * 0.12f) * stageScale
            drawOval(Color.Black.copy(alpha = 0.14f - lift * 0.06f), Offset((s - w) / 2, s * 0.835f), Size(w, s * 0.055f))
        }
        Image(
            painter = painterResource(PetSprites.id(speciesId, colorId, stage, frame)),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // sprites share one camera framing: the feet sit at ~86% of the frame height
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.86f)
                    scaleX = stageScale * (1f + lift * 0.04f) / breathe
                    scaleY = stageScale * (1f + lift * 0.04f) * breathe
                    translationY = -lift * s(size) * 0.1f
                },
        )
    }
}

private fun androidx.compose.ui.graphics.GraphicsLayerScope.s(size: Dp) = size.toPx()
