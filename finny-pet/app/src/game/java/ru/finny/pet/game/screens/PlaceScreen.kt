package ru.finny.pet.game.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import ru.finny.pet.R
import ru.finny.pet.domain.Category
import ru.finny.pet.domain.ShopItem
import ru.finny.pet.domain.town.EventDef
import ru.finny.pet.domain.town.EventEffect
import ru.finny.pet.domain.town.Fact
import ru.finny.pet.domain.town.Job
import ru.finny.pet.domain.town.JobGame
import ru.finny.pet.domain.town.PayKind
import ru.finny.pet.domain.town.PayOption
import ru.finny.pet.domain.town.Place
import ru.finny.pet.domain.town.Prices
import ru.finny.pet.domain.town.Resident
import ru.finny.pet.domain.town.ShelfItem
import ru.finny.pet.domain.town.Showcase
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
import ru.finny.pet.game.ui.posterRes

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
    var page by remember(place.id) { mutableIntStateOf(0) }
    // after a purchase the pay panel stays while the pet at the counter plays the action, then closes
    var paid by remember { mutableIntStateOf(0) }
    LaunchedEffect(paid) { if (paid > 0) { if (animate) delay(1500); pick = null } }
    // the pay sheet hides LINE and clears the old one; a purchase's line waits until it closes (правка №2)
    LaunchedEffect(pick) { vm.cashOpen = pick != null; if (pick != null) vm.lines.clear() }
    DisposableEffect(Unit) { onDispose { vm.cashOpen = false } }
    fun pay(id: String, o: PayOption) { o.source?.let { vm.buy(id, shop.id, it); paid++ } }

    val pages = Showcase.pages(prices.shelf(vm.state, shop.id))
    // the last keep item of the last page bought — that page is gone, stay on the one before
    val p = page.coerceAtMost(pages.lastIndex).coerceAtLeast(0)
    // item → the event of this place about it: its poster hangs over the item, its intro and «Пройти мимо» are in the pay sheet
    val events = vm.eventsAt(place.id).mapNotNull { e -> Showcase.eventItem(e)?.let { it to e } }.toMap()
    // the title of the first event whose item is on page i: «!» on the pager, the button's state for TalkBack
    fun eventOn(i: Int) = pages.getOrNull(i).orEmpty().flatten().firstNotNullOfOrNull { it?.item?.id?.let(events::get)?.title }
    val scroll = rememberScrollState()
    LaunchedEffect(p) { scroll.scrollTo(0) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Hud1(vm, inPlace = true)
            Hud2 { JarsHud(vm); Spacer(Modifier.weight(1f)); StatsCollapsed(vm) }
            ShopTabs(vm, place.id)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                // under the pay sheet the showcase leaves the TalkBack tree (the scrim takes taps only); after the scroll — it keeps its own
                val under = if (pick != null) Modifier.clearAndSetSemantics {} else Modifier
                ShopScene(vm, place, pages.getOrElse(p) { emptyList() }, events, greet = p == 0, Modifier.verticalScroll(scroll).then(under), onPick = { pick = it }) {
                    if (p > 0) Pager("Назад", next = false, eventOn(p - 1)) { page = p - 1 }
                    if (p < pages.lastIndex) Pager("Ещё ${pages[p + 1].flatten().count { it != null }}", next = true, eventOn(p + 1)) { page = p + 1 }
                }
                pick?.let { id ->
                    Box(Modifier.fillMaxSize().background(SCRIM).pointerInput(Unit) { detectTapGestures { pick = null } })
                    Kassa(vm, id, shop.id, prices.was(vm.state, id, shop.id), events[id], Modifier.align(Alignment.BottomCenter), onClose = { pick = null }) { o ->
                        val item = vm.item(id)
                        when (o.kind) {
                            PayKind.PAY -> if (o.source == item?.homeJar()) pay(id, o) else ask = o
                            PayKind.CHEAPER -> pick = o.itemId
                            PayKind.WAIT -> pick = null
                            PayKind.MAKE_GOAL -> { vm.makeGoal(o.itemId ?: id); pick = null }
                        }
                    }
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

// ---------- showcase (A1s, решение 39 а, макеты a1s_mock_1–5; формы — mock.py plastic/board/tag/poster/bubble) ----------

private val ITEM = 60.dp        // an item standing on a board
private val HANG = 20.dp        // board line → top of its tag
private val HANG_C = 6.dp       // counter line → top of its tag: the counter row is shorter, the floor stays on the first screen
private val SELLER = 170.dp     // ResidentPic box: the figure ≈ 150 dp (решение 39 а: продавец крупно)
private val TILT = floatArrayOf(-3f, 2f, -2f, 3f, -1f, 2f, -2f, 1f, 3f)   // tags hang a little crooked; static, not an animation
private val SHADE = Color(0xFF462A5A)
private val DEEP = Color(0xFFBE003A)     // sale price and the poster's first line: contrast ≥ 4.5 on the tag and the poster
private val OLD = Color(0xFF5C426C)      // the struck old price
private val STRING = Color(0xFF966E5A)
private val SCRIM = Color(0x78281446)
private enum class Tail { DOWN, LEFT, RIGHT }

/** Paper and edge of a tag and of the pay sheet by department; the lid tells the department, the colour only helps (ТЗ 3.6). */
private fun Category.paper(): Pair<Color, Color> = when (this) {
    Category.MANDATORY -> Color(0xFFFFF3F4) to Color(0xFFFF8AA0)
    Category.OPTIONAL -> Color(0xFFFFF7D6) to Color(0xFFF0B834)
    Category.UNPLANNED -> Color.White to Color(0xFFC8B4EB)
}

/** The jar a department is planned from (HUD-2): its lid on the tag, lid and word in the pay sheet. */
private fun Category.jar(): Pair<Int, String> = when (this) {
    Category.MANDATORY -> R.drawable.ui_lid_mandatory to "Нужное"
    Category.OPTIONAL -> R.drawable.ui_lid_optional to "Хочу"
    Category.UNPLANNED -> R.drawable.ui_lid_savings to "Запас"
}

/** Icon of a pay option's source: the lid of the jar the coins come from, the piggy for the savings. */
private fun Source.lid(): Int = when (this) {
    Source.NEED, Source.TRANSFER_NEED -> R.drawable.ui_lid_mandatory
    Source.WANT, Source.TRANSFER_WANT -> R.drawable.ui_lid_optional
    Source.RESERVE -> R.drawable.ui_lid_savings
    Source.SAVINGS -> R.drawable.ui_piggy
}

/** mock.py plastic(): a shade under it, a darker «lip» below the face, a white gloss fading to the middle. */
private fun DrawScope.plastic(at: Offset, sz: Size, color: Color, r: Dp, lip: Dp, border: Color? = null, bw: Dp = 0.dp, shade: Boolean = true) {
    val rr = CornerRadius(r.toPx())
    val face = Size(sz.width, sz.height - lip.toPx())
    if (shade) drawRoundRect(SHADE.copy(alpha = 0.2f), at + Offset(0f, lip.toPx() + 3.dp.toPx()), face, rr)
    if (lip > 0.dp) drawRoundRect(lerp(border ?: color, Color.Black, 0.22f), at + Offset(0f, lip.toPx()), face, rr)
    drawRoundRect(color, at, face, rr)
    if (border != null) {
        val b = bw.toPx()
        drawRoundRect(border, at + Offset(b / 2, b / 2), Size(face.width - b, face.height - b), CornerRadius(r.toPx() - b / 2), style = Stroke(b))
    }
    val i = bw.toPx() + 2.dp.toPx()
    drawRoundRect(
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.4f), Color.Transparent), at.y + i, at.y + face.height / 2),
        at + Offset(i, i), Size(face.width - 2 * i, face.height / 2 - i), CornerRadius((r.toPx() - i).coerceAtLeast(1f)),
    )
}

