package ru.finny.pet.game.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ru.finny.pet.R
import ru.finny.pet.domain.Need
import ru.finny.pet.game.GameViewModel
import ru.finny.pet.game.LocalAnimate
import ru.finny.pet.game.LocalLayout
import ru.finny.pet.game.orNone
import ru.finny.pet.game.ui.LocalParticles
import ru.finny.pet.game.LocalPetAction
import ru.finny.pet.game.Screen
import ru.finny.pet.game.ui.ButtonStyle
import ru.finny.pet.game.ui.G
import ru.finny.pet.game.ui.GameBar
import ru.finny.pet.game.ui.GameButton
import ru.finny.pet.game.ui.HudChip
import ru.finny.pet.game.ui.PetSprite
import ru.finny.pet.game.ui.PropButton
import ru.finny.pet.game.ui.SpeechBubble
import ru.finny.pet.game.ui.TypewriterText
import ru.finny.pet.game.ui.particleTarget

/**
 * The room: pet in the middle, HUD on top, prop buttons at the bottom.
 * Everything the ТЗ wants on the main screen is here at once (2.5.3): pet, coins, savings, goal, needs, next step.
 * Landscape: needs left, goal and «Сейчас» right. Portrait: one column top to bottom, nothing overlaps at 360 dp.
 */
