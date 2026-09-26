package ru.finny.pet.game.mock

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.unit.sp
import ru.finny.pet.R
import ru.finny.pet.game.ui.ButtonStyle
import ru.finny.pet.game.ui.G
import ru.finny.pet.game.ui.GameButton
import ru.finny.pet.game.ui.Panel

/** A shelf item: [stat] indexes STATS; [old] is the price before a sale. */
private class Item(val name: String, val price: Int, val need: Boolean, val stat: Int, val effect: Int, val icon: Int? = null, val emoji: String = "", val old: Int? = null)

private val MARKET = listOf(
    Item("Корм", 20, true, 0, 40, R.drawable.item_food_basic), Item("Каша", 15, true, 0, 25, R.drawable.item_food_lunch),
    Item("Мыло", 12, true, 1, 25, emoji = "🧼"), Item("Шампунь простой", 15, true, 1, 25, R.drawable.item_care_shampoo),
    Item("Шарик", 8, false, 2, 8, R.drawable.item_fun_balloon), Item("Мороженое", 10, false, 2, 8, emoji = "🍦"),
)
private val TENT = Item("Домик-палатка", 60, false, 2, 35, R.drawable.item_fun_tent)
private val FOMA = listOf(
    Item("Корм", 30, true, 0, 40, R.drawable.item_food_basic), Item("Мыло", 10, true, 1, 25, emoji = "🧼"),
    Item("Шампунь", 20, true, 1, 40, R.drawable.item_care_shampoo), Item("Мячик", 25, false, 2, 20, R.drawable.item_fun_ball),
    Item("Робот", 25, false, 2, 25, emoji = "🤖", old = 40), TENT,
)

// no-break space: the icon never parts from its word or number
private fun Item.tag() = if (need) "🍎\u00A0нужно" else "🎈\u00A0хочу"
private fun Item.effectText() = "${STATS[stat].emoji}\u00A0+$effect"

/** SHOP (§10.1, §6.4, §5.3): wallet sections in HUD-2, shop switch, shelf, pay panel. */
@Composable
internal fun MockShop() {
    val d = LocalMock.current.data
    val big = bigFont()
    var foma by remember { mutableStateOf(false) }
    var pick by remember { mutableStateOf<Item?>(null) }
    var sheet by remember { mutableIntStateOf(0) }
    Box(Modifier.fillMaxSize()) { Column(Modifier.fillMaxSize()) {
        Hud1(inPlace = true)
        Hud2 {
            listOf(R.drawable.ui_lid_mandatory to "Нужное", R.drawable.ui_lid_optional to "Хочу", R.drawable.ui_lid_savings to "Запас").forEachIndexed { i, (lid, word) ->
                Column(Modifier.clearAndSetSemantics { contentDescription = "$word ${d.banks[i]}" }.padding(horizontal = 2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(lid), null, Modifier.size(20.dp))
                        MockText("${d.banks[i]}", MaterialTheme.typography.titleMedium.copy(lineHeight = 22.sp), color = Color.White)
                    }
                    if (!big) MockText(word, MaterialTheme.typography.labelSmall, color = G.pink)
                }
            }
            Spacer(Modifier.weight(1f))
            StatsCollapsed()
        }
        Row(Modifier.fillMaxWidth().testTag("shop_tabs").padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GameButton("Рынок у реки", Modifier.weight(1f), minHeight = 48.dp, selected = !foma) { foma = false; pick = null }
            GameButton("У Фомы", Modifier.weight(1f), minHeight = 48.dp, selected = foma) { foma = true; pick = TENT }
        }
        FlowRow(
            // bottom padding = the pay sheet: every card can be scrolled out from under it
            Modifier.weight(1f).fillMaxWidth().testTag("shelf").verticalScroll(rememberScrollState()).padding(8.dp)
                .padding(bottom = with(LocalDensity.current) { (if (pick == null) 0 else sheet).toDp() }),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
        ) { (if (foma) FOMA else MARKET).forEach { ItemCard(it) { pick = it } } }
    } // the pay sheet lies over the shelf
        pick?.let { PayPanel(it, d, Modifier.align(Alignment.BottomCenter).onSizeChanged { s -> sheet = s.height }) { pick = null } }
    }
}

