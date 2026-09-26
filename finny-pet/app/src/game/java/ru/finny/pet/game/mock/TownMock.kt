package ru.finny.pet.game.mock

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.finny.pet.R
import ru.finny.pet.game.ui.ButtonStyle
import ru.finny.pet.game.ui.G
import ru.finny.pet.game.ui.GameButton

// ponytail: debug-only static mock-ups of the «Городок» screens (TOWN-S0c); no logic, removed by R8 in release.

internal enum class MockScreen { HOME, SHOP, JOB }

internal class MockData(val wallet: Int, val piggy: Int, val eta: String, val banks: List<Int>, val mail: Int, val stats: List<Int>)

private val NORMAL = MockData(88, 80, "≈2✉", listOf(40, 30, 18), 18, listOf(70, 80, 60))
private val MAXED = MockData(188, 150, "≈10✉", listOf(100, 70, 18), 36, listOf(100, 100, 100))

internal class Stat(val icon: Int, val word: String, val emoji: String)
internal val STATS = listOf(Stat(R.drawable.item_food_basic, "Сытость", "🍎"), Stat(R.drawable.item_care_shampoo, "Чистота", "🫧"), Stat(R.drawable.item_fun_ball, "Настроение", "🙂"))

internal class MockState {
    var max by mutableStateOf(false)
    var talk by mutableStateOf(false)
    val data get() = if (max) MAXED else NORMAL
    val clipped = mutableStateListOf<String>()
    val stack = mutableStateListOf(MockScreen.HOME)
    fun go(s: MockScreen) { if (s == MockScreen.HOME) stack.clear(); stack.add(s) }
}

internal val LocalMock = staticCompositionLocalOf { MockState() }

/** Entry chip on the parent screen (debug builds only) and the mock-up over the whole app. */
@Composable
fun TownMockHost(showEntry: Boolean) {
    var open by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        if (showEntry && !open) GameButton("Макеты „Городка“", Modifier.align(Alignment.BottomEnd).padding(8.dp), style = ButtonStyle.GOLD, minHeight = 48.dp) { open = true }
        if (open) TownMock { open = false }
    }
}

@Composable
private fun TownMock(onClose: () -> Unit) {
    val st = remember { MockState() }
    BackHandler { if (st.stack.size > 1) st.stack.removeAt(st.stack.lastIndex) else onClose() }
    // same cap as SceneFontScale in GameApp.kt: fixed geometry follows the system font scale up to 1.3
    val d = LocalDensity.current
    CompositionLocalProvider(LocalMock provides st, LocalDensity provides if (d.fontScale <= 1.3f) d else Density(d.density, 1.3f)) {
        Box(Modifier.fillMaxSize().background(G.purpleDeep).pointerInput(Unit) {}.semantics { testTagsAsResourceId = true }) {
            when (st.stack.last()) {
                MockScreen.HOME -> MockHome()
                MockScreen.SHOP -> MockShop()
                MockScreen.JOB -> MockJob()
            }
            val probe = "Обрезано: ${st.clipped.size}" + if (st.clipped.isEmpty()) "" else " — " + st.clipped.joinToString("; ")
            Box(Modifier.align(Alignment.TopEnd).size(1.dp).testTag("overflow").semantics { contentDescription = probe })
        }
    }
}

/** Font scale above 1.15: words give way to icons, the word moves to TalkBack. */
@Composable
internal fun bigFont() = LocalDensity.current.fontScale > 1.15f

/** Every text of the mock-up: no ellipsis, a clipped line is reported to the «overflow» probe. */
@Composable
internal fun MockText(text: String, style: TextStyle, modifier: Modifier = Modifier, color: Color = G.ink, maxLines: Int = 1, align: TextAlign? = null) {
    val clipped = LocalMock.current.clipped
    DisposableEffect(text) { onDispose { clipped.remove(text) } }
    Text(
        text, modifier, color = color, style = style.copy(localeList = RU), textAlign = align, maxLines = maxLines, softWrap = maxLines > 1, overflow = TextOverflow.Clip,
        onTextLayout = { r -> if (!r.hasVisualOverflow) clipped.remove(text) else if (text !in clipped) clipped.add(text) },
    )
}

// the game is Russian only: Russian hyphenation under any system locale
private val RU = LocaleList("ru")

internal fun Modifier.chip() = background(Color.White.copy(alpha = 0.92f), RoundedCornerShape(50))

