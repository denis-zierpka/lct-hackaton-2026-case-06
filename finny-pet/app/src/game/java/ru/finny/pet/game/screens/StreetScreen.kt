package ru.finny.pet.game.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.finny.pet.R
import ru.finny.pet.domain.town.OpensBy
import ru.finny.pet.domain.town.Place
import ru.finny.pet.domain.town.TownGoal
import ru.finny.pet.game.GameViewModel
import ru.finny.pet.game.ui.G
import ru.finny.pet.game.ui.Hud1
import ru.finny.pet.game.ui.Pic
import ru.finny.pet.game.ui.ResidentPic
import ru.finny.pet.game.ui.TText
import ru.finny.pet.game.ui.goalRes

/** Places on the street of this slice: those open from the start (places that open later are slice 2 or dream gates). */
internal fun GameViewModel.streetPlaces(): List<Place> = tc.places.filter { it.opensBy == OpensBy() }

/** Drawable of a street facade (fac_<placeId>, facade.py --all), or null — then an emoji is drawn. */
private fun facadeRes(id: String): Int? = when (id) {
    "home" -> R.drawable.fac_home
    "market" -> R.drawable.fac_market
    "foma" -> R.drawable.fac_foma
    "bakery" -> R.drawable.fac_bakery
    "park" -> R.drawable.fac_park
    "forest" -> R.drawable.fac_forest
    "zoo" -> R.drawable.fac_zoo
    else -> null
}

/** «Улица» (§E.1): a row of places, then the dream gates. */
@Composable
fun StreetScreen(vm: GameViewModel) {
    var ask by remember { mutableStateOf<TownGoal?>(null) }
    val gates = vm.tc.places.mapNotNull { p -> p.opensBy.goal?.let { g -> vm.tc.goals.firstOrNull { it.id == g } }?.let { p to it } }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Hud1(vm, inPlace = true)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // dark on the light sky of bg_street_port (TOWN-A1g2; contrast — TOWN-A1g1 приёмка п. 5) (правка №14, ТЗ 3.6)
                TText("Улица", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 16.dp), color = G.ink)
                LazyRow(Modifier.fillMaxWidth().testTag("street"), contentPadding = PaddingValues(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(vm.streetPlaces(), key = { it.id }) { PlaceCard(vm, it) }
                }
                gates.forEach { (p, g) -> Gate(vm, p, g) { if (vm.state.goal?.id == g.id) vm.say("Копим на «${g.title}»: ${vm.state.savings} / ${g.price}") else ask = g } }
            }
        }
        ask?.let { g ->
            val close = { ask = null }
            Ask("Копить на мечту «${g.title}»?", emptyList(), listOf("Да" to { close(); vm.chooseGoal(g.id) }, "Отмена" to close), close)
        }
    }
}

/** A place: its facade 120 dp, the resident 64 dp at its bottom centre, «!» in its top right corner (zones of facade.py);
 * the name and the title under it on White α 0,85, as on sheet № 38. */
@Composable
private fun PlaceCard(vm: GameViewModel, p: Place) {
    val resident = vm.tc.residents.firstOrNull { it.id == p.resident }
    val event = vm.eventsAt(p.id).isNotEmpty()
    Column(
        Modifier.width(120.dp)
            .clickable(role = Role.Button) { vm.openPlace(p.id) }
            .clearAndSetSemantics { contentDescription = p.title + (resident?.let { ", ${it.name}" } ?: "") + if (event) ", есть событие" else "" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Pic(facadeRes(p.id), "🏠", 120.dp)
            if (resident != null) ResidentPic(resident, 64.dp, Modifier.align(Alignment.BottomCenter))
            if (event) Box(Modifier.align(Alignment.TopEnd).padding(4.dp).size(32.dp).border(2.dp, Color.White, CircleShape).background(Color(0xFFE0004A), CircleShape), contentAlignment = Alignment.Center) {
                TText("!", style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 1)
            }
        }
        Column(Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(16.dp)).padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (resident != null) TText(resident.name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
            TText(p.title, style = MaterialTheme.typography.bodyMedium, maxLines = 3, align = TextAlign.Center)
        }
    }
}

/** A dream gate (№ 72 а): the gate's facade 64 dp, the dream picture and «Мечта «…»: sv / price», no lock. */
@Composable
private fun Gate(vm: GameViewModel, p: Place, g: TownGoal, onClick: () -> Unit) {
    val text = "Мечта «${g.title}»: ${vm.state.savings}\u00A0/\u00A0${g.price}"
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp).heightIn(min = 64.dp).background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp)).border(1.dp, G.purpleDeep, RoundedCornerShape(16.dp)) // the gate facade inside the round corner; the edge ≥ 3 : 1 on the sky and lawn (judge Р5, № 70)
            .clickable(role = Role.Button, onClick = onClick).clearAndSetSemantics { contentDescription = "${p.title}: калитка закрыта. Мечта «${g.title}», ${vm.state.savings} из ${g.price}" }.padding(end = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Pic(facadeRes(p.id), "🚪", 64.dp)
        Pic(goalRes(g.id), g.emoji, 48.dp)
        TText(text, modifier = Modifier.weight(1f).padding(vertical = 4.dp))
    }
}
