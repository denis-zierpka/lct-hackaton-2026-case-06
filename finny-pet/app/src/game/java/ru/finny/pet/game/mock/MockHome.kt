package ru.finny.pet.game.mock

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.finny.pet.R
import ru.finny.pet.game.ui.G
import ru.finny.pet.game.ui.GameButton

/** HOME (§10.1): HUD-1 56, HUD-2 48, «В городке» 56, the room takes the rest, bottom row 72. */
@Composable
internal fun MockHome() {
    val m = LocalMock.current
    val big = bigFont()
    Column(Modifier.fillMaxSize()) {
        Hud1(inPlace = false)
        Hud2 { STATS.forEachIndexed { i, s -> StatChip(s, m.data.stats[i]) } }
        Row(
            Modifier.fillMaxWidth().height(56.dp).testTag("town_card").padding(horizontal = 8.dp, vertical = 2.dp).chip()
                .clickable(role = Role.Button) { m.go(MockScreen.JOB) }.semantics { contentDescription = "В городке: Распродажа робота, Лавка" }.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.size(28.dp).background(G.magenta, CircleShape), contentAlignment = Alignment.Center) { MockText("!", MaterialTheme.typography.titleMedium, color = Color.White) }
            MockText("Распродажа робота · Лавка", MaterialTheme.typography.bodyMedium.copy(lineHeight = 18.sp), Modifier.weight(1f), maxLines = 2)
            MockText("→", MaterialTheme.typography.titleMedium, color = G.purpleDeep)
        }
        Room(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth().height(72.dp).testTag("bottom_row")) {
            listOf(
                Triple(R.drawable.ui_jar, "Банки", null), Triple(R.drawable.ui_bag, "Лавки", MockScreen.SHOP), Triple(R.drawable.ui_piggy, "Копилка", null),
                Triple(R.drawable.ui_book, "События", MockScreen.JOB), Triple(R.drawable.ui_trophy, "Дневник", null),
            ).forEach { (icon, label, to) ->
                Column(
                    Modifier.weight(1f).fillMaxHeight().clickable(role = Role.Button) { to?.let(m::go) }.semantics { if (big) contentDescription = label },
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                ) {
                    Image(painterResource(icon), null, Modifier.size(if (big) 44.dp else 36.dp))
                    // 14 sp: UX_ACCESSIBILITY.md «Исключения: 14 sp», строка «Городок», нижний ряд (16 sp не влезает в 72 dp)
                    if (!big) MockText(label, MaterialTheme.typography.labelSmall, color = Color.White)
                }
            }
        }
    }
}

