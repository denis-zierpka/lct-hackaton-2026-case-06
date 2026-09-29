package ru.finny.pet.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class Kind { COIN, CONFETTI, SPARKLE, HEART, BUBBLE, STAR }

private class Particle(
    val kind: Kind, var x: Float, var y: Float, var vx: Float, var vy: Float,
    val born: Long, val life: Long, val size: Float, val color: Color, var rot: Float, val spin: Float,
    val from: Offset? = null, val to: Offset? = null, val ctrl: Offset? = null,
)

/**
 * One particle system for the whole app, drawn on a full-screen Canvas above everything.
 * Named targets (coins HUD, piggy HUD, pet) are registered by the widgets via [target].
 */
class ParticleController {
    private val ps = ArrayList<Particle>()
    private val targets = HashMap<String, Offset>()
    var coinImage: ImageBitmap? = null
    /** False while animations are off (ТЗ 3.6): nothing new is spawned. */
    var enabled = true
    internal var tick by mutableLongStateOf(0L)
    private var now = 0L

    fun target(name: String, at: Offset) { targets[name] = at }
    fun targetOf(name: String): Offset? = targets[name]
    /** A new screen: targets of the old one are gone (TOWN-S1d §F — an effect plays only where both targets are). */
    fun clearTargets() = targets.clear()
    val isEmpty: Boolean get() = ps.isEmpty()

    /** Coins fly from [from] to a named target along an arc, then vanish (the HUD number animates on its own). */
    fun coins(from: Offset, toName: String, count: Int = 8, size: Float = 48f) {
        val to = targets[toName] ?: return
        if (!enabled) return
        repeat(count) { i ->
            val ctrl = Offset((from.x + to.x) / 2 + Random.nextFloat() * 200 - 100, minOf(from.y, to.y) - 150 - Random.nextFloat() * 120)
            ps += Particle(Kind.COIN, from.x, from.y, 0f, 0f, now + i * 55L, 620, size * (0.8f + Random.nextFloat() * 0.4f), G.gold, Random.nextFloat() * 6f, Random.nextFloat() * 6f - 3f, from, to, ctrl)
        }
    }

    fun confetti(center: Offset, count: Int = 60) {
        if (!enabled) return
        val colors = listOf(G.magenta, G.gold, G.lavender, G.green, G.pink, G.sky, Color.White)
        repeat(count) {
            val a = Random.nextFloat() * Math.PI.toFloat() * 2
            val sp = 350f + Random.nextFloat() * 500f
            ps += Particle(Kind.CONFETTI, center.x, center.y, cos(a) * sp, sin(a) * sp - 300f, now, 1400L + Random.nextLong(600), 10f + Random.nextFloat() * 10f, colors.random(), Random.nextFloat() * 6f, Random.nextFloat() * 10f - 5f)
        }
    }

    fun burst(center: Offset, kind: Kind, count: Int = 12, color: Color = G.gold, spread: Float = 260f) {
        if (!enabled) return
        repeat(count) {
            val a = Random.nextFloat() * Math.PI.toFloat() * 2
            val sp = spread * (0.4f + Random.nextFloat() * 0.6f)
            ps += Particle(kind, center.x, center.y, cos(a) * sp, sin(a) * sp - 120f, now, 900L + Random.nextLong(400), 14f + Random.nextFloat() * 12f, color, 0f, Random.nextFloat() * 4f - 2f)
        }
    }

    fun hearts(center: Offset) = burst(center, Kind.HEART, 7, G.magenta, 180f)
    fun sparkles(center: Offset) = burst(center, Kind.SPARKLE, 14, G.gold, 240f)
    fun bubbles(center: Offset) = burst(center, Kind.BUBBLE, 14, G.sky, 160f)
    fun stars(center: Offset) = burst(center, Kind.STAR, 10, G.gold, 300f)

