package ru.finny.pet.game.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ru.finny.pet.PetSprites
import ru.finny.pet.R
import ru.finny.pet.domain.Face
import ru.finny.pet.game.PetAct
import ru.finny.pet.game.PetActionState
import kotlin.math.sin

/**
 * The pet in the game: a pre-rendered 3D sprite with idle breathing, blinking, a happy hop, and short
 * actions (eating, washing, playing) that show a prop in front of it. Tap → [onTap] (chat / petting).
 * With [seen] a hop or action plays once: a key already shown is skipped when the sprite comes back.
 */
@Composable
fun PetSprite(
    speciesId: String,
    colorId: String,
    stage: Int,
    face: Face,
    animate: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    bounceKey: Int = 0,
    action: PetAct? = null,
    actionKey: Int = 0,
    description: String = "",
    seen: PetActionState? = null,
    onTap: (() -> Unit)? = null,
) {
    val t = rememberInfiniteTransition(label = "pet")
    val bob = if (animate) t.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart), label = "bob").value else 0f
    val blinkPhase = if (animate) t.animateFloat(0f, 1f, infiniteRepeatable(tween(3800, easing = LinearEasing), RepeatMode.Restart), label = "blink").value else 0f
    val hop = remember { Animatable(0f) }
    val tilt = remember { Animatable(0f) }
    var acting by remember { mutableStateOf<PetAct?>(null) }
    var chew by remember { mutableStateOf(false) }

    LaunchedEffect(bounceKey) {
        if (bounceKey > 0 && bounceKey != seen?.shownBounce) {
            seen?.shownBounce = bounceKey
            if (!animate) return@LaunchedEffect
            hop.snapTo(0f)
            hop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
            hop.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium))
        }
    }
    LaunchedEffect(actionKey) {
        if (actionKey > 0 && action != null && actionKey != seen?.shownAction) {
            seen?.shownAction = actionKey
            acting = action
            if (animate) {
                repeat(if (action == PetAct.EAT || action == PetAct.WASH) 6 else 3) {
                    chew = true; tilt.animateTo(if (it % 2 == 0) 1f else -1f, tween(140)); chew = false; delay(90)
                }
                tilt.animateTo(0f, spring())
            } else delay(1200)
            acting = null
        }
    }

    val blinking = (blinkPhase in 0.92f..0.96f && face != Face.SAD) || chew
    val frame = if (acting == PetAct.SLEEP) "blink" else if (blinking) "blink" else face.name.lowercase()
    val stageScale = when (stage) { 0 -> 0.86f; 1 -> 0.93f; else -> 1f }
    val breathe = 1f + sin(bob * 2 * Math.PI).toFloat() * 0.015f
    val lift = hop.value
    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier
            .size(size)
            .semantics { contentDescription = description; if (onTap != null) role = Role.Button }
            .then(if (onTap != null) Modifier.clickable(interactionSource = interaction, indication = null, onClick = onTap) else Modifier),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val s = this.size.minDimension
            val w = s * (0.48f - lift * 0.12f) * stageScale
            drawOval(Color.Black.copy(alpha = 0.16f - lift * 0.06f), Offset((s - w) / 2, s * 0.835f), Size(w, s * 0.055f))
        }
        Image(
            painter = painterResource(PetSprites.id(speciesId, colorId, stage, frame)),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0.5f, 0.86f)
                    scaleX = stageScale * (1f + lift * 0.04f) / breathe
                    scaleY = stageScale * (1f + lift * 0.04f) * breathe
                    translationY = -lift * size.toPx() * 0.1f
                    rotationZ = tilt.value * 4f
                },
        )
        // prop shown during an action
        val prop = when (acting) {
            PetAct.EAT -> R.drawable.item_food_basic
            PetAct.WASH -> R.drawable.item_care_shampoo
            PetAct.PLAY -> R.drawable.item_fun_ball
            else -> null
        }
        if (prop != null) {
            Image(
                painterResource(prop), contentDescription = null,
                modifier = Modifier.align(Alignment.BottomEnd).size(size * 0.36f).graphicsLayer { translationY = -size.toPx() * 0.06f; rotationZ = -tilt.value * 8f },
            )
        }
    }
}

/** Registers the centre of this composable as a particle target with the given name. */
fun Modifier.particleTarget(controller: ParticleController, name: String): Modifier =
    onGloballyPositioned { c ->
        val p = c.positionInRoot()
        controller.target(name, Offset(p.x + c.size.width / 2f, p.y + c.size.height / 2f))
    }