/** Plastic behind the node; the content keeps off the lip. */
private fun Modifier.plastic(color: Color, r: Dp, lip: Dp, border: Color? = null, bw: Dp = 0.dp) =
    drawBehind { plastic(Offset.Zero, size, color, r, lip, border, bw) }.padding(bottom = lip)

/** A board (mock.py board): the lighter top face — a trapezoid behind the items' feet at [y] — and the front below. */
private fun DrawScope.board(y: Float, x0: Float, x1: Float, wood: Color) {
    val d = 1.dp.toPx()
    drawPath(Path().apply { moveTo(x0 + 5.3f * d, y - 8.7f * d); lineTo(x1 - 5.3f * d, y - 8.7f * d); lineTo(x1, y + 1.3f * d); lineTo(x0, y + 1.3f * d); close() }, lerp(wood, Color.White, 0.35f))
    plastic(Offset(x0, y + 0.7f * d), Size(x1 - x0, 20 * d), wood, 5.3.dp, 2.dp)
}

/** The counter the last board stands on: the top face, a wooden edge and the front the tags hang over, down to [bottom]. */
private fun DrawScope.counter(y: Float, bottom: Float, wood: Color, front: Color) {
    val d = 1.dp.toPx()
    val (x0, x1) = 8 * d to size.width - 8 * d
    drawPath(Path().apply { moveTo(x0 + 5.3f * d, y - 8.7f * d); lineTo(x1 - 5.3f * d, y - 8.7f * d); lineTo(x1, y + 1.3f * d); lineTo(x0, y + 1.3f * d); close() }, lerp(wood, Color.White, 0.35f))
    plastic(Offset(x0, y + 6 * d), Size(x1 - x0, bottom - y - 6 * d), front, 6.dp, 3.dp)
    plastic(Offset(x0, y + 0.7f * d), Size(x1 - x0, 9 * d), wood, 4.dp, 0.dp, shade = false)
}

