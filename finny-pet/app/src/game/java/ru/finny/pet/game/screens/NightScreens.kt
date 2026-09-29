package ru.finny.pet.game.screens

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.finny.pet.PetSprites
import ru.finny.pet.R
import ru.finny.pet.domain.town.TweakDir
import ru.finny.pet.game.GameViewModel
import ru.finny.pet.game.Screen
import ru.finny.pet.game.ui.ButtonStyle
import ru.finny.pet.game.ui.G
import ru.finny.pet.game.ui.GameButton
import ru.finny.pet.game.ui.Panel
import ru.finny.pet.game.ui.TText

/** Night (§D.7): the pet sleeps, the day's line and the night's event results, then the morning. */
@Composable
fun NightScreen(vm: GameViewModel) {
    val s = vm.state
    val pet = s.pet ?: return
    var ask by rememberSaveable { mutableStateOf(false) }
    val night = vm.night
    Box(Modifier.fillMaxSize().background(G.purpleDeep.copy(alpha = 0.35f)).testTag("night")) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        ) {
            Image(painterResource(R.drawable.ui_moon), null, Modifier.size(72.dp))
            // № 91 б: the pet sleeps on furn_bed, paws on the blanket: 64 − 17 (blanket top) − 26 (empty under the paws of the 180 dp frame) = 21 dp
            Box(contentAlignment = Alignment.BottomCenter) {
                Image(painterResource(R.drawable.furn_bed), null, Modifier.size(128.dp, 64.dp))
                Image(painterResource(PetSprites.id(pet.speciesId, pet.colorId, vm.economy.stageIndex(pet.growth), "blink")), "${pet.name} спит", Modifier.padding(bottom = 21.dp).size(180.dp))
            }
            Panel(Modifier.fillMaxWidth(), padding = 12.dp) {
                night?.line?.text?.takeIf { it.isNotBlank() }?.let { TText(it) }
                night?.results?.forEach { TText(it.line, style = MaterialTheme.typography.bodyMedium) }
                TText("Можно закрыть игру — всё сохранено", style = MaterialTheme.typography.bodyMedium, color = G.inkSoft)
                GameButton("Проснуться", Modifier.fillMaxWidth(), ButtonStyle.GOLD, icon = painterResource(R.drawable.ui_sun)) { vm.wake() }
                if (s.demo && s.plan.confirmed) GameButton("Сразу к итогу недели", Modifier.fillMaxWidth(), ButtonStyle.PAPER, minHeight = 48.dp) { ask = true }
            }
        }
        if (ask) Ask(
            "Закончить неделю?", vm.weekEndPreview(),
            listOf("Закончить неделю" to { ask = false; vm.endWeek() }, "Отмена" to { ask = false }), { ask = false },
        )
    }
}

/** «Итог недели» (§D.8): jars by the summary, stamps got, growth, the line and why, events, a tweak, then the way out. */
@Composable
fun WeekEndScreen(vm: GameViewModel) {
    val s = vm.state
    val pet = s.pet ?: return
    val sum = s.history.lastOrNull() ?: return
    val report = vm.weekEnd
    var why by remember { mutableStateOf(false) }
    var tweaked by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    Box(Modifier.fillMaxSize().padding(8.dp)) {
        Panel(Modifier.fillMaxSize().testTag("weekend"), padding = 12.dp) {
            TText("Итог недели", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() }, color = G.purpleDeep)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PlanFactRows(sum.plan.mandatory, sum.factMandatory, sum.plan.optional, sum.factOptional, sum.plan.savings, sum.factSavings)
                listOf(
                    Triple(R.drawable.ui_lid_mandatory, "Нужное куплено", sum.mandatoryCovered),
                    Triple(R.drawable.ui_trophy, "По плану", sum.planKept),
                    Triple(R.drawable.ui_piggy, "Отложено", sum.saved),
                ).filter { it.third }.forEach { (icon, word, _) ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(40.dp).background(G.gold, CircleShape), contentAlignment = Alignment.Center) { Image(painterResource(icon), null, Modifier.size(30.dp)) }
                        TText(word, style = MaterialTheme.typography.titleMedium)
                    }
                }
                if (sum.score > 0) TText("Рост +${sum.score}", style = MaterialTheme.typography.titleMedium, color = G.purpleDeep)
                TText("${pet.name}: ${vm.economy.stageTitle(pet.growth)}")
                if (report != null) {
                    TText(report.line.text, style = MaterialTheme.typography.titleMedium)
                    if (report.line.why.isNotEmpty()) {
                        if (why) report.line.why.forEach { TText(it, style = MaterialTheme.typography.bodyMedium, color = G.inkSoft) }
                        else GameButton("Почему?", style = ButtonStyle.PAPER, minHeight = 48.dp) { why = true }
                    }
                    if (report.results.isNotEmpty()) {
                        Label("События недели")
                        report.results.forEach { TText(it.line, style = MaterialTheme.typography.bodyMedium) }
                    }
                }
                if (!tweaked) {
                    Label("Что поменяем в новом плане?")
                    vm.planTweaks().forEach { t ->
                        val word = when (t.dir) { TweakDir.NEED -> "Нужное"; TweakDir.WANT -> "Хочу"; TweakDir.SAVINGS -> "В копилку"; TweakDir.RESERVE -> "Запас" }
                        GameButton("$word +${t.n}", Modifier.fillMaxWidth(), ButtonStyle.PAPER, minHeight = 48.dp) { if (vm.chooseTweak(t)) tweaked = true }
                    }
                    GameButton("Как было", Modifier.fillMaxWidth(), ButtonStyle.PAPER, minHeight = 48.dp) { if (vm.chooseTweak(null)) tweaked = true }
                } else {
                    TText("Неделя позади! Всё сохранено", style = MaterialTheme.typography.titleMedium, align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    // one size for both answers (§9.2 №20): same width, height of the taller one
                    Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GameButton("Выйти", Modifier.weight(1f).fillMaxHeight(), ButtonStyle.PAPER, minHeight = 48.dp) { (context as? Activity)?.finish() }
                        GameButton("Играть дальше", Modifier.weight(1f).fillMaxHeight(), ButtonStyle.PAPER, minHeight = 48.dp) { vm.navigate(Screen.Night) }
                    }
                }
            }
        }
    }
}

