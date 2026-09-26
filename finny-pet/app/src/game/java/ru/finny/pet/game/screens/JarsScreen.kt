package ru.finny.pet.game.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.finny.pet.R
import ru.finny.pet.domain.town.Source
import ru.finny.pet.game.GameViewModel
import ru.finny.pet.game.ui.ButtonStyle
import ru.finny.pet.game.ui.G
import ru.finny.pet.game.ui.GameBar
import ru.finny.pet.game.ui.GameButton
import ru.finny.pet.game.ui.Hud1
import ru.finny.pet.game.ui.LocalParticles
import ru.finny.pet.game.ui.Panel
import ru.finny.pet.game.ui.Pic
import ru.finny.pet.game.ui.TText
import ru.finny.pet.game.ui.itemRes
import ru.finny.pet.game.ui.particleTarget

/** A home page: HUD-1 with ⌂ and a panel with the title and scrolling content; [overlay] for questions. */
@Composable
fun HomePage(vm: GameViewModel, title: String, tag: String, overlay: @Composable BoxScope.() -> Unit = {}, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Hud1(vm, inPlace = true)
            Panel(Modifier.weight(1f).fillMaxWidth().padding(8.dp).testTag(tag), padding = 12.dp) {
                TText(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() }, color = G.purpleDeep)
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
            }
        }
        overlay()
    }
}

private val LIDS = listOf(R.drawable.ui_lid_mandatory, R.drawable.ui_lid_optional, R.drawable.ui_lid_savings)

/** A question with [Да] [Отмена] over the page: the action label is the title, the engine's preview the lines. */
class Pending(val title: String, val lines: List<String>, val go: () -> Unit)

@Composable
fun BoxScope.PendingAsk(p: Pending?, close: () -> Unit) {
    p ?: return
    Ask(p.title, p.lines, listOf("Да" to { close(); p.go() }, "Отмена" to close), close)
}

/** Jars (§D.1): before the plan — «Разложи монеты», after it — «Банки: план и факт». */
@Composable
fun JarsScreen(vm: GameViewModel) {
    val s = vm.state
    var ask by rememberSaveable { mutableStateOf(false) }
    var pending by remember { mutableStateOf<Pending?>(null) }
    HomePage(vm, if (s.plan.confirmed) "Банки: план и факт" else "Разложи монеты", "jars", overlay = {
        if (ask) Ask(
            "Готово?", listOf("План не меняется до нового конверта — потом сравним, как вышло"),
            listOf("Да" to { ask = false; vm.confirmPlan() }, "Ещё подумаю" to { ask = false }), { ask = false },
        )
        PendingAsk(pending) { pending = null }
    }) {
        if (!s.plan.confirmed) PlanJars(vm) { ask = true } else {
            PlanFactRows(s.plan.mandatory, s.factMandatory, s.plan.optional, s.factOptional, s.plan.savings, s.factSavings)
            TText("Запас на всякий случай: ${s.reserve}")
            val n = vm.content.rules.planStep
            listOf(Triple(Source.RESERVE, "Из запаса в «Нужное» +$n", s.reserve), Triple(Source.WANT, "Из «Хочу» в «Нужное» +$n", s.jarWant)).forEach { (src, label, have) ->
                if (have >= n) GameButton(label, Modifier.fillMaxWidth(), ButtonStyle.PAPER, minHeight = 48.dp) {
                    pending = Pending(label, vm.transferPreview(src, n)) { vm.transfer(src, n) }
                }
            }
        }
    }
}

@Composable
private fun PlanJars(vm: GameViewModel, onDone: () -> Unit) {
    val s = vm.state
    val p = s.plan
    val step = vm.content.rules.planStep
    val unalloc = s.balance - p.total
    val particles = LocalParticles.current
    val values = listOf(p.mandatory, p.optional, p.savings)
    fun plan(i: Int, d: Int) {
        val v = values.toMutableList().also { it[i] += d }
        vm.setPlan(v[0], v[1], v[2])
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Нужное", "Хочу", "В копилку").forEachIndexed { i, word ->
            val can = unalloc >= step
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    Modifier.size(72.dp).alpha(if (can) 1f else 0.5f).background(G.paperTint, RoundedCornerShape(16.dp))
                        .clickable(enabled = can, role = Role.Button) { plan(i, step) }
                        .clearAndSetSemantics { contentDescription = "$word: ${values[i]}. Плюс $step" }
                        .then(if (i == 2) Modifier.particleTarget(particles, "jar_save") else Modifier),
                ) {
                    Image(painterResource(R.drawable.ui_jar), null, Modifier.align(Alignment.BottomCenter).size(64.dp))
                    Image(painterResource(LIDS[i]), null, Modifier.align(Alignment.TopCenter).size(36.dp))
                }
                TText(word, style = MaterialTheme.typography.titleSmall, maxLines = 2, align = TextAlign.Center)
                TText("${values[i]}", style = MaterialTheme.typography.titleLarge, color = G.purpleDeep, maxLines = 1)
                GameButton("−", Modifier.width(64.dp), ButtonStyle.PAPER, enabled = values[i] >= step, minHeight = 48.dp) { plan(i, -step) }
                if (i == 0) NeedList(vm)
            }
        }
    }
    if (unalloc < step) TText("Больше, чем есть, положить нельзя", color = G.inkSoft)
    TText("Не разложено: $unalloc — будет запасом на всякий случай")
    GameButton("Готово", Modifier.fillMaxWidth(), ButtonStyle.GOLD, onClick = onDone)
}

