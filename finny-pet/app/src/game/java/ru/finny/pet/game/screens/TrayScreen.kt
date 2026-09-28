package ru.finny.pet.game.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ru.finny.pet.domain.town.Tray
import ru.finny.pet.domain.town.TrayRound
import ru.finny.pet.game.GameViewModel
import ru.finny.pet.game.LocalAnimate
import ru.finny.pet.game.TrayCue
import ru.finny.pet.game.orNone
import ru.finny.pet.game.ui.ButtonStyle
import ru.finny.pet.game.ui.G
import ru.finny.pet.game.ui.GameButton
import ru.finny.pet.game.ui.LocalParticles
import ru.finny.pet.game.ui.Pic
import ru.finny.pet.game.ui.ResidentPic
import ru.finny.pet.game.ui.TText
import ru.finny.pet.game.ui.particleTarget

// counter colours of the bakery mock (tools/mock_bakery.py), UI only
private val CounterFront = Color(0xFFF4A261)
private val CounterEdge = Color(0xFFD68046)
private val CounterTop = Color(0xFFD9A36C)
private val CounterShade = Color(0xFFC48C58)
private val CounterSlat = Color(0xFFE29254)
private val TrayPlate = Color(0xFFFAF4E8)
private val TrayRim = Color(0xFFD2BEA0)
private val SlotDash = Color(0xFF8C7A66)
private val PaperEdge = Color(0xFFD8D3E2)

// ponytail: how long the «Спасибо!» line stays on screen — UI timing, not an economy number
private const val THANKS_MS = 1200L

/** The bakery counter strip: front, bottom edge, slats and the top plank with its shade; no semantics. */
@Composable
internal fun Counter(modifier: Modifier) = Canvas(modifier) {
    val (front, edge, top, shade) = listOf(13.dp.toPx(), 9.dp.toPx(), 19.dp.toPx(), 5.dp.toPx())
    drawRoundRect(CounterFront, Offset(0f, front), Size(size.width, size.height - front), CornerRadius(9.dp.toPx()))
    drawRect(CounterEdge, Offset(0f, size.height - edge), Size(size.width, edge))
    var x = 40.dp.toPx()
    while (x < size.width) { drawLine(CounterSlat, Offset(x, 23.dp.toPx()), Offset(x, size.height - front), 2.dp.toPx()); x += 80.dp.toPx() }
    drawRoundRect(CounterTop, Offset.Zero, Size(size.width, top), CornerRadius(7.dp.toPx()))
    drawRect(CounterShade, Offset(0f, top - shade), Size(size.width, shade))
}

private fun DrawScope.dashedCircle(color: Color, width: Dp, radius: Float = size.minDimension / 2 - width.toPx() / 2) =
    drawCircle(color, radius, style = Stroke(width.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))))

