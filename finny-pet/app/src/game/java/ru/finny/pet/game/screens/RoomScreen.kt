package ru.finny.pet.game.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.finny.pet.R
import ru.finny.pet.domain.town.petLine
import ru.finny.pet.game.GameViewModel
import ru.finny.pet.game.LocalAnimate
import ru.finny.pet.game.LocalPetAction
import ru.finny.pet.game.Screen
import ru.finny.pet.game.ui.G
import ru.finny.pet.game.ui.GameButton
import ru.finny.pet.game.ui.Hud1
import ru.finny.pet.game.ui.Hud2
import ru.finny.pet.game.ui.LocalParticles
import ru.finny.pet.game.ui.PetSprite
import ru.finny.pet.game.ui.Pic
import ru.finny.pet.game.ui.ResidentPic
import ru.finny.pet.game.ui.STATS
import ru.finny.pet.game.ui.StatChip
import ru.finny.pet.game.ui.TText
import ru.finny.pet.game.ui.Target
import ru.finny.pet.game.ui.bigFont
import ru.finny.pet.game.ui.chip
import ru.finny.pet.game.ui.itemRes
import ru.finny.pet.game.ui.particleTarget
import ru.finny.pet.game.ui.stats

/** HOME (§C, macket MockHome): HUD-1 56, HUD-2 48, «В городке» 56, the room takes the rest, bottom row 72. */
@Composable
fun RoomScreen(vm: GameViewModel) {
    val s = vm.state
    s.pet ?: return
    var panel by rememberSaveable { mutableStateOf<String?>(null) }
    val bed = {
        when {
            !s.plan.confirmed -> vm.navigate(Screen.Jars)
            s.day < vm.tc.rules.daysPerWeek -> vm.sleep()
            else -> panel = "bed"
        }
    }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Hud1(vm, inPlace = false)
            Hud2 { val v = vm.stats(); STATS.forEachIndexed { i, st -> StatChip(st, v[i]) } }
            TownCard(vm)
            Room(vm, Modifier.weight(1f), onPanel = { panel = it }, onBed = bed)
            BottomRow(vm)
        }
        val close = { panel = null }
        when (panel) {
            "mail" -> EnvelopePanel(vm, close)
            "fridge" -> Ask("Список нужного", vm.needList.map(vm::seenLine) + s.notes, listOf("Понятно" to close), close)
            "bed" -> BedAsk(vm, close)
        }
    }
}

/** «В городке» (§B.7): the first active event or order, else the calm line; a tap goes where the event lives. */
@Composable
private fun TownCard(vm: GameViewModel) {
    val card = vm.town.card(vm.state)
    val place = card.place?.let { p -> vm.tc.places.firstOrNull { it.id == p }?.title }
    val text = if (card.eventId != null && place != null) "${card.title} · $place" else card.title
    Row(
        Modifier.fillMaxWidth().height(56.dp).testTag("town_card").padding(horizontal = 8.dp, vertical = 2.dp).chip()
            .clickable(role = Role.Button) { card.eventId?.let { vm.goEvent(it) } ?: vm.navigate(Screen.Board) }
            .clearAndSetSemantics { contentDescription = "В городке: $text" }.padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (card.eventId != null) Box(Modifier.size(28.dp).background(G.magenta, CircleShape), contentAlignment = Alignment.Center) {
            TText("!", style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 1)
        }
        TText(text, style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 18.sp), modifier = Modifier.weight(1f), maxLines = 2)
        TText("→", style = MaterialTheme.typography.titleMedium, color = G.purpleDeep, maxLines = 1)
    }
}

