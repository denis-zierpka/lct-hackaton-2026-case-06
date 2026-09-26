package ru.finny.pet.game.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import ru.finny.pet.game.GameViewModel
import ru.finny.pet.game.LocalLayout
import ru.finny.pet.game.orNone
import ru.finny.pet.game.ui.ButtonStyle
import ru.finny.pet.game.ui.CloseButton
import ru.finny.pet.game.ui.G
import ru.finny.pet.game.ui.GameButton
import ru.finny.pet.game.ui.Panel
import ru.finny.pet.game.ui.TText

/**
 * A screen shown as a big panel floating over the room: title on top, close button at the top-left,
 * scrolling content inside. Landscape and portrait share the same content; only the panel size differs.
 */
@Composable
fun PanelScreen(vm: GameViewModel, title: String, onBack: () -> Unit = vm::back, scroll: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    val layout = LocalLayout.current
    Box(Modifier.fillMaxSize().background(G.scrim.copy(alpha = 0.35f))) {
        Box(Modifier.fillMaxSize().padding(start = if (layout.landscape) 64.dp else 8.dp, end = if (layout.landscape) 16.dp else 8.dp, top = if (layout.compact) 4.dp else 12.dp, bottom = if (layout.compact) 4.dp else 12.dp), contentAlignment = Alignment.Center) {
            Panel(Modifier.fillMaxSize().widthIn(max = 960.dp), padding = 0.dp) {
                Text(title, style = if (layout.compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall, color = G.purpleDeep, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(start = if (layout.landscape) 8.dp else 56.dp, end = 8.dp, top = if (layout.compact) 8.dp else 14.dp))
                val inner = Modifier.fillMaxSize().padding(horizontal = 16.dp).padding(bottom = 12.dp)
                if (scroll) Column(inner.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
                else Column(inner, verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
            }
        }
        CloseButton(onBack)
    }
}

/** Two columns in landscape, one in portrait. */
@Composable
fun Adaptive(left: @Composable ColumnScope.() -> Unit, right: @Composable ColumnScope.() -> Unit, leftWeight: Float = 1f, rightWeight: Float = 1f) {
    val layout = LocalLayout.current
    if (layout.landscape) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(leftWeight), verticalArrangement = Arrangement.spacedBy(10.dp), content = left)
            Column(Modifier.weight(rightWeight), verticalArrangement = Arrangement.spacedBy(10.dp), content = right)
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { left(); right() }
    }
}

@Composable
fun BoxScope.ConfirmPanel(title: String, lines: List<String>, confirmText: String, onConfirm: () -> Unit, onDismiss: () -> Unit, danger: Boolean = false) =
    Ask(title, lines, listOf("Отмена" to onDismiss, confirmText to onConfirm), onDismiss, primary = 1, danger = danger)

/**
 * A question over the screen: title, lines, buttons of one size in one row (or a column, if more than two).
 * [primary] is the index of the main button; back and a tap outside do [onDismiss].
 */
@Composable
fun BoxScope.Ask(title: String, lines: List<String>, buttons: List<Pair<String, () -> Unit>>, onDismiss: () -> Unit, primary: Int = 0, danger: Boolean = false) {
    BackHandler(onBack = onDismiss)
    Box(Modifier.fillMaxSize().background(G.scrim).pointerInput(Unit) {}) {}
    AnimatedVisibility(visible = true, enter = (fadeIn(tween(200)) + scaleIn(tween(260), initialScale = 0.85f)).orNone(), exit = fadeOut().orNone()) {
        Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Panel(Modifier.widthIn(max = 520.dp).semantics { paneTitle = title }) {
                TText(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() }, color = G.purpleDeep)
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    lines.forEach { m ->
                        Row { Text("•", style = MaterialTheme.typography.bodyLarge, color = G.magenta); Spacer(Modifier.size(8.dp)); TText(m) }
                    }
                }
                val style = { i: Int -> if (i != primary) ButtonStyle.PAPER else if (danger) ButtonStyle.MAGENTA else ButtonStyle.PRIMARY }
                if (buttons.size <= 2) Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    buttons.forEachIndexed { i, (t, f) -> GameButton(t, Modifier.weight(1f).fillMaxHeight(), style = style(i), minHeight = 48.dp, onClick = f) }
                } else buttons.forEachIndexed { i, (t, f) -> GameButton(t, Modifier.fillMaxWidth(), style = style(i), minHeight = 48.dp, onClick = f) }
            }
        }
    }
}

/** Section label inside panels: often a short phrase, so 16 sp (ТЗ 3.6). */
@Composable
fun Label(text: String, color: Color = G.inkSoft) = Text(text, style = MaterialTheme.typography.titleSmall, color = color)
