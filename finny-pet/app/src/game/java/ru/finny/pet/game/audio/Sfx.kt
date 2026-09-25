package ru.finny.pet.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import ru.finny.pet.R

enum class Sound(val res: Int) {
    COIN(R.raw.sfx_coin), POP(R.raw.sfx_pop), TAP(R.raw.sfx_tap), SUCCESS(R.raw.sfx_success), FAIL(R.raw.sfx_fail),
    WHOOSH(R.raw.sfx_whoosh), MUNCH(R.raw.sfx_munch), SPLASH(R.raw.sfx_splash), FANFARE(R.raw.sfx_fanfare),
    MATCH(R.raw.sfx_match), BOMB(R.raw.sfx_bomb), BUBBLE(R.raw.sfx_bubble), SLEEP(R.raw.sfx_sleep),
}

/**
 * Sound effects on a SoundPool plus a looping music track. [effects] and [music] follow the two parent-section
 * toggles (ТЗ 3.6: sounds can be switched off; nothing important is conveyed by sound alone).
 */
class Sfx(context: Context) {
    private val pool = SoundPool.Builder().setMaxStreams(6)
        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
        .build()
    private val ids: Map<Sound, Int> = Sound.entries.associateWith { pool.load(context, it.res, 1) }
    private val app = context.applicationContext
    private var player: MediaPlayer? = null
    var effects: Boolean = true
    var music: Boolean = false
        set(v) { field = v; if (!v) stopMusic() }

    fun play(s: Sound, volume: Float = 1f, rate: Float = 1f) {
        if (!effects) return
        ids[s]?.let { pool.play(it, volume, volume, 1, 0, rate) }
    }

    fun startMusic() {
        if (!music || player != null) return
        player = MediaPlayer.create(app, R.raw.music_loop)?.apply { isLooping = true; setVolume(0.35f, 0.35f); start() }
    }

    fun stopMusic() { player?.run { stop(); release() }; player = null }

    fun pause() { player?.takeIf { it.isPlaying }?.pause() }
    fun resume() { if (music) player?.takeIf { !it.isPlaying }?.start() }

    /** Frees the player and the pool when the UI leaves composition. */
    fun release() { stopMusic(); pool.release() }
}
