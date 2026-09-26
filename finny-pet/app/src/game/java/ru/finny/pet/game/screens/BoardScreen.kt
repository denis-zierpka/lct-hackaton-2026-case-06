package ru.finny.pet.game.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ru.finny.pet.game.GameViewModel
import ru.finny.pet.game.ui.Hud1
import ru.finny.pet.game.ui.TText

/** «События» (§E.5) — заглушка кодера 1, экран делает кодер 2. */
@Composable
fun BoardScreen(vm: GameViewModel) {
    Column(Modifier.fillMaxSize().testTag("events")) {
        Hud1(vm, inPlace = true)
        TText("События", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(16.dp), color = Color.White)
    }
}
