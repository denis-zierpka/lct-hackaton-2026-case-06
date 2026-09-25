package ru.finny.pet.game.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.finny.pet.R
import ru.finny.pet.domain.Cell
import ru.finny.pet.domain.Step
import ru.finny.pet.domain.Tile
import ru.finny.pet.domain.Turn
import ru.finny.pet.game.GameViewModel
import ru.finny.pet.game.LocalAnimate
import ru.finny.pet.game.LocalLayout
import ru.finny.pet.game.orNone
import ru.finny.pet.game.audio.Sound
import ru.finny.pet.game.ui.ButtonStyle
import ru.finny.pet.game.ui.CloseButton
import ru.finny.pet.game.ui.G
import ru.finny.pet.game.ui.GameButton
import ru.finny.pet.game.ui.LocalParticles
import ru.finny.pet.game.ui.Panel
import ru.finny.pet.game.ui.SpeechBubble
import kotlin.math.abs
import kotlin.math.roundToInt

private fun Tile.res(): Int = when (this) {
    Tile.COIN -> R.drawable.tile_coin
    Tile.APPLE -> R.drawable.tile_apple
    Tile.GIFT -> R.drawable.tile_gift
    Tile.PIGGY -> R.drawable.tile_piggy
    Tile.STAR -> R.drawable.tile_star
}

private fun Tile.label(): String = when (this) {
    Tile.COIN -> "монета"; Tile.APPLE -> "яблоко"; Tile.GIFT -> "подарок"; Tile.PIGGY -> "свинка"; Tile.STAR -> "звезда"
}

/** A tile on screen: its logical cell plus animated position (in cells) and scale. */
private class VTile(val id: Int, val tile: Tile, var col: Int, var row: Int) {
    val x = Animatable(col.toFloat())
    val y = Animatable(row.toFloat())
    val scale = Animatable(1f)
}

private class Popup(val text: String, val cell: Cell, val key: Long)

/**
 * «Монетки в ряд»: match-3 whose bombs are earned by answering the pet's questions.
 * Rules live in domain/Match3; this screen only animates the steps of each Turn.
 */
