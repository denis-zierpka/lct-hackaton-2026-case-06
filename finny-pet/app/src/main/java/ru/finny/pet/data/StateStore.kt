package ru.finny.pet.data

import android.content.Context
import android.util.Log
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import ru.finny.pet.domain.GameState
import java.io.File
import java.io.IOException

/**
 * One JSON file in filesDir, written via temp file + rename: a crash while writing the temp file leaves the
 * last good state untouched. ponytail: if rename fails (not seen on Android internal storage) the fallback
 * copy is not atomic; add a load-time fallback to state.json.tmp if that ever shows up.
 */
class StateStore(context: Context) {
    private val file = File(context.filesDir, "state.json")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun load(): GameState {
        if (!file.exists()) return GameState()
        val text = try {
            file.readText()
        } catch (e: IOException) {
            Log.w(TAG, "cannot read state, starting fresh without touching the file", e)
            return GameState()
        }
        return try {
            json.decodeFromString(GameState.serializer(), text)
        } catch (e: SerializationException) {
            Log.w(TAG, "corrupt state, keeping it as state.json.bad", e)
            file.renameTo(File(file.parentFile, "state.json.bad"))
            GameState()
        } catch (e: IllegalArgumentException) { // malformed enum/field values
            Log.w(TAG, "invalid state, keeping it as state.json.bad", e)
            file.renameTo(File(file.parentFile, "state.json.bad"))
            GameState()
        }
    }

    /** @return false when nothing could be written; the previous file stays intact unless the rename fallback copy fails midway. */
    fun save(state: GameState): Boolean = runCatching {
        val tmp = File(file.parentFile, "state.json.tmp")
        tmp.writeText(json.encodeToString(GameState.serializer(), state))
        if (!tmp.renameTo(file)) {
            // rename can fail on exotic filesystems; the tmp file is complete at this point
            tmp.copyTo(file, overwrite = true)
            tmp.delete()
        }
    }.onFailure { Log.w(TAG, "save failed, keeping previous file", it) }.isSuccess

    private companion object { const val TAG = "StateStore" }
}
