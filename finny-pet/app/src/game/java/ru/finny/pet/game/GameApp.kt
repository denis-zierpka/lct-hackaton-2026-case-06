package ru.finny.pet.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.finny.pet.R
import ru.finny.pet.game.audio.Sfx
import ru.finny.pet.game.audio.Sound
import ru.finny.pet.game.screens.CreatePetScreen
import ru.finny.pet.game.screens.GlossaryScreen
import ru.finny.pet.game.screens.IntroScreen
import ru.finny.pet.game.screens.MiniGameScreen
import ru.finny.pet.game.screens.ParentScreen
import ru.finny.pet.game.screens.PlanScreen
import ru.finny.pet.game.screens.ProgressScreen
import ru.finny.pet.game.screens.RoomScreen
import ru.finny.pet.game.screens.SavingsScreen
import ru.finny.pet.game.screens.ShopScreen
import ru.finny.pet.game.screens.TaskScreen
import ru.finny.pet.game.screens.TasksScreen
import ru.finny.pet.game.screens.TitleScreen
import ru.finny.pet.game.screens.WeekEndScreen
import ru.finny.pet.game.ui.ButtonStyle
import ru.finny.pet.game.ui.G
import ru.finny.pet.game.ui.GameButton
import ru.finny.pet.game.ui.GameTheme
import ru.finny.pet.game.ui.LocalParticles
import ru.finny.pet.game.ui.Panel
import ru.finny.pet.game.ui.ParticleController
import ru.finny.pet.game.ui.ParticleLayer

/** Layout facts every screen needs: orientation and whether we are on a small phone. */
class Layout(val landscape: Boolean, val compact: Boolean)

val LocalLayout = staticCompositionLocalOf { Layout(landscape = true, compact = false) }
val LocalSfx = staticCompositionLocalOf<Sfx?> { null }
val LocalVm = staticCompositionLocalOf<GameViewModel> { error("no view model") }

/** Latest pet action requested by the view model, with a key so the sprite replays it. */
class PetActionState { var action by mutableStateOf<PetAct?>(null); var key by mutableIntStateOf(0) }
val LocalPetAction = staticCompositionLocalOf { PetActionState() }

private val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)

@Composable
fun GameApp(vm: GameViewModel = viewModel()) {
    val context = LocalContext.current
    val sfx = remember { Sfx(context) }
    val particles = remember { ParticleController() }
    val petAction = remember { PetActionState() }
    particles.coinImage = ImageBitmap.imageResource(R.drawable.ui_coin)
    sfx.enabled = vm.state.sounds

    // pause music when the app goes to background
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_PAUSE) sfx.pause() else if (e == Lifecycle.Event.ON_RESUME) sfx.resume() }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs); sfx.stopMusic() }
    }
    LaunchedEffect(vm.screen, vm.state.sounds) {
        if (vm.state.sounds && vm.screen != Screen.Title) sfx.startMusic() else sfx.stopMusic()
    }
    LaunchedEffect(Unit) {
        vm.effects.collect { e ->
            when (e) {
                is Effect.Sfx -> sfx.play(e.sound)
                is Effect.Coins -> particles.targetOf("pet")?.let { particles.coins(it, e.toTarget, e.count) }
                is Effect.CoinsFrom -> particles.targetOf(e.fromTarget)?.let { particles.coins(it, e.toTarget, e.count) }
                Effect.Confetti -> particles.targetOf("center")?.let { particles.confetti(it) }
                Effect.Hearts -> particles.targetOf("pet")?.let { particles.hearts(it) }
                Effect.Sparkles -> particles.targetOf("pet")?.let { particles.sparkles(it) }
                Effect.Bubbles -> particles.targetOf("pet")?.let { particles.bubbles(it) }
                is Effect.PetAction -> { petAction.action = e.action; petAction.key++ }
            }
        }
    }

    GameTheme {
        BoxWithConstraints(Modifier.fillMaxSize().background(G.purpleDeep)) {
            val landscape = maxWidth > maxHeight
            val layout = Layout(landscape, compact = (if (landscape) maxHeight else maxWidth) < 420.dp)
            CompositionLocalProvider(LocalLayout provides layout, LocalSfx provides sfx, LocalParticles provides particles, LocalPetAction provides petAction, LocalVm provides vm) {
                val screen = vm.screen
                val atRoot = screen == Screen.Title || (screen == Screen.Room)
                BackHandler(enabled = !atRoot) { vm.back() }

                RoomBackground(landscape = landscape, evening = screen == Screen.WeekEnd || (screen == Screen.Room && vm.economy.canEndPeriod(vm.state) && vm.nextStep().third == Screen.WeekEnd))
                Box(Modifier.fillMaxSize().systemBarsPadding()) {
                    AnimatedContent(
                        targetState = screen,
                        transitionSpec = {
                            (fadeIn(tween(320, easing = Emphasized)) + scaleIn(tween(320, easing = Emphasized), initialScale = 0.96f))
                                .togetherWith(fadeOut(tween(160)) + scaleOut(tween(160), targetScale = 1.02f))
                        },
                        label = "screen",
                    ) { s ->
                        when (s) {
                            Screen.Title -> TitleScreen(vm)
                            Screen.Intro -> IntroScreen(vm)
                            Screen.CreatePet -> CreatePetScreen(vm)
                            Screen.Room -> RoomScreen(vm)
                            Screen.Plan -> PlanScreen(vm)
                            Screen.Shop -> ShopScreen(vm)
                            Screen.Savings -> SavingsScreen(vm)
                            Screen.Tasks -> TasksScreen(vm)
                            is Screen.Task -> TaskScreen(vm, s.id)
                            Screen.MiniGame -> MiniGameScreen(vm)
                            Screen.WeekEnd -> WeekEndScreen(vm)
                            Screen.Progress -> ProgressScreen(vm)
                            Screen.Parent -> ParentScreen(vm)
                            Screen.Glossary -> GlossaryScreen(vm)
                        }
                    }
                    // invisible centre target for confetti
                    Box(Modifier.align(Alignment.Center).size(1.dp).then(ru.finny.pet.game.ui.particleTargetModifier(particles, "center")))
                    FeedbackOverlay(vm)
                }
                ParticleLayer(particles)
            }
        }
    }
}