/** The room as a scheme: wall and floor in two theme colours, flat objects over them (room_port_day has other things baked in). */
@Composable
private fun Room(modifier: Modifier) {
    val m = LocalMock.current
    val d = m.data
    val label = MaterialTheme.typography.labelSmall
    Box(modifier.fillMaxWidth().testTag("room")) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().weight(0.6f).background(G.lavenderLight))
            Box(Modifier.fillMaxWidth().weight(0.4f).background(G.lavender))
        }
        Column(Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
            Row(Modifier.fillMaxWidth().height(88.dp).padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Target("Окно: улица. Машет житель, есть событие", 112.dp, 80.dp, color = G.sky) {
                    Image(painterResource(R.drawable.pet_bunny_blue_0_happy), null, Modifier.align(Alignment.CenterStart).padding(start = 4.dp).size(40.dp))
                    MockText("!", MaterialTheme.typography.titleMedium, Modifier.align(Alignment.TopEnd).padding(end = 6.dp), color = G.magenta)
                }
                Target("Банки: Нужное ${d.banks[0]}, Хочу ${d.banks[1]}, Запас ${d.banks[2]}", 152.dp, 72.dp) {
                    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                        listOf(R.drawable.ui_lid_mandatory, R.drawable.ui_lid_optional, R.drawable.ui_lid_savings).forEachIndexed { i, lid ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(Modifier.size(36.dp)) {
                                    Image(painterResource(R.drawable.ui_jar), null, Modifier.align(Alignment.BottomCenter).size(32.dp))
                                    Image(painterResource(lid), null, Modifier.align(Alignment.TopCenter).size(22.dp))
                                }
                                MockText("${d.banks[i]}", MaterialTheme.typography.titleSmall)
                            }
                        }
                    }
                }
                Target("Копилка ${d.piggy}", 64.dp, 64.dp) { Image(painterResource(R.drawable.ui_piggy), null, Modifier.size(52.dp)) }
            }
            // below the top strip: side columns spread over the whole height, pet right above the bed at the bottom
            Box(Modifier.fillMaxWidth().weight(1f).padding(top = 4.dp, bottom = 4.dp)) {
                val decor = MaterialTheme.typography.headlineSmall
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                        Target("Холодильник: список нужного и цены", 64.dp, 96.dp, color = Color.White) {
                            Box(Modifier.align(Alignment.TopCenter).padding(top = 32.dp).fillMaxWidth().height(2.dp).background(G.lavender))
                            MockText("Список", label, Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp).wrapContentWidth(unbounded = true))
                        }
                        // start decor: visible, no touch, not a target
                        MockText("🪴", decor, Modifier.align(Alignment.CenterHorizontally).clearAndSetSemantics {})
                        Target("Сундук: обустроить комнату", 64.dp, 48.dp) { MockText("🧳", MaterialTheme.typography.titleLarge) }
                        Target("Словарик", 48.dp, 48.dp) { Image(painterResource(R.drawable.ui_book), null, Modifier.size(36.dp)) }
                    }
                    Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                        Target("Лампа сломана: к Степану", 48.dp, 48.dp, color = G.gold) { MockText("💡", MaterialTheme.typography.titleLarge) }
                        Spacer(Modifier.weight(1f))
                        Target("Финни. Настроение хорошее. Нажми — что на уме", 112.dp, 112.dp, color = Color.Transparent, onClick = { m.talk = !m.talk }) {
                            Image(painterResource(R.drawable.pet_cat_orange_0_happy), null, Modifier.fillMaxSize())
                        }
                        Target("Кровать: сон", 128.dp, 64.dp) {
                            Row(verticalAlignment = Alignment.CenterVertically) { MockText("🛏", MaterialTheme.typography.titleLarge); Spacer(Modifier.width(6.dp)); MockText("Сон", label) }
                        }
                    }
                    Column(Modifier.fillMaxHeight(), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.SpaceBetween) {
                        Target("Дверь: на улицу", 64.dp, 136.dp, color = G.goldDark, onClick = { m.go(MockScreen.SHOP) }) {
                            Box(Modifier.align(Alignment.CenterEnd).padding(end = 8.dp).size(8.dp).background(G.purpleDeep, CircleShape))
                            MockText("Улица", label, Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp))
                        }
                        MockText("🛋", decor, Modifier.align(Alignment.CenterHorizontally).clearAndSetSemantics {})
                        Target("Почтовый ящик: плюс ${d.mail} придёт с новым конвертом", 48.dp, 48.dp) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Canvas(Modifier.size(22.dp, 14.dp)) { // envelope: an emoji line is taller than 24 dp at 1.3
                                    val s = Stroke(2.dp.toPx())
                                    drawRect(G.purpleDeep, style = s)
                                    drawPath(Path().apply { moveTo(0f, 0f); lineTo(size.width / 2, size.height * 0.6f); lineTo(size.width, 0f) }, G.purpleDeep, style = s)
                                }
                                MockText("+${d.mail}", label)
                            }
                        }
                    }
                }
                // the bubble stands over the pet's head (pet 112 + bed 64 minus the sprite's empty top), tail to the pet
                if (m.talk) Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 168.dp).fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(20.dp)).background(Color.White, RoundedCornerShape(20.dp)).padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        MockText("Животик урчит. Корм у реки — 20", MaterialTheme.typography.bodyMedium, Modifier.weight(1f).padding(start = 4.dp), maxLines = 3)
                        GameButton("К рынку", minHeight = 48.dp) { m.go(MockScreen.SHOP) }
                    }
                    Row(Modifier.fillMaxWidth()) {
                        Spacer(Modifier.width(72.dp))
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Canvas(Modifier.size(22.dp, 12.dp)) { drawPath(Path().apply { moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width / 2, size.height); close() }, Color.White) }
                        }
                        Spacer(Modifier.width(72.dp))
                    }
                }
            }
        }
    }
}
