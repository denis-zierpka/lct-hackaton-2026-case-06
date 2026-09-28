package ru.finny.pet.game.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import ru.finny.pet.R
import ru.finny.pet.domain.Category
import ru.finny.pet.domain.ShopItem
import ru.finny.pet.domain.town.EventDef
import ru.finny.pet.domain.town.Fact
import ru.finny.pet.domain.town.Job
import ru.finny.pet.domain.town.JobGame
import ru.finny.pet.domain.town.PayKind
import ru.finny.pet.domain.town.PayOption
import ru.finny.pet.domain.town.Place
import ru.finny.pet.domain.town.Prices
import ru.finny.pet.domain.town.ShelfItem
import ru.finny.pet.domain.town.Source
import ru.finny.pet.domain.town.Template
import ru.finny.pet.game.GameViewModel
import ru.finny.pet.game.LocalAnimate
import ru.finny.pet.game.LocalPetAction
import ru.finny.pet.game.Screen
import ru.finny.pet.game.ui.ButtonStyle
import ru.finny.pet.game.ui.G
import ru.finny.pet.game.ui.GameButton
import ru.finny.pet.game.ui.Hud1
import ru.finny.pet.game.ui.Hud2
import ru.finny.pet.game.ui.LocalParticles
import ru.finny.pet.game.ui.Panel
import ru.finny.pet.game.ui.PetSprite
import ru.finny.pet.game.ui.Pic
import ru.finny.pet.game.ui.ResidentPic
import ru.finny.pet.game.ui.SpeechBubble
import ru.finny.pet.game.ui.STATS
import ru.finny.pet.game.ui.StatsCollapsed
import ru.finny.pet.game.ui.TText
import ru.finny.pet.game.ui.bigFont
import ru.finny.pet.game.ui.chip
import ru.finny.pet.game.ui.itemRes
import ru.finny.pet.game.ui.particleTarget

/** A place of the town (§E.2, §E.3): a shop with the shelf and the pay panel, or a job with the order. */
@Composable
fun PlaceScreen(vm: GameViewModel, placeId: String) {
    val place = vm.tc.places.firstOrNull { it.id == placeId } ?: return
    if (place.template == Template.SHOP) ShopPlace(vm, place) else JobPlace(vm, place)
}

// ---------- SHOP (§E.2, macket MockShop) ----------

// no-break space: the icon never parts from its word
private fun ShopItem.tag(): String = when (category) {
    Category.MANDATORY -> "🍎 нужно"
    Category.OPTIONAL -> "🎈 хочу"
    Category.UNPLANNED -> ""
}

/** Nonzero effects: index in STATS and the amount. */
private fun ShopItem.effects(): List<Pair<Int, Int>> = listOf(hunger, clean, mood).withIndex().filter { it.value != 0 }.map { it.index to it.value }

private fun ShopItem.effectWords(): String = effects().joinToString(", ") { (i, v) -> "${STATS[i].word} +$v" }

/** The jar a purchase of this category is planned from: paying from it needs no extra question. */
private fun ShopItem.homeJar(): Source = when (category) {
    Category.MANDATORY -> Source.NEED
    Category.OPTIONAL -> Source.WANT
    Category.UNPLANNED -> Source.RESERVE
}

