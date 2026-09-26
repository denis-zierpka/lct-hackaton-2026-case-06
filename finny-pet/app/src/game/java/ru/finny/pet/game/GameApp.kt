package ru.finny.pet.game

import android.content.Context
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import ru.finny.pet.BuildConfig
import ru.finny.pet.R
import ru.finny.pet.game.audio.Sfx
import ru.finny.pet.game.audio.Sound
import ru.finny.pet.game.mock.TownMockHost
import ru.finny.pet.game.screens.ArrangeScreen
import ru.finny.pet.game.screens.BoardScreen
import ru.finny.pet.game.screens.CreatePetScreen
import ru.finny.pet.game.screens.GlossaryScreen
import ru.finny.pet.game.screens.IntroScreen
import ru.finny.pet.game.screens.JarsScreen
import ru.finny.pet.game.screens.NightScreen
import ru.finny.pet.game.screens.ParentScreen
import ru.finny.pet.game.screens.PlaceScreen
import ru.finny.pet.game.screens.ProgressScreen
import ru.finny.pet.game.screens.RoomScreen
import ru.finny.pet.game.screens.RoundScreen
import ru.finny.pet.game.screens.SavingsScreen
import ru.finny.pet.game.screens.StreetScreen
import ru.finny.pet.game.screens.TitleScreen
import ru.finny.pet.game.screens.WeekEndScreen
import ru.finny.pet.game.ui.G
import ru.finny.pet.game.ui.GameTheme
import ru.finny.pet.game.ui.LineHost
import ru.finny.pet.game.ui.LocalClipped
import ru.finny.pet.game.ui.LocalParticles
import ru.finny.pet.game.ui.ParticleController
import ru.finny.pet.game.ui.ParticleLayer
import ru.finny.pet.game.ui.particleTarget

/** Layout facts every screen needs: orientation and whether we are on a small phone. */
class Layout(val landscape: Boolean, val compact: Boolean)

val LocalLayout = staticCompositionLocalOf { Layout(landscape = true, compact = false) }

/**
 * Latest pet action requested by the view model, with a key so the sprite replays it.
 * [shownBounce]/[shownAction] remember what the room already played: coming back does not repeat it.
 */
class PetActionState {
    var action by mutableStateOf<PetAct?>(null)
    var key by mutableIntStateOf(0)
    var shownBounce = 0
    var shownAction = 0
}
val LocalPetAction = staticCompositionLocalOf { PetActionState() }

/** Whether anything moves: the parent toggle and the system «remove animations» setting together (ТЗ 3.6). */
val LocalAnimate = staticCompositionLocalOf { true }

/** The transition while animations are on, an instant appear/disappear otherwise. */
@Composable fun EnterTransition.orNone(): EnterTransition = if (LocalAnimate.current) this else EnterTransition.None
@Composable fun ExitTransition.orNone(): ExitTransition = if (LocalAnimate.current) this else ExitTransition.None

internal fun systemAnimates(context: Context) = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f

/** Screens with fixed geometry (HUD rows, room, town): their text follows the system font scale up to 1.3; panels follow it fully. */
@Composable
private fun SceneFontScale(content: @Composable () -> Unit) {
    val d = LocalDensity.current
    if (d.fontScale <= 1.3f) content() else CompositionLocalProvider(LocalDensity provides Density(d.density, 1.3f), content = content)
}

private fun Screen.fixed() = this != Screen.Parent && this != Screen.Glossary && this != Screen.Progress

private val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)

