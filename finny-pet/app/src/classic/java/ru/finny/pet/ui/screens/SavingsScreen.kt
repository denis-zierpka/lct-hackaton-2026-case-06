package ru.finny.pet.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.finny.pet.domain.Economy
import ru.finny.pet.domain.Goal
import ru.finny.pet.ui.BigButton
import ru.finny.pet.ui.Chip
import ru.finny.pet.ui.ConfirmDialog
import ru.finny.pet.ui.EmojiCircle
import ru.finny.pet.ui.Emphasis
import ru.finny.pet.ui.GameViewModel
import ru.finny.pet.ui.ScreenScaffold
import ru.finny.pet.ui.SectionCard
import ru.finny.pet.ui.Space

private val amounts = listOf(10, 20, 30, 50)

@Composable
fun SavingsScreen(vm: GameViewModel) {
    val s = vm.state
    val e = vm.economy
    val goal = s.goal
    var amount by rememberSaveable { mutableIntStateOf(20) }
    var confirmWithdraw by rememberSaveable { mutableStateOf(false) }
    var confirmAchieve by rememberSaveable { mutableStateOf(false) }
    var pickGoal by rememberSaveable { mutableStateOf(goal == null) }

    ScreenScaffold(title = "Копилка", onBack = null) {
        SectionCard(container = MaterialTheme.colorScheme.secondaryContainer) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                Icon(Icons.Outlined.Savings, contentDescription = null)
                Text("В копилке", style = MaterialTheme.typography.labelMedium)
            }
            Text("${s.savings}", style = MaterialTheme.typography.displaySmall)
            Text("На балансе: ${s.balance} монет", style = MaterialTheme.typography.bodyLarge)
        }

        if (goal != null && !pickGoal) {
            SectionCard(title = "Цель", icon = Icons.Outlined.Flag) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm + Space.xs)) {
                    EmojiCircle(goal.emoji)
                    Column {
                        Text(goal.title, style = MaterialTheme.typography.titleLarge)
                        Text("Стоит ${goal.price} монет", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                LinearProgressIndicator(
                    progress = { (s.savings.toFloat() / goal.price).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(12.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
                Text("Накоплено ${s.savings}, осталось ${e.remaining(s).coerceAtLeast(0)}.", style = MaterialTheme.typography.bodyLarge)
                val eta = e.goalEta(s)
                Text(
                    when {
                        e.remaining(s) <= 0 -> "Накоплено! Можно забрать цель."
                        eta == null -> "Откладывай каждую неделю — и я посчитаю, сколько недель осталось."
                        else -> "Ты откладываешь в среднем ${"%.0f".format(e.averageDeposit(s))} в неделю — осталось ${Economy.weeks(eta)}."
                    },
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (e.remaining(s) <= 0) BigButton("Забрать цель", Modifier.fillMaxWidth(), icon = Icons.Outlined.Celebration) { confirmAchieve = true }
                BigButton("Сменить цель", Modifier.fillMaxWidth(), emphasis = Emphasis.OUTLINED) { pickGoal = true }
            }
        } else {
            GoalPicker(vm) { pickGoal = false }
        }

        SectionCard(title = "Отложить или забрать", icon = Icons.Outlined.Savings) {
            Text("Сколько монет?", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                amounts.forEach { a -> Chip("$a", selected = amount == a) { amount = a } }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                BigButton("Отложить $amount", Modifier.weight(1f), enabled = s.plan.confirmed && amount <= s.balance) { vm.deposit(amount) }
                BigButton("Забрать $amount", Modifier.weight(1f), emphasis = Emphasis.OUTLINED, enabled = amount <= s.savings) { confirmWithdraw = true }
            }
            if (!s.plan.confirmed) Text("Откладывать можно после того, как составишь план.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (s.achievedGoals.isNotEmpty()) {
            SectionCard(title = "Достигнутые цели", icon = Icons.Outlined.EmojiEvents) {
                s.achievedGoals.forEach { Text("${it.emoji} ${it.title} — ${it.price} монет", style = MaterialTheme.typography.bodyLarge) }
            }
        }
    }

    if (confirmWithdraw) {
        ConfirmDialog(
            title = "Забрать $amount из копилки?",
            lines = e.withdrawPreview(s, amount),
            confirmText = "Забрать",
            icon = Icons.Outlined.Savings,
            onConfirm = { confirmWithdraw = false; vm.withdraw(amount) },
            onDismiss = { confirmWithdraw = false },
        )
    }
    if (confirmAchieve && goal != null) {
        ConfirmDialog(
            title = "Забрать «${goal.title}»?",
            lines = listOf("Из копилки уйдёт ${goal.price} монет, останется ${s.savings - goal.price}.", "Питомец очень обрадуется!"),
            confirmText = "Забрать",
            icon = Icons.Outlined.Celebration,
            onConfirm = { confirmAchieve = false; vm.achieveGoal() },
            onDismiss = { confirmAchieve = false },
        )
    }
}

@Composable
private fun GoalPicker(vm: GameViewModel, onDone: () -> Unit) {
    val c = vm.content
    var customTitle by rememberSaveable { mutableStateOf(c.customGoal.titles.first()) }
    var customPrice by rememberSaveable { mutableIntStateOf(c.customGoal.prices.first()) }
    Text("Выбери цель", style = MaterialTheme.typography.titleLarge)
    c.goals.forEach { g ->
        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm + Space.xs)) {
                EmojiCircle(g.emoji)
                Column(Modifier.weight(1f)) {
                    Text(g.title, style = MaterialTheme.typography.titleMedium)
                    Text("${g.price} монет", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                BigButton("Копить", emphasis = Emphasis.TONAL) { vm.chooseGoal(Goal(g.id, g.title, g.emoji, g.price)); onDone() }
            }
        }
    }
    SectionCard(title = "Своя цель", icon = Icons.Outlined.Flag) {
        Text("Что это?", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            c.customGoal.titles.forEach { t -> Chip(t, selected = customTitle == t) { customTitle = t } }
        }
        Text("Сколько стоит?", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            c.customGoal.prices.forEach { p -> Chip("$p", selected = customPrice == p) { customPrice = p } }
        }
        BigButton("Копить на «$customTitle» за $customPrice", Modifier.fillMaxWidth()) {
            vm.chooseGoal(vm.economy.customGoal(customTitle, customPrice)); onDone()
        }
    }
    if (vm.state.goal != null) BigButton("Оставить текущую цель", Modifier.fillMaxWidth(), emphasis = Emphasis.OUTLINED) { onDone() }
}
