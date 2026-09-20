package ru.finny.pet.data

import android.content.Context
import kotlinx.serialization.json.Json
import ru.finny.pet.domain.Content

object ContentRepository {
    private val json = Json { ignoreUnknownKeys = true }

    fun load(context: Context): Content =
        parse(context.assets.open("content/content.json").bufferedReader().use { it.readText() })

    fun parse(text: String): Content = json.decodeFromString(Content.serializer(), text)
}
