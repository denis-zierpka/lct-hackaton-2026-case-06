package ru.finny.pet.game.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ru.finny.pet.game.LocalAnimate

enum class ButtonStyle { PRIMARY, GOLD, GREEN, MAGENTA, PAPER, GHOST }

private fun ButtonStyle.fill(): Brush = when (this) {
    ButtonStyle.PRIMARY -> Brush.verticalGradient(listOf(Color(0xFF7A2BA6), G.purple))
    ButtonStyle.GOLD -> Brush.verticalGradient(listOf(Color(0xFFFFE08A), G.gold))
    ButtonStyle.GREEN -> Brush.verticalGradient(listOf(Color(0xFF5FD48F), G.green))
    ButtonStyle.MAGENTA -> Brush.verticalGradient(listOf(Color(0xFFFF5C8A), G.magenta))
    ButtonStyle.PAPER -> Brush.verticalGradient(listOf(Color.White, G.paperTint))
    ButtonStyle.GHOST -> Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.18f), Color.White.copy(alpha = 0.12f)))
}

private fun ButtonStyle.edge(): Color = when (this) {
    ButtonStyle.PRIMARY -> G.purpleDeep
    ButtonStyle.GOLD -> G.goldDark
    ButtonStyle.GREEN -> G.greenDark
    ButtonStyle.MAGENTA -> Color(0xFFB8003C)
    ButtonStyle.PAPER -> Color(0xFFD8D3E2)
    ButtonStyle.GHOST -> Color.White.copy(alpha = 0.25f)
}

private fun ButtonStyle.content(): Color = when (this) {
    ButtonStyle.GOLD, ButtonStyle.PAPER -> G.purpleDeep
    else -> Color.White
}

/**
 * Chunky game button: gradient top, darker 3D edge below, springs down when pressed.
 * Face ≥ [minHeight] tall, keep it ≥ 48 dp (ТЗ 3.6: touch targets ≥ 48 dp).
 * [selected] turns it into one option of a choice: magenta + «✓» when chosen, paper otherwise, announced as selected.
 * [tight] is for a one-word label in a narrow tab: 14 sp and 8 dp side paddings keep the word on one line.
 * [centered] puts the label in the middle of a button wider than its label (title screen); by default it sits at the start, like a list row.
 */
@Composable
fun GameButton(
    text: String,
    modifier: Modifier = Modifier,
    style: ButtonStyle = ButtonStyle.PRIMARY,
    enabled: Boolean = true,
    icon: Painter? = null,
    iconSize: Dp = 28.dp,
    minHeight: Dp = 56.dp,
    selected: Boolean? = null,
    tight: Boolean = false,
    centered: Boolean = false,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed && enabled) 1f else 0f, if (LocalAnimate.current) spring(stiffness = Spring.StiffnessHigh) else snap(), label = "press")
    val edge = 5.dp
    val shape = RoundedCornerShape(minHeight / 2)
    val look = when (selected) { null -> style; true -> ButtonStyle.MAGENTA; false -> ButtonStyle.PAPER }
    val edgeColor = look.edge()
    Box(
        modifier
            .graphicsLayer { alpha = if (enabled) 1f else 0.5f }
            .then(
                if (selected == null) Modifier.clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
                else Modifier.selectable(selected, interaction, null, enabled, Role.RadioButton, onClick),
            )
            .drawBehind {
                // 3D edge under the face
                val e = edge.toPx()
                drawRoundRect(edgeColor, topLeft = androidx.compose.ui.geometry.Offset(0f, e), size = androidx.compose.ui.geometry.Size(size.width, size.height - e), cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2))
            }
            .padding(bottom = edge),
        contentAlignment = if (centered) Alignment.Center else Alignment.TopStart,
    ) {
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer { translationY = press * edge.toPx() * 0.8f }
                .background(look.fill(), shape)
                .border(1.dp, Color.White.copy(alpha = 0.35f), shape),
        )
        Box(
            Modifier
                .defaultMinSize(minHeight = minHeight)
                .graphicsLayer { translationY = press * edge.toPx() * 0.8f }
                .padding(horizontal = if (tight) 8.dp else 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            CompositionLocalProvider(LocalContentColor provides look.content()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    if (icon != null) {
                        Image(icon, contentDescription = null, modifier = Modifier.size(iconSize))
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(text, style = if (tight) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge, color = look.content(), textAlign = TextAlign.Center, maxLines = 2)
                }
            }
        }
        if (selected == true) CheckBadge()
    }
}