@Composable
fun RoomScreen(vm: GameViewModel) {
    val s = vm.state
    val pet = s.pet ?: return
    val e = vm.economy
    val layout = LocalLayout.current
    val portrait = !layout.landscape
    val particles = LocalParticles.current
    val petAction = LocalPetAction.current
    var confirmEnd by rememberSaveable { mutableStateOf(false) }

    // The pet talks on its own: the next step first, then idle lines and questions.
    LaunchedEffect(pet.name, s.period, s.plan.confirmed, s.purchases.size, s.goal?.id) {
        delay(900)
        vm.say(vm.nextStep().first, ttlMs = 9000)
        while (true) {
            delay(14_000)
            if (vm.bubble == null) {
                if (e.availableQuiz(s).isNotEmpty() && (System.currentTimeMillis() / 14_000) % 3 == 0L) vm.askQuestion()
                else vm.say(vm.nextLine(pet.name), ttlMs = 8000)
            }
        }
    }
    val bubble = vm.bubble
    LaunchedEffect(bubble) { if (bubble != null && bubble.questionId == null) { delay(bubble.ttlMs); if (vm.bubble === bubble) vm.clearBubble() } }

    val openTasks = e.availableTasks(s).size
    val stage = e.stageIndex(pet.growth)
    val step = vm.nextStep()
    val gameLock = e.miniGameLock(s)
    val stageLine = "${e.stageTitle(pet.growth)} · неделя ${s.period}" + if (s.demo) " · демо" else ""
    val panelBg = G.purpleDeep.copy(alpha = 0.55f)

    val hud: @Composable () -> Unit = {
        Row(Modifier.fillMaxWidth().padding(horizontal = if (portrait) 8.dp else 12.dp, vertical = if (portrait) 2.dp else 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (portrait) 4.dp else 8.dp)) {
            HudChip(painterResource(R.drawable.ui_coin), s.balance, "Монеты", Modifier.particleTarget(particles, "coins"), compact = portrait)
            HudChip(painterResource(R.drawable.ui_piggy), s.savings, "Копилка", Modifier.particleTarget(particles, "piggy"), compact = portrait)
            Spacer(Modifier.weight(1f))
            PropButton("Прогресс", painterResource(R.drawable.ui_trophy), size = 48.dp, showLabel = false, tight = portrait, onClick = { vm.navigate(Screen.Progress) })
            PropButton("Подсказка", painterResource(R.drawable.ui_bubble_q), size = 48.dp, showLabel = false, tight = portrait, onClick = { vm.navigate(Screen.Intro) })
            PropButton("Взрослым", painterResource(R.drawable.ui_lock), size = 48.dp, showLabel = false, tight = portrait, onClick = { vm.navigate(Screen.Parent) })
        }
    }
    val petSprite: @Composable (Modifier, Dp) -> Unit = { mod, size ->
        PetSprite(
            speciesId = pet.speciesId, colorId = pet.colorId, stage = stage, face = e.face(pet), animate = LocalAnimate.current,
            size = size, bounceKey = vm.bounce, action = petAction.action, actionKey = petAction.key,
            description = "${pet.name}, ${e.stageTitle(pet.growth)}. ${e.faceReason(pet)}",
            modifier = mod.particleTarget(particles, "pet"), seen = petAction,
            onTap = vm::petTapped,
        )
    }
    // the active task stays on screen (2.5.3); the week end asks first, like the moon button
    val now: @Composable (Modifier) -> Unit = { mod ->
        GameButton("Сейчас: ${step.second}", mod.fillMaxWidth(), style = ButtonStyle.GOLD, minHeight = 48.dp) {
            if (step.third == Screen.WeekEnd) confirmEnd = true else vm.navigate(step.third)
        }
    }
    val actions: @Composable (Modifier) -> Unit = { mod ->
        Row(
            mod.fillMaxWidth().padding(horizontal = if (portrait) 0.dp else 4.dp, vertical = if (portrait) 2.dp else 6.dp),
            horizontalArrangement = Arrangement.spacedBy(if (portrait) 0.dp else 6.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.Bottom,
        ) {
            // portrait: long labels get more width, so «Копилка» fits at 14 sp even with six buttons
            fun m(label: String) = if (portrait) Modifier.weight(if (label.length > 5) 7f else 5f) else Modifier
            val bs = if (portrait) 48.dp else 64.dp
            PropButton("План", painterResource(R.drawable.ui_jar), m("План"), size = bs, tight = portrait, onClick = { vm.navigate(Screen.Plan) })
            PropButton("Магазин", painterResource(R.drawable.ui_bag), m("Магазин"), size = bs, tight = portrait, onClick = { vm.navigate(Screen.Shop) })
            PropButton("Копилка", painterResource(R.drawable.ui_piggy), m("Копилка"), size = bs, tight = portrait, onClick = { vm.navigate(Screen.Savings) })
            PropButton("Задания", painterResource(R.drawable.ui_book), m("Задания"), size = bs, badge = openTasks, tight = portrait, onClick = { vm.navigate(Screen.Tasks) })
            // closed until the plan: still tappable, the reason matters more than the lock (2.8)
            PropButton(
                "Игра", painterResource(R.drawable.ui_gamepad), m("Игра").alpha(if (gameLock != null) 0.5f else 1f), size = bs, badge = s.bombs, tight = portrait,
                description = if (gameLock != null) "Игра, закрыто до плана" else "Игра", onClick = { vm.startMiniGame() },
            )
            if (e.canEndPeriod(s)) {
                if (!portrait) Spacer(Modifier.width(8.dp))
                val end = if (portrait) "Спать" else "Завершить неделю"
                PropButton(end, painterResource(R.drawable.ui_moon), m(end), size = bs, tight = portrait, onClick = { confirmEnd = true })
            }
        }
    }
    val goal = s.goal

    Box(Modifier.fillMaxSize()) {
        if (!portrait) {
            hud()
            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 60.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    BubbleHost(vm, bubble, Modifier.height(if (layout.compact) 88.dp else 104.dp).widthIn(max = 420.dp))
                    petSprite(Modifier, 230.dp)
                }
            }
            // side panels grow with the font until the longest word fits, then phrases wrap (ТЗ 3.6)
            val sidePanelWidth = Modifier.widthIn(min = if (layout.compact) 170.dp else 200.dp, max = 240.dp).width(IntrinsicSize.Min)
            Column(
                Modifier.align(Alignment.CenterStart).padding(start = 12.dp, end = 12.dp, top = 60.dp)
                    .then(sidePanelWidth).background(panelBg, RoundedCornerShape(20.dp)).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(pet.name, style = MaterialTheme.typography.titleLarge, color = Color.White)
                Text(stageLine, style = MaterialTheme.typography.bodySmall, color = G.pink)
                GameBar("Сытость", pet.hunger, G.gold, dark = true, icon = painterResource(R.drawable.item_food_basic))
                GameBar("Чистота", pet.clean, G.sky, dark = true, icon = painterResource(R.drawable.item_care_shampoo))
                GameBar("Настроение", pet.mood, G.green, dark = true, icon = painterResource(R.drawable.item_fun_ball))
            }
            Column(
                Modifier.align(Alignment.CenterEnd).padding(start = 12.dp, end = 12.dp, top = 60.dp, bottom = 12.dp)
                    .then(sidePanelWidth).background(panelBg, RoundedCornerShape(20.dp)).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("Цель", style = MaterialTheme.typography.labelMedium, color = G.pink)
                if (goal != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Image(painterResource(goalRes(goal.id)), null, Modifier.size(44.dp))
                        Text(goal.title, style = MaterialTheme.typography.titleSmall, color = Color.White)
                    }
                    Text("${s.savings} из ${goal.price} монет", style = MaterialTheme.typography.bodySmall, color = Color.White)
                    GameBar("", s.savings, G.magenta, max = goal.price, dark = true)
                } else {
                    Text("Пока не выбрана", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                    GameButton("Выбрать", style = ButtonStyle.MAGENTA, minHeight = 48.dp) { vm.navigate(Screen.Savings) }
                }
                now(Modifier)
            }
            actions(Modifier.align(Alignment.BottomCenter))
        } else {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                hud()
                Column(
                    Modifier.padding(horizontal = 12.dp).fillMaxWidth().background(panelBg, RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(pet.name, style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text(stageLine, style = MaterialTheme.typography.bodySmall, color = G.pink)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NeedPill("Сытость", pet.hunger, G.gold, painterResource(R.drawable.item_food_basic), Modifier.weight(1f))
                        NeedPill("Чистота", pet.clean, G.sky, painterResource(R.drawable.item_care_shampoo), Modifier.weight(1f))
                        NeedPill("Настроение", pet.mood, G.green, painterResource(R.drawable.item_fun_ball), Modifier.weight(1f))
                    }
                }
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                    // the pet shrinks on a small phone; a long bubble may cover only the sprite's empty top margin
                    petSprite(Modifier.align(Alignment.BottomCenter), (maxHeight - 72.dp).coerceIn(120.dp, 260.dp))
                    BubbleHost(vm, bubble, Modifier.align(Alignment.TopCenter).widthIn(max = 420.dp))
                }
                now(Modifier.padding(horizontal = 12.dp))
                Row(
                    Modifier.padding(horizontal = 12.dp).fillMaxWidth().background(panelBg, RoundedCornerShape(20.dp))
                        .clickable { vm.navigate(Screen.Savings) }.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (goal != null) {
                        Image(painterResource(goalRes(goal.id)), null, Modifier.size(44.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Цель: ${goal.title} · ${s.savings} из ${goal.price}", style = MaterialTheme.typography.bodySmall, color = Color.White)
                            GameBar("", s.savings, G.magenta, max = goal.price, dark = true)
                        }
                    } else {
                        Text("Цель пока не выбрана", style = MaterialTheme.typography.bodyMedium, color = Color.White, modifier = Modifier.weight(1f))
                        GameButton("Выбрать", style = ButtonStyle.MAGENTA, minHeight = 48.dp) { vm.navigate(Screen.Savings) }
                    }
                }
                actions(Modifier)
            }
        }

        if (confirmEnd) {
            val food = s.purchases.any { it.need == Need.FOOD }
            val care = s.purchases.any { it.need == Need.CARE }
            ConfirmPanel(
                title = "Завершить неделю ${s.period}?",
                lines = listOfNotNull(
                    "${pet.name} получит итог недели, а ты — новые ${vm.content.rules.allowance} монет.",
                    if (!food) "Еда на этой неделе ещё не куплена — ${pet.name} проголодается." else null,
                    if (!care) "Уход на этой неделе ещё не куплен — ${pet.name} запачкается." else null,
                    if (s.factSavings <= 0) "Копилка на этой неделе не выросла." else null,
                ),
                confirmText = "Спать!",
                onConfirm = { confirmEnd = false; vm.endPeriod() },
                onDismiss = { confirmEnd = false },
            )
        }
    }
}

