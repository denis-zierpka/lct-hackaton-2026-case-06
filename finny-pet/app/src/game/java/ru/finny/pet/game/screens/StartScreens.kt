package ru.finny.pet.game.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.finny.pet.PetSprites
import ru.finny.pet.R
import ru.finny.pet.domain.Face
import ru.finny.pet.game.GameViewModel
import ru.finny.pet.game.LocalAnimate
import ru.finny.pet.game.LocalLayout
import ru.finny.pet.game.Screen
import ru.finny.pet.game.ui.ButtonStyle
import ru.finny.pet.game.ui.CheckBadge
import ru.finny.pet.game.ui.CloseButton
import ru.finny.pet.game.ui.G
import ru.finny.pet.game.ui.GameButton
import ru.finny.pet.game.ui.GameTextField
import ru.finny.pet.game.ui.Panel
import ru.finny.pet.game.ui.PetSprite
import ru.finny.pet.game.ui.SpeechBubble
import ru.finny.pet.game.ui.TText
import ru.finny.pet.game.ui.TypewriterText

@Composable
fun TitleScreen(vm: GameViewModel) {
    val s = vm.state
    val layout = LocalLayout.current
    Box(Modifier.fillMaxSize().background(G.purpleDeep.copy(alpha = 0.45f))) {
        val body: @Composable () -> Unit = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Питомец", style = MaterialTheme.typography.headlineMedium, color = G.pink)
                Text("Финни", style = MaterialTheme.typography.displayMedium, color = Color.White)
                Text("Копи, планируй, заботься", style = MaterialTheme.typography.bodyLarge, color = G.pink)
                Spacer(Modifier.height(8.dp))
                GameButton(if (s.hasProfile) "Продолжить" else "Играть", Modifier.widthIn(min = 220.dp), style = ButtonStyle.GOLD, minHeight = 60.dp, centered = true) { vm.start() }
                if (s.hasProfile) GameButton("Подсказка", Modifier.widthIn(min = 220.dp), style = ButtonStyle.GHOST, minHeight = 48.dp, centered = true) { vm.navigate(Screen.Intro) }
                GameButton("Для взрослого", Modifier.widthIn(min = 220.dp), style = ButtonStyle.GHOST, minHeight = 48.dp, icon = painterResource(R.drawable.ui_lock), iconSize = 24.dp, centered = true) { vm.navigate(Screen.Parent) }
                Text("Без регистрации. Данные остаются на устройстве.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center)
            }
        }
        val pet = s.pet
        val sprite: @Composable () -> Unit = {
            PetSprite(
                speciesId = pet?.speciesId ?: "cat", colorId = pet?.colorId ?: "orange", stage = pet?.let { vm.economy.stageIndex(it.growth) } ?: 1,
                face = Face.HAPPY, animate = LocalAnimate.current, size = if (layout.compact) 220.dp else 280.dp, description = "Финни", bounceKey = vm.bounce,
            )
        }
        if (layout.landscape) {
            Row(Modifier.fillMaxSize().padding(24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) { sprite(); body() }
        } else {
            Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { sprite(); Spacer(Modifier.height(16.dp)); body() }
        }
    }
}

/** An intro page (§D.11, §11 step 1): the pet's text, then icons, or cards «icon — words». */
private class Page(val text: String, val icons: List<Int> = emptyList(), val cards: List<Pair<Int, String>> = emptyList())

private val introPages = listOf(
    Page("Помоги питомцу вырасти: заботься о нём и копи на мечту", listOf(R.drawable.item_food_basic, R.drawable.item_care_shampoo, R.drawable.ui_piggy)),
    Page("", cards = listOf(R.drawable.ui_lid_mandatory to "Нужное — еда и мыло", R.drawable.ui_lid_optional to "Хочу — то, что радует", R.drawable.ui_lid_savings to "В копилку — на мечту")),
    Page("Каждую неделю почтальон приносит конверт. Заработанное придёт в следующем", listOf(R.drawable.ui_purse, R.drawable.ui_coin, R.drawable.ui_bag)),
)

