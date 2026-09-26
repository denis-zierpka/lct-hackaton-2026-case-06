package ru.finny.pet.game.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ru.finny.pet.domain.town.EventDef
import ru.finny.pet.game.GameViewModel
import ru.finny.pet.game.ui.ButtonStyle
import ru.finny.pet.game.ui.G
import ru.finny.pet.game.ui.GameButton
import ru.finny.pet.game.ui.Hud1
import ru.finny.pet.game.ui.Panel
import ru.finny.pet.game.ui.TText

/** «События» (§E.5): active events, job orders and, in the demo, every event to start by hand. */
@Composable
fun BoardScreen(vm: GameViewModel) {
    val s = vm.state
    val active = vm.town.activeEvents(s)
    val orders = vm.town.orders(s)
    val demo = vm.town.demoBoard(s)
    Column(Modifier.fillMaxSize()) {
        Hud1(vm, inPlace = true)
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).testTag("events").padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TText("События", style = MaterialTheme.typography.headlineSmall, color = Color.White)
            if (active.isEmpty() && orders.isEmpty()) TText("В городке спокойно", color = Color.White)
            active.forEach { e ->
                EventRow(vm, e) { if (e.place != null) GameButton("Перейти", Modifier.fillMaxWidth(), minHeight = 48.dp) { vm.goEvent(e.id) } }
            }
            orders.forEach { e ->
                EventRow(vm, e, bang = false) {
                    ShiftTokens(vm)
                    val place = e.place ?: vm.tc.jobs.firstOrNull { it.id == e.params.job }?.place
                    if (place != null) GameButton("К работе", Modifier.fillMaxWidth(), minHeight = 48.dp) { vm.openPlace(place) }
                }
            }
            if (demo.isNotEmpty()) {
                TText("Все события (демо)", style = MaterialTheme.typography.titleMedium, color = Color.White)
                demo.forEach { e -> EventRow(vm, e, bang = false) { GameButton("Начать", Modifier.fillMaxWidth(), ButtonStyle.PAPER, minHeight = 48.dp) { vm.startEvent(e.id) } } }
            }
        }
    }
}

/** One board row: title (with «!» for an event, «новое» if it came while playing), place, intro and the action. */
@Composable
private fun EventRow(vm: GameViewModel, e: EventDef, bang: Boolean = true, action: @Composable () -> Unit) {
    val place = e.place?.let { p -> vm.tc.places.firstOrNull { it.id == p }?.title }
    Panel(Modifier.fillMaxWidth(), padding = 12.dp) {
        TText((if (bang) "! " else "") + e.title + if (e.id in vm.fresh) " · новое" else "", style = MaterialTheme.typography.titleMedium)
        if (place != null) TText(place, style = MaterialTheme.typography.bodyMedium, color = G.inkSoft)
        TText(e.intro)
        action()
    }
}
