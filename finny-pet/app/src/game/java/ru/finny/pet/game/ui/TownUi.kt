package ru.finny.pet.game.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import ru.finny.pet.PetSprites
import ru.finny.pet.R
import ru.finny.pet.domain.Goal
import ru.finny.pet.domain.town.Resident
import ru.finny.pet.game.GameViewModel
import ru.finny.pet.game.Screen

// ---------- measurable text (TOWN-S1d §B.10) ----------

/** Texts clipped right now; the debug «overflow» probe reports them. */
val LocalClipped = staticCompositionLocalOf<SnapshotStateList<String>?> { null }

// the game is Russian only: Russian hyphenation under any system locale
private val RU = LocaleList("ru")

/** Every text of the town screens: no ellipsis, a clipped line is reported to the «overflow» probe. */
@Composable
fun TText(text: String, modifier: Modifier = Modifier, style: TextStyle = MaterialTheme.typography.bodyLarge, color: Color = G.ink, maxLines: Int = Int.MAX_VALUE, align: TextAlign? = null) {
    val clipped = LocalClipped.current
    DisposableEffect(text) { onDispose { clipped?.remove(text) } }
    Text(
        text, modifier, color = color, style = style.copy(localeList = RU), textAlign = align, maxLines = maxLines, softWrap = maxLines > 1, overflow = TextOverflow.Clip,
        onTextLayout = { r -> if (clipped != null) { if (!r.hasVisualOverflow) clipped.remove(text) else if (text !in clipped) clipped.add(text) } },
    )
}

/** Font scale above 1.15: words give way to icons, the word moves to TalkBack. */
@Composable
fun bigFont() = LocalDensity.current.fontScale > 1.15f

// Opaque: the place background sits under the chip and sign letters showed through (TOWN-A1c).
fun Modifier.chip() = background(Color.White, RoundedCornerShape(50))

// ---------- pictures (§B.9) ----------

/** Drawable of a shop item, or null — then the item's emoji is drawn. */
fun itemRes(id: String): Int? = when (id) {
    "food_basic" -> R.drawable.item_food_basic
    "food_lunch" -> R.drawable.item_food_lunch
    "care_shampoo" -> R.drawable.item_care_shampoo
    "care_brush" -> R.drawable.item_care_brush
    "care_vitamins" -> R.drawable.item_care_vitamins
    "fun_ball" -> R.drawable.item_fun_ball
    "fun_bow" -> R.drawable.item_fun_bow
    "fun_balloon" -> R.drawable.item_fun_balloon
    "fun_book" -> R.drawable.item_fun_book
    "fun_tent" -> R.drawable.item_fun_tent
    else -> null
}

/** Drawable of a dream: its own picture, the item's for «item:x», goal_custom for own and demo dreams, else null (emoji). */
fun goalRes(id: String): Int? = when {
    id == "goal_scooter" -> R.drawable.goal_scooter
    id == "goal_paints" -> R.drawable.goal_paints
    id == "goal_lego" -> R.drawable.goal_lego
    id == "goal_zoo" -> R.drawable.goal_zoo
    id.startsWith("item:") -> itemRes(id.removePrefix("item:"))
    id == "demo_goal" || id.startsWith("custom_") -> R.drawable.goal_custom
    else -> null
}

/** Picture or emoji in a [size] box. */
@Composable
fun Pic(res: Int?, emoji: String, size: Dp, modifier: Modifier = Modifier) {
    if (res != null) Image(painterResource(res), null, modifier.size(size))
    else Box(modifier.size(size).clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
        Text(emoji, style = MaterialTheme.typography.titleLarge.copy(fontSize = (size.value * 0.6f).sp, lineHeight = (size.value * 0.7f).sp))
    }
}

@Composable
fun GoalPic(goal: Goal, size: Dp, modifier: Modifier = Modifier) = Pic(goalRes(goal.id), goal.emoji, size, modifier)

/** Drawable of a resident (res_<id>, rendered by pet.py --residents), or null — then the pet frame of its look is drawn. */
fun residentRes(id: String): Int? = when (id) {
    "marta" -> R.drawable.res_marta
    "foma" -> R.drawable.res_foma
    "borya" -> R.drawable.res_borya
    "osya" -> R.drawable.res_osya
    "tosha" -> R.drawable.res_tosha
    "stepan" -> R.drawable.res_stepan
    "kesha" -> R.drawable.res_kesha
    "liza" -> R.drawable.res_liza
    "asya" -> R.drawable.res_asya
    else -> null
}

@Composable
fun ResidentPic(r: Resident, size: Dp, modifier: Modifier = Modifier) =
    Image(painterResource(residentRes(r.id) ?: PetSprites.id(r.look.species, r.look.color, 0, "happy")), r.name, modifier.size(size))

// ---------- HUD (§C, macket MockHome) ----------