/** One need in the portrait strip: icon, bar and number; TalkBack hears the name, colour is not the only signal (ТЗ 3.6). */
@Composable
private fun NeedPill(label: String, value: Int, color: Color, icon: Painter, modifier: Modifier = Modifier) {
    Row(modifier.clearAndSetSemantics { contentDescription = "$label $value из 100" }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Image(icon, null, Modifier.size(24.dp))
        GameBar("", value, color, Modifier.weight(1f), dark = true)
        Text("$value", style = MaterialTheme.typography.labelLarge, color = Color.White)
    }
}

@Composable
private fun BubbleHost(vm: GameViewModel, bubble: ru.finny.pet.game.Bubble?, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(visible = bubble != null, enter = (fadeIn(tween(200)) + scaleIn(tween(260), initialScale = 0.7f)).orNone(), exit = (fadeOut(tween(150)) + scaleOut(tween(150))).orNone()) {
            val b = bubble ?: return@AnimatedVisibility
            SpeechBubble(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                TypewriterText(b.text, animate = LocalAnimate.current)
                if (b.options.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        b.options.forEachIndexed { i, o -> GameButton(o, style = ButtonStyle.PAPER, minHeight = 48.dp) { vm.answerBubble(i) } }
                    }
                }
            }
        }
    }
}

fun goalRes(id: String): Int = when (id) {
    "goal_scooter" -> R.drawable.goal_scooter
    "goal_paints" -> R.drawable.goal_paints
    "goal_lego" -> R.drawable.goal_lego
    "goal_zoo" -> R.drawable.goal_zoo
    else -> R.drawable.goal_custom
}

fun itemRes(id: String): Int = when (id) {
    "food_basic" -> R.drawable.item_food_basic
    "food_lunch" -> R.drawable.item_food_lunch
    "care_shampoo" -> R.drawable.item_care_shampoo
    "care_brush" -> R.drawable.item_care_brush
    "care_vitamins" -> R.drawable.item_care_vitamins
    "fun_ball" -> R.drawable.item_fun_ball
    "fun_bow" -> R.drawable.item_fun_bow
    "fun_balloon" -> R.drawable.item_fun_balloon
    "fun_book" -> R.drawable.item_fun_book
    else -> R.drawable.item_fun_tent
}