@Composable
fun IntroScreen(vm: GameViewModel) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    val s = vm.state
    val pages = introPages
    val p = pages[page]
    val layout = LocalLayout.current
    Box(Modifier.fillMaxSize().background(G.purpleDeep.copy(alpha = 0.45f))) {
        val pet = s.pet
        val sprite: @Composable () -> Unit = {
            PetSprite(
                speciesId = pet?.speciesId ?: "cat", colorId = pet?.colorId ?: "orange", stage = pet?.let { vm.economy.stageIndex(it.growth) } ?: 1,
                face = Face.HAPPY, animate = LocalAnimate.current, size = if (layout.compact) 200.dp else 260.dp, description = "Питомец рассказывает",
            )
        }
        val bubble: @Composable () -> Unit = {
            Column(Modifier.widthIn(max = 440.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SpeechBubble(tailAtStart = layout.landscape) {
                    if (p.text.isNotEmpty()) TypewriterText(p.text, animate = LocalAnimate.current)
                    if (p.icons.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally), modifier = Modifier.fillMaxWidth()) {
                        p.icons.forEach { Image(painterResource(it), null, Modifier.size(56.dp)) }
                    }
                    p.cards.forEach { (icon, words) ->
                        Row(Modifier.fillMaxWidth().background(G.paperTint, RoundedCornerShape(16.dp)).padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.size(48.dp)) {
                                Image(painterResource(R.drawable.ui_jar), null, Modifier.align(Alignment.BottomCenter).size(44.dp))
                                Image(painterResource(icon), null, Modifier.align(Alignment.TopCenter).size(26.dp))
                            }
                            TText(words, modifier = Modifier.weight(1f))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.Center) {
                    pages.indices.forEach { i ->
                        Box(Modifier.padding(4.dp).size(if (i == page) 12.dp else 8.dp).background(if (i == page) G.gold else Color.White.copy(alpha = 0.5f), CircleShape))
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (page > 0) GameButton("Назад", Modifier.weight(1f), style = ButtonStyle.GHOST) { page-- }
                    when {
                        page < pages.size - 1 -> GameButton("Дальше", Modifier.weight(1f), style = ButtonStyle.GOLD) { page++ }
                        s.hasProfile -> GameButton("К питомцу", Modifier.weight(1f), style = ButtonStyle.GOLD) { vm.navigate(Screen.Room) }
                        else -> GameButton("Создать питомца", Modifier.weight(1f), style = ButtonStyle.GOLD) { vm.navigate(Screen.CreatePet) }
                    }
                }
            }
        }
        if (layout.landscape) {
            Row(Modifier.fillMaxSize().padding(horizontal = 56.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)) { sprite(); bubble() }
        } else {
            Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { bubble(); Spacer(Modifier.height(20.dp)); sprite() }
        }
        CloseButton(vm::back)
    }
}

@Composable
fun CreatePetScreen(vm: GameViewModel) {
    val c = vm.content
    val layout = LocalLayout.current
    var species by rememberSaveable { mutableStateOf(c.species.first().id) }
    var color by rememberSaveable { mutableStateOf(c.colors.first().id) }
    var name by rememberSaveable { mutableStateOf("") }
    val colorHex = mapOf("orange" to Color(0xFFF4A261), "blue" to Color(0xFF6FB1E0), "green" to Color(0xFF7BC47F))

    Box(Modifier.fillMaxSize().background(G.purpleDeep.copy(alpha = 0.45f))) {
        val preview: @Composable () -> Unit = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PetSprite(speciesId = species, colorId = color, stage = 0, face = Face.HAPPY, animate = LocalAnimate.current, size = if (layout.compact) 200.dp else 260.dp, description = "Питомец: ${c.species(species).title}", bounceKey = species.hashCode() + color.hashCode())
                Text(name.ifBlank { "Как тебя зовут?" }, style = MaterialTheme.typography.headlineSmall, color = if (name.isBlank()) G.pink else Color.White)
            }
        }
        val form: @Composable () -> Unit = {
            Panel(Modifier.widthIn(max = 480.dp), padding = 12.dp) {
                Label("Кто это?")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    c.species.forEach { sp ->
                        val selected = species == sp.id
                        Box(
                            Modifier.weight(1f)
                                .shadow(if (selected) 6.dp else 0.dp, RoundedCornerShape(18.dp))
                                .background(if (selected) G.pink else G.paperTint, RoundedCornerShape(18.dp))
                                .border(3.dp, if (selected) G.magenta else Color.Transparent, RoundedCornerShape(18.dp))
                                .selectable(selected, role = Role.RadioButton) { species = sp.id }
                                .semantics { contentDescription = sp.title }
                                .padding(4.dp),
                        ) {
                            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                Image(painterResource(PetSprites.id(sp.id, color, 0, "happy")), null, Modifier.size(if (layout.compact) 48.dp else 56.dp))
                                Text(sp.title, style = MaterialTheme.typography.labelMedium, color = G.purpleDeep)
                            }
                            if (selected) CheckBadge()
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Label("Цвет:")
                    c.colors.forEach { col ->
                        val selected = color == col.id
                        Box(
                            Modifier.size(48.dp).shadow(if (selected) 6.dp else 2.dp, CircleShape)
                                .background(colorHex[col.id] ?: G.gold, CircleShape)
                                .border(4.dp, if (selected) G.magenta else Color.White, CircleShape)
                                .selectable(selected, role = Role.RadioButton) { color = col.id }
                                .semantics { contentDescription = col.title },
                        ) { if (selected) CheckBadge(Alignment.Center) }
                    }
                }
                FlowRow(itemVerticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Label("Имя:")
                    listOf("Финни", "Пушок", "Бублик").forEach { n -> GameButton(n, selected = name == n, minHeight = 48.dp) { name = n } }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GameTextField(name, { name = it }, Modifier.weight(1f), hint = "Или своё имя", maxLength = c.rules.maxNameLength)
                    GameButton("Начать!", style = ButtonStyle.GOLD, enabled = name.isNotBlank()) { vm.createPet(name, species, color) }
                }
            }
        }
        if (layout.landscape) {
            Row(Modifier.fillMaxSize().padding(start = 56.dp, end = 24.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.weight(0.8f), contentAlignment = Alignment.Center) { preview() }
                Column(Modifier.weight(1.2f).verticalScroll(rememberScrollState())) { form() }
            }
        } else {
            Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) { Spacer(Modifier.height(40.dp)); preview(); form() }
        }
        CloseButton(vm::back)
    }
}
