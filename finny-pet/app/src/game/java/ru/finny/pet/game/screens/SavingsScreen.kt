package ru.finny.pet.game.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.finny.pet.R
import ru.finny.pet.domain.Economy
import ru.finny.pet.domain.Goal
import ru.finny.pet.game.GameViewModel
import ru.finny.pet.game.LocalLayout
import ru.finny.pet.game.ui.ButtonStyle
import ru.finny.pet.game.ui.G
import ru.finny.pet.game.ui.GameBar
import ru.finny.pet.game.ui.GameButton
import ru.finny.pet.game.ui.HudChip
import ru.finny.pet.game.ui.LocalParticles
import ru.finny.pet.game.ui.particleTarget

private val amounts = listOf(10, 20, 30, 50)

@Composable
fun SavingsScreen(vm: GameViewModel) {
    val s = vm.state
    val e = vm.economy
    val goal = s.goal
    val particles = LocalParticles.current
    var amount by rememberSaveable { mutableIntStateOf(20) }
    var confirmWithdraw by rememberSaveable { mutableStateOf(false) }
    var confirmAchieve by rememberSaveable { mutableStateOf(false) }
    var pickGoal by rememberSaveable { mutableStateOf(goal == null) }

    Box {
        PanelScreen(vm, "Копилка") {
            Adaptive(leftWeight = 0.9f, rightWeight = 1.1f, left = {
                Row(Modifier.fillMaxWidth().background(G.pink, RoundedCornerShape(20.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Image(painterResource(R.drawable.ui_piggy), null, Modifier.size(84.dp).particleTarget(particles, "piggy"))
                    Column(Modifier.weight(1f)) {
                        Text("${s.savings}", style = MaterialTheme.typography.displaySmall, color = G.purpleDeep)
                        Text("в копилке", style = MaterialTheme.typography.labelMedium, color = G.purpleDeep)
                    }
                    HudChip(painterResource(R.drawable.ui_coin), s.balance, "На балансе", Modifier.particleTarget(particles, "coins"))
                }
                Label("Отложить или забрать")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    amounts.forEach { a -> GameButton("$a", Modifier.width(64.dp), style = if (amount == a) ButtonStyle.MAGENTA else ButtonStyle.PAPER, minHeight = 48.dp) { amount = a } }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GameButton("Отложить $amount", Modifier.weight(1f), style = ButtonStyle.GREEN, enabled = s.plan.confirmed && amount <= s.balance) { vm.deposit(amount) }
                    GameButton("Забрать $amount", Modifier.weight(1f), style = ButtonStyle.PAPER, enabled = amount <= s.savings) { confirmWithdraw = true }
                }
                if (!s.plan.confirmed) Text("Откладывать можно после того, как составишь план.", style = MaterialTheme.typography.bodySmall, color = G.inkSoft)
            }, right = {
                if (goal != null && !pickGoal) {
                    Column(Modifier.fillMaxWidth().background(G.paperTint, RoundedCornerShape(20.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Image(painterResource(goalRes(goal.id)), null, Modifier.size(72.dp))
                            Column {
                                Label("Моя цель")
                                Text(goal.title, style = MaterialTheme.typography.titleLarge, color = G.ink)
                                Text("Стоит ${goal.price} монет", style = MaterialTheme.typography.bodyMedium, color = G.inkSoft)
                            }
                        }
                        GameBar("Накоплено", s.savings, G.magenta, max = goal.price)
                        val eta = e.goalEta(s)
                        Text(
                            when {
                                e.remaining(s) <= 0 -> "Накоплено! Можно забрать цель."
                                eta == null -> "Откладывай каждую неделю — и я посчитаю, сколько недель осталось."
                                else -> "Осталось ${e.remaining(s)}. Ты откладываешь в среднем ${"%.0f".format(e.averageDeposit(s))} в неделю — это ещё ${Economy.weeks(eta)}."
                            },
                            style = MaterialTheme.typography.bodyMedium, color = G.ink,
                        )
                        if (e.remaining(s) <= 0) GameButton("Забрать цель!", Modifier.fillMaxWidth(), style = ButtonStyle.GOLD) { confirmAchieve = true }
                        GameButton("Сменить цель", Modifier.fillMaxWidth(), style = ButtonStyle.PAPER, minHeight = 48.dp) { pickGoal = true }
                    }
                } else {
                    GoalPicker(vm) { pickGoal = false }
                }
                if (s.achievedGoals.isNotEmpty()) {
                    Label("Достигнутые цели")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        s.achievedGoals.forEach { g ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(88.dp)) {
                                Image(painterResource(goalRes(g.id)), null, Modifier.size(56.dp))
                                Text(g.title, style = MaterialTheme.typography.labelSmall, color = G.ink, textAlign = TextAlign.Center, maxLines = 2)
                            }
                        }
                    }
                }
            })
        }
        if (confirmWithdraw) {
            ConfirmPanel("Забрать $amount из копилки?", e.withdrawPreview(s, amount), "Забрать", onConfirm = { confirmWithdraw = false; vm.withdraw(amount) }, onDismiss = { confirmWithdraw = false })
        }
        if (confirmAchieve && goal != null) {
            ConfirmPanel("Забрать «${goal.title}»?", listOf("Из копилки уйдёт ${goal.price} монет, останется ${s.savings - goal.price}.", "Питомец очень обрадуется!"), "Забрать", onConfirm = { confirmAchieve = false; vm.achieveGoal() }, onDismiss = { confirmAchieve = false })
        }
    }
}

@Composable
private fun GoalPicker(vm: GameViewModel, onDone: () -> Unit) {
    val c = vm.content
    val layout = LocalLayout.current
    var customTitle by rememberSaveable { mutableStateOf(c.customGoal.titles.first()) }
    var customPrice by rememberSaveable { mutableIntStateOf(c.customGoal.prices.first()) }
    Label("Выбери цель")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        c.goals.forEach { g ->
            Column(
                Modifier.weight(1f).shadow(3.dp, RoundedCornerShape(18.dp)).background(Color.White, RoundedCornerShape(18.dp))
                    .clickable(role = Role.Button) { vm.chooseGoal(Goal(g.id, g.title, g.emoji, g.price)); onDone() }
                    .semantics(mergeDescendants = true) { contentDescription = "${g.title}, ${g.price} монет" }
                    .padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Image(painterResource(goalRes(g.id)), null, Modifier.size(56.dp))
                Text(g.title, style = MaterialTheme.typography.labelSmall, color = G.ink, textAlign = TextAlign.Center, maxLines = 2, minLines = 2)
                Text("${g.price}", style = MaterialTheme.typography.labelLarge, color = G.purpleDeep)
            }
        }
    }
    Label("Или своя цель")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        c.customGoal.titles.forEach { t -> GameButton(t, style = if (customTitle == t) ButtonStyle.MAGENTA else ButtonStyle.PAPER, minHeight = 44.dp) { customTitle = t } }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        c.customGoal.prices.forEach { p -> GameButton("$p", style = if (customPrice == p) ButtonStyle.MAGENTA else ButtonStyle.PAPER, minHeight = 44.dp) { customPrice = p } }
    }
    GameButton("Копить на «$customTitle» за $customPrice", Modifier.fillMaxWidth(), style = ButtonStyle.GOLD) { vm.chooseGoal(vm.economy.customGoal(customTitle, customPrice)); onDone() }
    if (vm.state.goal != null) GameButton("Оставить текущую цель", Modifier.fillMaxWidth(), style = ButtonStyle.PAPER, minHeight = 48.dp) { onDone() }
}
