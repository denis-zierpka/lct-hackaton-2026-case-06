package ru.finny.pet.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.finny.pet.ui.BigButton
import ru.finny.pet.ui.EmojiCircle
import ru.finny.pet.ui.FinnyColors
import ru.finny.pet.ui.GameViewModel
import ru.finny.pet.ui.ScreenScaffold
import ru.finny.pet.ui.SectionCard
import ru.finny.pet.ui.Space

private const val STEP = 10

@Composable
fun PlanScreen(vm: GameViewModel) {
    val s = vm.state
    ScreenScaffold(title = "План на неделю", onBack = null) {
        if (s.plan.confirmed) {
            ConfirmedPlan(vm)
            return@ScreenScaffold
        }
        // The draft lives in GameState (unconfirmed plan), so leaving the screen keeps it.
        val plan = s.plan
        val total = plan.total
        val rest = s.balance - total
        val canAdd = rest >= STEP

        SectionCard(container = MaterialTheme.colorScheme.primaryContainer) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                Icon(Icons.Outlined.Paid, contentDescription = null)
                Text("У тебя ${s.balance} монет", style = MaterialTheme.typography.titleMedium)
            }
            Text("Реши заранее, сколько на что. Потом сравним план с тем, что получилось.", style = MaterialTheme.typography.bodyMedium)
        }
        PlanRow("🍎", "Обязательное", "Еда и уход — без них никак", plan.mandatory, s.balance, MaterialTheme.colorScheme.primary, canAdd) { vm.setPlan(it, plan.optional, plan.savings) }
        PlanRow("🎁", "Желаемое", "Игрушки — приятно, но можно и без них", plan.optional, s.balance, MaterialTheme.colorScheme.tertiary, canAdd) { vm.setPlan(plan.mandatory, it, plan.savings) }
        PlanRow("🐷", "Копилка", "Откладываю на цель", plan.savings, s.balance, MaterialTheme.colorScheme.secondary, canAdd) { vm.setPlan(plan.mandatory, plan.optional, it) }

        SectionCard(container = if (rest < 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer) {
            Text("Распределено: $total из ${s.balance}", style = MaterialTheme.typography.titleMedium)
            Text(if (rest < 0) "Не хватает ${-rest} монет — уменьши что-нибудь" else "Остаток вне плана: $rest", style = MaterialTheme.typography.bodyLarge)
        }
        BigButton("Подтвердить план", modifier = Modifier.fillMaxWidth(), enabled = rest >= 0 && total > 0, icon = Icons.Filled.Check) { vm.confirmPlan() }
        Text("После подтверждения план нельзя менять до конца недели — так и в жизни.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PlanRow(emoji: String, title: String, hint: String, value: Int, max: Int, color: Color, canAdd: Boolean, onChange: (Int) -> Unit) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm + Space.xs)) {
            EmojiCircle(emoji, size = 48.dp)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(hint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            FilledTonalIconButton(onClick = { onChange((value - STEP).coerceAtLeast(0)) }, enabled = value > 0, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Filled.Remove, contentDescription = "Меньше на $STEP")
            }
            Text("$value", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            FilledTonalIconButton(onClick = { onChange(value + STEP) }, enabled = canAdd, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Filled.Add, contentDescription = "Больше на $STEP")
            }
        }
        LinearProgressIndicator(
            progress = { if (max == 0) 0f else (value.toFloat() / max).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
    }
}

/** Plan vs. fact for the running week (2.5.5: shown after confirmation). */
@Composable
fun ConfirmedPlan(vm: GameViewModel) {
    val s = vm.state
    Text("План принят. Вот как идут дела:", style = MaterialTheme.typography.bodyLarge)
    PlanFactCard(
        rows = listOf(
            Triple("🍎 Обязательное", s.plan.mandatory, s.factMandatory),
            Triple("🎁 Желаемое", s.plan.optional, s.factOptional),
            Triple("🐷 Копилка", s.plan.savings, s.factSavings),
        ),
        savingsRow = 2,
    )
    SectionCard(container = MaterialTheme.colorScheme.primaryContainer) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            Icon(Icons.Outlined.Paid, contentDescription = null)
            Text("Осталось монет: ${s.balance}", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun PlanFactCard(rows: List<Triple<String, Int, Int>>, savingsRow: Int) {
    SectionCard {
        Row(Modifier.fillMaxWidth()) {
            Text("", Modifier.weight(2f))
            Text("План", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Факт", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        rows.forEachIndexed { i, (title, plan, fact) ->
            val bad = if (i == savingsRow) fact < plan else fact > plan
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(2f), style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                Text("$plan", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.End)
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    Text("$fact", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = if (bad) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                    Icon(
                        if (bad) Icons.Outlined.Warning else Icons.Filled.Check, contentDescription = if (bad) "не по плану" else "по плану",
                        tint = if (bad) MaterialTheme.colorScheme.error else FinnyColors.success, modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}
