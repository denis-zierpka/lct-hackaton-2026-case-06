package ru.finny.pet.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CleanHands
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.finny.pet.domain.Need
import ru.finny.pet.ui.BigButton
import ru.finny.pet.ui.BrandHero
import ru.finny.pet.ui.CoinGold
import ru.finny.pet.ui.CoinGoldDark
import ru.finny.pet.ui.ConfirmDialog
import ru.finny.pet.ui.Emphasis
import ru.finny.pet.ui.FinnyColors
import ru.finny.pet.ui.GameViewModel
import ru.finny.pet.ui.OnBrand
import ru.finny.pet.ui.PetView
import ru.finny.pet.ui.Screen
import ru.finny.pet.ui.ScreenScaffold
import ru.finny.pet.ui.SectionCard
import ru.finny.pet.ui.Space
import ru.finny.pet.ui.StatRow
import ru.finny.pet.ui.TonalCircle

@Composable
fun HomeScreen(vm: GameViewModel) {
    val s = vm.state
    val pet = s.pet ?: return
    val e = vm.economy
    val c = vm.content
    var confirmEnd by rememberSaveable { mutableStateOf(false) }

    ScreenScaffold(
        title = "Неделя ${s.period}",
        onBack = null,
        actions = {
            if (s.demo) AssistChip(onClick = { vm.navigate(Screen.Parent) }, label = { Text("демо") }, modifier = Modifier.padding(end = Space.xs))
            IconButton(onClick = { vm.navigate(Screen.Progress) }) { Icon(Icons.Outlined.Insights, contentDescription = "Прогресс") }
            IconButton(onClick = { vm.navigate(Screen.Onboarding) }) { Icon(Icons.AutoMirrored.Outlined.HelpOutline, contentDescription = "Подсказка") }
            IconButton(onClick = { vm.navigate(Screen.Parent) }) { Icon(Icons.Outlined.Lock, contentDescription = "Для взрослого") }
        },
        fab = {
            if (e.canEndPeriod(s)) {
                ExtendedFloatingActionButton(
                    onClick = { confirmEnd = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Filled.Check, contentDescription = null) },
                    text = { Text("Завершить неделю", style = MaterialTheme.typography.labelLarge) },
                )
            }
        },
    ) {
        // Pet hero on the brand gradient: pet, identity, mood and the three needs — all at once (2.5.3)
        BrandHero {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                PetView(
                    speciesId = pet.speciesId, colorId = pet.colorId,
                    stage = e.stageIndex(pet.growth), face = e.face(pet), animate = s.animations, size = 172.dp,
                    bounceKey = vm.bounce, description = "${pet.name}, ${e.stageTitle(pet.growth)}",
                )
                Column(Modifier.weight(1f).padding(start = Space.sm), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                    Text(pet.name, style = MaterialTheme.typography.headlineMedium)
                    AssistChip(
                        onClick = { vm.navigate(Screen.Progress) },
                        label = { Text(e.stageTitle(pet.growth)) },
                        leadingIcon = { Icon(Icons.Outlined.EmojiEvents, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = OnBrand.copy(alpha = 0.16f), labelColor = OnBrand, leadingIconContentColor = OnBrand),
                        border = null,
                    )
                }
            }
            Text(e.faceReason(pet), style = MaterialTheme.typography.bodyMedium, color = OnBrand.copy(alpha = 0.85f))
            val track = OnBrand.copy(alpha = 0.22f)
            StatRow("Сытость", Icons.Outlined.Restaurant, pet.hunger, Color(0xFFFFB3C7), track = track)
            StatRow("Чистота", Icons.Outlined.CleanHands, pet.clean, Color(0xFFC9C3FF), track = track)
            StatRow("Настроение", Icons.Outlined.Mood, pet.mood, Color(0xFF8CE7B0), track = track)
        }

        // Money
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.sm + Space.xs)) {
            MoneyCard("Монеты", s.balance, Icons.Outlined.Paid, CoinGoldDark, CoinGold.copy(alpha = 0.35f)) { vm.navigate(Screen.Plan) }
            MoneyCard("Копилка", s.savings, Icons.Outlined.Savings, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.secondaryContainer) { vm.navigate(Screen.Savings) }
        }

        // Goal
        val goal = s.goal
        SectionCard(title = "Цель", icon = Icons.Outlined.Flag, onClick = { vm.navigate(Screen.Savings) }) {
            if (goal != null) {
                Text("${goal.emoji} ${goal.title}: ${s.savings} из ${goal.price} 🪙", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                LinearProgressIndicator(
                    progress = { (s.savings.toFloat() / goal.price).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(10.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
                Text(e.goalProgressMessage(s), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text("Цель пока не выбрана. Выбери, на что копить — так интереснее!", style = MaterialTheme.typography.bodyLarge)
            }
        }

        // Next step
        val step = activeStep(vm)
        SectionCard(container = MaterialTheme.colorScheme.secondaryContainer, title = "Сейчас", icon = Icons.Outlined.AutoAwesome) {
            Text(step.first, style = MaterialTheme.typography.bodyLarge)
            BigButton(step.second, modifier = Modifier.fillMaxWidth(), emphasis = Emphasis.HIGH) { vm.navigate(step.third) }
            if (!e.canEndPeriod(s)) Text("Кнопка «Завершить неделю» появится после плана.", style = MaterialTheme.typography.bodyMedium)
        }
    }

    if (confirmEnd) {
        val food = s.purchases.any { it.need == Need.FOOD }
        val care = s.purchases.any { it.need == Need.CARE }
        ConfirmDialog(
            title = "Завершить неделю ${s.period}?",
            lines = listOfNotNull(
                "Питомец получит итог недели, а ты — новые ${c.rules.allowance} монет.",
                if (!food) "Ты ещё не купил еду — питомец проголодается." else null,
                if (!care) "Ты ещё не купил уход — питомец запачкается." else null,
                if (s.factSavings <= 0) "Копилка на этой неделе не выросла." else null,
            ),
            confirmText = "Завершить",
            icon = Icons.Filled.Check,
            onConfirm = { confirmEnd = false; vm.endPeriod() },
            onDismiss = { confirmEnd = false },
        )
    }
}

@Composable
private fun RowScope.MoneyCard(label: String, amount: Int, icon: ImageVector, tint: Color, iconContainer: Color, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.weight(1f),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = FinnyColors.card),
    ) {
        Column(Modifier.padding(Space.md), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            TonalCircle(size = 40.dp, container = iconContainer) { Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp)) }
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$amount", style = MaterialTheme.typography.headlineMedium)
        }
    }
}

/** What to do next: (explanation, button text, screen). Guides the child through the cycle. */
private fun activeStep(vm: GameViewModel): Triple<String, String, Screen> {
    val s = vm.state
    val e = vm.economy
    return when {
        !s.plan.confirmed -> Triple("Раздели ${s.balance} монет на обязательное, желаемое и копилку.", "Составить план", Screen.Plan)
        s.purchases.none { it.need == Need.FOOD } -> Triple("Купи еду — без неё питомец проголодается.", "В магазин", Screen.Shop)
        s.purchases.none { it.need == Need.CARE } -> Triple("Купи уход — шампунь или расчёску.", "В магазин", Screen.Shop)
        s.goal == null -> Triple("Выбери цель, на которую будешь копить.", "Выбрать цель", Screen.Savings)
        s.factSavings <= 0 && s.balance > 0 -> Triple("Отложи немного в копилку — цель станет ближе.", "В копилку", Screen.Savings)
        e.availableTasks(s).isNotEmpty() -> Triple("Выполни задание и получи монеты.", "К заданиям", Screen.Tasks)
        else -> Triple("Всё сделано! Заверши неделю и посмотри итог.", "Посмотреть прогресс", Screen.Progress)
    }
}
