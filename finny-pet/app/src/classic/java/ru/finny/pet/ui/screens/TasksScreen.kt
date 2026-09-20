package ru.finny.pet.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.finny.pet.domain.TaskType
import ru.finny.pet.domain.Theme
import ru.finny.pet.ui.BigButton
import ru.finny.pet.ui.Emphasis
import ru.finny.pet.ui.FinnyColors
import ru.finny.pet.ui.GameViewModel
import ru.finny.pet.ui.Screen
import ru.finny.pet.ui.ScreenScaffold
import ru.finny.pet.ui.SectionCard
import ru.finny.pet.ui.Space
import ru.finny.pet.ui.TonalCircle

fun Theme.icon(): ImageVector = when (this) {
    Theme.BUDGET -> Icons.AutoMirrored.Outlined.EventNote
    Theme.SAVINGS -> Icons.Outlined.Savings
    Theme.SHOPPING -> Icons.Outlined.ShoppingCart
}

@Composable
fun TasksScreen(vm: GameViewModel) {
    val s = vm.state
    val e = vm.economy
    val available = e.availableTasks(s)
    val done = e.completedTasks(s)
    ScreenScaffold(title = "Задания", onBack = null) {
        SectionCard(container = MaterialTheme.colorScheme.tertiaryContainer) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                Icon(Icons.Outlined.Paid, contentDescription = null)
                Text("Верный ответ +${e.rules.rewardCorrect}, попытка +${e.rules.rewardWrong} монет", style = MaterialTheme.typography.titleSmall)
            }
            Text("Задание — это история с выбором. Объяснение будет в любом случае.", style = MaterialTheme.typography.bodyMedium)
        }
        if (available.isEmpty()) {
            SectionCard {
                Text(if (done.size == vm.content.tasks.size) "Все задания выполнены! Ты молодец." else "Новые задания откроются на следующей неделе.", style = MaterialTheme.typography.bodyLarge)
            }
        }
        Theme.entries.forEach { theme ->
            val list = available.filter { it.theme == theme }
            if (list.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm), modifier = Modifier.padding(top = Space.sm)) {
                    TonalCircle(size = 36.dp) { Icon(theme.icon(), contentDescription = null, modifier = Modifier.size(20.dp)) }
                    Text(theme.title, style = MaterialTheme.typography.titleMedium)
                }
                list.forEach { t ->
                    SectionCard {
                        Text(t.title, style = MaterialTheme.typography.titleMedium)
                        Text(t.situation, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        BigButton("Решить", Modifier.fillMaxWidth(), emphasis = Emphasis.TONAL) { vm.navigate(Screen.Task(t.id)) }
                    }
                }
            }
        }
        if (done.isNotEmpty()) {
            SectionCard(title = "Выполнено", icon = Icons.Filled.CheckCircle) {
                done.forEachIndexed { i, (t, r) ->
                    if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ListItem(
                        leadingContent = {
                            Icon(
                                if (r.correct) Icons.Filled.CheckCircle else Icons.Outlined.Circle, contentDescription = if (r.correct) "верно" else "попытка",
                                tint = if (r.correct) FinnyColors.success else MaterialTheme.colorScheme.outline,
                            )
                        },
                        headlineContent = { Text(t.title, style = MaterialTheme.typography.bodyLarge) },
                        supportingContent = { Text("${t.theme.title} · неделя ${r.period}", style = MaterialTheme.typography.bodyMedium) },
                        trailingContent = { Text("+${r.reward}", style = MaterialTheme.typography.titleMedium) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
            }
        }
    }
}

@Composable
fun TaskScreen(vm: GameViewModel, taskId: String) {
    val t = vm.content.task(taskId)
    var number by rememberSaveable { mutableStateOf("") }
    ScreenScaffold(title = t.title, onBack = vm::back) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            Icon(t.theme.icon(), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(t.theme.title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        SectionCard(container = MaterialTheme.colorScheme.tertiaryContainer, icon = Icons.Outlined.Lightbulb, title = "Ситуация") {
            Text(t.situation, style = MaterialTheme.typography.bodyLarge)
        }
        when (t.type) {
            TaskType.CHOICE -> {
                Text("Что выберешь?", style = MaterialTheme.typography.titleMedium)
                t.options.forEachIndexed { i, opt ->
                    OutlinedButton(onClick = { vm.answerChoice(t.id, i) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                        Text(opt.text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = Space.sm))
                    }
                }
            }
            TaskType.NUMBER -> {
                Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                    OutlinedTextField(
                        value = number,
                        onValueChange = { v -> if (v.length <= 4 && v.all { it.isDigit() }) number = v },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Ответ (число)") },
                        textStyle = MaterialTheme.typography.headlineMedium,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = MaterialTheme.shapes.medium,
                    )
                    BigButton("Ответить", Modifier.fillMaxWidth(), enabled = number.isNotBlank()) { vm.answerNumber(t.id, number.toIntOrNull()) }
                }
            }
        }
    }
}
