package ru.finny.pet.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ru.finny.pet.domain.Theme
import ru.finny.pet.ui.BigButton
import ru.finny.pet.ui.ConfirmDialog
import ru.finny.pet.ui.Emphasis
import ru.finny.pet.ui.GameViewModel
import ru.finny.pet.ui.ScreenScaffold
import ru.finny.pet.ui.SectionCard
import ru.finny.pet.ui.Space
import ru.finny.pet.ui.TonalCircle

private val educationalGoals = listOf(
    "Понимать назначение личного бюджета и что расходы не должны превышать доходы.",
    "Различать обязательные и необязательные расходы: необходимое и желаемое.",
    "Планировать простые покупки в условиях ограниченного бюджета.",
    "Ставить краткосрочную финансовую цель и регулярно откладывать.",
    "Оценивать свои финансовые решения и объяснять, к чему они привели.",
)

/** Gate: a multiplication example a 7-year-old is unlikely to solve quickly (ТЗ 2.5.12). */
@Composable
fun ParentScreen(vm: GameViewModel) {
    var unlocked by rememberSaveable { mutableStateOf(false) }
    val a = rememberSaveable { (6..9).random() }
    val b = rememberSaveable { (6..9).random() }
    var answer by rememberSaveable { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }

    ScreenScaffold(title = "Для взрослого", onBack = vm::back) {
        if (!unlocked) {
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm + Space.xs)) {
                    TonalCircle(size = 48.dp) { Icon(Icons.Outlined.Lock, contentDescription = null) }
                    Text("Этот раздел для родителей. Решите пример:", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                }
                Text("$a × $b = ?", style = MaterialTheme.typography.displaySmall)
                OutlinedTextField(
                    value = answer,
                    onValueChange = { v -> if (v.length <= 3 && v.all { it.isDigit() }) { answer = v; wrong = false } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = MaterialTheme.typography.headlineMedium,
                    isError = wrong,
                    supportingText = if (wrong) ({ Text("Неверно, попробуйте ещё раз.") }) else null,
                    shape = MaterialTheme.shapes.medium,
                )
                BigButton("Войти", Modifier.fillMaxWidth(), enabled = answer.isNotBlank(), icon = Icons.Outlined.Lock) {
                    if (answer.toIntOrNull() == a * b) unlocked = true else wrong = true
                }
            }
            return@ScreenScaffold
        }
        ParentPanel(vm)
    }
}

@Composable
private fun ParentPanel(vm: GameViewModel) {
    val s = vm.state
    val e = vm.economy
    var confirm by remember { mutableStateOf<String?>(null) }

    SectionCard(title = "Чему учит приложение", icon = Icons.Outlined.School) {
        educationalGoals.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
        Text("Основа: Единая рамка компетенций по финансовой грамотности — формулировки для младших школьников.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    SectionCard(title = "Прогресс ребёнка", icon = Icons.AutoMirrored.Outlined.TrendingUp) {
        val pet = s.pet
        if (pet == null) {
            Text("Профиль ещё не создан.", style = MaterialTheme.typography.bodyLarge)
        } else {
            Text("Питомец ${pet.name}, стадия «${e.stageTitle(pet.growth)}», игровая неделя ${s.period}.", style = MaterialTheme.typography.bodyLarge)
            Text("Завершено недель: ${s.history.size}. Целей достигнуто: ${s.achievedGoals.size}.", style = MaterialTheme.typography.bodyMedium)
            Theme.entries.forEach { th ->
                val done = e.completedTasks(s).count { it.first.theme == th }
                val total = vm.content.tasks.count { it.theme == th }
                Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(th.title, style = MaterialTheme.typography.bodyMedium)
                        Text("$done из $total", style = MaterialTheme.typography.labelMedium)
                    }
                    LinearProgressIndicator(progress = { if (total == 0) 0f else done.toFloat() / total }, modifier = Modifier.fillMaxWidth().height(6.dp), trackColor = MaterialTheme.colorScheme.surfaceContainerHighest)
                }
            }
            if (s.history.isNotEmpty()) {
                val good = s.history.count { it.score >= 2 }
                Text("Недель с хорошим балансом решений: $good из ${s.history.size}.", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    SectionCard(title = "Настройки", icon = Icons.Outlined.Settings) {
        SettingRow("Демо-режим", "Все задания открыты сразу", s.demo) { vm.setDemo(it) }
        SettingRow("Анимации питомца", "Покачивание и моргание", s.animations) { vm.setAnimations(it) }
    }

    SectionCard(title = "Профиль и данные", icon = Icons.Outlined.Shield) {
        Text("Все данные хранятся только на этом устройстве в одном файле. Приложение не собирает персональные данные, не выходит в интернет и не запрашивает разрешений.", style = MaterialTheme.typography.bodyMedium)
        BigButton("Создать тестовый профиль (демо)", Modifier.fillMaxWidth(), emphasis = Emphasis.TONAL, icon = Icons.Outlined.Science) { confirm = "test" }
        BigButton("Сбросить профиль", Modifier.fillMaxWidth(), emphasis = Emphasis.OUTLINED, enabled = s.hasProfile, icon = Icons.Outlined.Refresh) { confirm = "reset" }
        BigButton("Удалить профиль и данные", Modifier.fillMaxWidth(), emphasis = Emphasis.OUTLINED, enabled = s.hasProfile, icon = Icons.Outlined.DeleteOutline) { confirm = "delete" }
        Text("Версия 1.2.0 · Прототип для конкурса, без рекламы и покупок.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    confirm?.let { kind ->
        val (title, lines) = when (kind) {
            "test" -> "Создать тестовый профиль?" to listOf("Текущий прогресс будет стёрт.", "Включится демо-режим: все задания открыты, недели идут подряд.", "Затем нужно заново создать питомца — как при первом запуске.")
            "reset" -> "Сбросить профиль?" to listOf("Прогресс, покупки и копилка обнулятся.", "Настройка демо-режима сохранится.")
            else -> "Удалить профиль и данные?" to listOf("Файл с данными будет очищен полностью.", "Это действие нельзя отменить.")
        }
        ConfirmDialog(
            title = title, lines = lines, confirmText = "Да, продолжить",
            icon = if (kind == "delete") Icons.Outlined.DeleteOutline else Icons.Outlined.Refresh,
            onConfirm = {
                confirm = null
                when (kind) { "test" -> vm.createTestProfile(); "reset" -> vm.resetProfile(); else -> vm.deleteProfile() }
            },
            onDismiss = { confirm = null },
        )
    }
}

@Composable
private fun SettingRow(label: String, hint: String, value: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(label, style = MaterialTheme.typography.bodyLarge) },
        supportingContent = { Text(hint, style = MaterialTheme.typography.bodyMedium) },
        trailingContent = { Switch(checked = value, onCheckedChange = onChange) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}