/** The room as a scheme: wall and floor, flat objects over them, the pet above the bed. */
@Composable
private fun Room(vm: GameViewModel, modifier: Modifier, onPanel: (String) -> Unit, onBed: () -> Unit) {
    val s = vm.state
    val pet = s.pet ?: return
    val label = MaterialTheme.typography.labelSmall
    val particles = LocalParticles.current
    val resident = vm.town.residentOfWeek(s)
    val streetEvent = vm.town.activeEvents(s).any { it.place != null && it.place != "home" }
    val mail = s.envelope.sumOf { it.amount }
    Box(modifier.fillMaxWidth().testTag("room")) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().weight(0.6f).background(G.lavenderLight))
            Box(Modifier.fillMaxWidth().weight(0.4f).background(G.lavender))
        }
        Column(Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
            Row(Modifier.fillMaxWidth().height(88.dp).padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Target("Окно: улица" + (resident?.let { ", машет ${it.name}" } ?: "") + (if (streetEvent) ", есть событие" else ""),112.dp, 80.dp, color = G.sky, onClick = { vm.navigate(Screen.Street) }) {
                    if (resident != null) ResidentPic(resident, 40.dp, Modifier.align(Alignment.CenterStart).padding(start = 4.dp))
                    if (streetEvent) TText("!", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.TopEnd).padding(end = 6.dp), color = G.magenta, maxLines = 1)
                }
                Shelf(vm)
                Target("Копилка ${s.savings}", 64.dp, 64.dp, onClick = { vm.navigate(Screen.Savings) }) {
                    Image(painterResource(R.drawable.ui_piggy), null, Modifier.size(52.dp))
                }
            }
            Box(Modifier.fillMaxWidth().weight(1f).padding(top = 4.dp, bottom = 4.dp)) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                        Target("Холодильник: список нужного и цены", 64.dp, 96.dp, color = Color.White, onClick = { onPanel("fridge") }) {
                            Box(Modifier.align(Alignment.TopCenter).padding(top = 32.dp).fillMaxWidth().height(2.dp).background(G.lavender))
                            TText("Список", style = label, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp).wrapContentWidth(unbounded = true), maxLines = 1)
                        }
                        SpotThing(vm, "spot_3", Modifier.align(Alignment.CenterHorizontally))
                        Target("Сундук: обустроить комнату", 64.dp, 48.dp, onClick = { vm.navigate(Screen.Arrange) }) { TText("🧳", style = MaterialTheme.typography.titleLarge, maxLines = 1) }
                        Target("Словарик", 48.dp, 48.dp, onClick = { vm.navigate(Screen.Glossary) }) { Image(painterResource(R.drawable.ui_book), null, Modifier.size(36.dp)) }
                    }
                    BoxWithConstraints(Modifier.weight(1f).fillMaxHeight()) {
                        val petSize = minOf(120.dp, maxHeight - 64.dp - 36.dp).coerceAtLeast(64.dp)
                        SpotThing(vm, "spot_2", Modifier.align(Alignment.TopCenter))
                        SpotThing(vm, "spot_1", Modifier.align(Alignment.BottomStart).padding(bottom = 68.dp))
                        SpotThing(vm, "spot_6", Modifier.align(Alignment.BottomEnd).padding(bottom = 68.dp))
                        PetSprite(
                            speciesId = pet.speciesId, colorId = pet.colorId, stage = vm.economy.stageIndex(pet.growth), face = vm.face, animate = LocalAnimate.current,
                            size = petSize, bounceKey = vm.bounce, action = LocalPetAction.current.action, actionKey = LocalPetAction.current.key, seen = LocalPetAction.current,
                            description = "${pet.name}. ${vm.economy.stageTitle(pet.growth)}. Нажми — что на уме",
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 64.dp).particleTarget(particles, "pet"),
                            onTap = vm::petTapped,
                        )
                        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(64.dp)) {
                            SpotThing(vm, "spot_4", Modifier.align(Alignment.CenterStart))
                            Target("Кровать: сон", 128.dp, 64.dp, Modifier.align(Alignment.Center), onClick = onBed) {
                                Row(verticalAlignment = Alignment.CenterVertically) { TText("🛏", style = MaterialTheme.typography.titleLarge, maxLines = 1); Spacer(Modifier.width(6.dp)); TText("Сон", style = label, maxLines = 1) }
                            }
                            SpotThing(vm, "spot_5", Modifier.align(Alignment.CenterEnd))
                        }
                    }
                    Column(Modifier.fillMaxHeight(), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.SpaceBetween) {
                        Target("Дверь: на улицу", 64.dp, 136.dp, color = G.goldDark, onClick = { vm.navigate(Screen.Street) }) {
                            Box(Modifier.align(Alignment.CenterEnd).padding(end = 8.dp).size(8.dp).background(G.purpleDeep, CircleShape))
                            TText("Улица", style = label, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp), maxLines = 1)
                        }
                        Mailbox(mail, Modifier.particleTarget(particles, "mail")) { onPanel("mail") }
                    }
                }
                PetBubble(vm)
            }
        }
    }
}

