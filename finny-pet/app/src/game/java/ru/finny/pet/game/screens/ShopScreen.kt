package ru.finny.pet.game.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.finny.pet.R
import ru.finny.pet.domain.Category
import ru.finny.pet.domain.ShopItem
import ru.finny.pet.game.GameViewModel
import ru.finny.pet.game.LocalLayout
import ru.finny.pet.game.ui.G
import ru.finny.pet.game.ui.GameButton
import ru.finny.pet.game.ui.HudChip
import ru.finny.pet.game.ui.LocalParticles
import ru.finny.pet.game.ui.particleTarget

fun ShopItem.effectText(): String = listOfNotNull(
    "сытость +$hunger".takeIf { hunger > 0 },
    "чистота +$clean".takeIf { clean > 0 },
    "настроение +$mood".takeIf { mood > 0 },
).joinToString(", ").replaceFirstChar { it.uppercase() }

fun Category.label(): String = if (this == Category.MANDATORY) "Обязательное" else "Желаемое"

/** A shelf of 3D items. Tap a card → a confirmation with price, category and effect (2.5.6). */
@Composable
fun ShopScreen(vm: GameViewModel) {
    val s = vm.state
    val layout = LocalLayout.current
    val particles = LocalParticles.current
    var pendingId by rememberSaveable { mutableStateOf<String?>(null) }
    var category by rememberSaveable { mutableStateOf(Category.MANDATORY) }
    val pending = pendingId?.let { id -> vm.content.items.firstOrNull { it.id == id } }

    Box {
        PanelScreen(vm, "Магазин") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GameButton("Обязательное", Modifier.weight(1f), selected = category == Category.MANDATORY, minHeight = 48.dp, icon = if (layout.landscape) painterResource(R.drawable.ui_lid_mandatory) else null, iconSize = 26.dp) { category = Category.MANDATORY }
                GameButton("Желаемое", Modifier.weight(1f), selected = category == Category.OPTIONAL, minHeight = 48.dp, icon = if (layout.landscape) painterResource(R.drawable.ui_lid_optional) else null, iconSize = 26.dp) { category = Category.OPTIONAL }
                if (layout.landscape) HudChip(painterResource(R.drawable.ui_coin), s.balance, "Монеты", Modifier.particleTarget(particles, "coins"))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (category == Category.MANDATORY) "Нужно каждую неделю: еда и уход. Без них питомец грустит." else "Радует питомца, но можно отложить на потом.",
                    style = MaterialTheme.typography.bodySmall, color = G.inkSoft, modifier = Modifier.weight(1f),
                )
                if (!layout.landscape) HudChip(painterResource(R.drawable.ui_coin), s.balance, "Монеты", Modifier.particleTarget(particles, "coins"))
            }
            if (!s.plan.confirmed) {
                Text("Сначала составь план на неделю — тогда можно покупать.", style = MaterialTheme.typography.bodyLarge, color = G.purpleDeep, modifier = Modifier.fillMaxWidth().background(G.pink, RoundedCornerShape(16.dp)).padding(12.dp))
            }
            // the shelf
            Column(Modifier.fillMaxWidth().background(Color(0xFFF3E7D9), RoundedCornerShape(20.dp)).padding(10.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), maxItemsInEachRow = if (layout.landscape) 5 else 2) {
                    vm.content.items.filter { it.category == category }.forEach { item ->
                        val affordable = item.price <= s.balance
                        Column(
                            Modifier.width(if (layout.landscape) (if (layout.compact) 118.dp else 136.dp) else 150.dp)
                                .shadow(4.dp, RoundedCornerShape(18.dp))
                                .background(Color.White, RoundedCornerShape(18.dp))
                                .clickable(role = Role.Button) { if (affordable && s.plan.confirmed) pendingId = item.id else vm.buy(item.id) }
                                .semantics { contentDescription = "${item.title}, ${item.price} монет, ${item.effectText()}" }
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Image(painterResource(itemRes(item.id)), null, Modifier.size(if (layout.compact) 64.dp else 76.dp).alpha(if (affordable) 1f else 0.45f))
                            Text(item.title, style = MaterialTheme.typography.labelMedium, color = G.ink, textAlign = TextAlign.Center, maxLines = 2, minLines = 2)
                            Text(item.effectText(), style = MaterialTheme.typography.bodySmall, color = G.greenDark, textAlign = TextAlign.Center, minLines = 2)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth().background(if (affordable) G.gold else G.paperTint, RoundedCornerShape(50)).padding(vertical = 4.dp)) {
                                Image(painterResource(R.drawable.ui_coin), null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp))
                                Text("${item.price}", style = MaterialTheme.typography.labelLarge, color = G.purpleDeep)
                            }
                            if (!affordable) Text("не хватает", style = MaterialTheme.typography.bodySmall, color = G.red)
                        }
                    }
                }
                Box(Modifier.fillMaxWidth().height(10.dp).padding(top = 4.dp).background(Color(0xFFD9BF9C), RoundedCornerShape(4.dp)))
            }
            if (s.purchases.isNotEmpty()) {
                Label("Куплено на этой неделе")
                s.purchases.forEach { p ->
                    Row(Modifier.fillMaxWidth().background(G.paperTint, RoundedCornerShape(14.dp)).padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Image(painterResource(itemRes(p.itemId)), null, Modifier.size(32.dp))
                        Text(p.title, style = MaterialTheme.typography.bodyLarge, color = G.ink, modifier = Modifier.weight(1f))
                        Text(p.category.label(), style = MaterialTheme.typography.labelSmall, color = G.inkSoft)
                        Text("−${p.price}", style = MaterialTheme.typography.titleSmall, color = G.purpleDeep)
                    }
                }
                Text("Итого: обязательное ${s.factMandatory}, желаемое ${s.factOptional}", style = MaterialTheme.typography.bodySmall, color = G.inkSoft)
            }
        }
        pending?.let { item ->
            ConfirmPanel(
                title = "Купить «${item.title}»?",
                lines = listOf(
                    "Цена: ${item.price} монет, у тебя ${s.balance}. Останется ${s.balance - item.price}.",
                    "Категория: ${item.category.label()}.",
                    "Питомцу: ${item.effectText()}.",
                ),
                confirmText = "Купить",
                onConfirm = { pendingId = null; vm.buy(item.id) },
                onDismiss = { pendingId = null },
            )
        }
    }
}
