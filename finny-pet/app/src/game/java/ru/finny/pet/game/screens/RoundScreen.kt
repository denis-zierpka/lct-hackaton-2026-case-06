package ru.finny.pet.game.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ru.finny.pet.game.GameViewModel
import ru.finny.pet.game.ui.ButtonStyle
import ru.finny.pet.game.ui.G
import ru.finny.pet.game.ui.GameButton
import ru.finny.pet.game.ui.Hud1
import ru.finny.pet.game.ui.Hud2
import ru.finny.pet.game.ui.LocalParticles
import ru.finny.pet.game.ui.Panel
import ru.finny.pet.game.ui.ResidentPic
import ru.finny.pet.game.ui.StatsCollapsed
import ru.finny.pet.game.ui.TText
import ru.finny.pet.game.ui.particleTarget

/** A job round (§E.4): match-3 or three errands; «Закончить», ⌂ and «Назад» finish the shift and show its result. */
@Composable
fun RoundScreen(vm: GameViewModel, jobId: String) {
    val job = vm.tc.jobs.firstOrNull { it.id == jobId }
    val result = vm.roundResult
    Column(Modifier.fillMaxSize()) {
        Hud1(vm, inPlace = true)
        Hud2 { StatsCollapsed(vm); MailChip(vm) }
        when {
            result != null -> Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Panel(Modifier.fillMaxWidth().padding(8.dp).testTag("job_result").particleTarget(LocalParticles.current, "job_result")) {
                    vm.tc.residents.firstOrNull { it.id == job?.resident }?.let { ResidentPic(it, 72.dp, Modifier.align(Alignment.CenterHorizontally)) }
                    TText("Спасибо за помощь!", style = MaterialTheme.typography.headlineSmall)
                    TText(result.text)
                    result.why.forEach { TText(it, style = MaterialTheme.typography.bodyMedium, color = G.inkSoft) }
                    GameButton("Готово", Modifier.fillMaxWidth(), minHeight = 48.dp) { vm.closeRound() }
                }
            }
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
