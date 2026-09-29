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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
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
import ru.finny.pet.game.ui.GoalPic
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
        if (card.eventId != null) Box(Modifier.size(28.dp).background(Color(0xFFE0004A), CircleShape), contentAlignment = Alignment.Center) {
            TText("!", style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 1)
        }
        TText(text, style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 18.sp), modifier = Modifier.weight(1f), maxLines = 2)
        TText("→", style = MaterialTheme.typography.titleMedium, color = G.purpleDeep, maxLines = 1)
    }
}

/** Floor line of room_port_day (room.py, 1080 × 1920 px): the back wall meets the floor at y = 1228 (TOWN-A1d2, № 76 а). */
private const val FLOOR_PX = 1228f

/** The room as a scheme (№ 120–124): along the wall on the floor line — fridge, chest, armchair, door; on the floor — bed, pet, mailbox. */
@Composable
private fun Room(vm: GameViewModel, modifier: Modifier, onPanel: (String) -> Unit, onBed: () -> Unit) {
    val s = vm.state
    val pet = s.pet ?: return
    // a light halo: labels lie right on the sprites («Список» on the fridge's dark rim, A1d1 judges)
    val label = MaterialTheme.typography.labelSmall.copy(shadow = Shadow(Color.White, blurRadius = 8f))
    val particles = LocalParticles.current
    val resident = vm.town.residentOfWeek(s)
    val streetEvent = vm.town.activeEvents(s).any { it.place != null && it.place != "home" }
    val mail = s.envelope.sumOf { it.amount }
    val dream = s.achievedGoals.lastOrNull()
    val density = LocalDensity.current
    var floorY by remember { mutableStateOf(0.dp) } // № 76 а: the floor line in the box below; 0 — the first frame
    var bedH by remember { mutableStateOf(162.dp) }
    Box(modifier.fillMaxWidth().testTag("room")) {
        // № 123: the clock just under the jar shelf, right of the window — a sprite held by the top row, not drawn in the Crop
        // background (S23 ×1,22 put it 69 dp under the shelf); 360 × 640 where room_port_day had it: 119..161 × 271..313 dp
        Image(painterResource(R.drawable.furn_clock), null, Modifier.offset(119.dp, 77.dp).size(42.dp))
        Column(Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
            Row(Modifier.fillMaxWidth().height(88.dp).padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Target("Окно: улица" + (resident?.let { ", машет ${it.name}" } ?: "") + (if (streetEvent) ", есть событие" else "") + (dream?.let { ", у забора мечта «${it.title}»" } ?: ""), 112.dp, 80.dp, color = Color.Transparent, onClick = { vm.navigate(Screen.Street) }) {
                    Image(painterResource(R.drawable.furn_window), null, Modifier.fillMaxSize())
                    // feet on the sill (71 dp of furn_window): 492 of the 512 frame = 61,5 dp down the 64 dp resident (TOWN-A1f)
                    if (resident != null) ResidentPic(resident, 64.dp, Modifier.align(Alignment.TopStart).padding(start = 8.dp, top = 10.dp))
                    // decision 25: the last reached dream on the grass by the fence behind the glass (fence 50..62, grass 62,7..65 dp), right of the resident
                    dream?.let { GoalPic(it, 24.dp, Modifier.align(Alignment.TopStart).padding(start = 60.dp, top = 42.dp)) }
                    // № 75 а, № 85 б: #E0004A 28 dp in a white 2 dp ring, 32 dp; the «!» line (31 dp at 1.3) is not clipped by the circle
                    if (streetEvent) Box(Modifier.align(Alignment.TopEnd).padding(4.dp).size(32.dp).background(Color.White, CircleShape).padding(2.dp).background(Color(0xFFE0004A), CircleShape), contentAlignment = Alignment.Center) {
                        TText("!", style = MaterialTheme.typography.titleMedium, modifier = Modifier.wrapContentSize(unbounded = true), color = Color.White, maxLines = 1)
                    }
                }
                Shelf(vm)
                Target("Копилка ${s.savings}", 64.dp, 64.dp, onClick = { vm.navigate(Screen.Savings) }) {
                    Image(painterResource(R.drawable.ui_piggy), null, Modifier.size(52.dp))
                }
            }
            Box(Modifier.fillMaxWidth().weight(1f).padding(top = 4.dp, bottom = 4.dp).onGloballyPositioned { c ->
                // № 76 а: the wall things stand on the floor line of room_port_day, drawn Crop over the whole window (GameApp.RoomBackground)
                val root = c.findRootCoordinates().size
                val k = maxOf(root.width / 1080f, root.height / 1920f)
                val floor = with(density) { (root.height / 2f + (FLOOR_PX - 960f) * k - c.positionInRoot().y).toDp() }
                val zone = with(density) { c.size.height.toDp() }
                // not below «zone − 124»: the pet (120) stands under the armchair; 360 × 640: 123 of 254, S23: ≈ 220 of 410
                floorY = floor.coerceAtMost(zone - 124.dp)
                // № 121: the bed up to 150 × 162 dp, 12 dp over the bottom and under the chest's front edge (floor + 13); 360 × 640: 106
                bedH = (zone - 12.dp - floorY - 13.dp).coerceIn(48.dp, 162.dp)
            }) {
                // a thing by the wall: x from the box side, its back-bottom edge on the floor line, so the frame bottom is `d` lower — the
                // depth of the thing in its own sprite (room.py camera: fridge 9, chest 13, armchair 20, a 36 dp thing 3); the door is in
                // the wall, d = 0. Lambda offset — floorY is State (lint UseOfNonLambdaOffsetOverload)
                fun Modifier.wall(x: Dp, h: Dp, d: Dp = 0.dp) = offset { IntOffset(x.roundToPx(), (floorY + d - h).roundToPx()) }
                // wall picture right of the clock (360 × 640 119..161 × 271..313 dp), left of the armchair's back
                SpotThing(vm, "spot_2", Modifier.padding(start = 154.dp, top = 2.dp))
                Target("Холодильник: список нужного и цены", 64.dp, 96.dp, Modifier.wall(0.dp, 96.dp, 9.dp), color = Color.Transparent, onClick = { onPanel("fridge") }) {
                    Image(painterResource(R.drawable.furn_fridge), null, Modifier.fillMaxSize())
                    TText("Список", style = label, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp).wrapContentWidth(unbounded = true), maxLines = 1)
                }
                // № 122: the chest right of the fridge; spot_3 («стол») on its lid (the dome is 9 dp under the 48 dp frame top)
                Target("Сундук: обустроить комнату", 64.dp, 48.dp, Modifier.wall(68.dp, 48.dp, 13.dp), color = Color.Transparent, onClick = { vm.navigate(Screen.Arrange) }) { Image(painterResource(R.drawable.furn_chest), null, Modifier.fillMaxSize()) }
                SpotThing(vm, "spot_3", Modifier.wall(82.dp, 72.dp, 13.dp))
                SpotThing(vm, "spot_5", Modifier.wall(132.dp, 36.dp, 3.dp))
                Target("Дверь: на улицу", 64.dp, 136.dp, Modifier.align(Alignment.TopEnd).wall(0.dp, 136.dp), color = Color.Transparent, onClick = { vm.navigate(Screen.Street) }) {
                    Image(painterResource(R.drawable.furn_door), null, Modifier.fillMaxSize())
                    TText("Улица", style = label, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp), maxLines = 1)
                }
                // № 120: the armchair (starter of spot_4) 108 dp by the door, 4 dp off it; «Словарик» on its seat (the cushion 73..84 dp of the sprite)
                SpotThing(vm, "spot_4", Modifier.align(Alignment.TopEnd).wall((-68).dp, 108.dp, 20.dp), 108.dp)
                Target("Словарик", 48.dp, 48.dp, Modifier.align(Alignment.TopEnd).wall((-98).dp, 65.dp, 20.dp), color = Color.Transparent, onClick = { vm.navigate(Screen.Glossary) }) { Image(painterResource(R.drawable.ui_book), null, Modifier.size(36.dp)) }
                // the floor, on the box bottom: № 121 the bed on the left edge headboard to the wall (furn_bed_v), in front of it spot_1 and spot_6,
                // the pet, № 124 the mailbox
                val bedW = bedH * (150f / 162f)
                Target("Кровать: сон", bedW, bedH, Modifier.align(Alignment.BottomStart).padding(bottom = 12.dp), color = Color.Transparent, onClick = onBed) {
                    Image(painterResource(R.drawable.furn_bed_v), null, Modifier.fillMaxSize())
                    TText("Сон", style = label, maxLines = 1)
                }
                SpotThing(vm, "spot_1", Modifier.align(Alignment.BottomStart).padding(start = bedW * 0.4f - 18.dp))
                SpotThing(vm, "spot_6", Modifier.align(Alignment.BottomStart).padding(start = bedW * 0.4f + 22.dp))
                PetSprite(
                    speciesId = pet.speciesId, colorId = pet.colorId, stage = vm.economy.stageIndex(pet.growth), face = vm.face, animate = LocalAnimate.current,
                    size = 120.dp, bounceKey = vm.bounce, action = LocalPetAction.current.action, actionKey = LocalPetAction.current.key, seen = LocalPetAction.current,
                    description = "${pet.name}. ${vm.economy.stageTitle(pet.growth)}. Нажми — что на уме",
                    // 12 dp right of the middle: between the bed and the mailbox (360 dp: 132..252)
                    modifier = Modifier.align(Alignment.BottomCenter).offset(x = 12.dp).particleTarget(particles, "pet"),
                    onTap = vm::petTapped,
                )
                Mailbox(mail, Modifier.align(Alignment.BottomEnd).particleTarget(particles, "mail")) { onPanel("mail") }
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
    Target(desc, 152.dp, 72.dp, color = Color.Transparent, onClick = { vm.navigate(Screen.Jars) }) {
        Image(painterResource(R.drawable.furn_shelf), null, Modifier.fillMaxSize())
        // jars stand on the board (42 dp of furn_shelf; ui_jar's glass ends 5 dp above its 36 dp box); offset, not padding: 36 + 26 dp at 1.3 would not fit
        Column(Modifier.align(Alignment.TopCenter).offset(y = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
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

/** A thing on a room spot: a starter or a placed item, 36 dp (the armchair 108, № 120), no touch; an empty spot is not drawn.
 *  A child's thing never stands on a starter's spot: Town.place refuses («Здесь стоит кресло»), «Обустроить» does not open it. */
@Composable
private fun SpotThing(vm: GameViewModel, spotId: String, modifier: Modifier, size: Dp = 36.dp) {
    val starter = vm.tc.homeItems.firstOrNull { it.spot == spotId }
    val placed = vm.state.placed[spotId]?.let(vm::item)
    when {
        starter != null -> Pic(itemRes(starter.id), starter.emoji, size, modifier)
        placed != null -> Pic(itemRes(placed.id), placed.emoji, size, modifier.clearAndSetSemantics {})
    }
}

/** Mailbox «✉ +N»: only the envelope pulses, and only while it has coins and animations are on (§9.2 №17). */
@Composable
private fun Mailbox(mail: Int, modifier: Modifier, onClick: () -> Unit) {
    val pulse = mail > 0 && LocalAnimate.current
    val scale = if (pulse) rememberInfiniteTransition(label = "mail").animateFloat(1f, 1.15f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "pulse").value else 1f
    // № 124: 96 dp; the envelope and «+N» twice as big, on the box body (5..77 × 15..62 dp of furn_mailbox, the post under it)
    Target("Почтовый ящик: плюс $mail придёт с новым конвертом", 96.dp, 96.dp, modifier, color = Color.Transparent, onClick = onClick) {
        Image(painterResource(R.drawable.furn_mailbox), null, Modifier.fillMaxSize())
        Column(Modifier.offset(x = (-7).dp, y = (-10).dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Canvas(Modifier.size(44.dp, 28.dp).graphicsLayer { scaleX = scale; scaleY = scale }) { // envelope: an emoji line is taller than 24 dp at 1.3
                val st = Stroke(4.dp.toPx())
                drawRect(G.purpleDeep, style = st)
                drawPath(Path().apply { moveTo(0f, 0f); lineTo(size.width / 2, size.height * 0.6f); lineTo(size.width, 0f) }, G.purpleDeep, style = st)
            }
            TText("+$mail", style = MaterialTheme.typography.titleMedium, maxLines = 1)
        }
    }
}

/** What is on the pet's mind (§A): the line over its head and, for a need, the way to the shop or the street. */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.PetBubble(vm: GameViewModel) {
    val line = vm.petLine ?: return
    val shop = line.shop?.let { id -> vm.tc.shops.firstOrNull { it.id == id } }
    Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 112.dp).fillMaxWidth()) { // the tail 8 dp into the top of the 120 dp pet on the floor
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