/** «✓» on the chosen option: a choice is never shown by colour alone (ТЗ 3.6). TalkBack hears «selected» from the option itself. */
@Composable
fun BoxScope.CheckBadge(alignment: Alignment = Alignment.TopEnd) {
    Box(Modifier.align(alignment).size(22.dp).background(G.green, CircleShape).border(2.dp, Color.White, CircleShape).clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
        Text("✓", style = MaterialTheme.typography.labelSmall, color = Color.White)
    }
}

/**
 * Round icon button with a 3D prop image and a label under it: the room's action buttons.
 * [tight] drops the side paddings so a narrow portrait row still shows every label in full.
 */
@Composable
fun PropButton(
    label: String,
    icon: Painter,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    badge: Int = 0,
    tint: Color = Color.White,
    showLabel: Boolean = true,
    tight: Boolean = false,
    description: String = label,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, if (LocalAnimate.current) spring(dampingRatio = Spring.DampingRatioMediumBouncy) else snap(), label = "scale")
    Column(
        modifier
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = if (badge > 0) "$description, $badge" else description }
            .padding(horizontal = if (tight) 0.dp else 2.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(size).graphicsLayer { scaleX = scale; scaleY = scale }, contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(size).shadow(6.dp, CircleShape, ambientColor = G.purpleDeep, spotColor = G.purpleDeep)
                    .background(Brush.verticalGradient(listOf(Color.White, G.pink)), CircleShape)
                    .border(3.dp, Color.White, CircleShape),
            )
            Image(icon, contentDescription = null, modifier = Modifier.size(size * 0.78f))
            if (badge > 0) {
                Box(
                    Modifier.align(Alignment.TopEnd).size(24.dp).background(G.magenta, CircleShape).border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Text("$badge", style = MaterialTheme.typography.labelSmall, color = Color.White) }
            }
        }
        if (showLabel) {
            Spacer(Modifier.height(2.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = tint, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.background(G.purpleDeep.copy(alpha = 0.55f), RoundedCornerShape(50)).padding(horizontal = if (tight) 1.dp else 4.dp, vertical = 2.dp))
        }
    }
}

/** White rounded panel floating over the room. */
@Composable
fun Panel(modifier: Modifier = Modifier, color: Color = G.paper, padding: Dp = 16.dp, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .shadow(10.dp, RoundedCornerShape(G.radius), ambientColor = G.purpleDeep.copy(alpha = 0.5f), spotColor = G.purpleDeep.copy(alpha = 0.5f))
            .background(color, RoundedCornerShape(G.radius))
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

/** Pill with a prop icon and an animated number: coins, savings, week. [compact] fits two chips and three buttons into 360 dp. */
@Composable
fun HudChip(icon: Painter, value: Int, label: String, modifier: Modifier = Modifier, suffix: String = "", compact: Boolean = false) {
    val shown by animateIntAsState(value, if (LocalAnimate.current) tween(600) else snap(), label = "hud")
    Row(
        modifier
            .clearAndSetSemantics { contentDescription = "$label $value" }
            .shadow(4.dp, RoundedCornerShape(50), ambientColor = G.purpleDeep, spotColor = G.purpleDeep)
            .background(Color.White.copy(alpha = 0.92f), RoundedCornerShape(50))
            .padding(start = if (compact) 4.dp else 6.dp, end = if (compact) 10.dp else 14.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(icon, contentDescription = null, modifier = Modifier.size(if (compact) 28.dp else 34.dp))
        Spacer(Modifier.width(if (compact) 4.dp else 6.dp))
        Text("$shown$suffix", style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium, color = G.purpleDeep)
    }
}

/** Rounded gradient bar with a label and a number: never colour alone (ТЗ 3.6). TalkBack hears it once; a label that already has numbers («Тема: 0 из 3») is read as is. */
@Composable
fun GameBar(label: String, value: Int, color: Color, modifier: Modifier = Modifier, max: Int = 100, icon: Painter? = null, dark: Boolean = false) {
    val p by animateFloatAsState((value.toFloat() / max).coerceIn(0f, 1f), if (LocalAnimate.current) spring(stiffness = Spring.StiffnessLow) else snap(), label = "bar")
    val textColor = if (dark) Color.White else G.ink
    Column(modifier.clearAndSetSemantics { contentDescription = if (label.any(Char::isDigit)) label else "$label $value из $max" }, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        if (label.isNotEmpty()) Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) { Image(icon, contentDescription = null, modifier = Modifier.size(22.dp)); Spacer(Modifier.width(6.dp)) }
            Text(label, style = MaterialTheme.typography.labelMedium, color = textColor, modifier = Modifier.weight(1f))
            Text("$value", style = MaterialTheme.typography.labelLarge, color = textColor)
        }
        Box(Modifier.fillMaxWidth().height(12.dp).background((if (dark) Color.White else G.ink).copy(alpha = 0.15f), RoundedCornerShape(6.dp))) {
            Box(Modifier.fillMaxWidth(p).height(12.dp).background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.8f), color)), RoundedCornerShape(6.dp)))
        }
    }
}

