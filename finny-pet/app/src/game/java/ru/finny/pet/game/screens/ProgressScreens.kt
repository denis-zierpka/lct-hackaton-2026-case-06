package ru.finny.pet.game.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ru.finny.pet.PetSprites
import ru.finny.pet.R
import ru.finny.pet.domain.LedgerEntry
import ru.finny.pet.domain.PeriodSummary
import ru.finny.pet.game.GameViewModel
import ru.finny.pet.game.Screen
import ru.finny.pet.game.ui.ButtonStyle
import ru.finny.pet.game.ui.G
import ru.finny.pet.game.ui.GameButton
import ru.finny.pet.game.ui.TText

/** «Дневник» until the diary of slice 2 (§D.9): growth and stages, week results, the journal and the next envelope. */
@Composable
fun ProgressScreen(vm: GameViewModel) {
    val s = vm.state
    val e = vm.economy
    val pet = s.pet ?: return
    PanelScreen(vm, "Дневник") {
        Label("Рост ${pet.name}")
        Row(Modifier.fillMaxWidth().background(G.pink, RoundedCornerShape(20.dp)).padding(10.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
            val stage = e.stageIndex(pet.growth)
            e.rules.stageTitles.forEachIndexed { i, title ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(painterResource(PetSprites.id(pet.speciesId, pet.colorId, i, "happy")), null, Modifier.size((56 + i * 14).dp).alpha(if (i <= stage) 1f else 0.3f))
                    Text(title, style = MaterialTheme.typography.labelMedium, color = if (i <= stage) G.purpleDeep else G.inkSoft)
                }
            }
        }
        val next = e.nextStageLeft(pet.growth)
        TText("Очки роста: ${pet.growth}. " + (next?.let { "До следующей стадии: $it." } ?: "Высшая стадия!"))
        TText("Рост даётся за три вещи в неделю: нужное куплено, траты по плану, копилка выросла.", style = MaterialTheme.typography.bodyMedium, color = G.inkSoft)
        if (s.history.isNotEmpty()) {
            Label("Итоги недель")
            s.history.asReversed().forEach { WeekRow(it) }
        }
        if (s.records.isNotEmpty()) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Image(painterResource(R.drawable.ui_gamepad), null, Modifier.size(32.dp))
            TText("Рекорды: " + s.records.entries.joinToString { (id, n) -> "${vm.tc.jobs.firstOrNull { it.id == id }?.title ?: id} $n" }, style = MaterialTheme.typography.bodyMedium)
        }
        if (s.envelope.isNotEmpty()) {
            Label("Придёт с новым конвертом: +${s.envelope.sumOf { it.amount }}")
            s.envelope.forEach { LedgerRow(it) }
        }
        Label("Журнал")
        s.ledger.asReversed().forEach { LedgerRow(it) }
        if (s.achievedGoals.isNotEmpty()) TText("Мечты сбылись: ${s.achievedGoals.joinToString { it.title }}", style = MaterialTheme.typography.bodyMedium)
        GameButton("Справка", Modifier.fillMaxWidth(), style = ButtonStyle.PAPER, icon = painterResource(R.drawable.ui_book), iconSize = 26.dp) { vm.navigate(Screen.Glossary) }
    }
}

@Composable
private fun LedgerRow(l: LedgerEntry) {
    Row(Modifier.fillMaxWidth().background(G.paperTint, RoundedCornerShape(12.dp)).padding(8.dp)) {
        TText(l.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        TText(if (l.amount > 0) "+${l.amount}" else "${l.amount}", style = MaterialTheme.typography.titleSmall, color = if (l.amount >= 0) G.greenDark else G.ink, maxLines = 1)
    }
}

/** One week: the stamps got (a word, not a colour), growth. */
@Composable
private fun WeekRow(sum: PeriodSummary) {
    val got = listOf("Нужное куплено" to sum.mandatoryCovered, "По плану" to sum.planKept, "Отложено" to sum.saved).filter { it.second }.joinToString(", ") { it.first }
    Column(Modifier.fillMaxWidth().background(G.paperTint, RoundedCornerShape(16.dp)).padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        TText("Неделя ${sum.period}" + if (got.isNotEmpty()) ": ✓ $got" else "", style = MaterialTheme.typography.bodyMedium)
        TText("Рост: +${sum.score} (${sum.growthBefore} → ${sum.growthAfter})", style = MaterialTheme.typography.titleSmall, color = G.purpleDeep)
    }
}

@Composable
fun GlossaryScreen(vm: GameViewModel) {
    PanelScreen(vm, "Справка") {
        vm.content.glossary.forEach { g ->
            Row(Modifier.fillMaxWidth().background(G.paperTint, RoundedCornerShape(16.dp)).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.ui_book), null, Modifier.size(36.dp))
                Column {
                    TText(g.term, style = MaterialTheme.typography.titleSmall, color = G.purpleDeep)
                    TText(g.definition, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