private fun starPath(c: Offset, r: Float) = Path().apply {
    for (i in 0 until 10) {
        val a = Math.PI * (i * 36 - 90) / 180; val k = if (i % 2 == 0) r else r * 0.45f
        val (x, y) = c.x + k * Math.cos(a).toFloat() to c.y + k * Math.sin(a).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

@Composable
private fun Star(size: Dp) = Canvas(Modifier.size(size)) {
    val p = starPath(center, this.size.minDimension / 2 - 1.dp.toPx())
    drawPath(p, G.gold)
    drawPath(p, G.purpleDeep, style = Stroke(1.5.dp.toPx()))
}

/** White bubble with a tail: down at the start (customer) or up at [tailX] (Borya's «?»); no tail when [tail] is false. */
@Composable
private fun Bubble(modifier: Modifier, radius: Dp, shadow: Dp, pad: Dp, tailUp: Boolean = false, tailX: Dp = 20.dp, tail: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    Box(modifier) {
        Column(
            Modifier.shadow(shadow, RoundedCornerShape(radius), ambientColor = G.purpleDeep, spotColor = G.purpleDeep)
                .background(Color.White, RoundedCornerShape(radius)).padding(pad),
            verticalArrangement = Arrangement.spacedBy(8.dp), content = content,
        )
        if (tail) Canvas(Modifier.align(if (tailUp) Alignment.TopStart else Alignment.BottomStart).offset(x = tailX).size(22.dp, 12.dp)
            .graphicsLayer { translationY = (if (tailUp) -11 else 11).dp.toPx() }) {
            val p = Path().apply {
                if (tailUp) { moveTo(0f, size.height); lineTo(size.width, size.height); lineTo(size.width * 0.5f, 0f) }
                else { moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width * 0.35f, size.height) }
                close()
            }
            drawPath(p, Color.White)
        }
    }
}

/** A new pastry in its slot: scaleIn with animations, at once without them. */
@Composable
private fun PopIn(slot: Int, item: String, animate: Boolean, content: @Composable () -> Unit) =
    AnimatedVisibility(remember(slot, item) { MutableTransitionState(!animate).apply { targetState = true } }, enter = scaleIn(tween(150)).orNone()) { content() }

/** Customer frame P and Borya frame for a scene of height [a] (TOWN-J1-1a § 4). */
private fun customerFrame(a: Dp): Dp = minOf((a * 0.62f).coerceIn(128.dp, 220.dp), a - 56.dp + 48.dp - 84.dp)

/** The «Поднос» round (TOWN-J1-1a § 4–§ 6): scene, showcase, shift row; pointer, riddle bubble and live region on top. */
@Composable
fun TrayScreen(vm: GameViewModel, modifier: Modifier) {
    val r = vm.tray ?: return
    val job = vm.tc.jobs.firstOrNull { it.id == r.jobId } ?: return
    val emoji = { id: String -> job.menu.firstOrNull { it.id == id }?.emoji.orEmpty() }
    val thanks = vm.trayThanks
    LaunchedEffect(thanks) { if (thanks != null) { delay(THANKS_MS); vm.trayThanksDone() } }
    val shown = if (thanks != null) r.index - 1 else minOf(r.index, r.orders.lastIndex)
    val riddle = vm.riddleOpen
    val modal = if (riddle != null) Modifier.clearAndSetSemantics {} else Modifier
    val bounds = remember { mutableStateMapOf<String, Rect>() }
    var root by remember { mutableStateOf(Rect.Zero) }
    val density = LocalDensity.current
    fun Modifier.bound(key: String) = onGloballyPositioned { bounds[key] = it.boundsInRoot() }
    BoxWithConstraints(modifier.fillMaxWidth().onGloballyPositioned { root = it.boundsInRoot() }) {
        val b = (maxHeight * 0.145f).coerceIn(64.dp, 88.dp)
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth().then(modal).semantics { isTraversalGroup = true }) {
                TrayScene(vm, r, job.resident, shown, emoji) { bounds["ask"] = it }
            }
            Column(Modifier.then(modal).padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(2) { row ->
                    Row(Modifier.fillMaxWidth().height(b), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(3) { col ->
                            val id = r.menu.getOrNull(row * 3 + col)
                            if (id == null) Spacer(Modifier.weight(1f)) else {
                                val k = r.tray.count { it == id }; val title = vm.pastryTitle(id)
                                Box(
                                    Modifier.weight(1f).fillMaxHeight().bound("p:$id").shadow(4.dp, RoundedCornerShape(16.dp)).background(Color.White, RoundedCornerShape(16.dp))
                                        .clickable(role = Role.Button, onClickLabel = "Положить на поднос") { vm.trayPut(id) }
                                        .semantics { contentDescription = title; if (k > 0) stateDescription = "на подносе: $k" },
                                    contentAlignment = Alignment.Center,
                                ) { Pic(null, emoji(id), 56.dp) }
                            }
                        }
                    }
                }
            }
            Row(
                Modifier.then(modal).padding(start = 8.dp, end = 8.dp, bottom = 8.dp).fillMaxWidth().heightIn(min = 56.dp).testTag("job_row")
                    .background(G.paperTint, RoundedCornerShape(16.dp)).padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StarRow(r)
                GameButton("Отдать", Modifier.weight(1f).bound("give"), ButtonStyle.PRIMARY, minHeight = 48.dp) { vm.trayGive() }
            }
        }

        // pointer of the first shift: the next thing to tap, static, no semantics
        if (r.pointer && thanks == null && !r.done) {
            val full = r.tray.size == r.orders[r.index].items.size
            bounds[if (full) "give" else "p:" + Tray.missing(r).first()]?.let { t ->
                val x = with(density) { (t.right - root.left).toDp() } - 44.dp
                val y = with(density) { (t.bottom - root.top).toDp() } - 44.dp
                Pic(null, "☝️", 40.dp, Modifier.offset(x, y))
            }
        }

        if (riddle != null) {
            BackHandler { vm.closeRiddle() }
            Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { vm.closeRiddle() } })
            // the «?» as laid out, in TrayScreen coordinates
            val ask = bounds["ask"]
            val askBottom = with(density) { ((ask?.bottom ?: 0f) - root.top).toDp() }
            val askX = with(density) { ((ask?.center?.x ?: 0f) - root.left).toDp() }
            var bubbleH by remember { mutableIntStateOf(0) }
            val bubbleTop = maxHeight - 8.dp - with(density) { bubbleH.toDp() }
            val line = vm.riddleLine
            val face = RoundedCornerShape(24.dp)
            // потолок 8 dp от верха — «Закончить» под открытой загадкой недоступна, модальность её и так закрывает
            Bubble(
                Modifier.align(Alignment.BottomCenter).padding(8.dp).fillMaxWidth().heightIn(max = (maxHeight - 16.dp).coerceAtLeast(48.dp))
                    .onSizeChanged { bubbleH = it.height }
                    .pointerInput(Unit) { detectTapGestures {} }.semantics { paneTitle = "Загадка Бори" },
                radius = 20.dp, shadow = 8.dp, pad = 14.dp, tailUp = true, tailX = askX - 8.dp - 11.dp,
                tail = ask != null && bubbleH > 0 && bubbleTop >= askBottom,
            ) {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (line == null) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TText("Загадка Бори", Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.titleMedium, color = G.purple)
                            Box(
                                Modifier.clickable(role = Role.Button) { vm.closeRiddle() }.border(2.dp, G.purple, face)
                                    .heightIn(min = 48.dp).padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) { TText("Не сейчас", style = MaterialTheme.typography.bodyLarge, color = G.purple) }
                        }
                        // строка пользы — только когда подсказке правда есть что класть на поднос (решение № 55 б)
                        if (Tray.hint(r, vm.tc.rules.riddleHint) != r) TText("Отгадаешь — Боря поможет с подносом", style = MaterialTheme.typography.bodyMedium, color = G.inkSoft)
                        TText(riddle.question)
                        riddle.options.forEachIndexed { i, o ->
                            Box(
                                Modifier.fillMaxWidth().clickable(role = Role.Button) { vm.trayAnswer(i) }
                                    .drawBehind { drawRoundRect(PaperEdge, Offset(0f, 5.dp.toPx()), Size(size.width, size.height - 5.dp.toPx()), CornerRadius(24.dp.toPx())) }
                                    .padding(bottom = 5.dp).background(Brush.verticalGradient(listOf(Color.White, G.paperTint)), face)
                                    .heightIn(min = 48.dp).padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.CenterStart,
                            ) { TText(o, style = MaterialTheme.typography.bodyLarge, color = G.purpleDeep) }
                        }
                    } else {
                        TText(line.text)
                        line.why.forEach { TText(it, style = MaterialTheme.typography.bodyMedium, color = G.inkSoft) }
                        GameButton("Дальше", Modifier.fillMaxWidth(), ButtonStyle.PRIMARY, minHeight = 48.dp) { vm.closeRiddle() }
                    }
                }
            }
        }

        // live region: always composed, read after «Отдать»
        Box(Modifier.size(1.dp).semantics { contentDescription = vm.trayNote; liveRegion = LiveRegionMode.Polite; traversalIndex = 1000f })
    }
}