/** Text that types itself out, for the pet's speech. Skips animation when [animate] is false. */
@Composable
fun TypewriterText(text: String, animate: Boolean, modifier: Modifier = Modifier, color: Color = G.ink, onDone: () -> Unit = {}) {
    var shown by remember(text) { mutableIntStateOf(if (animate) 0 else text.length) }
    LaunchedEffect(text, animate) {
        if (animate) {
            while (shown < text.length) { delay(18); shown++ }
            onDone()
        } else onDone()
    }
    Text(text.take(shown), style = MaterialTheme.typography.bodyLarge, color = color, modifier = modifier)
}

/** Rounded speech bubble with a small tail at the bottom-left; the pet talks through it. */
@Composable
fun SpeechBubble(modifier: Modifier = Modifier, tailAtStart: Boolean = true, tail: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    Box(modifier) {
        Column(
            Modifier
                .shadow(8.dp, RoundedCornerShape(20.dp), ambientColor = G.purpleDeep, spotColor = G.purpleDeep)
                .background(Color.White, RoundedCornerShape(20.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
        if (tail) Canvas(Modifier.align(if (tailAtStart) Alignment.BottomStart else Alignment.BottomEnd).padding(start = 28.dp, end = 28.dp).size(22.dp, 14.dp).graphicsLayer { translationY = 12.dp.toPx() }) {
            val p = Path().apply {
                moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width * 0.35f, size.height); close()
            }
            drawPath(p, Color.White)
        }
    }
}

/** Small round "close" button used on every panel: the back action is always at the top-left (ТЗ 3.6). */
@Composable
fun BoxScope.CloseButton(onClick: () -> Unit, label: String = "Назад") {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, if (LocalAnimate.current) spring() else snap(), label = "close")
    Box(
        Modifier.align(Alignment.TopStart).padding(6.dp).size(48.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(4.dp, CircleShape).background(Color.White, CircleShape)
            .semantics { contentDescription = label }
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(20.dp)) {
            val s = 3.dp.toPx()
            drawLine(G.purpleDeep, Offset(size.width, size.height / 2), Offset(0f, size.height / 2), s, androidx.compose.ui.graphics.StrokeCap.Round)
            drawLine(G.purpleDeep, Offset(0f, size.height / 2), Offset(size.width * 0.45f, 0f), s, androidx.compose.ui.graphics.StrokeCap.Round)
            drawLine(G.purpleDeep, Offset(0f, size.height / 2), Offset(size.width * 0.45f, size.height), s, androidx.compose.ui.graphics.StrokeCap.Round)
        }
    }
}

@Composable
fun RowScope.Weight(w: Float = 1f) = Spacer(Modifier.weight(w))
