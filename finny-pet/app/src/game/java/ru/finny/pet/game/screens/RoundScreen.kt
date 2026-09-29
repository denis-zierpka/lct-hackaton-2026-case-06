package ru.finny.pet.game.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import ru.finny.pet.domain.Face
import ru.finny.pet.domain.town.Job
import ru.finny.pet.domain.town.JobGame
import ru.finny.pet.game.GameViewModel
import ru.finny.pet.game.Line
import ru.finny.pet.game.LocalAnimate
import ru.finny.pet.game.LocalPetAction
import ru.finny.pet.game.ui.ButtonStyle
import ru.finny.pet.game.ui.G
import ru.finny.pet.game.ui.GameButton
import ru.finny.pet.game.ui.Hud1
import ru.finny.pet.game.ui.Hud2
import ru.finny.pet.game.ui.LocalParticles
import ru.finny.pet.game.ui.PetSprite
import ru.finny.pet.game.ui.ResidentPic
import ru.finny.pet.game.ui.SpeechBubble
import ru.finny.pet.game.ui.StatsCollapsed
import ru.finny.pet.game.ui.TText
import ru.finny.pet.game.ui.bigFont
import ru.finny.pet.game.ui.particleTarget

/** A job round (§E.4): match-3 or three errands; «Закончить», ⌂ and «Назад» finish the shift and show its result. */
@Composable
fun RoundScreen(vm: GameViewModel, jobId: String) {
    val job = vm.tc.jobs.firstOrNull { it.id == jobId }
    val result = vm.roundResult
    Column(Modifier.fillMaxSize()) {
        Hud1(vm, inPlace = true, inRound = true)
        Hud2 { StatsCollapsed(vm); MailChip(vm) }
        when {
            result != null -> ShiftResult(vm, job, result, Modifier.weight(1f))
            job?.game == JobGame.TRAY && vm.tray != null -> TrayScreen(vm, Modifier.weight(1f))
            vm.match != null -> MiniGameScreen(vm, Modifier.weight(1f))
            else -> Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // dark, not white on the light place background (TOWN-A1c) (правка №14, ТЗ 3.6)
                TText(job?.title ?: jobId, style = MaterialTheme.typography.headlineSmall, color = G.ink)
                vm.taps.forEachIndexed { i, done ->
                    GameButton((if (done) "✓ " else "") + job?.tasks?.getOrNull(i).orEmpty(), Modifier.fillMaxWidth(), if (done) ButtonStyle.GREEN else ButtonStyle.PAPER, minHeight = 48.dp) { vm.tapTask(i) }
                }
                // paper card, dark tokens and text — same as the MATCH3 job_row (правка №12 / R9, ТЗ 3.6)
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("job_row").background(G.paperTint, RoundedCornerShape(16.dp)).padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(Modifier.weight(1f)) { ShiftTokens(vm, label = false, color = G.ink) }
                    GameButton("Закончить", minHeight = 48.dp) { vm.finishRound() }
                }
            }
        }
    }
}

