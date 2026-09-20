package ru.finny.pet.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Every screen: M3 top app bar (title, optional back arrow, actions), scrollable column of
 * content on the 8dp grid, content width capped at 640dp on wide windows.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenScaffold(
    title: String,
    onBack: (() -> Unit)?,
    actions: @Composable RowScope.() -> Unit = {},
    fab: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                        }
                    }
                },
                actions = actions,
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = fab,
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Space.md),
                verticalArrangement = Arrangement.spacedBy(Space.sm + Space.xs),
            ) {
                Spacer(Modifier.height(Space.xs))
                content()
                Spacer(Modifier.height(96.dp)) // room for the FAB
            }
        }
    }
}

enum class Emphasis { HIGH, TONAL, OUTLINED }

/** 56dp M3 button (L size): Filled / Filled tonal / Outlined by emphasis. */
@Composable
fun BigButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    emphasis: Emphasis = Emphasis.HIGH,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    val label: @Composable RowScope.() -> Unit = {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(Space.sm))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
    val m = modifier.heightIn(min = 56.dp)
    when (emphasis) {
        Emphasis.HIGH -> Button(onClick = onClick, enabled = enabled, modifier = m, content = label)
        Emphasis.TONAL -> FilledTonalButton(onClick = onClick, enabled = enabled, modifier = m, content = label)
        Emphasis.OUTLINED -> OutlinedButton(onClick = onClick, enabled = enabled, modifier = m, content = label)
    }
}

/** Filled M3 card; the default container is [FinnyColors.card] (white on the light ground). Pass [onClick] for a clickable card. */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    container: Color = FinnyColors.card,
    title: String? = null,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CardDefaults.cardColors(containerColor = container, contentColor = contentColorFor(container))
    val body: @Composable ColumnScope.() -> Unit = {
        Column(Modifier.padding(Space.md), verticalArrangement = Arrangement.spacedBy(Space.sm + Space.xs)) {
            if (title != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    // On plain surfaces the icon takes the primary accent; on tinted containers it inherits the on-colour.
                    val tint = if (container == FinnyColors.card) MaterialTheme.colorScheme.primary else LocalContentColor.current
                    if (icon != null) Icon(icon, contentDescription = null, tint = tint)
                    Text(title, style = MaterialTheme.typography.titleMedium)
                }
            }
            content()
        }
    }
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, colors = colors, content = body)
    } else {
        Card(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, colors = colors, content = body)
    }
}

/**
 * Hero surface in the ЛЦТ purple→magenta gradient with white content — the pet, the week summary,
 * the onboarding cover. Used once per screen at most so the gradient stays a signature, not wallpaper.
 */
@Composable
fun BrandHero(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(BrandGradient)
            .padding(Space.md),
        verticalArrangement = Arrangement.spacedBy(Space.sm + Space.xs),
    ) {
        CompositionLocalProvider(LocalContentColor provides OnBrand) { content() }
    }
}

/** Icon (or emoji) inside a tonal circle — the app's recurring visual motif. */
@Composable
fun TonalCircle(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 48.dp,
    container: Color = MaterialTheme.colorScheme.primaryContainer,
    content: @Composable () -> Unit,
) {
    Box(modifier.size(size).background(container, CircleShape), contentAlignment = Alignment.Center) { content() }
}

@Composable
fun EmojiCircle(emoji: String, size: androidx.compose.ui.unit.Dp = 56.dp, container: Color = MaterialTheme.colorScheme.surfaceContainerHighest) {
    TonalCircle(size = size, container = container) {
        Text(emoji, style = if (size >= 56.dp) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge)
    }
}

/** Label + number + animated bar: colour is never the only signal (ТЗ 3.6). */
@Composable
fun StatRow(label: String, icon: ImageVector, value: Int, color: Color, max: Int = 100, track: Color = MaterialTheme.colorScheme.surfaceContainerHighest) {
    val progress by animateFloatAsState(targetValue = (value.toFloat() / max).coerceIn(0f, 1f), label = "stat")
    Column(Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = "$label $value из $max" }, verticalArrangement = Arrangement.spacedBy(Space.xs)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(Space.sm))
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text("$value", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(10.dp),
            color = color,
            trackColor = track,
        )
    }
}

@Composable
fun MessageList(messages: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        messages.forEach { m ->
            Row {
                Text("•", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(Space.sm))
                Text(m, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
fun FeedbackDialog(title: String, messages: List<String>, icon: ImageVector, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(icon, contentDescription = null) },
        title = { Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center) },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) { MessageList(messages) } },
        confirmButton = { BigButton("Понятно", onClick = onDismiss) },
    )
}

@Composable
fun ConfirmDialog(title: String, lines: List<String>, confirmText: String, icon: ImageVector, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(icon, contentDescription = null) },
        title = { Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center) },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) { MessageList(lines) } },
        confirmButton = { BigButton(confirmText, onClick = onConfirm) },
        dismissButton = { BigButton("Отмена", emphasis = Emphasis.OUTLINED, onClick = onDismiss) },
    )
}

/** M3 filter chip sized for small fingers (48dp tall). */
@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text, style = MaterialTheme.typography.labelLarge) },
        modifier = Modifier.heightIn(min = 48.dp),
        leadingIcon = if (selected) ({ Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }) else null,
    )
}
