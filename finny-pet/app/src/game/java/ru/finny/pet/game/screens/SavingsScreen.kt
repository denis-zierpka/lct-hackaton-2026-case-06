package ru.finny.pet.game.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.finny.pet.domain.Goal
import ru.finny.pet.domain.town.Source
import ru.finny.pet.game.GameViewModel
import ru.finny.pet.game.ui.ButtonStyle
import ru.finny.pet.game.ui.CheckBadge
import ru.finny.pet.game.ui.G
import ru.finny.pet.game.ui.GameBar
import ru.finny.pet.game.ui.GameButton
import ru.finny.pet.game.ui.GoalPic
import ru.finny.pet.game.ui.TText

/** «Копилка» (§D.2): the dream, deposits and withdrawals through Town, the showcase and own dream, dreams come true. */
@Composable
fun SavingsScreen(vm: GameViewModel) {
    val s = vm.state
    val goal = s.goal
    val n = vm.content.rules.planStep
    var pending by remember { mutableStateOf<Pending?>(null) }
    HomePage(vm, "Копилка", "savings", overlay = { PendingAsk(pending) { pending = null } }) {
        if (goal != null) {
            val left = maxOf(goal.price - s.savings, 0)
            val eta = vm.economy.goalEta(s)?.takeIf { it > 0 }
            Row(
                Modifier.fillMaxWidth().background(G.pink, RoundedCornerShape(20.dp)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GoalPic(goal, 72.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TText(goal.title, style = MaterialTheme.typography.titleLarge)
                    // non-break spaces: «≈ N ✉» never wraps mid-phrase (правка №9)
                    TText("${s.savings} / ${goal.price}, ещё $left" + (eta?.let { ", ≈ $it ✉" } ?: ""))
                    GameBar("", s.savings, G.magenta, max = goal.price)
                }
            }
            if (s.savings >= goal.price) GameButton("Получить мечту", Modifier.fillMaxWidth(), ButtonStyle.GOLD) {
                pending = Pending("Получить мечту «${goal.title}»?", emptyList()) { vm.achieveGoal() }
            }
        } else {
            TText("Выбери мечту")
        }
        if (s.plan.confirmed) {
            listOf(Triple(Source.RESERVE, "В копилку $n из запаса", s.reserve), Triple(Source.WANT, "В копилку $n из «Хочу»", s.jarWant)).forEach { (src, label, have) ->
                if (have >= n) GameButton(label, Modifier.fillMaxWidth(), ButtonStyle.GREEN, minHeight = 48.dp) {
                    pending = Pending(label, vm.depositPreview(src, n)) { vm.deposit(src, n) }
                }
            }
        } else if (s.savings >= n) {
            val label = "Взять $n из копилки"
            GameButton(label, Modifier.fillMaxWidth(), ButtonStyle.PAPER, minHeight = 48.dp) { pending = Pending(label, vm.withdrawPreview(n)) { vm.withdraw(n) } }
        }
        Label("Мечты")
        vm.tc.goals.forEach { g ->
            val mine = goal?.id == g.id
            Box {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 64.dp).background(if (mine) G.pink else G.paperTint, RoundedCornerShape(16.dp))
                        .clickable(role = Role.Button) { vm.chooseGoal(g.id) }.clearAndSetSemantics { contentDescription = "${g.title} — ${g.price}" + if (mine) ", копим" else "" }.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    GoalPic(Goal(g.id, g.title, g.emoji, g.price), 48.dp)
                    TText("${g.title} — ${g.price}", modifier = Modifier.weight(1f))
                }
                if (mine) CheckBadge()
            }
        }
        OwnDream(vm)
        if (s.achievedGoals.isNotEmpty()) {
            Label("Мечты сбылись")
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                s.achievedGoals.forEach { g ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        GoalPic(g, 56.dp)
                        TText(g.title, style = MaterialTheme.typography.bodyMedium, maxLines = 2, align = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
private fun OwnDream(vm: GameViewModel) {
    val c = vm.content.customGoal
    var title by rememberSaveable { mutableStateOf(c.titles.first()) }
    var price by rememberSaveable { mutableIntStateOf(c.prices.first()) }
    Label("Своя мечта")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        c.titles.forEach { t -> GameButton(t, selected = title == t, minHeight = 48.dp) { title = t } }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        c.prices.forEach { p -> GameButton("$p", selected = price == p, minHeight = 48.dp) { price = p } }
    }
    GameButton("Копить на «$title» — $price", Modifier.fillMaxWidth(), ButtonStyle.GOLD, minHeight = 48.dp) { vm.chooseGoal(vm.economy.customGoal(title, price)) }
}
