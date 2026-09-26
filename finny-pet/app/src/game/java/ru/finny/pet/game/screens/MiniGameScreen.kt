package ru.finny.pet.game.screens

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import ru.finny.pet.game.orNone
import ru.finny.pet.game.audio.Sound
import ru.finny.pet.game.ui.ButtonStyle
import ru.finny.pet.game.ui.G
import ru.finny.pet.game.ui.GameButton
import ru.finny.pet.game.ui.LocalParticles
import ru.finny.pet.game.ui.TText
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
 * The match-3 round of a job (§E.4): the board and the job row. Bombs come from «Загадка Бори» and the job level.
 * Rules live in domain/Match3; this screen only animates the steps of each Turn.
 */
@Composable
fun MiniGameScreen(vm: GameViewModel, modifier: Modifier = Modifier) {
    val m = vm.match ?: return
    val particles = LocalParticles.current
    val scope = rememberCoroutineScope()
    val tiles = remember { mutableStateListOf<VTile>() }
    var nextId by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Cell?>(null) }
    var bombMode by remember { mutableStateOf(false) }
    // no moves left: the board stops taking taps while the shift result comes (the view model resets it with a new round)
    var over by vm::matchOver
    val popups = remember { mutableStateListOf<Popup>() }
    var popupKey by remember { mutableLongStateOf(0L) }
    var boardOrigin by remember { mutableStateOf(Offset.Zero) }
    var cellPx by remember { mutableFloatStateOf(1f) }
    val animate = LocalAnimate.current
    val swapSpec: AnimationSpec<Float> = if (animate) spring(stiffness = Spring.StiffnessMediumLow) else snap()
    // the moves are over: «Закончить» by itself (system back, ⌂ and the button go through the view model)
    suspend fun ended() { if (vm.match?.finished == true) { over = true; delay(300); vm.finishRound() } }

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
                    popups += Popup("+${step.points}", step.cells.first(), popupKey++)
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
        }
    }

    fun trySwap(a: Cell, b: Cell) {
        if (busy || over) return
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
                ended()
            }
            busy = false
        }
    }

    fun tryBomb(c: Cell) {
        if (busy) return
        val turn = vm.matchBomb(c) ?: return
        bombMode = false
        scope.launch { busy = true; particles.stars(centerOf(c)); play(turn); ended(); busy = false }
    }

    fun onTap(c: Cell) {
        if (busy || over) return
        if (bombMode) { tryBomb(c); return }
        val sel = selected
        if (sel == null) { selected = c; vm.sfx(Sound.TAP); return }
        if (sel == c) { selected = null; return }
        if (ru.finny.pet.domain.Match3.adjacent(sel, c)) { selected = null; trySwap(sel, c) } else { selected = c; vm.sfx(Sound.TAP) }
    }

    val cur = vm.match ?: m

    Column(modifier.fillMaxWidth()) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
            val cell = minOf(maxWidth / cur.width, maxHeight / cur.height)
            val px = with(LocalDensity.current) { cell.toPx() }
            cellPx = px
            Box(
                Modifier.size(cell * cur.width, cell * cur.height)
                    .testTag("board")
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
                            onDrag = { change, _ ->
                                val st = start ?: return@detectDragGestures
                                if (done || bombMode || over) return@detectDragGestures
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
                // fixed cells for measuring (§B.10): the tiles move, the cells stay
                for (r in 0 until cur.height) for (c in 0 until cur.width) Box(Modifier.offset(cell * c, cell * r).size(cell).testTag("cell_${r}_$c"))
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
                        androidx.compose.animation.AnimatedVisibility(shown, enter = (fadeIn() + scaleIn(initialScale = 0.5f)).orNone(), exit = fadeOut().orNone()) {
                            TText(p.text, style = MaterialTheme.typography.headlineSmall, color = G.gold, maxLines = 1, modifier = Modifier.offset { IntOffset((p.cell.col * px).roundToInt(), ((p.cell.row - 0.4f) * px).roundToInt()) })
                        }
                    }
                }
            }
        }
        // job row: shift tokens · score and moves · bombs · «Закончить»
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("job_row").padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                ShiftTokens(vm, label = false, color = G.gold)
                TText(if (bombMode) "Куда бомбочку? Нажми на клетку" else "Счёт ${cur.score} · Ходы ${cur.movesLeft}", style = MaterialTheme.typography.bodyMedium, color = Color.White)
            }
            GameButton(
                "💣 ×${cur.bombs}", Modifier.semantics { contentDescription = "Бомбочки: ${cur.bombs}" },
                style = if (bombMode) ButtonStyle.MAGENTA else ButtonStyle.GOLD, enabled = cur.bombs > 0 && !busy && !over, minHeight = 48.dp,
            ) { bombMode = !bombMode; selected = null }
            GameButton("Закончить", minHeight = 48.dp, enabled = !busy) { vm.finishRound() }
        }
    }
}