/** «У Фомы»: the back wall of the rack with two posts; hides the rack drawn into bg_foma_port (mock.py foma_rack). */
private fun DrawScope.backWall(bottom: Float) {
    val d = 1.dp.toPx()
    drawRect(Color(0xFFF0DECC), Offset(20 * d, 0f), Size(size.width - 40 * d, bottom))
    for (x in listOf(10 * d, size.width - 24 * d)) plastic(Offset(x, 0f), Size(14 * d, bottom), Color(0xFFE2B076), 4.dp, 0.dp, shade = false)
}

/**
 * The showcase of a shop page: boards of 3 items with price tags on strings, the last board of the page is the counter;
 * event posters over their items; the seller large on the floor with a bubble, the place's first order (Marta: crates,
 * «Начать смену»; not filtered by resident — another resident's order would stand in the seller's bubble, ANTI-SCOPE), the
 * pager. One page is at most 3 boards (Showcase.pages).
 */
@Composable
private fun ShopScene(
    vm: GameViewModel, place: Place, rows: List<List<ShelfItem?>>, events: Map<String, EventDef>, greet: Boolean,
    modifier: Modifier, onPick: (String) -> Unit, pager: @Composable () -> Unit,
) {
    val tagH = with(LocalDensity.current) { 19.sp.toDp() } + 22.dp
    val slab = HANG_C + tagH + 6.dp
    // ponytail: art by place id, like placeBackground — «У Фомы» is a room with a rack, the market is open air
    val indoor = place.id == "foma"
    val (wood, front) = if (indoor) Color(0xFFECBE86) to Color(0xFFFAE07F) else Color(0xFFF5C892) to Color(0xFF8FD9BE)
    val resident = vm.tc.residents.firstOrNull { it.id == place.resident }
    val order = vm.ordersAt(place.id).firstOrNull()
    val job = order?.let { o -> vm.tc.jobs.firstOrNull { it.id == o.params.job } }
    val talk = order != null || greet
    // the seller's bubble in the empty left slot of the board over the counter (макет 1); no such slot or any font above 1.0 —
    // on the floor, up to 240 dp (at 1.15 the slot of 124 dp broke words and the bubble grew over the tag above)
    val scaled = LocalDensity.current.fontScale > 1f
    val inSlot = talk && !scaled && rows.size >= 2 && rows[rows.size - 2][0] == null
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val w = maxWidth
        Column {
            Column(Modifier.testTag("shelf").drawBehind { if (indoor && rows.isNotEmpty()) backWall(size.height - slab.toPx()) }) {
                // ponytail: the seller reaches ≈ 160 dp over the floor — a page of one board gets room above it
                Spacer(Modifier.height(maxOf(2.dp, 160.dp - 125.dp * rows.size)))
                rows.forEachIndexed { r, row ->
                    val counter = r == rows.lastIndex
                    val (start, end) = if (counter) 76.dp to 16.dp else 19.dp to 19.dp
                    val slot = (w - start - end) / 3
                    val posters = row.withIndex().mapNotNull { (i, s) -> s?.let { events[it.item.id] }?.let { Triple(i, s, it) } }
                    if (posters.isNotEmpty()) Box(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                        val pw = if (posters.size == 1) minOf(250.dp, w - 24.dp) else (w - 24.dp) / 2 - 4.dp
                        posters.forEach { (i, s, e) ->
                            val cx = start + slot * (i + 0.5f)
                            val x = (cx - pw / 2).coerceIn(12.dp, w - 12.dp - pw)
                            Poster(e, cx - x, Modifier.offset(x = x).width(pw)) { onPick(s.item.id) }
                        }
                    }
                    val h = ITEM + 2.dp + if (counter) slab else HANG + tagH
                    Box(
                        Modifier.fillMaxWidth().height(h).drawBehind {
                            val y = (ITEM + 2.dp).toPx()
                            // the market's lower back board behind the 2nd board and the counter: the counter and bowl painted into
                            // bg_market_port stand still under the scroll and looked like an item without a tag (круг критиков 1)
                            if (!indoor && r > 0) drawRect(lerp(front, Color.White, 0.55f), Offset(19.dp.toPx(), 0f), Size(size.width - 38.dp.toPx(), size.height))
                            if (counter) counter(y, size.height, wood, front) else board(y, 19.dp.toPx(), size.width - 19.dp.toPx(), wood)
                        },
                    ) {
                        Row(Modifier.fillMaxSize().padding(start = start, end = end)) {
                            row.forEachIndexed { i, s ->
                                if (s == null) Spacer(Modifier.weight(1f))
                                else Slot(s, TILT[(r * 3 + i) % TILT.size], if (counter) HANG_C else HANG, tagH, Modifier.weight(1f)) { onPick(s.item.id) }
                            }
                        }
                        // over the seller's head: the bubble's bottom (tail tip) meets the top of the figure
                        if (inSlot && r == rows.size - 2) Box(Modifier.offset(x = 8.dp).size(124.dp, h + ITEM + 2.dp + slab - SELLER + 2.dp)) {
                            SellerBubble(vm, resident, order, job, Tail.DOWN, Modifier.align(Alignment.BottomStart).wrapContentHeight(Alignment.Bottom, unbounded = true))
                        }
                    }
                }
            }
            Box(Modifier.fillMaxWidth()) {
                // the figure's feet on the floor at the counter; it takes no room, the boards above keep their places. No
                // semantics: its 170 dp box would cover the counter's first item and cut that item's TalkBack bounds
                if (resident != null) ResidentPic(resident, SELLER, Modifier.clearAndSetSemantics {}.layout { m, _ ->
                    val pl = m.measure(Constraints())
                    layout(0, 0) { pl.place((-46).dp.roundToPx(), -(SELLER - 10.dp).roundToPx()) }
                })
                // 10 dp up onto the counter's lip: Foma's three-line greeting stays whole over the bottom of 360 × 640 at 1.0
                FlowRow(
                    Modifier.offset(y = (-10).dp).fillMaxWidth().padding(start = 92.dp, end = 12.dp, top = 2.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // 136 dp: the bubble and «Ещё 9» share one line at 1.0 on 360 dp (макет 2); above 1.0 they take two lines
                    if (talk && !inSlot) SellerBubble(vm, resident, order, job, Tail.LEFT, Modifier.padding(end = 8.dp).widthIn(max = if (scaled) 240.dp else 136.dp))
                    if (job != null && vm.shiftQuote(job.id).canPlay) {
                        Crates()
                        GameButton("Начать смену", style = ButtonStyle.PRIMARY, minHeight = 48.dp) { vm.startRound(job.id) }
                    }
                    pager()
                }
            }
        }
    }
}

