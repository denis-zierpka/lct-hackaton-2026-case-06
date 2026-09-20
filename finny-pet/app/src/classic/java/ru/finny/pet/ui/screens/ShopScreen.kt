package ru.finny.pet.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.finny.pet.domain.Category
import ru.finny.pet.domain.ShopItem
import ru.finny.pet.ui.BigButton
import ru.finny.pet.ui.CoinGoldDark
import ru.finny.pet.ui.FinnyColors
import ru.finny.pet.ui.ConfirmDialog
import ru.finny.pet.ui.EmojiCircle
import ru.finny.pet.ui.Emphasis
import ru.finny.pet.ui.GameViewModel
import ru.finny.pet.ui.ScreenScaffold
import ru.finny.pet.ui.SectionCard
import ru.finny.pet.ui.Space

fun ShopItem.effectText(): String = listOfNotNull(
    "сытость +$hunger".takeIf { hunger > 0 },
    "чистота +$clean".takeIf { clean > 0 },
    "настроение +$mood".takeIf { mood > 0 },
).joinToString(", ").replaceFirstChar { it.uppercase() }

fun Category.label(): String = if (this == Category.MANDATORY) "🍎 Обязательное" else "🎁 Желаемое"

@Composable
fun ShopScreen(vm: GameViewModel) {
    val s = vm.state
    var pendingId by rememberSaveable { mutableStateOf<String?>(null) }
    val pending = pendingId?.let { id -> vm.content.items.firstOrNull { it.id == id } }
    var category by rememberSaveable { mutableStateOf(Category.MANDATORY) }

    ScreenScaffold(
        title = "Магазин",
        onBack = null,
        actions = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = Space.md).clearAndSetSemantics { contentDescription = "Баланс: ${s.balance} монет" },
            ) {
                Icon(Icons.Outlined.Paid, contentDescription = null, tint = CoinGoldDark)
                Text(" ${s.balance}", style = MaterialTheme.typography.titleMedium)
            }
        },
    ) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf(Category.MANDATORY, Category.OPTIONAL).forEachIndexed { i, cat ->
                SegmentedButton(
                    selected = category == cat,
                    onClick = { category = cat },
                    shape = SegmentedButtonDefaults.itemShape(index = i, count = 2),
                    label = { Text(cat.label(), style = MaterialTheme.typography.labelLarge) },
                )
            }
        }
        Text(
            if (category == Category.MANDATORY) "Нужно каждую неделю: еда и уход. Без них питомец грустит." else "Радует питомца, но можно отложить на потом.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!s.plan.confirmed) {
            SectionCard(container = MaterialTheme.colorScheme.primaryContainer) {
                Text("Сначала составь план на неделю — тогда можно покупать.", style = MaterialTheme.typography.bodyLarge)
            }
        }
        vm.content.items.filter { it.category == category }.forEach { item ->
            val affordable = item.price <= s.balance
            SectionCard(container = if (affordable) FinnyColors.card else MaterialTheme.colorScheme.surfaceContainer) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm + Space.xs)) {
                    EmojiCircle(item.emoji)
                    Column(Modifier.weight(1f)) {
                        Text(item.title, style = MaterialTheme.typography.titleMedium)
                        Text(item.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(item.effectText(), style = MaterialTheme.typography.labelMedium, color = FinnyColors.success, fontWeight = FontWeight.SemiBold)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                        Icon(Icons.Outlined.Paid, contentDescription = null, tint = CoinGoldDark)
                        Text("${item.price}", style = MaterialTheme.typography.titleLarge)
                    }
                    // Unaffordable or no plan yet: skip the confirmation and go straight to the explanation (2.5.6).
                    when {
                        !affordable -> BigButton("Не хватает", emphasis = Emphasis.OUTLINED) { vm.buy(item.id) }
                        !s.plan.confirmed -> BigButton("Купить", emphasis = Emphasis.TONAL) { vm.buy(item.id) }
                        else -> BigButton("Купить", icon = Icons.Outlined.ShoppingCart) { pendingId = item.id }
                    }
                }
            }
        }
        if (s.purchases.isNotEmpty()) {
            SectionCard(title = "Куплено на этой неделе", icon = Icons.Outlined.History) {
                s.purchases.forEachIndexed { i, p ->
                    if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ListItem(
                        headlineContent = { Text(p.title, style = MaterialTheme.typography.bodyLarge) },
                        supportingContent = { Text(if (p.category == Category.MANDATORY) "обязательное" else "желаемое", style = MaterialTheme.typography.bodyMedium) },
                        trailingContent = { Text("−${p.price}", style = MaterialTheme.typography.titleMedium) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
                Text("Итого: обязательное ${s.factMandatory}, желаемое ${s.factOptional}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    pending?.let { item ->
        ConfirmDialog(
            title = "Купить «${item.title}»?",
            lines = listOf(
                "Цена: ${item.price} монет, у тебя ${s.balance}. Останется ${s.balance - item.price}.",
                "Категория: ${item.category.label()}",
                "Питомцу: ${item.effectText()}",
            ),
            confirmText = "Купить",
            icon = Icons.Outlined.ShoppingCart,
            onConfirm = { pendingId = null; vm.buy(item.id) },
            onDismiss = { pendingId = null },
        )
    }
}