/** Scene: customer and Borya behind the counter, the customer's bubble, «?», the tray and «Закончить». */
@Composable
private fun TrayScene(vm: GameViewModel, r: TrayRound, host: String, shown: Int, emoji: (String) -> String, onAskPlaced: (Rect) -> Unit) {
    val animate = LocalAnimate.current
    val particles = LocalParticles.current
    val thanks = vm.trayThanks
    val miss = vm.trayMiss
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val counterTop = maxHeight - 56.dp
        val p = customerFrame(maxHeight)
        val f = p * 0.8f
        val feet = counterTop + 48.dp
        val order = r.orders[shown]

        val jump = remember { Animatable(0f) }
        LaunchedEffect(thanks) { if (thanks != null && animate) { jump.animateTo(-16f, tween(150)); jump.animateTo(0f, tween(200)) } }
        val tr = if (animate) fadeIn(tween(200)) togetherWith fadeOut(tween(120)) else EnterTransition.None togetherWith ExitTransition.None
        AnimatedContent(
            targetState = shown, transitionSpec = { tr }, label = "customer",
            modifier = Modifier.offset(y = feet - p).graphicsLayer { translationY = jump.value.dp.toPx() }.particleTarget(particles, "customer").clearAndSetSemantics {},
        ) { i -> vm.tc.residents.firstOrNull { it.id == r.orders[i].customer }?.let { ResidentPic(it, p) } }
        vm.tc.residents.firstOrNull { it.id == host }?.let {
            ResidentPic(it, f, Modifier.offset(x = maxWidth - f, y = feet - f).clearAndSetSemantics {})
        }
        Counter(Modifier.offset(y = counterTop).fillMaxWidth().height(56.dp))

        // the tray: centre on the counter's top edge; slots of the shown order
        if (thanks != null || !r.done) {
            val seen = remember { mutableIntStateOf(vm.trayWobble) }
            val tilt = remember { Animatable(0f) }
            LaunchedEffect(vm.trayWobble) {
                if (vm.trayWobble != seen.intValue) {
                    seen.intValue = vm.trayWobble
                    if (animate) { tilt.animateTo(4f, tween(60)); tilt.animateTo(-4f, tween(120)); tilt.animateTo(0f, tween(60)) }
                }
            }
            val plate = RoundedCornerShape(32.dp)
            val full = vm.trayCue == TrayCue.FULL
            Row(
                Modifier.align(Alignment.TopCenter).offset(y = counterTop - 32.dp).graphicsLayer { rotationZ = tilt.value }
                    .background(TrayPlate, plate).border(if (full) 3.dp else 2.dp, if (full) G.purple else TrayRim, plate)
                    .semantics { contentDescription = vm.trayText(r) }.padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                repeat(order.items.size) { i ->
                    val item = r.tray.getOrNull(i)
                    if (item != null) {
                        val title = vm.pastryTitle(item)
                        Box(
                            Modifier.size(48.dp).background(Color.White, CircleShape).clickable(role = Role.Button, onClickLabel = "Убрать") { vm.trayTake(i) }
                                .semantics { contentDescription = "$title на подносе" },
                            contentAlignment = Alignment.Center,
                        ) {
                            PopIn(i, item, animate) { Pic(null, emoji(item), 40.dp) }
                        }
                    } else {
                        val short = vm.trayCue == TrayCue.SHORT
                        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                            Canvas(Modifier.size(40.dp)) { dashedCircle(if (short) G.purple else SlotDash, if (short) 4.dp else 2.dp) }
                        }
                    }
                }
            }
        }

        // the customer's bubble at the head: order pictures, ✓ and dashed circles after a mismatch, or «Спасибо!»
        val bubbleBottom = feet - p + p * 0.05f - 4.dp
        val name = vm.residentName(thanks?.customer ?: order.customer)
        val desc = when {
            thanks != null -> "$name: спасибо!" + if (thanks.star) " Звезда" else ""
            else -> "$name. Заказ: ${vm.trayList(order.items)}" + (miss?.let { ". Ещё нужно: ${vm.trayList(it.missing)}" } ?: "")
        }
        if (thanks != null || !r.done) Bubble(
            Modifier.layout { m, c ->
                val pl = m.measure(c.copy(minWidth = 0, minHeight = 0, maxWidth = (maxWidth - 16.dp).roundToPx()))
                layout(pl.width, pl.height) { pl.place(8.dp.roundToPx(), maxOf(8.dp.roundToPx(), bubbleBottom.roundToPx() - pl.height)) }
            }.clearAndSetSemantics { contentDescription = desc },
            radius = 16.dp, shadow = 4.dp, pad = 8.dp,
        ) {
            if (thanks != null) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TText("Спасибо!", style = MaterialTheme.typography.titleMedium, maxLines = 1)
                if (thanks.star) {
                    Star(24.dp)
                    TText("+1", style = MaterialTheme.typography.titleMedium, color = G.purpleDeep, maxLines = 1)
                }
            } else {
                val s = if (order.items.size <= 3) 48.dp else 40.dp
                val left = miss?.missing.orEmpty().groupingBy { it }.eachCount()
                val total = order.items.groupingBy { it }.eachCount()
                val seen = mutableMapOf<String, Int>()
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    order.items.forEach { id ->
                        val k = (seen[id] ?: 0) + 1
                        seen[id] = k
                        val ok = miss != null && k <= (total[id] ?: 0) - (left[id] ?: 0)
                        val gap = miss != null && !ok
                        Box(Modifier.size(s).drawBehind { if (gap) dashedCircle(G.purple, 3.dp) }) {
                            Pic(null, emoji(id), s)
                            if (ok) Canvas(Modifier.align(Alignment.BottomEnd).size(20.dp)) {
                                drawCircle(G.greenDark)
                                val w = size.width
                                val tick = Path().apply { moveTo(w * 0.27f, w * 0.52f); lineTo(w * 0.44f, w * 0.68f); lineTo(w * 0.74f, w * 0.34f) }
                                drawPath(tick, Color.White, style = Stroke(2.dp.toPx()))
                            }
                        }
                    }
                }
            }
        }

        // Borya's riddle: static «?» at his left edge, above the tray plate
        if (vm.trayRiddle() != null) {
            val y = minOf(feet - f + 48.dp, counterTop - 32.dp - 4.dp) - 48.dp
            Box(
                Modifier.offset(x = maxWidth - f, y = y).onGloballyPositioned { onAskPlaced(it.boundsInRoot()) }.size(48.dp).background(G.purple, CircleShape)
                    .clickable(role = Role.Button) { vm.openRiddle() }.semantics { contentDescription = "Загадка Бори" },
                contentAlignment = Alignment.Center,
            ) { TText("?", Modifier.clearAndSetSemantics {}, style = MaterialTheme.typography.titleLarge, color = Color.White, maxLines = 1) }
        }

        GameButton("Закончить", Modifier.align(Alignment.TopEnd).padding(8.dp).semantics { traversalIndex = -1f }, ButtonStyle.PAPER, minHeight = 48.dp) { vm.finishRound() }
    }
}

/** TalkBack ряда звёзд подноса (раунд и итог, TOWN-J1-1b1). */
internal fun starsText(r: TrayRound) = "Обслужено ${r.results.size} из ${r.orders.size}, звёзд ${r.stars}"
/** ★ «+1» — с первого раза, ● — обслужен, ○ — ещё придёт (раунд) / не пришёл (итог); один узел TalkBack [starsText]. */
@Composable internal fun StarRow(r: TrayRound) {
    Row(
        Modifier.clearAndSetSemantics { contentDescription = starsText(r) },
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        r.orders.indices.forEach { i ->
            when (r.results.getOrNull(i)) {
                true -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Star(24.dp); TText("+1", style = MaterialTheme.typography.bodyMedium, color = G.purpleDeep, maxLines = 1)
                }
                false -> Box(Modifier.size(16.dp).background(G.purpleDeep, CircleShape))
                null -> Box(Modifier.size(16.dp).border(2.dp, G.purpleDeep, CircleShape))
            }
        }
    }
}
