package ru.finny.pet.game.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.finny.pet.R
import ru.finny.pet.game.GameViewModel
import ru.finny.pet.game.LocalAnimate
import ru.finny.pet.game.LocalLayout
import ru.finny.pet.game.LocalVm
import ru.finny.pet.game.ui.ButtonStyle
import ru.finny.pet.game.ui.G
import ru.finny.pet.game.ui.GameBar
import ru.finny.pet.game.ui.GameButton
import ru.finny.pet.game.ui.LocalParticles
import ru.finny.pet.game.ui.particleTarget
import ru.finny.pet.game.audio.Sound

private const val STEP = 10

/** Three jars and a purse: tap "+" and a coin flies from the purse into the jar. Rules stay in Economy.setPlan. */
@Composable
fun PlanScreen(vm: GameViewModel) {
    val s = vm.state
    val particles = LocalParticles.current
    val layout = LocalLayout.current
    PanelScreen(vm, if (s.plan.confirmed) "План на неделю принят" else "План на неделю") {
        if (s.plan.confirmed) {
            ConfirmedPlan(vm)
            return@PanelScreen
        }
        val plan = s.plan
        val rest = s.balance - plan.total
        val canAdd = rest >= STEP

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.particleTarget(particles, "purse"), contentAlignment = Alignment.Center) {
                Image(painterResource(R.drawable.ui_purse), null, Modifier.size(if (layout.compact) 52.dp else 64.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(if (rest < 0) "Не хватает ${-rest} монет" else "В кошельке: $rest из ${s.balance}", style = MaterialTheme.typography.titleMedium, color = if (rest < 0) G.red else G.purpleDeep)
                Text("Реши заранее, сколько на что. Потом сравним план с тем, что вышло.", style = MaterialTheme.typography.bodySmall, color = G.inkSoft)
            }
            if (layout.landscape) GameButton("Подтвердить", style = ButtonStyle.GOLD, minHeight = 48.dp, enabled = rest >= 0 && plan.total > 0) { vm.confirmPlan() }
        }
        val jars: @Composable RowScope.() -> Unit = {
            Jar("Обязательное", "еда и уход", R.drawable.ui_lid_mandatory, plan.mandatory, s.balance, G.mandatory, canAdd, "jar0", Modifier.weight(1f)) { vm.setPlan(it, plan.optional, plan.savings) }
            Jar("Желаемое", "игрушки", R.drawable.ui_lid_optional, plan.optional, s.balance, G.optional, canAdd, "jar1", Modifier.weight(1f)) { vm.setPlan(plan.mandatory, it, plan.savings) }
            Jar("Копилка", "на цель", R.drawable.ui_lid_savings, plan.savings, s.balance, G.savings, canAdd, "jar2", Modifier.weight(1f)) { vm.setPlan(plan.mandatory, plan.optional, it) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) { jars() }
        if (!layout.landscape) GameButton("Подтвердить план", Modifier.fillMaxWidth(), style = ButtonStyle.GOLD, enabled = rest >= 0 && plan.total > 0) { vm.confirmPlan() }
        Text("После подтверждения план нельзя менять до конца недели — так и в жизни.", style = MaterialTheme.typography.bodySmall, color = G.inkSoft, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun Jar(title: String, hint: String, lid: Int, value: Int, max: Int, color: Color, canAdd: Boolean, target: String, modifier: Modifier, onChange: (Int) -> Unit) {
    val particles = LocalParticles.current
    val vm = LocalVm.current
    val layout = LocalLayout.current
    val fill by animateFloatAsState((value.toFloat() / max.coerceAtLeast(1)).coerceIn(0f, 1f), if (LocalAnimate.current) spring(stiffness = Spring.StiffnessLow) else snap(), label = "fill")
    val jarH: Dp = if (layout.compact) 84.dp else 110.dp
    Column(modifier.background(G.paperTint, RoundedCornerShape(20.dp)).padding(if (layout.landscape) 8.dp else 4.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = if (layout.landscape) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelMedium, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(hint, style = MaterialTheme.typography.bodySmall, color = G.inkSoft, textAlign = TextAlign.Center)
        Box(Modifier.height(jarH + 20.dp).fillMaxWidth().particleTarget(particles, target), contentAlignment = Alignment.BottomCenter) {
            // The jar sprite is a 512px frame with the glass in the middle ~52% width, ~78% height; coins stack inside that area.
            Box(Modifier.size(jarH), contentAlignment = Alignment.BottomCenter) {
                Box(Modifier.padding(bottom = jarH * 0.09f).size(jarH * 0.46f, jarH * 0.7f).clip(RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp)), contentAlignment = Alignment.BottomCenter) {
                    Box(Modifier.fillMaxWidth().height(jarH * 0.7f * fill).background(color.copy(alpha = 0.35f)))
                    Column(verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally) {
                        val coins = (value / STEP).coerceAtMost(9)
                        repeat(coins) { i -> Image(painterResource(R.drawable.ui_coin), null, Modifier.size(jarH * 0.4f, jarH * 0.075f).graphicsLayer { rotationZ = if (i % 2 == 0) -5f else 5f; scaleY = 0.55f }) }
                    }
                }
                Image(painterResource(R.drawable.ui_jar), null, Modifier.size(jarH))
            }
            Image(painterResource(lid), null, Modifier.size(jarH * 0.5f).align(Alignment.TopCenter).offset(y = (-4).dp))
        }
        val minus: @Composable () -> Unit = {
            GameButton("−", Modifier.width(48.dp), style = ButtonStyle.PAPER, enabled = value > 0, minHeight = 48.dp) { onChange((value - STEP).coerceAtLeast(0)); vm.sfx(Sound.POP) }
        }
        val number: @Composable () -> Unit = { Text("$value", style = MaterialTheme.typography.titleLarge, color = G.purpleDeep, modifier = Modifier.widthIn(min = 36.dp), textAlign = TextAlign.Center) }
        val plus: @Composable () -> Unit = {
            GameButton("+", Modifier.width(48.dp), style = ButtonStyle.PRIMARY, enabled = canAdd, minHeight = 48.dp) {
                onChange(value + STEP)
                particles.targetOf("purse")?.let { particles.coins(it, target, 1, 40f) }
                vm.sfx(Sound.COIN)
            }
        }
        // a portrait jar is ~90 dp wide: two 48 dp buttons (ТЗ 3.6) do not fit side by side, so they stack
        if (layout.landscape) Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) { minus(); number(); plus() }
        else Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) { plus(); number(); minus() }
    }
}

/** Plan vs. fact for the running week (2.5.5: shown after confirmation). */
@Composable
fun ConfirmedPlan(vm: GameViewModel) {
    val s = vm.state
    Text("Вот как идут дела:", style = MaterialTheme.typography.bodyLarge, color = G.ink)
    PlanFact(listOf(
        Triple("Обязательное", s.plan.mandatory, s.factMandatory),
        Triple("Желаемое", s.plan.optional, s.factOptional),
        Triple("Копилка", s.plan.savings, s.factSavings),
    ))
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Image(painterResource(R.drawable.ui_purse), null, Modifier.size(44.dp))
        Text("Осталось монет: ${s.balance}", style = MaterialTheme.typography.titleMedium, color = G.purpleDeep)
    }
}

/** Plan/fact bars with a ✓ or ! mark: colour is never the only signal (ТЗ 3.6). */
@Composable
fun PlanFact(rows: List<Triple<String, Int, Int>>) {
    val colors = listOf(G.mandatory, G.optional, G.savings)
    val icons = listOf(R.drawable.ui_lid_mandatory, R.drawable.ui_lid_optional, R.drawable.ui_lid_savings)
    rows.forEachIndexed { i, (title, plan, fact) ->
        val bad = if (i == 2) fact < plan else fact > plan
        Row(Modifier.fillMaxWidth().background(G.paperTint, RoundedCornerShape(16.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Image(painterResource(icons[i]), null, Modifier.size(40.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleSmall, color = G.ink, modifier = Modifier.weight(1f))
                    Text("план $plan · факт $fact", style = MaterialTheme.typography.labelMedium, color = if (bad) G.red else G.greenDark)
                }
                GameBar("", fact, colors[i], max = maxOf(plan, fact, 1))
            }
            Box(Modifier.size(36.dp).background(if (bad) G.red else G.green, RoundedCornerShape(50)), contentAlignment = Alignment.Center) {
                Text(if (bad) "!" else "✓", style = MaterialTheme.typography.titleMedium, color = Color.White)
            }
        }
    }
}
