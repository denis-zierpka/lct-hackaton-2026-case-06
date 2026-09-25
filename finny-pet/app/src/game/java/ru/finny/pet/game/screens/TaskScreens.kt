package ru.finny.pet.game.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.finny.pet.R
import ru.finny.pet.domain.TaskType
import ru.finny.pet.domain.Theme
import ru.finny.pet.game.GameViewModel
import ru.finny.pet.game.LocalAnimate
import ru.finny.pet.game.LocalLayout
import ru.finny.pet.game.Screen
import ru.finny.pet.game.ui.ButtonStyle
import ru.finny.pet.game.ui.G
import ru.finny.pet.game.ui.GameButton
import ru.finny.pet.game.ui.GameTextField
import ru.finny.pet.game.ui.PetSprite
import ru.finny.pet.game.ui.SpeechBubble
import ru.finny.pet.game.ui.TypewriterText

fun Theme.icon(): Int = when (this) {
    Theme.BUDGET -> R.drawable.ui_jar
    Theme.SAVINGS -> R.drawable.ui_piggy
    Theme.SHOPPING -> R.drawable.ui_bag
}

fun Theme.color(): Color = when (this) {
    Theme.BUDGET -> G.lavender
    Theme.SAVINGS -> G.magenta
    Theme.SHOPPING -> G.gold
}

@Composable
fun TasksScreen(vm: GameViewModel) {
    val s = vm.state
    val e = vm.economy
    val available = e.availableTasks(s)
    val done = e.completedTasks(s)
    PanelScreen(vm, "Задания") {
        Row(Modifier.fillMaxWidth().background(G.lavenderLight, RoundedCornerShape(16.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Image(painterResource(R.drawable.ui_coin), null, Modifier.size(36.dp))
            Column {
                Text("Верный ответ +${e.rules.rewardCorrect}, попытка +${e.rules.rewardWrong} монет", style = MaterialTheme.typography.titleSmall, color = G.purpleDeep)
                Text("Задание — это история с выбором. Объяснение будет в любом случае.", style = MaterialTheme.typography.bodySmall, color = G.inkSoft)
            }
        }
        if (available.isEmpty()) {
            Text(if (done.size == vm.content.tasks.size) "Все задания выполнены! Ты молодец." else "Новые задания откроются на следующей неделе.", style = MaterialTheme.typography.bodyLarge, color = G.ink)
        }
        Theme.entries.forEach { theme ->
            val list = available.filter { it.theme == theme }
            if (list.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Image(painterResource(theme.icon()), null, Modifier.size(36.dp))
                    Text(theme.title, style = MaterialTheme.typography.titleMedium, color = G.purpleDeep)
                }
                list.forEach { t ->
                    Row(Modifier.fillMaxWidth().background(G.paperTint, RoundedCornerShape(18.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(t.title, style = MaterialTheme.typography.titleSmall, color = G.ink)
                            Text(t.situation, style = MaterialTheme.typography.bodySmall, color = G.inkSoft, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        GameButton("Решить", style = ButtonStyle.PRIMARY, minHeight = 48.dp) { vm.navigate(Screen.Task(t.id)) }
                    }
                }
            }
        }
        if (done.isNotEmpty()) {
            Label("Выполнено")
            done.forEach { (t, r) ->
                Row(Modifier.fillMaxWidth().background(G.paperTint, RoundedCornerShape(14.dp)).padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(30.dp).background(if (r.correct) G.green else G.inkSoft, RoundedCornerShape(50)), contentAlignment = Alignment.Center) {
                        Text(if (r.correct) "✓" else "·", style = MaterialTheme.typography.labelLarge, color = Color.White)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(t.title, style = MaterialTheme.typography.bodyLarge, color = G.ink)
                        Text("${t.theme.title} · неделя ${r.period}", style = MaterialTheme.typography.bodySmall, color = G.inkSoft)
                    }
                    Text("+${r.reward}", style = MaterialTheme.typography.titleSmall, color = G.purpleDeep)
                }
            }
        }
    }
}

/** The task as a scene: the pet tells the situation in a bubble, the answers are big buttons. */
@Composable
fun TaskScreen(vm: GameViewModel, taskId: String) {
    val t = vm.content.task(taskId)
    val s = vm.state
    val pet = s.pet
    val layout = LocalLayout.current
    var number by rememberSaveable { mutableStateOf("") }
    PanelScreen(vm, t.title) {
        val story: @Composable () -> Unit = {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (pet != null) PetSprite(pet.speciesId, pet.colorId, vm.economy.stageIndex(pet.growth), vm.face, LocalAnimate.current, size = if (layout.compact) 120.dp else 150.dp, description = "")
                SpeechBubble(Modifier.weight(1f).padding(bottom = 30.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Image(painterResource(t.theme.icon()), null, Modifier.size(24.dp))
                        Text(t.theme.title, style = MaterialTheme.typography.labelMedium, color = t.theme.color())
                    }
                    TypewriterText(t.situation, animate = LocalAnimate.current)
                }
            }
        }
        val answers: @Composable () -> Unit = {
            when (t.type) {
                TaskType.CHOICE -> {
                    Label("Что выберешь?")
                    t.options.forEachIndexed { i, opt -> GameButton(opt.text, Modifier.fillMaxWidth(), style = ButtonStyle.PAPER, minHeight = 56.dp) { vm.answerChoice(t.id, i) } }
                }
                TaskType.NUMBER -> {
                    Label("Введи число")
                    GameTextField(number, { v -> if (v.length <= 4 && v.all { it.isDigit() }) number = v }, Modifier.widthIn(max = 260.dp), hint = "Ответ", number = true, maxLength = 4, big = true)
                    GameButton("Ответить", Modifier.fillMaxWidth(), style = ButtonStyle.GOLD, enabled = number.isNotBlank()) { vm.answerNumber(t.id, number.toIntOrNull()) }
                }
            }
        }
        if (layout.landscape) Adaptive(left = { story() }, right = { answers() }, leftWeight = 1.1f, rightWeight = 1f) else { story(); answers() }
    }
}