@Composable
private fun ShopPlace(vm: GameViewModel, place: Place) {
    val shop = vm.tc.shops.firstOrNull { it.place == place.id } ?: return
    val prices = remember { Prices(vm.content) }
    val animate = LocalAnimate.current
    var pick by remember(place.id) { mutableStateOf<String?>(null) }
    var ask by remember { mutableStateOf<PayOption?>(null) }
    var sheet by remember { mutableIntStateOf(0) }
    // after a purchase the pay panel stays while the pet at the counter plays the action, then closes
    var paid by remember { mutableIntStateOf(0) }
    LaunchedEffect(paid) { if (paid > 0) { if (animate) delay(1500); pick = null } }
    // the pay sheet hides LINE and clears the old one; a purchase's line waits until it closes (правка №2)
    LaunchedEffect(pick) { vm.cashOpen = pick != null; if (pick != null) vm.lines.clear() }
    DisposableEffect(Unit) { onDispose { vm.cashOpen = false } }
    fun pay(id: String, o: PayOption) { o.source?.let { vm.buy(id, shop.id, it); paid++ } }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Hud1(vm, inPlace = true)
            Hud2 { JarsHud(vm); Spacer(Modifier.weight(1f)); StatsCollapsed(vm) }
            ShopTabs(vm, place.id)
            Column(
                // bottom padding = the pay sheet: every card can be scrolled out from under it
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(8.dp)
                    .padding(bottom = with(LocalDensity.current) { (if (pick == null) 0 else sheet).toDp() }),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PlaceEvents(vm, place.id)
                val shelf = prices.shelf(vm.state, shop.id)
                // above 1.15: two wide columns so names have room, not the auto-fit narrow cards (правка №13)
                if (bigFont()) {
                    Column(Modifier.fillMaxWidth().testTag("shelf"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        shelf.chunked(2).forEach { row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { ItemCard(it, Modifier.weight(1f)) { pick = it.item.id } }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                } else {
                    FlowRow(Modifier.fillMaxWidth().testTag("shelf"), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        shelf.forEach { ItemCard(it, Modifier.width(104.dp)) { pick = it.item.id } }
                    }
                }
                vm.ordersAt(place.id).forEach { order ->
                    vm.tc.jobs.firstOrNull { it.id == order.params.job }?.let { OrderCard(vm, it, order) }
                }
            }
        }
        pick?.let { id ->
            PayPanel(vm, id, shop.id, Modifier.align(Alignment.BottomCenter).onSizeChanged { sheet = it.height }, onClose = { pick = null }) { o ->
                val item = vm.item(id)
                when (o.kind) {
                    PayKind.PAY -> if (o.source == item?.homeJar()) pay(id, o) else ask = o
                    PayKind.CHEAPER -> pick = o.itemId
                    PayKind.WAIT -> pick = null
                    PayKind.MAKE_GOAL -> { vm.makeGoal(o.itemId ?: id); pick = null }
                }
            }
        }
        ask?.let { o ->
            val close = { ask = null }
            Ask(o.label, o.preview, listOf("Да" to { close(); pick?.let { pay(it, o) }; Unit }, "Отмена" to close), close)
        }
    }
}

/** HUD-2 of a shop: after the plan three wallet sections (lid + number, the word under it), before it — «Не разложено». */
@Composable
private fun RowScope.JarsHud(vm: GameViewModel) {
    val s = vm.state
    if (!s.plan.confirmed) {
        // dark, not white on the light place background (TOWN-A1c) (правка R7, ТЗ 3.6)
        TText("Не разложено ${s.balance - s.plan.total}", style = MaterialTheme.typography.titleSmall, color = G.ink, maxLines = 1)
        return
    }
    val big = bigFont()
    listOf(
        Triple(R.drawable.ui_lid_mandatory, "Нужное", s.jarNeed),
        Triple(R.drawable.ui_lid_optional, "Хочу", s.jarWant),
        Triple(R.drawable.ui_lid_savings, "Запас", s.reserve),
    ).forEach { (lid, word, n) ->
        Column(Modifier.clearAndSetSemantics { contentDescription = "$word $n" }.padding(horizontal = 2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(lid), null, Modifier.size(20.dp))
                // dark, not white/pink on the light place background (TOWN-A1c) (правка R7, ТЗ 3.6)
                TText("$n", style = MaterialTheme.typography.titleMedium.copy(lineHeight = 22.sp), color = G.ink, maxLines = 1)
            }
            // 14 sp: UX_ACCESSIBILITY.md «Исключения: 14 sp» — подписи отделений HUD лавки
            if (!big) TText(word, style = MaterialTheme.typography.labelSmall, color = G.ink, maxLines = 1)
        }
    }
}

/** «Рынок у реки» / «У Фомы»: every switch goes through openPlace (prices, ENTER triggers). */
@Composable
private fun ShopTabs(vm: GameViewModel, placeId: String) {
    val street = vm.streetPlaces().map { it.id }
    Row(Modifier.fillMaxWidth().testTag("shop_tabs").padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        vm.tc.shops.filter { it.place in street }.forEach { sh ->
            GameButton(sh.title.removePrefix("Лавка ").trim('«', '»'), Modifier.weight(1f), minHeight = 48.dp, selected = sh.place == placeId) {
                if (sh.place != placeId) vm.openPlace(sh.place)
            }
        }
    }
}