/** One item on a board: the picture, the string, the tag. TalkBack: one node with the words of the former card (A1s R8). */
@Composable
private fun Slot(s: ShelfItem, tilt: Float, hang: Dp, tagH: Dp, modifier: Modifier, onClick: () -> Unit) {
    val item = s.item
    val desc = listOf(item.title, "${s.price} монет", item.tag().drop(3), item.effectWords()).filter { it.isNotBlank() }.joinToString(", ")
    Column(
        modifier.fillMaxHeight().clickable(role = Role.Button, onClick = onClick).clearAndSetSemantics { contentDescription = desc }
            .drawBehind {
                val x = size.width / 2
                val y = (ITEM + 2.dp).toPx()
                drawLine(STRING, Offset(x, y + if (hang == HANG) 18.dp.toPx() else 6.dp.toPx()), Offset(x, y + (hang + 7.dp).toPx()), 1.3.dp.toPx())
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ShelfPic(item, ITEM)
        Spacer(Modifier.height(2.dp + hang))
        PriceTag(s, tilt, tagH)
    }
}

/** Opaque bounds of the item pictures (alpha ≥ 40 as mock.py sprite: item_fun_balloon has a see-through pad), once per drawable. */
private val opaque = HashMap<Int, IntRect>()

/**
 * An item standing on a board (mock.py sprite + place): the picture cropped to its opaque part, fitted into [size] × 1.1 by
 * [size], its bottom on the bottom edge, a soft contact shade under it; no picture — the emoji (Pic). Pic keeps its margins
 * for the other screens.
 */
@Composable
private fun ShelfPic(item: ShopItem, size: Dp, modifier: Modifier = Modifier) {
    val res = itemRes(item.id)
    if (res == null) { Pic(null, item.emoji, size, modifier); return }
    val img = ImageBitmap.imageResource(res)
    val box = opaque.getOrPut(res) {
        val px = IntArray(img.width * img.height).also { img.readPixels(it) }
        var (l, t, r, b) = intArrayOf(img.width, img.height, -1, -1)
        for (i in px.indices) if (px[i] ushr 24 >= 40) { val x = i % img.width; val y = i / img.width; l = minOf(l, x); t = minOf(t, y); r = maxOf(r, x); b = maxOf(b, y) }
        if (r < 0) IntRect(0, 0, img.width, img.height) else IntRect(l, t, r + 1, b + 1)
    }
    Image(
        BitmapPainter(img, box.topLeft, box.size), null,
        modifier.size(size * 1.1f, size).drawBehind {
            val w = box.width * minOf(this.size.width / box.width, this.size.height / box.height) * 0.84f
            drawOval(SHADE.copy(alpha = 0.18f), Offset((this.size.width - w) / 2, this.size.height - 4.dp.toPx()), Size(w, 8.dp.toPx()))
        },
        alignment = Alignment.BottomCenter,
    )
}

/** mock.py tag(): paper on a string, the ring at the top, coin and price 19 sp; a sale — the old price struck, the new dark red. */
@Composable
private fun PriceTag(s: ShelfItem, tilt: Float, h: Dp) {
    val (paper, edge) = s.item.category.paper()
    val coin = with(LocalDensity.current) { 17.sp.toDp() }
    val price = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp, lineHeight = 22.sp, fontWeight = FontWeight.ExtraBold)
    Box(Modifier.rotate(tilt)) {
        Row(
            Modifier.height(h).widthIn(min = 67.dp).plastic(paper, 10.dp, 0.dp, if (s.was != null) G.magenta else edge, 2.dp)
                .drawBehind {
                    val c = Offset(size.width / 2, 6.7.dp.toPx())
                    drawCircle(Color.White, 3.3.dp.toPx(), c)
                    drawCircle(edge, 3.3.dp.toPx(), c, style = Stroke(1.3.dp.toPx()))
                }
                .padding(start = 11.dp, end = 11.dp, top = 11.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
        ) {
            if (s.was != null) {
                TText("${s.was}", Modifier.strike(), MaterialTheme.typography.titleSmall, OLD, maxLines = 1)
                Spacer(Modifier.width(5.dp))
            }
            Image(painterResource(R.drawable.ui_coin), null, Modifier.size(coin))
            Spacer(Modifier.width(3.dp))
            TText("${s.price}", style = price, color = if (s.was != null) DEEP else G.purpleDeep, maxLines = 1)
        }
        Image(painterResource(s.item.category.jar().first), null, Modifier.offset((-10).dp, (-13).dp).size(32.dp))
    }
}

/** The old price of a sale: a pink stroke from bottom left to top right (mock.py tag). */
private fun Modifier.strike() = drawWithContent {
    drawContent()
    drawLine(G.magenta, Offset(-2.dp.toPx(), size.height * 0.72f), Offset(size.width + 2.dp.toPx(), size.height * 0.3f), 2.dp.toPx())
}

/**
 * The poster of an event over its item; the tap opens the item. Words: the ad's quoted words of the intro (Пк3, P3), else the
 * title of a price event (C1 «Распродажа робота»), else the intro's first sentence — C3's title «Мечта почти твоя» over the
 * kite would call the kite the dream, and C3's lesson is to pass it by.
 */
@Composable
private fun Poster(e: EventDef, tailX: Dp, modifier: Modifier, onClick: () -> Unit) {
    val words = Regex("«(.+)»").find(e.intro)?.groupValues?.get(1)
        ?: e.title.takeIf { e.setup.any { it is EventEffect.Price || it is EventEffect.Offer } }
        ?: Regex("^.+?[.!?](?=\\s|$)").find(e.intro)?.value ?: e.intro
    val style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold, lineHeight = 19.sp)
    Box(modifier.clickable(role = Role.Button, onClick = onClick).clearAndSetSemantics { contentDescription = "${e.title}. ${e.intro}" }) {
        Column(
            Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .drawBehind {
                    val x = tailX.toPx()
                    val d = 1.dp.toPx()
                    drawPath(Path().apply { moveTo(x - 7 * d, size.height - 3 * d); lineTo(x + 7 * d, size.height - 3 * d); lineTo(x, size.height + 9 * d); close() }, Color.White)
                }
                .plastic(Color(0xFFFFE278), 7.dp, 2.dp, Color.White, 2.3.dp).padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
        ) {
            words.split(Regex("(?<=[!?.]) ")).forEachIndexed { i, t -> TText(t, style = style, color = if (i == 0) DEEP else G.purple, align = TextAlign.Center) }
        }
        Badge(Modifier.offset((-5).dp, (-5).dp))
    }
}