    internal fun step(t: Long, dtSec: Float) {
        now = t
        val it = ps.iterator()
        while (it.hasNext()) {
            val p = it.next()
            if (t < p.born) continue
            val age = t - p.born
            if (age > p.life) { it.remove(); continue }
            if (p.kind == Kind.COIN && p.from != null && p.to != null && p.ctrl != null) {
                val u = (age.toFloat() / p.life).coerceIn(0f, 1f)
                val e = u * u * (3 - 2 * u)
                p.x = (1 - e) * (1 - e) * p.from.x + 2 * (1 - e) * e * p.ctrl.x + e * e * p.to.x
                p.y = (1 - e) * (1 - e) * p.from.y + 2 * (1 - e) * e * p.ctrl.y + e * e * p.to.y
                p.rot += p.spin * dtSec
            } else {
                val g = when (p.kind) { Kind.BUBBLE -> -250f; Kind.HEART -> -60f; Kind.SPARKLE, Kind.STAR -> 500f; else -> 900f }
                p.vy += g * dtSec
                p.vx *= (1 - 1.8f * dtSec)
                p.x += p.vx * dtSec; p.y += p.vy * dtSec; p.rot += p.spin * dtSec
            }
        }
        tick = t
    }

    internal fun draw(scope: DrawScope) = with(scope) {
        for (p in ps) {
            if (tick < p.born) continue
            val age = tick - p.born
            val u = (age.toFloat() / p.life).coerceIn(0f, 1f)
            val alpha = if (p.kind == Kind.COIN) 1f else (1f - u * u)
            when (p.kind) {
                Kind.COIN -> {
                    val img = coinImage
                    val s = p.size * (1f + 0.15f * sin(p.rot))
                    if (img != null) drawImage(img, dstOffset = IntOffset((p.x - s / 2).toInt(), (p.y - s / 2).toInt()), dstSize = IntSize(s.toInt(), s.toInt()))
                    else drawCircle(G.gold, s / 2, Offset(p.x, p.y))
                }
                Kind.CONFETTI -> rotate(p.rot * 57f, Offset(p.x, p.y)) {
                    drawRect(p.color.copy(alpha = alpha), Offset(p.x - p.size / 2, p.y - p.size / 4), Size(p.size, p.size / 2))
                }
                Kind.SPARKLE -> rotate(p.rot * 57f, Offset(p.x, p.y)) {
                    val r = p.size * (1f - u)
                    drawLine(p.color.copy(alpha = alpha), Offset(p.x - r, p.y), Offset(p.x + r, p.y), 3f)
                    drawLine(p.color.copy(alpha = alpha), Offset(p.x, p.y - r), Offset(p.x, p.y + r), 3f)
                }
                Kind.STAR -> translate(p.x, p.y) { rotate(p.rot * 57f, Offset.Zero) { drawPath(star(p.size * (1.2f - 0.6f * u)), p.color.copy(alpha = alpha)) } }
                Kind.HEART -> translate(p.x, p.y) { drawPath(heart(p.size * (1f + 0.4f * u)), p.color.copy(alpha = alpha)) }
                Kind.BUBBLE -> {
                    drawCircle(Color.White.copy(alpha = alpha * 0.55f), p.size / 2, Offset(p.x, p.y))
                    drawCircle(p.color.copy(alpha = alpha * 0.8f), p.size / 2, Offset(p.x, p.y), style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
                    drawCircle(Color.White.copy(alpha = alpha), p.size / 8, Offset(p.x - p.size / 5, p.y - p.size / 5))
                }
            }
        }
    }

    private fun star(r: Float): Path = Path().apply {
        for (i in 0 until 10) {
            val rad = if (i % 2 == 0) r else r * 0.45f
            val a = (i * 36 - 90) * Math.PI.toFloat() / 180
            val x = cos(a) * rad; val y = sin(a) * rad
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }

    private fun heart(s: Float): Path = Path().apply {
        moveTo(0f, s * 0.35f)
        cubicTo(-s * 0.9f, -s * 0.3f, -s * 0.45f, -s * 0.8f, 0f, -s * 0.3f)
        cubicTo(s * 0.45f, -s * 0.8f, s * 0.9f, -s * 0.3f, 0f, s * 0.35f)
        close()
    }
}

val LocalParticles = staticCompositionLocalOf { ParticleController() }

@Composable
fun ParticleLayer(controller: ParticleController) {
    LaunchedEffect(controller) {
        var last = 0L
        while (true) {
            withFrameNanos { t ->
                val ms = t / 1_000_000
                val dt = if (last == 0L) 0f else ((ms - last) / 1000f).coerceAtMost(0.05f)
                last = ms
                controller.step(ms, dt)
            }
        }
    }
    val tick = controller.tick // subscribe to redraw
    Canvas(Modifier.fillMaxSize()) { if (tick >= 0) controller.draw(this) }
}