/** The shift result in the scene (TOWN-J1-1b1): the resident on the floor, the pet, a bubble with the pay; «Почему?» and «Готово» below. */
@Composable
private fun ShiftResult(vm: GameViewModel, job: Job?, result: Line, modifier: Modifier) {
    val pay = vm.roundPay
    val r = vm.tray?.takeIf { pay != null && job?.game == JobGame.TRAY }
    val act = LocalPetAction.current
    val desc = listOfNotNull(
        if (pay != null) "${vm.residentName(job?.resident ?: "")}: спасибо!" else null,
        r?.let(::starsText),
        (pay?.let { "Заработали ${it.total}: " } ?: "") +
            result.text.replace("за ★", if (pay?.bonus == 1) "за звезду" else "за звёзды"),
        result.why.firstOrNull(),
        *result.why.drop(1).toTypedArray(),
    ).joinToString(" ") { dot(it) }
    BoxWithConstraints(modifier.fillMaxWidth().padding(8.dp)) {
        val floor = maxHeight - 64.dp // buttons row 61 dp (56 + edge 5) + gap 3
        val f = (floor - 8.dp).coerceIn(160.dp, if (bigFont()) 216.dp else 232.dp) // ≤ 232 dp, при крупном шрифте ≤ 216 (№ 63 в3)
        val bx = 8.dp + f * 0.45f
        val head = floor - f + f * 0.1f
        val density = LocalDensity.current
        var residentRect by remember { mutableStateOf<Rect?>(null) }; var bubbleRect by remember { mutableStateOf<Rect?>(null) }
        vm.tc.residents.firstOrNull { it.id == job?.resident }?.let { ResidentPic(it, f, Modifier.offset(x = 8.dp - f * 0.3f, y = floor - f).onGloballyPositioned { c -> residentRect = c.boundsInParent() }.clearAndSetSemantics { testTag = "result_resident" }) }
        if (job?.game == JobGame.TRAY) Counter(Modifier.offset(y = floor - 56.dp).fillMaxWidth().height(56.dp).testTag("result_counter"))
        // № 83 в: сторона питомца — по замеру облачка, не по шрифту. Облачко мерится один раз под потолком «перед жителем»
        // (floor − 16 dp); не выше потолка справа (floor − 120 = floor − 16 − 104 dp над питомцем) — питомец справа, под
        // облачком, иначе перед жителем. Питомец и облачко — в одном Layout: сторона и места обоих — в одном проходе.
        Layout(contents = listOf<@Composable () -> Unit>(
            { vm.state.pet?.let { pet -> PetSprite(
                speciesId = pet.speciesId, colorId = pet.colorId, stage = vm.economy.stageIndex(pet.growth), face = Face.HAPPY, animate = LocalAnimate.current,
                modifier = Modifier.clearAndSetSemantics { testTag = "result_pet" }, size = 96.dp, bounceKey = vm.bounce, action = act.action, actionKey = act.key, seen = act,
            ) } },
            { SpeechBubble(
                Modifier.onGloballyPositioned { c -> bubbleRect = c.boundsInParent() }.width(maxWidth - bx).heightIn(max = floor - 16.dp).clearAndSetSemantics {
                    contentDescription = desc; testTag = "job_result"
                    paneTitle = if (pay != null) "${vm.residentName(job?.resident ?: "")}: спасибо! Заработали ${pay.total}" else result.text
                },
                tail = false,
            ) {
                if (pay != null) TText("Спасибо!", style = MaterialTheme.typography.titleMedium, color = G.ink, maxLines = 1)
                r?.let { StarRow(it) }
                pay?.let {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TText("✉ +${it.total}", Modifier.particleTarget(LocalParticles.current, "job_result"), style = MaterialTheme.typography.headlineMedium, color = G.purple, maxLines = 1)
                        TText("всего", style = MaterialTheme.typography.bodyMedium, color = G.inkSoft, maxLines = 1)
                    }
                }
                TText(result.text, style = MaterialTheme.typography.bodyMedium, color = G.ink)
                result.why.firstOrNull()?.let { TText(it, style = MaterialTheme.typography.bodyMedium, color = G.inkSoft) }
            } },
        )) { (pets, bubbles), c ->
            val b = bubbles.first().measure(c)
            val side = b.height <= (floor - 120.dp).roundToPx()
            val bottom = if (side) floor - 8.dp - 104.dp else floor - 8.dp
            val p = pets.firstOrNull()?.measure(c)
            layout(c.maxWidth, c.maxHeight) {
                p?.place((if (side) maxWidth - 104.dp else 8.dp).roundToPx(), (floor - 82.dp).roundToPx()) // № 87 б: лапы спрайта — 0,86 кадра, на полу
                b.place(bx.roundToPx(), minOf(head.roundToPx(), bottom.roundToPx() - b.height).coerceAtLeast(8.dp.roundToPx()))
            }
        }
        val resident = residentRect; val bubble = bubbleRect
        if (resident != null && bubble != null) with(density) {
            val hy = if (bubble.height.toDp() < 62.dp) (bubble.top + bubble.bottom).toDp() / 2f
            else (resident.top.toDp() + resident.height.toDp() * 0.37f).coerceIn(bubble.top.toDp() + 31.dp, bubble.bottom.toDp() - 31.dp)
            Canvas(Modifier.offset(x = bubble.left.toDp() - 14.dp, y = hy - 11.dp).size(16.dp, 22.dp).testTag("result_tail")) {
                val p = Path().apply { moveTo(size.width, 0f); lineTo(size.width, size.height); lineTo(0f, size.height / 2f); close() }
                drawPath(p, Color.White)
            }
        }
        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (result.why.size > 1) GameButton("Почему?", Modifier.weight(1f), ButtonStyle.PAPER, minHeight = 56.dp) { result.why.drop(1).forEach { vm.say(it) } }
            GameButton("Готово", Modifier.weight(1f), ButtonStyle.PRIMARY, minHeight = 56.dp) { vm.lines.clear(); vm.closeRound() }
        }
    }
}