@Composable
private fun ItemCard(it: ShelfItem, modifier: Modifier, onClick: () -> Unit) {
    val item = it.item
    val body = MaterialTheme.typography.bodyMedium
    val price = if (it.was != null) "${it.was}→${it.price}" else "${it.price}"
    val desc = listOf(item.title, "${it.price} монет", item.tag().drop(3), item.effectWords()).filter { s -> s.isNotBlank() }.joinToString(", ")
    Column(
        modifier.heightIn(min = 64.dp).background(Color.White, RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClick = onClick)
            .clearAndSetSemantics { contentDescription = desc }.padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Pic(itemRes(item.id), item.emoji, 40.dp)
        // 14 sp: UX_ACCESSIBILITY.md «Исключения: 14 sp» — названия товаров на карточках; hyphens = None: no mid-word
        // break, but a real «-» (e.g. «Игрушка-подарок») still needs a break point — a zero-width space after it
        // gives the layout one, instead of an emergency mid-word cut (правка R4)
        TText(
            item.title.replace("-", "-​"), style = MaterialTheme.typography.labelSmall.copy(hyphens = Hyphens.None, lineBreak = LineBreak.Paragraph),
            maxLines = 2, align = TextAlign.Center,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            TText(price, style = MaterialTheme.typography.titleSmall, maxLines = 1)
            Image(painterResource(R.drawable.ui_coin), null, Modifier.size(18.dp))
        }
        if (it.was != null) TText("такая распродажа бывает", style = body, maxLines = 4, align = TextAlign.Center)
        if (item.tag().isNotEmpty()) {
            // above 1.15 the word alone (drop(3): the icon and the no-break space)
            if (bigFont()) TText(item.tag().drop(3), style = body, maxLines = 1) else TText(item.tag(), style = body, maxLines = 2, align = TextAlign.Center)
        }
        item.effects().forEach { (i, v) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(STATS[i].icon), null, Modifier.size(20.dp))
                TText("+$v", style = body, maxLines = 1)
            }
        }
    }
}

/** The pay sheet over the shelf: the pet at the counter, the item line, the quote and its options (main ones, the rest under «Ещё»). */
@Composable
private fun PayPanel(vm: GameViewModel, itemId: String, shopId: String, modifier: Modifier, onClose: () -> Unit, onOption: (PayOption) -> Unit) {
    val item = vm.item(itemId) ?: return
    val q = vm.quote(itemId, shopId)
    val pet = vm.state.pet
    val particles = LocalParticles.current
    val act = LocalPetAction.current
    val text = MaterialTheme.typography.bodyMedium
    var more by remember(itemId) { mutableStateOf(false) }
    Panel(modifier.padding(top = 56.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(8.dp).testTag("pay_panel"), padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (pet != null) PetSprite(
                speciesId = pet.speciesId, colorId = pet.colorId, stage = vm.economy.stageIndex(pet.growth), face = vm.face, animate = LocalAnimate.current,
                size = 64.dp, action = act.action, actionKey = act.key, seen = act, description = pet.name,
                modifier = Modifier.particleTarget(particles, "pet"),
            )
            TText("Касса", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 1)
            Box(Modifier.size(48.dp).background(G.paperTint, CircleShape).clickable(role = Role.Button, onClick = onClose).semantics { contentDescription = "Закрыть кассу" }, contentAlignment = Alignment.Center) {
                TText("✕", style = MaterialTheme.typography.titleMedium, color = G.purpleDeep, maxLines = 1)
            }
        }
        TText(listOf(item.title, "${q.price}", item.tag(), item.effectWords()).filter { it.isNotBlank() }.joinToString(" · "), style = text)
        if (q.line.isNotBlank()) TText(q.line, style = text)
        if (q.note.isNotBlank()) TText(q.note, style = text, color = G.inkSoft)
        // before the plan: [Разложить] under the quote's line, no jar to pay from yet (правка №5)
        if (!vm.state.plan.confirmed) GameButton("Разложить", Modifier.fillMaxWidth(), minHeight = 48.dp) { onClose(); vm.navigate(Screen.Jars) }
        val (main, rest) = q.options.partition { !it.more }
        val option: @Composable (PayOption) -> Unit = { o ->
            val home = o.kind == PayKind.PAY && o.source == item.homeJar()
            GameButton(o.label, Modifier.fillMaxWidth(), if (home) ButtonStyle.PRIMARY else ButtonStyle.PAPER, minHeight = 48.dp) { onOption(o) }
            o.preview.firstOrNull()?.let { TText(it, style = text, color = G.inkSoft) }
        }
        main.forEach { option(it) }
        if (rest.isNotEmpty()) {
            if (!more) GameButton("Ещё ▾", Modifier.fillMaxWidth(), ButtonStyle.PAPER, minHeight = 48.dp) { more = true }
            else rest.forEach { option(it) }
        }
    }
}

// ---------- JOB (§E.3, macket MockJob) ----------

