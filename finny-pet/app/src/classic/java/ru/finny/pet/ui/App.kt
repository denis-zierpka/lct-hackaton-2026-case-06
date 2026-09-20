package ru.finny.pet.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.finny.pet.ui.screens.CreatePetScreen
import ru.finny.pet.ui.screens.GlossaryScreen
import ru.finny.pet.ui.screens.HomeScreen
import ru.finny.pet.ui.screens.OnboardingScreen
import ru.finny.pet.ui.screens.ParentScreen
import ru.finny.pet.ui.screens.PlanScreen
import ru.finny.pet.ui.screens.ProgressScreen
import ru.finny.pet.ui.screens.SavingsScreen
import ru.finny.pet.ui.screens.ShopScreen
import ru.finny.pet.ui.screens.SummaryScreen
import ru.finny.pet.ui.screens.TaskScreen
import ru.finny.pet.ui.screens.TasksScreen

private class Destination(val screen: Screen, val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

private val destinations = listOf(
    Destination(Screen.Home, "Питомец", Icons.Outlined.Pets, Icons.Filled.Pets),
    Destination(Screen.Plan, "План", Icons.AutoMirrored.Outlined.EventNote, Icons.AutoMirrored.Filled.EventNote),
    Destination(Screen.Shop, "Магазин", Icons.Outlined.ShoppingCart, Icons.Filled.ShoppingCart),
    Destination(Screen.Savings, "Копилка", Icons.Outlined.Savings, Icons.Filled.Savings),
    Destination(Screen.Tasks, "Задания", Icons.Outlined.Star, Icons.Filled.Star),
)

/** M3 emphasized easing for screen transitions. */
private val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
private val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

@Composable
fun App(vm: GameViewModel = viewModel()) {
    FinnyTheme {
        val screen = vm.screen
        val atRoot = screen == Screen.Home || (screen == Screen.Onboarding && !vm.state.hasProfile)
        BackHandler(enabled = !atRoot) { vm.back() }

        val topLevel = vm.state.hasProfile && destinations.any { it.screen == screen }
        if (topLevel) {
            // Bottom bar on phones, rail from 600dp wide (tablets and phone landscape) — M3 navigation guidance.
            val widthDp = LocalConfiguration.current.screenWidthDp
            NavigationSuiteScaffold(
                layoutType = if (widthDp >= 600) NavigationSuiteType.NavigationRail else NavigationSuiteType.NavigationBar,
                navigationSuiteItems = {
                    val openTasks = vm.economy.availableTasks(vm.state).size
                    destinations.forEach { d ->
                        val selected = screen == d.screen
                        item(
                            selected = selected,
                            onClick = { vm.navigate(d.screen) },
                            icon = { Icon(if (selected) d.selectedIcon else d.icon, contentDescription = null) },
                            label = { Text(d.label) },
                            badge = if (d.screen == Screen.Tasks && openTasks > 0) ({
                                Badge(modifier = Modifier.semantics { contentDescription = "$openTasks новых заданий" }) { Text("$openTasks") }
                            }) else null,
                        )
                    }
                },
            ) { ScreenHost(vm) }
        } else {
            ScreenHost(vm)
        }

        vm.feedback?.let { FeedbackDialog(it.title, it.messages, it.icon, vm::dismissFeedback) }
        vm.error?.let { e ->
            FeedbackDialog("Пока не получится", listOf(e.message) + e.hints.map { "Вариант: $it" }, Icons.Outlined.Info, vm::dismissError)
        }
    }
}

@Composable
private fun ScreenHost(vm: GameViewModel) {
    AnimatedContent(
        targetState = vm.screen,
        transitionSpec = {
            (fadeIn(tween(350, easing = EmphasizedDecelerate)) + slideInVertically(tween(350, easing = EmphasizedDecelerate)) { it / 24 })
                .togetherWith(fadeOut(tween(150, easing = EmphasizedAccelerate)))
        },
        label = "screen",
    ) { s ->
        when (s) {
            Screen.Onboarding -> OnboardingScreen(vm)
            Screen.CreatePet -> CreatePetScreen(vm)
            Screen.Home -> HomeScreen(vm)
            Screen.Plan -> PlanScreen(vm)
            Screen.Shop -> ShopScreen(vm)
            Screen.Savings -> SavingsScreen(vm)
            Screen.Tasks -> TasksScreen(vm)
            is Screen.Task -> TaskScreen(vm, s.id)
            Screen.Progress -> ProgressScreen(vm)
            Screen.Glossary -> GlossaryScreen(vm)
            Screen.Parent -> ParentScreen(vm)
            Screen.Summary -> SummaryScreen(vm)
        }
    }
}