@Composable
fun MiniGameScreen(vm: GameViewModel) {
    val m = vm.match ?: run { vm.navigate(ru.finny.pet.game.Screen.Room); return }
    val s = vm.state
    val layout = LocalLayout.current
    val particles = LocalParticles.current
    val scope = rememberCoroutineScope()
    val tiles = remember { mutableStateListOf<VTile>() }
    var nextId by remember { mutableStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Cell?>(null) }
    var bombMode by remember { mutableStateOf(false) }
    var over by remember { mutableStateOf(false) }
    val popups = remember { mutableStateListOf<Popup>() }
    var boardOrigin by remember { mutableStateOf(Offset.Zero) }
    var cellPx by remember { mutableStateOf(1f) }
    val animate = LocalAnimate.current
    val swapSpec: AnimationSpec<Float> = if (animate) spring(stiffness = Spring.StiffnessMediumLow) else snap()
    // system back = «Закончить»: first the round-over panel, then the result is taken; there is no exit that skips it
    BackHandler { if (over) vm.finishMiniGame() else { over = true; bombMode = false } }

    // initial board
    LaunchedEffect(Unit) {
        if (tiles.isEmpty()) {
            for (row in 0 until m.height) for (col in 0 until m.width) {
                m[Cell(col, row)]?.let { tiles += VTile(nextId++, it, col, row) }
            }
        }
    }

    fun at(c: Cell): VTile? = tiles.firstOrNull { it.col == c.col && it.row == c.row }
    fun centerOf(c: Cell) = Offset(boardOrigin.x + (c.col + 0.5f) * cellPx, boardOrigin.y + (c.row + 0.5f) * cellPx)

    suspend fun play(turn: Turn) {
        for (step in turn.steps) {
            when (step) {
                is Step.Swap -> {
                    val a = at(step.a); val b = at(step.b)
                    if (a != null && b != null) {
                        a.col = step.b.col; a.row = step.b.row; b.col = step.a.col; b.row = step.a.row
                        coroutineScope {
                            launch { a.x.animateTo(a.col.toFloat(), swapSpec) }
                            launch { a.y.animateTo(a.row.toFloat(), swapSpec) }
                            launch { b.x.animateTo(b.col.toFloat(), swapSpec) }
                            launch { b.y.animateTo(b.row.toFloat(), swapSpec) }
                        }
                    }
                }
                is Step.Match -> {
                    vm.sfx(Sound.MATCH)
                    val victims = step.cells.mapNotNull { at(it) }
                    val centre = step.cells.map { centerOf(it) }.let { list -> Offset(list.map { it.x }.average().toFloat(), list.map { it.y }.average().toFloat()) }
                    particles.sparkles(centre)
                    popups += Popup("+${step.points}", step.cells.first(), System.nanoTime())
                    if (animate) coroutineScope { victims.map { v -> async { v.scale.animateTo(0f, tween(220)) } }.awaitAll() }
                    tiles.removeAll(victims.toSet())
                }
                is Step.Fall -> {
                    val moving = step.moves.mapNotNull { (from, to) -> at(from)?.also { it.col = to.col; it.row = to.row } }
                    if (animate) coroutineScope { moving.map { t -> async { t.y.animateTo(t.row.toFloat(), spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium)) } }.awaitAll() }
                    else moving.forEach { t -> t.y.snapTo(t.row.toFloat()) }
                }
                is Step.Refill -> {
                    val fresh = step.cells.map { (c, tile) -> VTile(nextId++, tile, c.col, c.row).also { v -> tiles += v } }
                    if (animate) {
                        fresh.forEach { v -> v.y.snapTo(v.row.toFloat() - m.height - 0.5f) }
                        coroutineScope { fresh.map { v -> async { v.y.animateTo(v.row.toFloat(), spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium)) } }.awaitAll() }
                    }
                }
            }
        }
        // the engine may have reshuffled the board when no move was left: resync visuals
        val st = vm.match ?: return
        val mismatch = tiles.size != st.tiles.count { it != null } || tiles.any { st[Cell(it.col, it.row)] != it.tile }
        if (mismatch) {
            tiles.clear()
            for (row in 0 until st.height) for (col in 0 until st.width) st[Cell(col, row)]?.let { tiles += VTile(nextId++, it, col, row) }
            vm.say("Ходов не осталось — я перемешал поле!", 4000)
        }
    }

    fun trySwap(a: Cell, b: Cell) {
        if (busy) return
        val turn = vm.matchSwap(a, b)
        scope.launch {
            busy = true
            if (turn == null) {
                val va = at(a); val vb = at(b)
                if (va != null && vb != null && animate) {
                    val dx = (b.col - a.col) * 0.25f; val dy = (b.row - a.row) * 0.25f
                    coroutineScope {
                        launch { va.x.animateTo(a.col + dx, tween(90)); va.x.animateTo(a.col.toFloat(), spring(dampingRatio = 0.4f)) }
                        launch { va.y.animateTo(a.row + dy, tween(90)); va.y.animateTo(a.row.toFloat(), spring(dampingRatio = 0.4f)) }
                        launch { vb.x.animateTo(b.col - dx, tween(90)); vb.x.animateTo(b.col.toFloat(), spring(dampingRatio = 0.4f)) }
                        launch { vb.y.animateTo(b.row - dy, tween(90)); vb.y.animateTo(b.row.toFloat(), spring(dampingRatio = 0.4f)) }
                    }
                }
            } else {
                play(turn)
                if (vm.match?.finished == true) { delay(300); over = true }
            }
            busy = false
        }
    }

    fun tryBomb(c: Cell) {
        if (busy) return
        val turn = vm.matchBomb(c) ?: return
        bombMode = false
        scope.launch { busy = true; particles.stars(centerOf(c)); play(turn); if (vm.match?.finished == true) { delay(300); over = true }; busy = false }
    }

    fun onTap(c: Cell) {
        if (busy || over) return
        if (bombMode) { tryBomb(c); return }
        val sel = selected
        if (sel == null) { selected = c; vm.sfx(Sound.TAP); return }
        if (sel == c) { selected = null; return }
        if (ru.finny.pet.domain.Match3.adjacent(sel, c)) { selected = null; trySwap(sel, c) } else { selected = c; vm.sfx(Sound.TAP) }
    }

    val bubble = vm.bubble
    val cur = vm.match ?: m

    Box(Modifier.fillMaxSize().background(G.purpleDeep.copy(alpha = 0.6f))) {
        val hud: @Composable () -> Unit = {
            Column(Modifier.widthIn(max = 260.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Монетки в ряд", style = MaterialTheme.typography.titleLarge, color = Color.White, textAlign = TextAlign.Center)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Stat("Очки", "${cur.score}")
                    Stat("Ходы", "${cur.movesLeft}")
                }
                Text("Монет за игру: до ${vm.economy.miniGameCoinsLeft(s)}. Очков на монету: ${vm.content.rules.miniGameScorePerCoin}.", style = MaterialTheme.typography.bodySmall, color = G.pink, textAlign = TextAlign.Center)
                GameButton(if (bombMode) "Куда бомбочку?" else "Бомбочка ×${cur.bombs}", Modifier.fillMaxWidth(), style = if (bombMode) ButtonStyle.MAGENTA else ButtonStyle.GOLD, enabled = cur.bombs > 0 && !busy, icon = painterResource(R.drawable.tile_bomb), iconSize = 30.dp) { bombMode = !bombMode; selected = null }
                GameButton("Вопрос → бомбочка", Modifier.fillMaxWidth(), style = ButtonStyle.PAPER, minHeight = 48.dp, enabled = bubble == null && vm.economy.availableQuiz(s).isNotEmpty()) { vm.askQuestion() }
                GameButton("Закончить", Modifier.fillMaxWidth(), style = ButtonStyle.GHOST, minHeight = 48.dp, enabled = !busy) { over = true }
            }
        }
        val board: @Composable (Modifier) -> Unit = { mod ->
            BoxWithConstraints(mod, contentAlignment = Alignment.Center) {
                val cell = minOf(maxWidth / cur.width, maxHeight / cur.height)
                val px = with(LocalDensity.current) { cell.toPx() }
                cellPx = px
                Box(
                    Modifier.size(cell * cur.width, cell * cur.height)
                        .onGloballyPositioned { boardOrigin = it.positionInRoot() }
                        .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
                        .border(2.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .pointerInput(busy, over, bombMode) {
                            detectTapGestures { p -> onTap(Cell((p.x / px).toInt().coerceIn(0, cur.width - 1), (p.y / px).toInt().coerceIn(0, cur.height - 1))) }
                        }
                        .pointerInput(busy, over, bombMode) {
                            var start: Cell? = null; var done = false
                            detectDragGestures(
                                onDragStart = { p -> start = Cell((p.x / px).toInt().coerceIn(0, cur.width - 1), (p.y / px).toInt().coerceIn(0, cur.height - 1)); done = false },
                                onDrag = { change, drag ->
                                    val st = start ?: return@detectDragGestures
                                    if (done || bombMode) return@detectDragGestures
                                    val total = change.position - Offset((st.col + 0.5f) * px, (st.row + 0.5f) * px)
                                    if (abs(total.x) > px * 0.35f || abs(total.y) > px * 0.35f) {
                                        val n = if (abs(total.x) > abs(total.y)) Cell(st.col + if (total.x > 0) 1 else -1, st.row) else Cell(st.col, st.row + if (total.y > 0) 1 else -1)
                                        done = true; selected = null
                                        if (n.col in 0 until cur.width && n.row in 0 until cur.height) trySwap(st, n)
                                    }
                                },
                            )
                        },
                ) {
                    tiles.forEach { v ->
                        val sel = selected?.let { it.col == v.col && it.row == v.row } == true
                        Image(
                            painterResource(v.tile.res()), contentDescription = v.tile.label(),
                            modifier = Modifier
                                .offset { IntOffset((v.x.value * px).roundToInt(), (v.y.value * px).roundToInt()) }
                                .size(cell)
                                .padding(cell * 0.06f)
                                .graphicsLayer { scaleX = v.scale.value * (if (sel) 1.12f else 1f); scaleY = v.scale.value * (if (sel) 1.12f else 1f) }
                                .then(if (sel) Modifier.border(3.dp, G.gold, RoundedCornerShape(12.dp)) else Modifier),
                        )
                    }
                    popups.forEach { p ->
                        key(p.key) {
                            var shown by remember { mutableStateOf(false) }
                            LaunchedEffect(p.key) { shown = true; delay(900); popups.remove(p) }
                            AnimatedVisibility(shown, enter = (fadeIn() + scaleIn(initialScale = 0.5f)).orNone(), exit = fadeOut().orNone()) {
                                Text(p.text, style = MaterialTheme.typography.headlineSmall, color = G.gold, modifier = Modifier.offset { IntOffset((p.cell.col * px).roundToInt(), ((p.cell.row - 0.4f) * px).roundToInt()) })
                            }
                        }
                    }
                }
            }
        }
        if (layout.landscape) {
            Row(Modifier.fillMaxSize().padding(start = 60.dp, end = 16.dp, top = 8.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                board(Modifier.weight(1.4f).fillMaxSize())
                Box(Modifier.weight(0.8f), contentAlignment = Alignment.Center) { hud() }
            }
        } else {
            Column(Modifier.fillMaxSize().padding(start = 8.dp, end = 8.dp, top = 56.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                hud(); board(Modifier.weight(1f).fillMaxWidth())
            }
        }
        CloseButton({ over = true }, label = "Закончить")

        // the pet asks a question → a bomb
        val pet = s.pet
        AnimatedVisibility(bubble != null, enter = (fadeIn() + scaleIn(initialScale = 0.85f)).orNone(), exit = fadeOut().orNone()) {
            val b = bubble ?: return@AnimatedVisibility
            Box(Modifier.fillMaxSize().background(G.scrim).pointerInput(Unit) {}, contentAlignment = Alignment.Center) {
                Row(Modifier.padding(16.dp).widthIn(max = 640.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (pet != null) Image(painterResource(ru.finny.pet.PetSprites.id(pet.speciesId, pet.colorId, vm.economy.stageIndex(pet.growth), "happy")), null, Modifier.size(if (layout.compact) 96.dp else 130.dp))
                    SpeechBubble(Modifier.weight(1f).padding(bottom = 20.dp)) {
                        Text(b.text, style = MaterialTheme.typography.bodyLarge, color = G.ink)
                        if (b.options.isNotEmpty()) b.options.forEachIndexed { i, o -> GameButton(o, Modifier.fillMaxWidth(), style = ButtonStyle.PAPER, minHeight = 48.dp) { vm.answerBubble(i) } }
                        else GameButton("Понятно", Modifier.fillMaxWidth(), style = ButtonStyle.PRIMARY, minHeight = 48.dp) { vm.clearBubble() }
                    }
                }
            }
        }

        // round over
        AnimatedVisibility(over, enter = (fadeIn() + scaleIn(initialScale = 0.8f)).orNone(), exit = fadeOut().orNone()) {
            Box(Modifier.fillMaxSize().background(G.scrim), contentAlignment = Alignment.Center) {
                Panel(Modifier.widthIn(max = 420.dp)) {
                    Text("Игра окончена!", style = MaterialTheme.typography.headlineSmall, color = G.purpleDeep)
                    Text("Очки: ${cur.score}. Монеты: +${(cur.score / vm.content.rules.miniGameScorePerCoin).coerceAtMost(vm.economy.miniGameCoinsLeft(s))}", style = MaterialTheme.typography.bodyLarge, color = G.ink)
                    GameButton("Забрать монеты", Modifier.fillMaxWidth(), style = ButtonStyle.GOLD, icon = painterResource(R.drawable.ui_coin), iconSize = 26.dp) { vm.finishMiniGame() }
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(Modifier.background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = G.pink)
        Text(value, style = MaterialTheme.typography.headlineSmall, color = Color.White)
    }
}