@Composable
private fun JobPlace(vm: GameViewModel, place: Place) {
    val job = vm.tc.jobs.firstOrNull { it.place == place.id }
    val order = job?.let { j -> vm.ordersAt(place.id).firstOrNull { it.params.job == j.id } }
    Column(Modifier.fillMaxSize()) {
        Hud1(vm, inPlace = true)
        Hud2 { StatsCollapsed(vm); MailChip(vm) }
        // tray job: the order is a scene; place events push it down, never squeeze it below 360 dp (TOWN-J1-1a § 3)
        if (job?.game == JobGame.TRAY) BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val h = maxHeight
            var e by remember { mutableIntStateOf(0) }
            val eDp = with(LocalDensity.current) { e.toDp() }
            Column(Modifier.verticalScroll(rememberScrollState()).padding(8.dp)) {
                Column(Modifier.onSizeChanged { e = it.height }.padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TText(place.title, style = MaterialTheme.typography.headlineSmall, color = G.ink)
                    PlaceEvents(vm, place.id)
                }
                TrayJobScene(vm, job, order, Modifier.height(max(h - 16.dp - eDp, 360.dp)))
            }
        } else Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // dark, not white on the light place background (TOWN-A1c) (правка №14, ТЗ 3.6)
            TText(place.title, style = MaterialTheme.typography.headlineSmall, color = G.ink)
            PlaceEvents(vm, place.id)
            if (job != null) OrderCard(vm, job, order)
        }
    }
}

private fun dot(t: String) = if (t.endsWith(".") || t.endsWith("!") || t.endsWith("?")) t else "$t."

/** The order of a tray job in the scene: Borya behind the counter, a bubble with the pay and shifts, «Начать смену». */
@Composable
private fun TrayJobScene(vm: GameViewModel, job: Job, order: EventDef?, modifier: Modifier) {
    val q = vm.shiftQuote(job.id)
    val resident = vm.tc.residents.firstOrNull { it.id == job.resident }
    val text = order?.intro ?: resident?.lines?.firstOrNull() ?: job.title
    val intro = vm.trayIntro(job.id)
    val max = vm.tc.rules.shiftsPerWeek
    val n = vm.state.shiftsThisPeriod.coerceIn(0, max)
    val desc = if (q.canPlay) "Заказ: ${dot(text)}" + (intro?.let { " ${dot(it)}" } ?: "") + " Оплата от ${q.base} до ${q.top} монет. Смены: $n из $max"
    else "${dot(q.line)} Смены: $n из $max"
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val f = (maxHeight - 72.dp - 72.dp + 56.dp - 8.dp).coerceIn(160.dp, 264.dp)
        // «Домой» после лимита стоит на месте «Начать смену» — раскладка сцены одна для обоих состояний (№ 59 б)
        val counterTop = maxHeight - 72.dp - 72.dp
        if (resident != null) ResidentPic(resident, f, Modifier.offset(x = 8.dp - f * 0.3f, y = counterTop + 56.dp - f))
        Counter(Modifier.offset(y = counterTop).fillMaxWidth().height(72.dp))
        val bx = 8.dp + f * 0.45f
        // at Borya's head (frame top + 0.1·F), lifted so its bottom stays 4 dp above the counter, never above y 8
        val head = counterTop + 56.dp - f + f * 0.1f
        SpeechBubble(
            Modifier.layout { m, c ->
                val pl = m.measure(c)
                val y = minOf(head.roundToPx(), (counterTop - 4.dp).roundToPx() - pl.height).coerceAtLeast(8.dp.roundToPx())
                layout(pl.width, pl.height) { pl.place(bx.roundToPx(), y) }
            }.width(maxWidth - bx).heightIn(max = counterTop - 12.dp).clearAndSetSemantics { contentDescription = desc },
        ) {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (q.canPlay) {
                    TText(text, style = MaterialTheme.typography.titleMedium, color = G.ink)
                    if (intro != null) TText(intro, style = MaterialTheme.typography.bodyLarge, color = G.purple)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TText("${q.base}–${q.top}", style = MaterialTheme.typography.headlineMedium, color = G.purple, maxLines = 1)
                        Image(painterResource(R.drawable.ui_coin), null, Modifier.size(32.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TText("Смены", maxLines = 1)
                        ShiftTokens(vm, label = false)
                    }
                } else {
                    TText(q.line)
                    ShiftTokens(vm, label = false)
                }
            }
        }
        if (q.canPlay) GameButton("Начать смену", Modifier.align(Alignment.BottomCenter).fillMaxWidth(), ButtonStyle.PRIMARY, minHeight = 56.dp) { vm.startRound(job.id) }
        else GameButton("Домой", Modifier.align(Alignment.BottomCenter).fillMaxWidth(), ButtonStyle.PRIMARY, minHeight = 56.dp) { vm.home() }
    }
}