/** «!» of an event (mock.py badge), drawn: no text, its words are in the poster's description and the pager's state. */
@Composable
private fun Badge(modifier: Modifier) = Canvas(modifier.size(22.dp)) {
    val c = size.width / 2
    drawCircle(G.magenta, c)
    drawCircle(Color.White, c - 1.dp.toPx(), style = Stroke(2.dp.toPx()))
    drawLine(Color.White, Offset(c, size.height * 0.26f), Offset(c, size.height * 0.58f), 3.dp.toPx(), StrokeCap.Round)
    drawCircle(Color.White, 1.7.dp.toPx(), Offset(c, size.height * 0.76f))
}

/** mock.py bubble(): white, round, a soft shade; the tail points down at the start (the head below) or to a side. */
@Composable
private fun Cloud(modifier: Modifier, tail: Tail, content: @Composable ColumnScope.() -> Unit) = Column(
    modifier.drawBehind {
        val d = 1.dp.toPx()
        val (w, h) = size.width to size.height
        drawRoundRect(SHADE.copy(alpha = 0.2f), Offset(0f, 3 * d), size, CornerRadius(13 * d))
        val t = when (tail) {
            Tail.DOWN -> listOf(Offset(18 * d, h - 2 * d), Offset(34 * d, h - 2 * d), Offset(24 * d, h + 9 * d))
            Tail.LEFT -> listOf(Offset(2 * d, h / 2 - 8 * d), Offset(2 * d, h / 2 + 8 * d), Offset(-10 * d, h / 2 + 12 * d))
            Tail.RIGHT -> listOf(Offset(w - 2 * d, h / 2 - 8 * d), Offset(w - 2 * d, h / 2 + 8 * d), Offset(w + 10 * d, h / 2 + 12 * d))
        }
        drawPath(Path().apply { moveTo(t[0].x, t[0].y); lineTo(t[1].x, t[1].y); lineTo(t[2].x, t[2].y); close() }, Color.White)
        drawRoundRect(Color.White, cornerRadius = CornerRadius(13 * d))
    }.padding(horizontal = 10.dp, vertical = 6.dp),
    verticalArrangement = Arrangement.spacedBy(2.dp), content = content,
)