/** Round 48 dp icon button of the HUD. */
@Composable
fun IconBox(icon: Int, desc: String, onClick: () -> Unit) {
    Box(Modifier.size(48.dp).chip().clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = desc }, contentAlignment = Alignment.Center) {
        Image(painterResource(icon), null, Modifier.size(34.dp))
    }
}

/** A room object: flat shape, Role.Button and a TalkBack description.
 * clearAndSetSemantics: one merged node at the full w × h (a plain .semantics{} left a full-size description-only
 * child node next to a sliver clickable one in TalkBack — R6, same cause as правка №6 on the bottom row). */
@Composable
fun Target(desc: String, w: Dp, h: Dp, modifier: Modifier = Modifier, color: Color = Color.White.copy(alpha = 0.85f), onClick: () -> Unit, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier.size(w, h).background(color, RoundedCornerShape(10.dp)).clickable(role = Role.Button, onClick = onClick).clearAndSetSemantics { contentDescription = desc },
        contentAlignment = Alignment.Center, content = content,
    )
}

/** HUD-1: [⌂] wallet (tap — the jars sheet), piggy bank with the dream, «?», [🔑] at home only. */
@Composable
fun Hud1(vm: GameViewModel, inPlace: Boolean, onHome: () -> Unit = vm::home) {
    val s = vm.state
    val particles = LocalParticles.current
    val num = MaterialTheme.typography.titleMedium
    val big = bigFont()
    var sheet by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().height(56.dp).testTag("hud1").padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (inPlace) Box(Modifier.size(48.dp).chip().clickable(role = Role.Button, onClick = onHome).semantics { contentDescription = "Домой" }, contentAlignment = Alignment.Center) {
            TText("⌂", style = MaterialTheme.typography.titleLarge, color = G.purpleDeep, maxLines = 1)
        }
        Box {
            Row(
                Modifier.heightIn(min = 48.dp).chip().clickable(role = Role.Button) { sheet = !sheet }
                    .semantics { contentDescription = "Кошелёк ${s.balance}" }.padding(horizontal = 6.dp).particleTarget(particles, "coins"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(painterResource(R.drawable.ui_purse), null, Modifier.size(24.dp))
                TText("${s.balance} ▾", style = num, modifier = Modifier.padding(start = 4.dp), color = G.purpleDeep, maxLines = 1)
            }
            if (sheet) Popup(alignment = Alignment.TopStart, offset = androidx.compose.ui.unit.IntOffset(0, with(LocalDensity.current) { 52.dp.roundToPx() }), onDismissRequest = { sheet = false }) {
                Panel(Modifier.widthIn(max = 320.dp).clickable { sheet = false }, padding = 12.dp) {
                    // before the plan: balance − plan.total (UNALLOC, §D.1/§E.2), not reserve — reserve == balance until
                    // confirmPlan sets jarNeed/jarWant, so «Не разложено» used to show the wrong number here (R10)
                    TText(if (s.plan.confirmed) "Нужное ${s.jarNeed} · Хочу ${s.jarWant} · Запас ${s.reserve}" else "Не разложено ${s.balance - s.plan.total}")
                    val mail = s.envelope.sumOf { it.amount }
                    if (s.envelope.isNotEmpty()) TText("✉ +$mail")
                }
            }
        }
        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.CenterStart) { PiggyChip(vm, big) }
        IconBox(R.drawable.ui_bubble_q, "Подсказка") { vm.navigate(Screen.Intro) }
        if (!inPlace) IconBox(R.drawable.ui_lock, "Взрослым") { vm.navigate(Screen.Parent) }
    }
}

