package ru.finny.pet.game.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ru.finny.pet.PetSprites
import ru.finny.pet.R
import ru.finny.pet.domain.Face
import ru.finny.pet.domain.PeriodSummary
import ru.finny.pet.domain.Theme
import ru.finny.pet.game.Effect
import ru.finny.pet.game.GameViewModel
import ru.finny.pet.game.LocalAnimate
import ru.finny.pet.game.LocalLayout
import ru.finny.pet.game.Screen
import ru.finny.pet.game.orNone
import ru.finny.pet.game.audio.Sound
import ru.finny.pet.game.ui.ButtonStyle
import ru.finny.pet.game.ui.G
import ru.finny.pet.game.ui.GameBar
import ru.finny.pet.game.ui.GameButton
import ru.finny.pet.game.ui.Panel
import ru.finny.pet.game.ui.PetSprite
import ru.finny.pet.game.ui.SpeechBubble

/** Night passed: stamps appear one by one, the growth bar fills, the pet grows if a stage was reached. */
@Composable
fun WeekEndScreen(vm: GameViewModel) {
    val sum = vm.lastSummary ?: run { vm.navigate(Screen.Room); return }
    val s = vm.state
    val pet = s.pet ?: return
    val layout = LocalLayout.current
    val animate = LocalAnimate.current
    var shown by remember { mutableIntStateOf(if (animate) 0 else 5) }
    LaunchedEffect(sum.period) {
        if (animate) {
            for (i in 1..4) { delay(if (i == 1) 700L else 550L); shown = i; vm.sfx(if (i == 1 && sum.mandatoryCovered || i == 2 && sum.planKept || i == 3 && sum.saved) Sound.SUCCESS else Sound.POP) }
            delay(500); shown = 5
            if (sum.stageAfter > sum.stageBefore) { vm.sfx(Sound.FANFARE); vm.effects.tryEmit(Effect.Confetti) }
        }
    }
    Box(Modifier.fillMaxSize().background(G.purpleDeep.copy(alpha = 0.55f))) {
        val petCol: @Composable () -> Unit = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Неделя ${sum.period} завершена", style = MaterialTheme.typography.headlineSmall, color = Color.White, textAlign = TextAlign.Center)
                PetSprite(pet.speciesId, pet.colorId, if (shown >= 5) sum.stageAfter else sum.stageBefore, if (sum.score >= 2) Face.HAPPY else Face.NEUTRAL, animate, size = if (layout.compact) 170.dp else 220.dp, description = "", bounceKey = if (shown >= 5) 1 else 0)
                if (sum.stageAfter > sum.stageBefore) AnimatedVisibility(shown >= 5, enter = (fadeIn() + scaleIn(initialScale = 0.5f)).orNone()) {
                    Text("${pet.name} вырос! Теперь ${vm.economy.stageTitle(sum.growthAfter).lowercase()}", style = MaterialTheme.typography.titleMedium, color = G.gold, textAlign = TextAlign.Center)
                }
            }
        }
        val report: @Composable (Modifier) -> Unit = { mod ->
            Panel(mod.widthIn(max = 560.dp), padding = 12.dp) {
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Stamp("Еда и уход куплены", sum.mandatoryCovered, shown >= 1)
                Stamp("Траты по плану", sum.planKept, shown >= 2)
                Stamp("Копилка выросла", sum.saved, shown >= 3)
                AnimatedVisibility(shown >= 4, enter = fadeIn(tween(300)).orNone()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        GameBar("Рост: +${sum.score} (${sum.growthBefore} → ${sum.growthAfter})", sum.growthAfter, G.gold, max = vm.economy.rules.stageThresholds.last().coerceAtLeast(1), icon = painterResource(R.drawable.ui_trophy))
                        PlanFact(listOf(Triple("Обязательное", sum.plan.mandatory, sum.factMandatory), Triple("Желаемое", sum.plan.optional, sum.factOptional), Triple("Копилка", sum.plan.savings, sum.factSavings)))
                    }
                }
                AnimatedVisibility(shown >= 5, enter = fadeIn(tween(300)).orNone()) {
                    SpeechBubble(tailAtStart = true) {
                        Label("Что случилось и почему", G.purpleDeep)
                        sum.messages.forEach { m -> Text("• $m", style = MaterialTheme.typography.bodyMedium, color = G.ink) }
                    }
                }
                }
                GameButton("Дальше, к неделе ${sum.period + 1}", Modifier.fillMaxWidth(), style = ButtonStyle.GOLD, enabled = shown >= 4) { vm.navigate(Screen.Room) }
            }
        }
        if (layout.landscape) {
            Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(0.7f), contentAlignment = Alignment.Center) { petCol() }
                Box(Modifier.weight(1.3f).fillMaxSize(), contentAlignment = Alignment.Center) { report(Modifier.fillMaxSize()) }
            }
        } else {
            Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) { petCol(); Spacer(Modifier.size(8.dp)); report(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun Stamp(text: String, ok: Boolean, visible: Boolean) {
    Row(Modifier.fillMaxWidth().alpha(if (visible) 1f else 0.15f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        AnimatedVisibility(visible, enter = (scaleIn(tween(350), initialScale = 2.2f) + fadeIn()).orNone()) {
            Box(Modifier.size(38.dp).background(if (ok) G.green else G.paperTint, RoundedCornerShape(50)), contentAlignment = Alignment.Center) {
                Text(if (ok) "✓" else "–", style = MaterialTheme.typography.titleLarge, color = if (ok) Color.White else G.inkSoft)
            }
        }
        if (!visible) Box(Modifier.size(38.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = G.ink)
    }
}

@Composable
fun ProgressScreen(vm: GameViewModel) {
    val s = vm.state
    val e = vm.economy
    val pet = s.pet ?: return
    PanelScreen(vm, "Прогресс") {
        Adaptive(left = {
            Label("Рост ${pet.name}")
            Row(Modifier.fillMaxWidth().background(G.pink, RoundedCornerShape(20.dp)).padding(10.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                val stage = e.stageIndex(pet.growth)
                e.rules.stageTitles.forEachIndexed { i, title ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(painterResource(PetSprites.id(pet.speciesId, pet.colorId, i, "happy")), null, Modifier.size((56 + i * 14).dp).alpha(if (i <= stage) 1f else 0.3f))
                        Text(title, style = MaterialTheme.typography.labelMedium, color = if (i <= stage) G.purpleDeep else G.inkSoft)
                    }
                }
            }
            val next = e.nextStageLeft(pet.growth)
            Text("Очки роста: ${pet.growth}. " + (next?.let { "До следующей стадии: $it." } ?: "Высшая стадия!"), style = MaterialTheme.typography.bodyLarge, color = G.ink)
            Text("Рост даётся за три вещи в неделю: еда и уход куплены, траты по плану, копилка выросла.", style = MaterialTheme.typography.bodySmall, color = G.inkSoft)
            Label("Задания по темам")
            Theme.entries.forEach { th ->
                val total = vm.content.tasks.count { it.theme == th }
                val done = e.completedTasks(s).count { it.first.theme == th }
                GameBar("${th.title}: $done из $total", done, th.color(), max = total.coerceAtLeast(1), icon = painterResource(th.icon()))
            }
            val quizDone = s.quizResults.count { it.correct }
            Text("Вопросы питомца: $quizDone из ${vm.content.quiz.size}. Бомбочек для игры: ${s.bombs}.", style = MaterialTheme.typography.bodyMedium, color = G.ink)
        }, right = {
            Label("Цель")
            val goal = s.goal
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (goal != null) { Image(painterResource(goalRes(goal.id)), null, Modifier.size(48.dp)); Text("${goal.title}: ${s.savings} из ${goal.price} монет", style = MaterialTheme.typography.bodyLarge, color = G.ink) }
                else Text("Не выбрана", style = MaterialTheme.typography.bodyLarge, color = G.ink)
            }
            if (s.achievedGoals.isNotEmpty()) Text("Достигнуто: ${s.achievedGoals.joinToString { it.title }}", style = MaterialTheme.typography.bodySmall, color = G.inkSoft)
            Label("Откуда монеты на этой неделе")
            s.ledger.forEach { l ->
                Row(Modifier.fillMaxWidth().background(G.paperTint, RoundedCornerShape(12.dp)).padding(8.dp)) {
                    Text(l.text, style = MaterialTheme.typography.bodyMedium, color = G.ink, modifier = Modifier.weight(1f))
                    Text(if (l.amount > 0) "+${l.amount}" else "${l.amount}", style = MaterialTheme.typography.titleSmall, color = if (l.amount >= 0) G.greenDark else G.ink)
                }
            }
            val last = vm.lastSummary
            if (last != null) { Label("Итог недели ${last.period}"); LastSummary(last) }
            GameButton("Справка: что такое бюджет", Modifier.fillMaxWidth(), style = ButtonStyle.PAPER, icon = painterResource(R.drawable.ui_book), iconSize = 26.dp) { vm.navigate(Screen.Glossary) }
        })
    }
}

@Composable
private fun LastSummary(sum: PeriodSummary) {
    Column(Modifier.fillMaxWidth().background(G.paperTint, RoundedCornerShape(16.dp)).padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf("Еда и уход куплены" to sum.mandatoryCovered, "Траты по плану" to sum.planKept, "Копилка выросла" to sum.saved).forEach { (t, ok) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (ok) "✓" else "–", style = MaterialTheme.typography.titleMedium, color = if (ok) G.greenDark else G.inkSoft, modifier = Modifier.width(20.dp))
                Text(t, style = MaterialTheme.typography.bodyMedium, color = G.ink)
            }
        }
        Text("Рост: +${sum.score} (${sum.growthBefore} → ${sum.growthAfter})", style = MaterialTheme.typography.titleSmall, color = G.purpleDeep)
    }
}

@Composable
fun GlossaryScreen(vm: GameViewModel) {
    PanelScreen(vm, "Справка") {
        vm.content.glossary.forEach { g ->
            Row(Modifier.fillMaxWidth().background(G.paperTint, RoundedCornerShape(16.dp)).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.ui_book), null, Modifier.size(36.dp))
                Column {
                    Text(g.term, style = MaterialTheme.typography.titleSmall, color = G.purpleDeep)
                    Text(g.definition, style = MaterialTheme.typography.bodyMedium, color = G.ink)
                }
            }
        }
    }
}