@Composable
fun GameApp(vm: GameViewModel = viewModel()) {
    val context = LocalContext.current
    val sfx = remember { Sfx(context) }
    DisposableEffect(sfx) { onDispose { sfx.release() } }
    val particles = remember { ParticleController() }
    val clipped = remember { mutableStateListOf<String>() }
    val petAction = vm.petAction
    var systemAnim by remember { mutableStateOf(systemAnimates(context)) }
    val animate = vm.state.animations && systemAnim
    particles.coinImage = ImageBitmap.imageResource(R.drawable.ui_coin)
    particles.enabled = animate
    sfx.effects = vm.state.sounds
    sfx.music = vm.state.music

    // pause music when the app goes to background; re-read the system animation scale on return
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_PAUSE) sfx.pause() else if (e == Lifecycle.Event.ON_RESUME) { sfx.resume(); systemAnim = systemAnimates(context) } }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    val screen = vm.screen
    // a new screen: targets of the old one are gone, the new one registers its own (§F)
    remember(screen) { particles.clearTargets(); screen }
    LaunchedEffect(screen, vm.state.music) {
        if (vm.state.music && screen != Screen.Title) sfx.startMusic() else sfx.stopMusic()
    }
    LaunchedEffect(Unit) {
        vm.effects.collect { e ->
            when (e) {
                is Effect.Sfx -> sfx.play(e.sound)
                is Effect.CoinsFrom -> {
                    // the screen with the targets may still be composing: wait a moment for both, else no coins
                    withTimeoutOrNull(700) { while (particles.targetOf(e.fromTarget) == null || particles.targetOf(e.toTarget) == null) delay(16) }
                    val from = particles.targetOf(e.fromTarget)
                    if (from != null && particles.targetOf(e.toTarget) != null) particles.coins(from, e.toTarget, e.count)
                }
                Effect.Confetti -> particles.targetOf("center")?.let { particles.confetti(it) }
                Effect.Hearts -> particles.targetOf("pet")?.let { particles.hearts(it) }
                is Effect.PetAction -> {
                    petAction.action = e.action; petAction.key++
                    if (e.action == PetAct.EAT) sfx.play(Sound.MUNCH) else if (e.action == PetAct.WASH) sfx.play(Sound.SPLASH)
                }
            }
        }
    }

    GameTheme {
        BoxWithConstraints(Modifier.fillMaxSize().background(G.purpleDeep).semantics { testTagsAsResourceId = true }) {
            val landscape = maxWidth > maxHeight
            val layout = Layout(landscape, compact = (if (landscape) maxHeight else maxWidth) < 420.dp)
            CompositionLocalProvider(
                LocalLayout provides layout, LocalParticles provides particles, LocalPetAction provides petAction,
                LocalAnimate provides animate, LocalClipped provides clipped,
            ) {
                // at the bottom of the stack (Room, Night, Title) the system back closes the app without a dialog (§9.2 №15)
                BackHandler(enabled = vm.canGoBack) { vm.back() }

                RoomBackground(landscape = landscape, evening = screen == Screen.Night || screen == Screen.WeekEnd)
                Box(Modifier.fillMaxSize().systemBarsPadding()) {
                    AnimatedContent(
                        targetState = screen,
                        transitionSpec = {
                            if (!animate) EnterTransition.None togetherWith ExitTransition.None
                            else (fadeIn(tween(320, easing = Emphasized)) + scaleIn(tween(320, easing = Emphasized), initialScale = 0.96f))
                                .togetherWith(fadeOut(tween(160)) + scaleOut(tween(160), targetScale = 1.02f))
                        },
                        label = "screen",
                    ) { s ->
                        val body: @Composable () -> Unit = {
                            when (s) {
                                Screen.Title -> TitleScreen(vm)
                                Screen.Intro -> IntroScreen(vm)
                                Screen.CreatePet -> CreatePetScreen(vm)
                                Screen.Room -> RoomScreen(vm)
                                Screen.Jars -> JarsScreen(vm)
                                Screen.Savings -> SavingsScreen(vm)
                                Screen.Arrange -> ArrangeScreen(vm)
                                Screen.Night -> NightScreen(vm)
                                Screen.WeekEnd -> WeekEndScreen(vm)
                                Screen.Street -> StreetScreen(vm)
                                is Screen.Place -> PlaceScreen(vm, s.placeId)
                                is Screen.Round -> RoundScreen(vm, s.jobId)
                                Screen.Board -> BoardScreen(vm)
                                Screen.Progress -> ProgressScreen(vm)
                                Screen.Parent -> ParentScreen(vm)
                                Screen.Glossary -> GlossaryScreen(vm)
                            }
                        }
                        if (s.fixed()) SceneFontScale(body) else body()
                    }
                    // invisible centre target for confetti
                    Box(Modifier.align(Alignment.Center).size(1.dp).particleTarget(particles, "center"))
                    Box(Modifier.align(Alignment.BottomCenter)) { SceneFontScale { LineHost(vm) } }
                    if (BuildConfig.DEBUG) {
                        TownMockHost(showEntry = screen == Screen.Parent)
                        // debug probe (WORKFLOW №17): how many texts are clipped right now, and which; no node in release
                        val probe = "Обрезано: ${clipped.size}" + if (clipped.isEmpty()) "" else " — " + clipped.joinToString("; ")
                        Box(Modifier.align(Alignment.TopEnd).size(1.dp).testTag("overflow").semantics { contentDescription = probe })
                    }
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
        AnimatedVisibility(visible = evening, enter = fadeIn(tween(900)).orNone(), exit = fadeOut(tween(900)).orNone()) {
            Image(painterResource(if (landscape) R.drawable.room_land_evening else R.drawable.room_port_evening), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
    }
}
