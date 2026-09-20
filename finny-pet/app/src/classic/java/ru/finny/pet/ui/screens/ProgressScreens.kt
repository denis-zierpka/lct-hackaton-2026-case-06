package ru.finny.pet.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ru.finny.pet.domain.PeriodSummary
import ru.finny.pet.domain.Theme
import ru.finny.pet.ui.BigButton
import ru.finny.pet.ui.Emphasis
import ru.finny.pet.ui.FinnyColors
import ru.finny.pet.ui.GameViewModel
import ru.finny.pet.ui.MessageList
import ru.finny.pet.ui.Screen
import ru.finny.pet.ui.ScreenScaffold
import ru.finny.pet.ui.SectionCard
import ru.finny.pet.ui.Space
import ru.finny.pet.ui.TonalCircle

@Composable
fun ProgressScreen(vm: GameViewModel) {
    val s = vm.state
    val e = vm.economy
    val pet = s.pet ?: return
    ScreenScaffold(title = "Прогресс", onBack = vm::back) {
        SectionCard(container = MaterialTheme.colorScheme.primaryContainer, title = "${pet.name}: ${e.stageTitle(pet.growth)}", icon = Icons.Outlined.EmojiEvents) {
            val next = e.nextStageLeft(pet.growth)
            val thresholds = e.rules.stageThresholds
            val stage = e.stageIndex(pet.growth)
            val from = thresholds[stage]
            val to = thresholds.getOrNull(stage + 1)
            if (to != null) {
                LinearProgressIndicator(
                    progress = { ((pet.growth - from).toFloat() / (to - from)).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(10.dp),
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
            }
            Text("Очки роста: ${pet.growth}. " + (next?.let { "До следующей стадии: $it." } ?: "Высшая стадия!"), style = MaterialTheme.typography.bodyLarge)
            Text("Рост даётся за три вещи в неделю: еда и уход куплены, траты по плану, копилка выросла.", style = MaterialTheme.typography.bodyMedium)
        }
        val goal = s.goal
        SectionCard(title = "Цель", icon = Icons.Outlined.Flag) {
            Text(if (goal != null) "${goal.emoji} ${goal.title}: ${s.savings} из ${goal.price} монет" else "Не выбрана", style = MaterialTheme.typography.bodyLarge)
            if (s.achievedGoals.isNotEmpty()) Text("Достигнуто: ${s.achievedGoals.joinToString { it.title }}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SectionCard(title = "Задания по темам", icon = Icons.Outlined.Star) {
            Theme.entries.forEach { th ->
                val total = vm.content.tasks.count { it.theme == th }
                val done = e.completedTasks(s).count { it.first.theme == th }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    TonalCircle(size = 32.dp) { Icon(th.icon(), contentDescription = null, modifier = Modifier.size(18.dp)) }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(th.title, style = MaterialTheme.typography.bodyLarge)
                            Text("$done из $total", style = MaterialTheme.typography.labelLarge)
                        }
                        LinearProgressIndicator(progress = { if (total == 0) 0f else done.toFloat() / total }, modifier = Modifier.fillMaxWidth().height(8.dp), trackColor = MaterialTheme.colorScheme.surfaceContainerHighest)
                    }
                }
            }
        }
        SectionCard(title = "Откуда монеты на этой неделе", icon = Icons.Outlined.History) {
            s.ledger.forEachIndexed { i, l ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ListItem(
                    headlineContent = { Text(l.text, style = MaterialTheme.typography.bodyLarge) },
                    trailingContent = {
                        Text(
                            if (l.amount > 0) "+${l.amount}" else "${l.amount}", style = MaterialTheme.typography.titleMedium,
                            color = if (l.amount >= 0) FinnyColors.success else MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        }
        val last = vm.lastSummary
        if (last != null) {
            Text("Итог недели ${last.period}", style = MaterialTheme.typography.titleLarge)
            SummaryCard(last)
        } else {
            Text("Итог появится после первой завершённой недели.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        BigButton("Справка: что такое бюджет", Modifier.fillMaxWidth(), emphasis = Emphasis.TONAL, icon = Icons.AutoMirrored.Outlined.MenuBook) { vm.navigate(Screen.Glossary) }
    }
}

@Composable
fun SummaryCard(sum: PeriodSummary) {
    SectionCard {
        CheckLine("Еда и уход куплены", sum.mandatoryCovered)
        CheckLine("Траты по плану", sum.planKept)
        CheckLine("Копилка выросла", sum.saved)
        Text("Рост: +${sum.score} (${sum.growthBefore} → ${sum.growthAfter})", style = MaterialTheme.typography.titleMedium)
    }
    PlanFactCard(
        rows = listOf(
            Triple("🍎 Обязательное", sum.plan.mandatory, sum.factMandatory),
            Triple("🎁 Желаемое", sum.plan.optional, sum.factOptional),
            Triple("🐷 Копилка", sum.plan.savings, sum.factSavings),
        ),
        savingsRow = 2,
    )
}

@Composable
private fun CheckLine(text: String, ok: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
        Icon(
            if (ok) Icons.Filled.CheckCircle else Icons.Outlined.Circle, contentDescription = if (ok) "выполнено" else "не выполнено",
            tint = if (ok) FinnyColors.success else MaterialTheme.colorScheme.outline,
        )
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun SummaryScreen(vm: GameViewModel) {
    val sum = vm.lastSummary ?: return // unreachable: Summary is shown only right after endPeriod
    ScreenScaffold(title = "Неделя ${sum.period} завершена", onBack = null) {
        SummaryCard(sum)
        SectionCard(container = MaterialTheme.colorScheme.tertiaryContainer, title = "Что случилось и почему", icon = Icons.Outlined.Lightbulb) {
            MessageList(sum.messages)
        }
        BigButton("Дальше, к неделе ${sum.period + 1}", Modifier.fillMaxWidth(), icon = Icons.AutoMirrored.Filled.ArrowForward) { vm.navigate(Screen.Home) }
    }
}

@Composable
fun GlossaryScreen(vm: GameViewModel) {
    ScreenScaffold(title = "Справка", onBack = vm::back) {
        SectionCard {
            vm.content.glossary.forEachIndexed { i, g ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ListItem(
                    leadingContent = { Icon(Icons.AutoMirrored.Outlined.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    headlineContent = { Text(g.term, style = MaterialTheme.typography.titleMedium) },
                    supportingContent = { Text(g.definition, style = MaterialTheme.typography.bodyLarge) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        }
    }
}