@Composable
private fun PiggyChip(vm: GameViewModel, big: Boolean) {
    val s = vm.state
    val goal = s.goal
    val particles = LocalParticles.current
    val num = MaterialTheme.typography.titleMedium
    val eta = if (goal != null) vm.economy.goalEta(s)?.takeIf { it > 0 } else null
    val etaText = eta?.let { "≈$it✉" }
    // «примерно конвертов: N» — grammatically correct for any N (R5), not «примерно N конвертов»
    val desc = if (goal != null) "Копилка: мечта «${goal.title}», ${s.savings} из ${goal.price}" + (eta?.let { ", примерно конвертов: $it" } ?: "") else "Копилка ${s.savings}. Выбери мечту"
    Row(
        (if (big) Modifier.fillMaxHeight() else Modifier.heightIn(min = 48.dp)).chip().clickable(role = Role.Button) { vm.navigate(Screen.Savings) }
            .clearAndSetSemantics { contentDescription = desc }.padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (goal != null) GoalPic(goal, 28.dp, Modifier.particleTarget(particles, "piggy"))
        else Image(painterResource(R.drawable.ui_piggy), null, Modifier.size(28.dp).particleTarget(particles, "piggy"))
        val amount = if (goal != null) "${s.savings}/${goal.price}" else "${s.savings}"
        when {
            goal == null && big -> TText(amount, style = num, modifier = Modifier.padding(start = 4.dp), color = G.purpleDeep, maxLines = 1)
            // 14 sp: UX_ACCESSIBILITY.md «Исключения: 14 sp» — «Выбери мечту» рядом с числом, не под ним, в двух узких
            // строках 16 sp: три строки (число + два слова) не влезают в 48 dp (правка R3)
            goal == null -> Row(Modifier.padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TText(amount, style = num.copy(lineHeight = 20.sp), color = G.purpleDeep, maxLines = 1)
                TText(
                    "Выбери мечту", style = MaterialTheme.typography.labelSmall.copy(lineHeight = 16.sp), color = G.purpleDeep,
                    maxLines = 2, modifier = Modifier.widthIn(max = 68.dp),
                )
            }
            big -> Column(Modifier.padding(start = 4.dp)) {
                TText(amount, style = MaterialTheme.typography.titleSmall, color = G.purpleDeep, maxLines = 1)
                if (etaText != null) TText(etaText, style = MaterialTheme.typography.labelSmall, color = G.purpleDeep, maxLines = 1)
            }
            else -> FlowRow(Modifier.padding(start = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), itemVerticalAlignment = Alignment.CenterVertically) {
                TText(amount, style = num.copy(lineHeight = 20.sp), color = G.purpleDeep, maxLines = 1)
                if (etaText != null) TText(etaText, style = MaterialTheme.typography.titleSmall.copy(lineHeight = 18.sp), color = G.purpleDeep, maxLines = 1)
            }
        }
    }
}

/** A pet stat: icon, word for TalkBack, value 0–100. */
class StatInfo(val icon: Int, val word: String)

val STATS = listOf(StatInfo(R.drawable.item_food_basic, "Сытость"), StatInfo(R.drawable.item_care_shampoo, "Чистота"), StatInfo(R.drawable.item_fun_ball, "Настроение"))

fun GameViewModel.stats(): List<Int> = state.pet?.let { listOf(it.hunger, it.clean, it.mood) } ?: listOf(0, 0, 0)

/** Icon + number with a 0–100 bar under the number; the word is for TalkBack only. */
@Composable
fun StatChip(s: StatInfo, v: Int, desc: String = "${s.word} $v из 100") {
    Row(Modifier.heightIn(min = 44.dp).chip().clearAndSetSemantics { contentDescription = desc }.padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(s.icon), null, Modifier.size(28.dp))
        Column(Modifier.padding(start = 4.dp)) {
            TText("$v", style = MaterialTheme.typography.titleMedium.copy(lineHeight = 20.sp), color = G.purpleDeep, maxLines = 1)
            Box(Modifier.width(32.dp).height(5.dp).background(G.ink.copy(alpha = 0.15f), CircleShape)) {
                Box(Modifier.fillMaxWidth(v.coerceIn(0, 100) / 100f).height(5.dp).background(G.green, CircleShape))
            }
        }
    }
}

/** Collapsed stats chip of the town screens: the average number, all three in TalkBack. */
@Composable
fun StatsCollapsed(vm: GameViewModel) {
    val v = vm.stats()
    StatChip(STATS[2], v.sum() / v.size, STATS.indices.joinToString(", ") { "${STATS[it].word} ${v[it]}" })
}

/** The HUD-2 row: 48 dp, full width. */
@Composable
fun Hud2(content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth().height(48.dp).testTag("hud2").padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp), content = content)
}

// ---------- LINE (§B.3) ----------

/** The engine's line over the screen: pet portrait 48 dp, the whole line (no clip), [Почему?] opens why; a tap closes it.
 * [maxHeight] caps the bubble at ≤ 40 % of the screen (§B.3 уточнение п.3); a longer line scrolls inside it. */
@Composable
fun LineHost(vm: GameViewModel, modifier: Modifier = Modifier, maxHeight: Dp = Dp.Infinity) {
    val line = vm.lines.firstOrNull() ?: return
    val pet = vm.state.pet
    var why by remember(line) { mutableStateOf(false) }
    Column(
        modifier.fillMaxWidth().padding(8.dp).testTag("line").shadow(8.dp, RoundedCornerShape(20.dp)).background(Color.White, RoundedCornerShape(20.dp))
            .clickable(onClickLabel = "Закрыть") { vm.closeLine() }.heightIn(max = maxHeight).verticalScroll(rememberScrollState()).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (pet != null) Image(painterResource(PetSprites.id(pet.speciesId, pet.colorId, vm.economy.stageIndex(pet.growth), "happy")), null, Modifier.size(48.dp))
            TText(line.text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        }
        if (line.why.isNotEmpty()) {
            if (why) line.why.forEach { TText(it, style = MaterialTheme.typography.bodyMedium, color = G.inkSoft) }
            else Row { Spacer(Modifier.width(56.dp)); GameButton("Почему?", style = ButtonStyle.PAPER, minHeight = 48.dp) { why = true } }
        }
    }
}