/** Shelf with the jars: empty before the plan, the numbers after it (→ Jars). */
@Composable
private fun Shelf(vm: GameViewModel) {
    val s = vm.state
    val planned = s.plan.confirmed
    val nums = listOf(s.jarNeed, s.jarWant, s.reserve)
    val desc = if (planned) "Банки: Нужное ${nums[0]}, Хочу ${nums[1]}, Запас ${nums[2]}" else "Банки: разложи монеты"
    Target(desc, 152.dp, 72.dp, onClick = { vm.navigate(Screen.Jars) }) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                listOf(R.drawable.ui_lid_mandatory, R.drawable.ui_lid_optional, R.drawable.ui_lid_savings).forEachIndexed { i, lid ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(36.dp)) {
                            Image(painterResource(R.drawable.ui_jar), null, Modifier.align(Alignment.BottomCenter).size(32.dp))
                            Image(painterResource(lid), null, Modifier.align(Alignment.TopCenter).size(22.dp))
                        }
                        if (planned) TText("${nums[i]}", style = MaterialTheme.typography.titleSmall, maxLines = 1)
                    }
                }
            }
            if (!planned) TText("Разложи", style = MaterialTheme.typography.titleSmall, maxLines = 1)
        }
    }
}

/** A thing on a room spot: a starter or a placed item, ≈ 36 dp, no touch; an empty spot is not drawn. */
@Composable
private fun SpotThing(vm: GameViewModel, spotId: String, modifier: Modifier) {
    val starter = vm.tc.homeItems.firstOrNull { it.spot == spotId }
    val placed = vm.state.placed[spotId]?.let(vm::item)
    when {
        starter != null -> Pic(null, starter.emoji, 36.dp, modifier)
        placed != null -> Pic(itemRes(placed.id), placed.emoji, 36.dp, modifier.clearAndSetSemantics {})
    }
}

/** Mailbox «✉ +N»: only the envelope pulses, and only while it has coins and animations are on (§9.2 №17). */
@Composable
private fun Mailbox(mail: Int, modifier: Modifier, onClick: () -> Unit) {
    val pulse = mail > 0 && LocalAnimate.current
    val scale = if (pulse) rememberInfiniteTransition(label = "mail").animateFloat(1f, 1.15f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "pulse").value else 1f
    Target("Почтовый ящик: плюс $mail придёт с новым конвертом", 48.dp, 48.dp, modifier, onClick = onClick) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Canvas(Modifier.size(22.dp, 14.dp).graphicsLayer { scaleX = scale; scaleY = scale }) { // envelope: an emoji line is taller than 24 dp at 1.3
                val st = Stroke(2.dp.toPx())
                drawRect(G.purpleDeep, style = st)
                drawPath(Path().apply { moveTo(0f, 0f); lineTo(size.width / 2, size.height * 0.6f); lineTo(size.width, 0f) }, G.purpleDeep, style = st)
            }
            TText("+$mail", style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

/** What is on the pet's mind (§A): the line over its head and, for a need, the way to the shop or the street. */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.PetBubble(vm: GameViewModel) {
    val line = vm.petLine ?: return
    val shop = line.shop?.let { id -> vm.tc.shops.firstOrNull { it.id == id } }
    Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 176.dp).fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(20.dp)).background(Color.White, RoundedCornerShape(20.dp)).clickable(onClickLabel = "Закрыть") { vm.closePetLine() }.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TText(line.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f).padding(start = 4.dp), maxLines = 3)
            when {
                shop != null -> GameButton(shop.title, minHeight = 48.dp) { vm.openPlace(shop.place) }
                line.need != null -> GameButton("На улицу", minHeight = 48.dp) { vm.navigate(Screen.Street) }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(72.dp))
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(22.dp, 12.dp)) { drawPath(Path().apply { moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width / 2, size.height); close() }, Color.White) }
            }
            Spacer(Modifier.width(72.dp))
        }
    }
}