/** The seller's bubble: Marta's order (its title, coin +pay; TalkBack — the intro, the pay line, the shifts) or the resident's line. */
@Composable
private fun SellerBubble(vm: GameViewModel, resident: Resident?, order: EventDef?, job: Job?, tail: Tail, modifier: Modifier) {
    val text = order?.title ?: resident?.lines?.firstOrNull() ?: return
    val q = job?.let { vm.shiftQuote(it.id) }
    val max = vm.tc.rules.shiftsPerWeek
    val desc = if (order != null && q != null) "${dot(order.title)} ${dot(order.intro)} ${dot(q.line)} Смены: ${vm.state.shiftsThisPeriod.coerceIn(0, max)} из $max" else text
    Cloud(modifier.clearAndSetSemantics { contentDescription = desc }, tail) {
        // 18 sp lines, Medium for a line: three short lines fit over the seller's head at 1.0 (макет 1)
        val style = if (order != null) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium
        TText(text.replace(Regex("(?<=(^|\\s)[А-Яа-яЁёA-Za-z]) "), "\u00A0"), style = style.copy(lineHeight = 18.sp), color = if (order != null) G.purple else G.ink)
        if (q != null) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Image(painterResource(R.drawable.ui_coin), null, Modifier.size(22.dp))
            TText(if (q.top > q.base) "+${q.base}–${q.top}" else "+${q.base}", style = MaterialTheme.typography.titleMedium, color = G.purple, maxLines = 1)
        }
    }
}

