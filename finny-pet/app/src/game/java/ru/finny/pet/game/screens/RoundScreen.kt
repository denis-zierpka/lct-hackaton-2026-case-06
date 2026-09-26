package ru.finny.pet.game.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import ru.finny.pet.game.ui.StatsCollapsed
import ru.finny.pet.game.ui.TText
import ru.finny.pet.game.ui.chip
import ru.finny.pet.game.ui.particleTarget

/** Раунд работы (§E.4) — заглушка кодера 1: ⌂ и «Назад» = «Закончить» → итог; раскладку делает кодер 2. */
@Composable
fun RoundScreen(vm: GameViewModel, jobId: String) {
    val job = vm.tc.jobs.firstOrNull { it.id == jobId }
    val particles = LocalParticles.current
    val result = vm.roundResult
    Column(Modifier.fillMaxSize()) {
        Hud1(vm, inPlace = true)
        Hud2 {
            StatsCollapsed(vm)
            Box(Modifier.heightIn(min = 44.dp).chip().particleTarget(particles, "mail").padding(horizontal = 10.dp)) {
                TText("✉ +${vm.state.envelope.sumOf { it.amount }}", style = MaterialTheme.typography.titleMedium, color = G.purpleDeep, maxLines = 1)
            }
        }
        when {
            result != null -> Panel(Modifier.fillMaxWidth().padding(8.dp).testTag("job_result").particleTarget(particles, "job_result")) {
                TText("Спасибо за помощь!", style = MaterialTheme.typography.headlineSmall)
                TText(result.text)
                result.why.forEach { TText(it, style = MaterialTheme.typography.bodyMedium, color = G.inkSoft) }
                GameButton("Готово", Modifier.fillMaxWidth(), minHeight = 48.dp) { vm.closeRound() }
            }
            vm.match != null -> Box(Modifier.weight(1f)) { MiniGameScreen(vm) }
            else -> Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TText(job?.title ?: jobId, style = MaterialTheme.typography.headlineSmall, color = Color.White)
                vm.taps.forEachIndexed { i, done ->
                    GameButton((if (done) "✓ " else "") + job?.tasks?.getOrNull(i).orEmpty(), Modifier.fillMaxWidth(), ButtonStyle.PAPER, minHeight = 48.dp) { vm.tapTask(i) }
                }
                GameButton("Закончить", Modifier.fillMaxWidth(), minHeight = 48.dp) { vm.finishRound() }
            }
        }
    }
}