/** «✉ +N» of HUD-2: what comes with the next envelope; the target the shift pay flies to. */
@Composable
internal fun MailChip(vm: GameViewModel) {
    val mail = vm.state.envelope.sumOf { it.amount }
    Box(
        Modifier.heightIn(min = 44.dp).chip().particleTarget(LocalParticles.current, "mail").clearAndSetSemantics { contentDescription = "Придёт с новым конвертом: $mail" }
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) { TText("✉ +$mail", style = MaterialTheme.typography.titleMedium, color = G.purpleDeep, maxLines = 1) }
}

/** «●●○»: shifts of this week out of shiftsPerWeek; the numbers are for TalkBack. */
@Composable
internal fun ShiftTokens(vm: GameViewModel, label: Boolean = true, color: Color = G.ink) {
    val max = vm.tc.rules.shiftsPerWeek
    val n = vm.state.shiftsThisPeriod.coerceIn(0, max)
    val dots = "●".repeat(n) + "○".repeat(max - n)
    TText(if (label) "Смены на неделе: $dots" else dots, Modifier.clearAndSetSemantics { contentDescription = "Смены: $n из $max" }, color = color, maxLines = if (label) 2 else 1)
}

/** Events of this place: title, intro and «Пройти мимо» when the event has a Skip outcome. */
@Composable
internal fun PlaceEvents(vm: GameViewModel, placeId: String) {
    vm.eventsAt(placeId).forEach { e ->
        Panel(Modifier.fillMaxWidth(), padding = 12.dp) {
            TText("! ${e.title}", style = MaterialTheme.typography.titleMedium)
            TText(e.intro)
            if (e.outcomes.any { it.fact == Fact.Skip }) GameButton("Пройти мимо", Modifier.fillMaxWidth(), ButtonStyle.PAPER, minHeight = 48.dp) { vm.pass(e.id) }
        }
    }
}

/** The order card: the resident, the order's intro, the pay line, the shift tokens, «Начать смену» and the riddle of a match-3 job. */
@Composable
private fun OrderCard(vm: GameViewModel, job: Job, order: EventDef?) {
    val q = vm.shiftQuote(job.id)
    val resident = vm.tc.residents.firstOrNull { it.id == job.resident }
    Panel(Modifier.fillMaxWidth().testTag("job_order"), padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (resident != null) Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ResidentPic(resident, 72.dp)
                TText(resident.name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
            }
            TText(order?.intro ?: resident?.lines?.firstOrNull() ?: job.title, modifier = Modifier.weight(1f))
        }
        TText(q.line)
        ShiftTokens(vm)
        if (q.canPlay) GameButton("Начать смену", Modifier.fillMaxWidth(), minHeight = 48.dp) { vm.startRound(job.id) }
        if (job.game == JobGame.MATCH3) Riddle(vm)
    }
}

/** «Загадка Бори»: offer → question with options → answerRiddle; «Нет, спасибо» hides it until the screen comes back. */
@Composable
private fun Riddle(vm: GameViewModel) {
    val q = vm.nextQuestion() ?: return
    var step by remember(q.id) { mutableIntStateOf(0) }
    when (step) {
        0 -> {
            TText("Загадка Бори: отгадаешь — бомбочка")
            // heightIn(min = 48.dp) OUTER, then height(IntrinsicSize.Min): the row is coerced to ≥ 48 dp first, then
            // both buttons fillMaxHeight to that same coerced height — equal size, never below 48 dp at 1.0 or 1.3 (R8)
            Row(Modifier.heightIn(min = 48.dp).height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GameButton("Ответить", Modifier.weight(1f).fillMaxHeight(), ButtonStyle.PAPER, minHeight = 48.dp) { step = 1 }
                GameButton("Нет, спасибо", Modifier.weight(1f).fillMaxHeight(), ButtonStyle.PAPER, minHeight = 48.dp) { step = 2 }
            }
        }
        1 -> {
            TText(q.question)
            q.options.forEachIndexed { i, o -> GameButton(o, Modifier.fillMaxWidth(), ButtonStyle.PAPER, minHeight = 48.dp) { vm.answerRiddle(q.id, i) } }
        }
    }
}