/** Round 48 dp icon button of the HUD. */
@Composable
internal fun IconBox(icon: Int, desc: String, onClick: () -> Unit = {}) {
    Box(Modifier.size(48.dp).chip().clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = desc }, contentAlignment = Alignment.Center) {
        Image(painterResource(icon), null, Modifier.size(34.dp))
    }
}

/** A room object: flat shape, Role.Button and a TalkBack description. */
@Composable
internal fun Target(desc: String, w: Dp, h: Dp, modifier: Modifier = Modifier, color: Color = Color.White.copy(alpha = 0.85f), onClick: () -> Unit = {}, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier.size(w, h).background(color, RoundedCornerShape(10.dp)).clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = desc },
        contentAlignment = Alignment.Center, content = content,
    )
}

/** HUD-1: [⌂] wallet (long press — max numbers), piggy bank with the dream picture, «?», [🔑]. */
@Composable
internal fun Hud1(inPlace: Boolean) {
    val m = LocalMock.current
    val d = m.data
    val num = MaterialTheme.typography.titleMedium
    Row(Modifier.fillMaxWidth().height(56.dp).testTag("hud1").padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (inPlace) Box(Modifier.size(48.dp).chip().clickable(role = Role.Button) { m.go(MockScreen.HOME) }.semantics { contentDescription = "Домой" }, contentAlignment = Alignment.Center) {
            MockText("⌂", MaterialTheme.typography.titleLarge, color = G.purpleDeep)
        }
        Row(
            Modifier.heightIn(min = 48.dp).chip().combinedClickable(onLongClick = { m.max = !m.max }) {}
                .semantics { contentDescription = "Кошелёк ${d.wallet}. Удержи — макс. числа" }.padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(painterResource(R.drawable.ui_purse), null, Modifier.size(24.dp))
            MockText("${d.wallet} ▾", num, Modifier.padding(start = 4.dp), color = G.purpleDeep)
        }
        val big = bigFont()
        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
            Row(
                // above 1.15 two lines take the whole 56 dp row
                (if (big) Modifier.fillMaxHeight() else Modifier.heightIn(min = 48.dp)).chip()
                    .clearAndSetSemantics { contentDescription = "Копилка: мечта самокат, ${d.piggy} из 150, ${d.eta}" }.padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(painterResource(R.drawable.goal_scooter), null, Modifier.size(28.dp))
                if (big) Column(Modifier.padding(start = 4.dp)) {
                    MockText("${d.piggy}/150", MaterialTheme.typography.titleSmall, color = G.purpleDeep)
                    MockText(d.eta, MaterialTheme.typography.labelSmall, color = G.purpleDeep)
                } else FlowRow(Modifier.padding(start = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), itemVerticalAlignment = Alignment.CenterVertically) {
                    // one line if it fits, the ETA goes under the number otherwise
                    MockText("${d.piggy}/150", num.copy(lineHeight = 20.sp), color = G.purpleDeep)
                    MockText(d.eta, MaterialTheme.typography.titleSmall.copy(lineHeight = 18.sp), color = G.purpleDeep)
                }
            }
        }
        IconBox(R.drawable.ui_bubble_q, "Подсказка")
        if (!inPlace) IconBox(R.drawable.ui_lock, "Взрослым")
    }
}

/** Icon + number with a 0–100 bar under the number; the word is for TalkBack only. */
@Composable
internal fun StatChip(s: Stat, v: Int, desc: String = "${s.word} $v из 100") {
    Row(Modifier.heightIn(min = 44.dp).chip().clearAndSetSemantics { contentDescription = desc }.padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(s.icon), null, Modifier.size(28.dp))
        Column(Modifier.padding(start = 4.dp)) {
            MockText("$v", MaterialTheme.typography.titleMedium.copy(lineHeight = 20.sp), color = G.purpleDeep)
            Box(Modifier.width(32.dp).height(5.dp).background(G.ink.copy(alpha = 0.15f), CircleShape)) {
                Box(Modifier.fillMaxWidth(v / 100f).height(5.dp).background(G.green, CircleShape))
            }
        }
    }
}

/** Collapsed stats chip of SHOP and JOB: the average number, all three in TalkBack. */
@Composable
internal fun StatsCollapsed() {
    val v = LocalMock.current.data.stats
    StatChip(STATS[2], v.sum() / v.size, STATS.indices.joinToString(", ") { "${STATS[it].word} ${v[it]}" })
}

/** The HUD-2 row: 48 dp, full width. */
@Composable
internal fun Hud2(content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth().height(48.dp).testTag("hud2").padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp), content = content)
}
