package ru.finny.pet.game.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.view.SoftwareKeyboardControllerCompat
import ru.finny.pet.BuildConfig
import ru.finny.pet.domain.Theme
import ru.finny.pet.domain.Case
import ru.finny.pet.domain.Economy
import ru.finny.pet.game.GameViewModel
import ru.finny.pet.game.systemAnimates
import ru.finny.pet.game.ui.ButtonStyle
import ru.finny.pet.game.ui.G
import ru.finny.pet.game.ui.GameBar
import ru.finny.pet.game.ui.GameButton
import ru.finny.pet.game.ui.GameTextField

private val educationalGoals = listOf(
    "Понимать назначение личного бюджета и что расходы не должны превышать доходы.",
    "Различать обязательные и необязательные расходы: необходимое и желаемое.",
    "Планировать простые покупки в условиях ограниченного бюджета.",
    "Ставить краткосрочную финансовую цель и регулярно откладывать.",
    "Оценивать свои финансовые решения и объяснять, к чему они привели.",
)

/** Gate: a two-digit multiplication a 7-year-old is unlikely to solve quickly (ТЗ 2.5.12); a wrong answer gets a new example. */
@Composable
fun ParentScreen(vm: GameViewModel) {
    // remember (not rememberSaveable): process death must re-lock the gate, only rotation may keep it open
    var unlocked by remember { mutableStateOf(false) }
    var a by rememberSaveable { mutableIntStateOf((12..19).random()) }
    var b by rememberSaveable { mutableIntStateOf((3..9).random()) }
    var answer by rememberSaveable { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<String?>(null) }
    var bonus by remember { mutableStateOf<Int?>(null) }
    val view = LocalView.current
    Box(Modifier.fillMaxSize()) {
        PanelScreen(vm, "Для взрослого") {
            if (!unlocked) {
                Column(Modifier.widthIn(max = 420.dp).align(Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Этот раздел для родителей. Решите пример:", style = MaterialTheme.typography.bodyLarge, color = G.ink)
                    Text("$a × $b = ?", style = MaterialTheme.typography.displaySmall, color = G.purpleDeep)
                    // clearing the field after a miss echoes "" back: only real typing hides the hint
                    GameTextField(answer, { v -> if (v.length <= 3 && v.all { it.isDigit() }) { answer = v; if (v.isNotEmpty()) wrong = false } }, hint = "Ответ", number = true, maxLength = 3, big = true)
                    if (wrong) Text("Неверно. Вот новый пример.", style = MaterialTheme.typography.bodyMedium, color = G.red)
                    GameButton("Войти", Modifier.fillMaxWidth(), style = ButtonStyle.PRIMARY, enabled = answer.isNotBlank()) {
                        if (answer.toIntOrNull() == a * b) {
                            // the answer field leaves with the gate, but Android keeps its keyboard over the panel
                            view.findFocus()?.clearFocus()
                            SoftwareKeyboardControllerCompat(view).hide()
                            unlocked = true
                            answer = ""
                        }
                        // a wrong answer must not repeat the same pair
                        else { wrong = true; answer = ""; val (oa, ob) = a to b; do { a = (12..19).random(); b = (3..9).random() } while (a == oa && b == ob) }
                    }
                }
                return@PanelScreen
            }
            ParentPanel(vm, onConfirm = { confirm = it }, onBonus = { bonus = it })
        }
        if (unlocked) {
            confirm?.let { kind ->
                val (title, lines) = when (kind) {
                    "test" -> "Создать тестовый профиль?" to listOf("Текущий прогресс будет стёрт.", "Включится демо-режим: все задания открыты, недели идут подряд.", "Затем нужно заново создать питомца — как при первом запуске.")
                    "reset" -> "Сбросить профиль?" to listOf("Питомец, прогресс, покупки и копилка обнулятся.", "Настройки сохранятся.")
                    else -> "Удалить профиль и данные?" to listOf("Файл с данными будет очищен полностью.", "Это действие нельзя отменить.")
                }
                ConfirmPanel(title, lines, "Да, продолжить", danger = kind == "delete", onConfirm = {
                    confirm = null
                    when (kind) { "test" -> vm.createTestProfile(); "reset" -> vm.resetProfile(); else -> vm.deleteProfile() }
                }, onDismiss = { confirm = null })
            }
            bonus?.let { i ->
                val e = vm.economy
                val reason = vm.content.parentBonusReasons[i]
                ConfirmPanel("Начислить ${Economy.coins(e.rules.parentBonusAmount, Case.ACC)}?", listOf("За: «$reason»", "Монеты появятся у ребёнка сразу, в журнале будет запись."), "Начислить",
                    onConfirm = { bonus = null; vm.parentBonus(i) }, onDismiss = { bonus = null })
            }
        }
    }
}

@Composable
private fun ParentPanel(vm: GameViewModel, onConfirm: (String) -> Unit, onBonus: (Int) -> Unit) {
    val s = vm.state
    val e = vm.economy
    val context = LocalContext.current
    val sysAnimates = systemAnimates(context)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Adaptive(left = {
                Label("Чему учит игра")
                educationalGoals.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium, color = G.ink) }
                Text("Основа: Единая рамка компетенций по финансовой грамотности — формулировки для младших школьников.", style = MaterialTheme.typography.bodySmall, color = G.inkSoft)
                Label("Прогресс ребёнка")
                val pet = s.pet
                if (pet == null) Text("Профиль ещё не создан.", style = MaterialTheme.typography.bodyLarge, color = G.ink)
                else {
                    Text("Питомец ${pet.name}, стадия «${e.stageTitle(pet.growth)}», игровая неделя ${s.period}. Завершено недель: ${s.history.size}. Целей достигнуто: ${s.achievedGoals.size}.", style = MaterialTheme.typography.bodyMedium, color = G.ink)
                    Theme.entries.forEach { th ->
                        val done = e.completedTasks(s).count { it.first.theme == th }
                        val total = vm.content.tasks.count { it.theme == th }
                        GameBar("${th.title}: $done из $total", done, th.color(), max = total.coerceAtLeast(1))
                    }
                    if (s.history.isNotEmpty()) Text("Недель с хорошим балансом решений: ${s.history.count { it.score >= 2 }} из ${s.history.size}.", style = MaterialTheme.typography.bodyMedium, color = G.ink)
                }
                // 2.5.12: coins for deeds, limited per week; each grant is a ledger entry the child sees
                Label("Бонус ребёнку")
                val left = e.parentBonusesLeft(s)
                Text("Бонусов в неделю: до ${e.rules.parentBonusPerPeriod}, каждый — +${Economy.coins(e.rules.parentBonusAmount)}. За дела, а не за оценки. Осталось на этой неделе: $left.", style = MaterialTheme.typography.bodyMedium, color = G.ink)
                when {
                    !s.hasProfile -> Text("Бонус можно начислить, когда питомец создан.", style = MaterialTheme.typography.bodyMedium, color = G.inkSoft)
                    left == 0 -> Text("Все бонусы этой недели начислены. Новые — со следующей игровой недели.", style = MaterialTheme.typography.bodyMedium, color = G.inkSoft)
                }
                vm.content.parentBonusReasons.forEachIndexed { i, reason ->
                    GameButton(reason, Modifier.fillMaxWidth(), style = ButtonStyle.PAPER, enabled = s.hasProfile && left > 0, minHeight = 48.dp) { onBonus(i) }
                }
            }, right = {
                Label("Настройки")
                SettingRow("Звуки", "Эффекты при действиях", s.sounds) { vm.setSounds(it) }
                SettingRow("Музыка", "Фоновая мелодия", s.music) { vm.setMusic(it) }
                // off also when the system «remove animations» is on; that case can't be toggled from here
                SettingRow("Анимации", if (sysAnimates) "Движение питомца и эффекты" else "Выключены в настройках Android", s.animations, enabled = sysAnimates) { vm.setAnimations(it) }
                SettingRow("Демо-режим", "Все задания открыты сразу", s.demo) { vm.setDemo(it) }
                Label("Профиль и данные")
                Text("Все данные хранятся только на этом устройстве в одном файле. Игра не собирает персональные данные, не выходит в интернет и не запрашивает разрешений.", style = MaterialTheme.typography.bodySmall, color = G.inkSoft)
                GameButton("Создать тестовый профиль (демо)", Modifier.fillMaxWidth(), style = ButtonStyle.PRIMARY, minHeight = 48.dp) { onConfirm("test") }
                GameButton("Сбросить профиль", Modifier.fillMaxWidth(), style = ButtonStyle.PAPER, enabled = s.hasProfile, minHeight = 48.dp) { onConfirm("reset") }
                GameButton("Удалить профиль и данные", Modifier.fillMaxWidth(), style = ButtonStyle.PAPER, enabled = s.hasProfile, minHeight = 48.dp) { onConfirm("delete") }
                Text("Версия ${BuildConfig.VERSION_NAME} · Прототип для конкурса, без рекламы и покупок.", style = MaterialTheme.typography.bodySmall, color = G.inkSoft)
            })
    }
}

@Composable
private fun SettingRow(label: String, hint: String, value: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    // toggleable on the whole row: TalkBack reads label + state as one node
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).background(G.paperTint, RoundedCornerShape(14.dp))
            .toggleable(value = value, enabled = enabled, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall, color = G.ink)
            Text(hint, style = MaterialTheme.typography.bodySmall, color = G.inkSoft)
        }
        Switch(checked = value, onCheckedChange = null, enabled = enabled, colors = SwitchDefaults.colors(checkedTrackColor = G.purple))
    }
}