/** Marta's cardboard boxes by her order (mock.py crates), drawn: no box emoji, the debug emoji probe stays 0. */
@Composable
private fun Crates() = Canvas(Modifier.size(52.dp, 48.dp)) {
    val d = 1.dp.toPx()
    val (card, tape) = Color(0xFFD9A066) to Color(0xFFB5793F)
    plastic(Offset(2 * d, 18 * d), Size(46 * d, 30 * d), card, 3.dp, 2.dp)
    drawRect(tape, Offset(21 * d, 18 * d), Size(8 * d, 12 * d))
    rotate(8f, Offset(26 * d, 10 * d)) {
        plastic(Offset(10 * d, 0f), Size(32 * d, 20 * d), card, 3.dp, 2.dp)
        drawRect(tape, Offset(22 * d, 0f), Size(8 * d, 9 * d))
    }
}

/**
 * «Ещё N ▶» / «◀ Назад» of «У Фомы»: 48 dp, the arrow drawn in magenta (макет 2), after the words forward, before them back —
 * ponytail: GameButton's icon stands first, right to left puts it last (no trailing slot in Widgets.kt). «!» when that page
 * has an event's item; the [event]'s title is the button's state for TalkBack.
 */
@Composable
private fun Pager(label: String, next: Boolean, event: String?, onClick: () -> Unit) = Box {
    CompositionLocalProvider(LocalLayoutDirection provides if (next) LayoutDirection.Rtl else LayoutDirection.Ltr) {
        val arrow = remember(next) { Arrow(next) }
        GameButton(label, if (event != null) Modifier.semantics { stateDescription = event } else Modifier, ButtonStyle.PAPER, icon = arrow, iconSize = 18.dp, minHeight = 48.dp, onClick = onClick)
    }
    if (event != null) Badge(Modifier.offset((-6).dp, (-6).dp))
}

/** The pager's triangle (макет 2: ≈ 13 × 18 dp), forward or back. */
private class Arrow(val next: Boolean) : Painter() {
    override val intrinsicSize = Size.Unspecified
    override fun DrawScope.onDraw() {
        val (x0, x1) = size.width * 0.14f to size.width * 0.86f
        val (tip, base) = if (next) x1 to x0 else x0 to x1
        drawPath(Path().apply { moveTo(base, 0f); lineTo(tip, size.height / 2); lineTo(base, size.height); close() }, G.magenta)
    }
}

/** Касса-ценник (макет a1s_mock_4): name and ✕ stay put (WORKFLOW № 38), under them the body scrolls: department (lid +
 * word), price, the item and the pet with its effect; the quote's line, the event's intro, the options (main, the rest under
 * «Ещё ▾»), «Пройти мимо» of the event. */