@Composable
private fun ItemCard(it: Item, onClick: () -> Unit) {
    val body = MaterialTheme.typography.bodyMedium
    Column(
        Modifier.width(104.dp).heightIn(min = 64.dp).background(Color.White, RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClick = onClick)
            .clearAndSetSemantics { contentDescription = "${it.name}, ${it.price} монет, ${it.tag().drop(3)}, ${STATS[it.stat].word} плюс ${it.effect}" }.padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (it.icon != null) Image(painterResource(it.icon), null, Modifier.size(40.dp))
        else Box(Modifier.size(40.dp).background(G.pink, CircleShape), contentAlignment = Alignment.Center) { MockText(it.emoji, MaterialTheme.typography.titleLarge) }
        MockText(it.name, MaterialTheme.typography.labelSmall.copy(hyphens = Hyphens.Auto, lineBreak = LineBreak.Paragraph), maxLines = 2, align = TextAlign.Center)
        Row(verticalAlignment = Alignment.CenterVertically) {
            MockText(if (it.old != null) "${it.old}→${it.price}" else "${it.price}", MaterialTheme.typography.titleSmall)
            Image(painterResource(R.drawable.ui_coin), null, Modifier.size(18.dp))
        }
        if (it.old != null) MockText("такая распродажа бывает", body, maxLines = 4, align = TextAlign.Center)
        // at 1.3 the word may go under its icon: two lines instead of a clipped one
        // above 1.15 the word alone, never wrapped (drop(3): the icon and the no-break space)
        if (bigFont()) MockText(it.tag().drop(3), body) else MockText(it.tag(), body, maxLines = 2, align = TextAlign.Center)
        MockText(it.effectText(), body, maxLines = 2, align = TextAlign.Center)
    }
}

/** Pay panel: a bottom sheet over the shelf with its own scroll; a plain buy, or the shortage with three options and the rest under «Ещё». */
@Composable
private fun PayPanel(it: Item, d: MockData, modifier: Modifier, onClose: () -> Unit) {
    val from = if (it.need) "Нужного" else "Хочу"
    val bank = d.banks[if (it.need) 0 else 1]
    val text = MaterialTheme.typography.bodyMedium
    var more by remember(it) { mutableStateOf(false) }
    Panel(modifier.padding(top = 56.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(8.dp).testTag("pay_panel"), padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MockText("Касса", MaterialTheme.typography.titleMedium, Modifier.weight(1f))
            Box(Modifier.size(48.dp).background(G.paperTint, CircleShape).clickable(role = Role.Button, onClick = onClose).semantics { contentDescription = "Закрыть кассу" }, contentAlignment = Alignment.Center) {
                MockText("✕", MaterialTheme.typography.titleMedium, color = G.purpleDeep)
            }
        }
        if (it.price <= bank) {
            MockText("${it.name} · ${it.price} · ${it.tag()} · ${it.effectText()}", text, maxLines = 2)
            MockText("из „$from“: $bank → ${bank - it.price}", text)
            GameButton("Купить", Modifier.fillMaxWidth(), minHeight = 48.dp) {}
        } else {
            val lack = it.price - bank
            MockText("В „Хочу“ $bank, домик стоит ${it.price}. Не хватает $lack. Запас ${d.banks[2]} — на нужное и на всякий случай.", text, maxLines = 6)
            listOf("Сделать мечтой", "Подождать нового конверта", "Дешевле: коврик 20").forEach { o -> GameButton(o, Modifier.fillMaxWidth(), ButtonStyle.PAPER, minHeight = 48.dp) {} }
            if (!more) GameButton("Ещё ▾", Modifier.fillMaxWidth(), ButtonStyle.PAPER, minHeight = 48.dp) { more = true }
            else listOf("Взять $lack из „Нужного“", "Из копилки: самокат отодвинется").forEach { o -> GameButton(o, Modifier.fillMaxWidth(), ButtonStyle.PAPER, minHeight = 48.dp) {} }
        }
    }
}