/** The week's list under «Нужное»: seen prices and their sum, or «?» and a hint to look in the shops. */
@Composable
private fun NeedList(vm: GameViewModel) {
    val s = vm.state
    val parts = vm.needList.map { id -> (vm.item(id)?.title ?: id) to s.seenPrices[id]?.price }
    if (parts.isNotEmpty()) {
        val text = if (parts.all { it.second != null }) {
            "Список: " + parts.joinToString(" + ") { "${it.first} ~${it.second}" } + " = ${parts.sumOf { it.second ?: 0 }}"
        } else {
            "Список: " + parts.joinToString(" + ") { (t, p) -> if (p != null) "$t ~$p" else "$t ?" } + " — загляни в лавки"
        }
        TText(text, style = MaterialTheme.typography.bodyMedium, align = TextAlign.Center)
    }
    if (s.plan.mandatory == 0) TText("Еда и уход нужны каждую неделю", style = MaterialTheme.typography.bodyMedium, align = TextAlign.Center)
}

/** Plan and fact of the three jars: icon, numbers, bar; «✓» only where the plan holds (not a report card). */
@Composable
fun PlanFactRows(m: Int, fm: Int, o: Int, fo: Int, sv: Int, fs: Int) {
    val saved = maxOf(fs, 0)
    listOf(
        Triple("Нужное: план $m, потрачено $fm", fm <= m, fm to m),
        Triple("Хочу: план $o, потрачено $fo", fo <= o, fo to o),
        Triple("Копилка: план $sv, отложено $saved", saved >= sv, saved to sv),
    ).forEachIndexed { i, (text, kept, bar) ->
        Row(
            Modifier.fillMaxWidth().background(G.paperTint, RoundedCornerShape(14.dp)).padding(8.dp).clearAndSetSemantics { contentDescription = text + if (kept) ", по плану" else "" },
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Image(painterResource(LIDS[i]), null, Modifier.size(32.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TText(text, style = MaterialTheme.typography.bodyMedium)
                GameBar("", bar.first, listOf(G.mandatory, G.optional, G.savings)[i], max = bar.second.coerceAtLeast(1))
            }
            if (kept) TText("✓", style = MaterialTheme.typography.titleLarge, color = G.greenDark, maxLines = 1)
        }
    }
}

/** Arrange (§D.5): six spots 3 × 2, starters stay; a tap on a spot — the chest's things of that type. */
@Composable
fun ArrangeScreen(vm: GameViewModel) {
    val s = vm.state
    var picked by rememberSaveable { mutableStateOf<String?>(null) }
    val slotWord = mapOf("floor" to "пол", "wall" to "стена", "table" to "стол")
    HomePage(vm, "Обустроить комнату", "arrange") {
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = 3) {
            vm.tc.spots.forEach { spot ->
                val starter = vm.tc.homeItems.firstOrNull { it.spot == spot.id }
                val item = s.placed[spot.id]?.let(vm::item)
                val word = slotWord[spot.slot] ?: spot.slot
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(64.dp).background(if (picked == spot.id) G.pink else G.paperTint, RoundedCornerShape(12.dp))
                            .then(if (starter == null) Modifier.clickable(role = Role.Button) { picked = spot.id } else Modifier)
                            .clearAndSetSemantics { contentDescription = "$word: " + (starter?.title ?: item?.title ?: "пусто") },
                        contentAlignment = Alignment.Center,
                    ) {
                        when {
                            starter != null -> Pic(null, starter.emoji, 40.dp)
                            item != null -> Pic(itemRes(item.id), item.emoji, 40.dp)
                        }
                    }
                    TText(word, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                }
            }
        }
        picked?.let { spotId ->
            val spot = vm.tc.spots.first { it.id == spotId }
            val things = s.owned.mapNotNull(vm::item).filter { it.slot == spot.slot }
            if (things.isEmpty()) TText("В сундуке нет вещей для этого места", color = G.inkSoft)
            things.forEach { t ->
                GameButton(t.title, Modifier.fillMaxWidth(), ButtonStyle.PAPER, minHeight = 48.dp, selected = s.placed[spotId] == t.id) { vm.place(spotId, t.id) }
            }
            if (s.placed[spotId] != null) GameButton("Убрать в сундук", Modifier.fillMaxWidth(), ButtonStyle.PAPER, minHeight = 48.dp) { vm.place(spotId, null) }
        }
        GameButton("Готово", Modifier.fillMaxWidth(), ButtonStyle.GOLD) { vm.back() }
    }
}