@Composable
private fun Kassa(vm: GameViewModel, itemId: String, shopId: String, was: Int?, event: EventDef?, modifier: Modifier, onClose: () -> Unit, onOption: (PayOption) -> Unit) {
    val item = vm.item(itemId) ?: return
    val q = vm.quote(itemId, shopId)
    val pet = vm.state.pet
    val particles = LocalParticles.current
    val act = LocalPetAction.current
    val text = MaterialTheme.typography.bodyMedium
    var more by remember(itemId) { mutableStateOf(false) }
    val (paper, edge) = item.category.paper()
    val (lid, word) = item.category.jar()
    BackHandler(onBack = onClose)
    Column(modifier.padding(horizontal = 10.dp, vertical = 6.dp).fillMaxWidth().plastic(paper, 20.dp, 4.dp, edge, 2.7.dp).testTag("pay_panel")) {
        Row(Modifier.padding(start = 14.dp, top = 14.dp, end = 14.dp, bottom = 8.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TText(item.title, Modifier.weight(1f), MaterialTheme.typography.headlineMedium, G.purpleDeep)
            Box(Modifier.size(48.dp).background(Color.White, CircleShape).border(1.5.dp, Color(0xFFDCC8F0), CircleShape).clickable(role = Role.Button, onClick = onClose).semantics { contentDescription = "Закрыть кассу" }, contentAlignment = Alignment.Center) {
                TText("✕", style = MaterialTheme.typography.titleMedium, color = G.purpleDeep, maxLines = 1)
            }
        }
        Column(
            Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(start = 14.dp, end = 14.dp, bottom = 14.dp).testTag("pay_body"),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.heightIn(min = 36.dp).background(Color.White, RoundedCornerShape(50)).border(2.dp, edge, RoundedCornerShape(50)).padding(start = 4.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Image(painterResource(lid), null, Modifier.size(28.dp))
                    TText(word, style = MaterialTheme.typography.titleSmall, color = G.purple, maxLines = 1)
                }
                Spacer(Modifier.weight(1f))
                if (was != null) {
                    TText("$was", Modifier.strike(), MaterialTheme.typography.titleMedium, OLD, maxLines = 1)
                    Spacer(Modifier.width(8.dp))
                }
                TText("${q.price}", style = MaterialTheme.typography.displaySmall, color = if (was != null) DEEP else G.purpleDeep, maxLines = 1)
                Image(painterResource(R.drawable.ui_coin), null, Modifier.size(34.dp))
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Box(Modifier.width(96.dp), contentAlignment = Alignment.BottomCenter) {
                    Box(Modifier.size(96.dp, 18.dp).plastic(Color(0xFFCDB9F0), 9.dp, 3.dp))
                    ShelfPic(item, 80.dp, Modifier.padding(bottom = 8.dp))
                }
                // weighted: measured after the item and the pet — the cloud gives way, the pet keeps its 80 dp at 1.3; above 1.0 the
                // stat's word gives way to its icon (the words — in the cloud's description, bigFont's rule)
                Box(Modifier.weight(1f).padding(bottom = 18.dp), contentAlignment = Alignment.BottomEnd) {
                    Cloud(Modifier.clearAndSetSemantics { contentDescription = item.effectWords() }, Tail.RIGHT) {
                        item.effects().forEach { (i, v) ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Image(painterResource(STATS[i].icon), null, Modifier.size(24.dp))
                                TText("+$v", style = MaterialTheme.typography.titleLarge, color = G.greenDark, maxLines = 1)
                            }
                            if (LocalDensity.current.fontScale <= 1f) TText(STATS[i].word, style = text, maxLines = 1)
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                if (pet != null) PetSprite(
                    speciesId = pet.speciesId, colorId = pet.colorId, stage = vm.economy.stageIndex(pet.growth), face = vm.face, animate = LocalAnimate.current,
                    size = 80.dp, action = act.action, actionKey = act.key, seen = act, description = pet.name,
                    modifier = Modifier.particleTarget(particles, "pet"),
                )
            }
            if (q.line.isNotBlank()) TText(q.line, style = text)
            if (q.note.isNotBlank()) TText(q.note, style = text, color = G.inkSoft)
            event?.let { TText(it.intro, style = text) }
            // before the plan: [Разложить] under the quote's line, no jar to pay from yet (правка №5)
            if (!vm.state.plan.confirmed) GameButton("Разложить", Modifier.fillMaxWidth(), minHeight = 48.dp) { onClose(); vm.navigate(Screen.Jars) }
            val (main, rest) = q.options.partition { !it.more }
            val option: @Composable (PayOption) -> Unit = { o ->
                val home = o.kind == PayKind.PAY && o.source == item.homeJar()
                val icon = when (o.kind) { PayKind.CHEAPER -> o.itemId?.let(::itemRes); PayKind.PAY -> o.source?.lid(); else -> null }
                GameButton(o.label, Modifier.fillMaxWidth(), if (home) ButtonStyle.PRIMARY else ButtonStyle.PAPER, icon = icon?.let { painterResource(it) }, minHeight = 48.dp) { onOption(o) }
                // an option that asks first shows its preview in the question (Ask); one that acts at once — here
                if (o.kind != PayKind.PAY || home) o.preview.firstOrNull()?.let { TText(it, style = text, color = G.inkSoft) }
            }
            main.forEach { option(it) }
            if (rest.isNotEmpty()) {
                if (!more) GameButton("Ещё ▾", Modifier.fillMaxWidth(), ButtonStyle.PAPER, minHeight = 48.dp) { more = true }
                else rest.forEach { option(it) }
            }
            event?.takeIf { e -> e.outcomes.any { it.fact == Fact.Skip } }?.let { e ->
                GameButton("Пройти мимо", Modifier.fillMaxWidth(), ButtonStyle.PAPER, minHeight = 48.dp) { vm.pass(e.id); onClose() }
            }
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

internal fun dot(t: String) = if (t.endsWith(".") || t.endsWith("!") || t.endsWith("?")) t else "$t."

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

/** Events of this place: the poster if the event has one, title, intro and «Пройти мимо» when the event has a Skip outcome. */
@Composable
internal fun PlaceEvents(vm: GameViewModel, placeId: String) {
    vm.eventsAt(placeId).forEach { e ->
        Panel(Modifier.fillMaxWidth(), padding = 12.dp) {
            // № 96, плакат строкой: 64 dp left of the title and intro, no description — the card's text is read (TOWN-A1e2)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                posterRes(e.id)?.let { Image(painterResource(it), null, Modifier.size(64.dp)) }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TText("! ${e.title}", style = MaterialTheme.typography.titleMedium)
                    TText(e.intro)
                }
            }
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