/** Bottom row 5 × 72 dp: Банки, Лавки, Копилка, События, Дневник; above 1.15 font only icons. */
@Composable
private fun BottomRow(vm: GameViewModel) {
    val big = bigFont()
    Row(Modifier.fillMaxWidth().height(72.dp).testTag("bottom_row")) {
        listOf<Triple<Int, String, () -> Unit>>(
            Triple(R.drawable.ui_jar, "Банки") { vm.navigate(Screen.Jars) },
            Triple(R.drawable.ui_bag, "Лавки") { vm.openPlace(vm.lastShop) },
            Triple(R.drawable.ui_piggy, "Копилка") { vm.navigate(Screen.Savings) },
            Triple(R.drawable.ui_book, "События") { vm.navigate(Screen.Board) },
            Triple(R.drawable.ui_trophy, "Дневник") { vm.navigate(Screen.Progress) },
        ).forEach { (icon, word, go) ->
            // clearAndSetSemantics: one merged target per button, not a stray node from the icon or the label (правка №6)
            Column(
                Modifier.weight(1f).fillMaxHeight().clickable(role = Role.Button, onClick = go).clearAndSetSemantics { contentDescription = word },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
            ) {
                Image(painterResource(icon), null, Modifier.size(if (big) 44.dp else 36.dp))
                // 14 sp: UX_ACCESSIBILITY.md «Исключения: 14 sp», нижний ряд (16 sp не влезает в 72 dp)
                // dark, not white on the light room background (TOWN-A1c) (правка R7, ТЗ 3.6)
                if (!big) TText(word, style = MaterialTheme.typography.labelSmall, color = G.ink, maxLines = 1)
            }
        }
    }
}

/** Envelope panel (§D.3): what comes with the next envelope and why the salary waits. */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.EnvelopePanel(vm: GameViewModel, close: () -> Unit) {
    val env = vm.state.envelope
    var why by remember { mutableStateOf(false) }
    val lines = env.map { "${it.text} +${it.amount}" } + if (why) listOf("Зарплату приносит почтальон вместе с карманными — сначала работа, потом зарплата") else emptyList()
    Ask(
        "Придёт с новым конвертом: +${env.sumOf { it.amount }}", lines,
        if (why) listOf("Понятно" to close) else listOf("Почему?" to { why = true }, "Понятно" to close), close, primary = if (why) 0 else 1,
    )
}

/** Last day of the week: the bed asks first (§D.6). */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.BedAsk(vm: GameViewModel, close: () -> Unit) {
    val preview = vm.weekEndPreview()
    val buttons: List<Pair<String, () -> Unit>> = if (preview.isEmpty()) {
        listOf("Спать" to { close(); vm.endWeek() }, "Отмена" to close)
    } else {
        val shopId = vm.town.petLine(vm.state, 0).shop
        val shop = vm.tc.shops.firstOrNull { it.id == shopId }
        listOf(
            (shop?.title ?: "На улицу") to { close(); if (shop != null) vm.openPlace(shop.place) else vm.navigate(Screen.Street) },
            "Всё равно спать" to { close(); vm.endWeek() },
        )
    }
    Ask("Закончить неделю?", preview, buttons, close)
}