@Composable
private fun RoomBackground(landscape: Boolean, evening: Boolean) {
    Box(Modifier.fillMaxSize()) {
        Image(painterResource(if (landscape) R.drawable.room_land_day else R.drawable.room_port_day), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        AnimatedVisibility(visible = evening, enter = fadeIn(tween(900)), exit = fadeOut(tween(900))) {
            Image(painterResource(if (landscape) R.drawable.room_land_evening else R.drawable.room_port_evening), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
    }
}

/** Game-style modal with the pet's portrait: replaces AlertDialog for every outcome and error. */
@Composable
private fun FeedbackOverlay(vm: GameViewModel) {
    val fb = vm.feedback
    AnimatedVisibility(visible = fb != null, enter = fadeIn(tween(200)), exit = fadeOut(tween(200))) {
        Box(Modifier.fillMaxSize().background(G.scrim)) {}
    }
    AnimatedVisibility(
        visible = fb != null,
        enter = fadeIn(tween(220)) + scaleIn(tween(320, easing = Emphasized), initialScale = 0.8f),
        exit = fadeOut(tween(160)) + scaleOut(tween(160), targetScale = 0.9f),
    ) {
        val f = fb ?: return@AnimatedVisibility
        Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Panel(Modifier.widthIn(max = 560.dp).heightIn(max = 400.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val pet = vm.state.pet
                    if (pet != null) {
                        Image(
                            painterResource(ru.finny.pet.PetSprites.id(pet.speciesId, pet.colorId, vm.economy.stageIndex(pet.growth), if (f.mood == Mood.OOPS) "sad" else "happy")),
                            contentDescription = null, modifier = Modifier.size(64.dp),
                        )
                    }
                    Text(f.title, style = MaterialTheme.typography.headlineSmall, color = G.purpleDeep, modifier = Modifier.weight(1f))
                }
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    f.messages.forEach { m ->
                        Row {
                            Text("•", style = MaterialTheme.typography.bodyLarge, color = G.magenta)
                            Spacer(Modifier.size(8.dp))
                            Text(m, style = MaterialTheme.typography.bodyLarge, color = G.ink)
                        }
                    }
                }
                GameButton("Понятно", Modifier.fillMaxWidth(), style = if (f.mood == Mood.OOPS) ButtonStyle.PAPER else ButtonStyle.PRIMARY, onClick = vm::dismissFeedback)
            }
        }
    }
}

/** Shared header line used by panel screens: a title on the room with a close button on the left. */
@Composable
fun PanelTitle(text: String) {
    Text(
        text, style = MaterialTheme.typography.headlineSmall, color = androidx.compose.ui.graphics.Color.White, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 64.dp, vertical = 8.dp),
    )
}
