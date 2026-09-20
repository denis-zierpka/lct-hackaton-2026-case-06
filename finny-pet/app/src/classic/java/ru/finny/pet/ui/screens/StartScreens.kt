package ru.finny.pet.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ru.finny.pet.domain.Face
import ru.finny.pet.ui.BigButton
import ru.finny.pet.ui.BrandHero
import ru.finny.pet.ui.CoinGold
import ru.finny.pet.ui.OnBrand
import ru.finny.pet.ui.Chip
import ru.finny.pet.ui.Emphasis
import ru.finny.pet.ui.GameViewModel
import ru.finny.pet.ui.PetView
import ru.finny.pet.ui.Screen
import ru.finny.pet.ui.ScreenScaffold
import ru.finny.pet.ui.SectionCard
import ru.finny.pet.ui.Space
import ru.finny.pet.ui.TonalCircle

private data class Page(val title: String, val text: String)

private val pages = listOf(
    Page("Привет! Это твой питомец", "Ему нужны еда, уход и радость. Ты решаешь, на что тратить монеты, а питомец показывает, что из этого вышло."),
    Page("Три решения каждую неделю", "Ты получаешь 100 монет и делишь их на три части:\n🍎 Обязательное — еда и уход.\n🎁 Желаемое — игрушки.\n🐷 Копилка — на мечту."),
    Page("Ошибаться можно", "После каждого решения ты увидишь, что изменилось и почему. Не получилось на этой неделе — исправишь на следующей. Питомец всегда рядом."),
)

@Composable
fun OnboardingScreen(vm: GameViewModel) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    val hasProfile = vm.state.hasProfile
    val p = pages[page]
    ScreenScaffold(title = if (hasProfile) "Подсказка" else "Знакомство", onBack = if (hasProfile) vm::back else null) {
        BrandHero {
            Box(Modifier.fillMaxWidth().padding(vertical = Space.sm), contentAlignment = Alignment.Center) {
                when (page) {
                    0 -> PetView(speciesId = "cat", colorId = "orange", stage = 1, face = Face.HAPPY, animate = vm.state.animations, size = 224.dp, description = "Котик Финни")
                    1 -> TonalCircle(size = 120.dp, container = OnBrand.copy(alpha = 0.16f)) { Icon(Icons.Outlined.Paid, contentDescription = null, modifier = Modifier.size(64.dp), tint = CoinGold) }
                    else -> TonalCircle(size = 120.dp, container = OnBrand.copy(alpha = 0.16f)) { Icon(Icons.Outlined.Lightbulb, contentDescription = null, modifier = Modifier.size(64.dp)) }
                }
            }
            Text(p.title, style = MaterialTheme.typography.headlineSmall)
            Text(p.text, style = MaterialTheme.typography.bodyLarge)
        }
        // page indicator dots
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            pages.indices.forEach { i ->
                Box(
                    Modifier.padding(Space.xs).size(if (i == page) 12.dp else 8.dp)
                        .background(if (i == page) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, CircleShape),
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.sm + Space.xs)) {
            if (page > 0) BigButton("Назад", modifier = Modifier.weight(1f), emphasis = Emphasis.OUTLINED) { page-- }
            when {
                page < pages.size - 1 -> BigButton("Дальше", modifier = Modifier.weight(1f), icon = Icons.AutoMirrored.Filled.ArrowForward) { page++ }
                hasProfile -> BigButton("К питомцу", modifier = Modifier.weight(1f), icon = Icons.Outlined.Pets) { vm.navigate(Screen.Home) }
                else -> BigButton("Создать питомца", modifier = Modifier.weight(1f), icon = Icons.Outlined.Pets) { vm.startCreatePet() }
            }
        }
        if (!hasProfile) {
            Spacer(Modifier.height(Space.md))
            if (vm.state.demo) Text("Демо-режим включён", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            BigButton("Для взрослого", emphasis = Emphasis.OUTLINED, icon = Icons.Outlined.Lock) { vm.navigate(Screen.Parent) }
            Text("Без регистрации. Все данные остаются на этом устройстве.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun CreatePetScreen(vm: GameViewModel) {
    val c = vm.content
    var species by rememberSaveable { mutableStateOf(c.species.first().id) }
    var color by rememberSaveable { mutableStateOf(c.colors.first().id) }
    var name by rememberSaveable { mutableStateOf("") }
    ScreenScaffold(title = "Твой питомец", onBack = vm::back) {
        BrandHero {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                PetView(
                    speciesId = species, colorId = color, stage = 0, face = Face.HAPPY,
                    animate = vm.state.animations, size = 224.dp, description = "Питомец: ${c.species(species).title}",
                )
                Text(name.ifBlank { "Как тебя зовут?" }, style = MaterialTheme.typography.headlineSmall, color = if (name.isBlank()) OnBrand.copy(alpha = 0.7f) else OnBrand)
            }
        }
        SectionCard(title = "Кто это?") {
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                c.species.forEach { s -> Chip(s.title, selected = species == s.id) { species = s.id } }
            }
        }
        SectionCard(title = "Какого цвета?") {
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                c.colors.forEach { col -> Chip(col.title, selected = color == col.id) { color = col.id } }
            }
        }
        SectionCard(title = "Как зовут?") {
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                listOf("Финни", "Пушок", "Искра").forEach { n -> Chip(n, selected = name == n) { name = n } }
            }
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= c.rules.maxNameLength) name = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Или придумай своё") },
                textStyle = MaterialTheme.typography.bodyLarge,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                shape = MaterialTheme.shapes.medium,
            )
        }
        BigButton("Начать игру", modifier = Modifier.fillMaxWidth(), enabled = name.isNotBlank(), icon = Icons.AutoMirrored.Filled.ArrowForward) {
            vm.createPet(name, species, color)
        }
    }
}