private val TILES = listOf(R.drawable.tile_apple to "яблоко", R.drawable.tile_coin to "монета", R.drawable.tile_star to "звезда", R.drawable.tile_gift to "подарок", R.drawable.tile_piggy to "свинка", R.drawable.tile_bomb to "бомбочка")

/** JOB (§5.2, §14): order → round → result. */
@Composable
internal fun MockJob() {
    val m = LocalMock.current
    val body = MaterialTheme.typography.bodyLarge
    var step by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        Hud1(inPlace = true)
        Hud2 {
            StatsCollapsed()
            Box(Modifier.heightIn(min = 44.dp).chip().padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                MockText("✉ +${m.data.mail}", MaterialTheme.typography.titleMedium, color = G.purpleDeep)
            }
        }
        when (step) {
            0 -> Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Panel(Modifier.fillMaxWidth().padding(8.dp).testTag("job_order")) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(R.drawable.pet_puppy_green_0_happy), "Боря", Modifier.size(72.dp))
                        MockText("Пекарне нужен помощник!", body, Modifier.weight(1f), maxLines = 3)
                    }
                    MockText("База 6 монет + до 4 за булочки", body, maxLines = 2)
                    MockText("Смены на неделе: ●●○ 2 из 3", body, maxLines = 2)
                    GameButton("Начать смену", Modifier.fillMaxWidth(), minHeight = 48.dp) { step = 1 }
                    MockText("Загадка Бори: ответишь — бомбочка", body, maxLines = 3)
                    // one size for both answers: same width, height of the taller one
                    Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GameButton("Ответить", Modifier.weight(1f).fillMaxHeight(), ButtonStyle.PAPER, minHeight = 48.dp) {}
                        GameButton("Нет, спасибо", Modifier.weight(1f).fillMaxHeight(), ButtonStyle.PAPER, minHeight = 48.dp) {}
                    }
                }
            }
            1 -> {
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                    val cell = minOf(maxWidth / 6, maxHeight / 6)
                    Column(Modifier.testTag("board").background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(12.dp))) {
                        repeat(6) { r ->
                            Row { repeat(6) { c -> val (t, name) = TILES[(r * 2 + c * 3 + r * c) % 6]; Image(painterResource(t), name, Modifier.size(cell).testTag("cell_${r}_$c").padding(1.dp)) } }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().height(56.dp).testTag("job_row").padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MockText("●●○", MaterialTheme.typography.titleMedium, Modifier.semantics { contentDescription = "Смены: 2 из 3" }, color = G.gold)
                    MockText("Счёт 90", body, Modifier.weight(1f), color = Color.White)
                    GameButton("Закончить", minHeight = 48.dp) { step = 2 }
                }
            }
            else -> Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Panel(Modifier.fillMaxWidth().padding(8.dp).testTag("job_result")) {
                    Image(painterResource(R.drawable.pet_puppy_green_0_happy), "Боря", Modifier.size(72.dp).align(Alignment.CenterHorizontally))
                    MockText("Спасибо, помощник!", MaterialTheme.typography.headlineSmall, maxLines = 2)
                    MockText("База 6 + 3 за булочки. ✉ +9 — придёт с новым конвертом", body, maxLines = 4)
                    MockText("Смен у Бори: 4 / 6 до уровня 2", body, maxLines = 2)
                    GameButton("Готово", Modifier.fillMaxWidth(), minHeight = 48.dp) { m.go(MockScreen.HOME) }
                }
            }
        }
    }
}
