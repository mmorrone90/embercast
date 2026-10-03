package dev.firemirror.phone

import android.content.Context
import android.media.AudioManager
import android.util.Log

/** Keeps app playback on the TV while silencing the phone's media speaker. */
internal class PhoneMediaVolume(context: Context) {
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val saved = context.getSharedPreferences("sharing_media_volume", Context.MODE_PRIVATE)

    @Synchronized
    fun mute(): Boolean {
        if (audio.isVolumeFixed) return false
        val current = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        if (current == 0) return true
        // Persist first so a restarted app can restore the phone after a crash.
        if (!saved.edit().putInt(PREVIOUS_VOLUME, current).commit()) return false
        return try {
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
            if (audio.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) true
            else {
                saved.edit().remove(PREVIOUS_VOLUME).apply()
                false
            }
        } catch (e: SecurityException) {
            saved.edit().remove(PREVIOUS_VOLUME).apply()
            Log.w(TAG, "Could not silence phone media", e)
            false
        }
    }

    @Synchronized
    fun restore() {
        if (!saved.contains(PREVIOUS_VOLUME)) return
        val previous = saved.getInt(PREVIOUS_VOLUME, 0)
        try {
            // Keep a nonzero volume choice the user made manually during sharing.
            if (audio.getStreamVolume(AudioManager.STREAM_MUSIC) != 0) {
                saved.edit().remove(PREVIOUS_VOLUME).apply()
                return
            }
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, previous, 0)
            if (audio.getStreamVolume(AudioManager.STREAM_MUSIC) == previous)
                saved.edit().remove(PREVIOUS_VOLUME).apply()
        } catch (e: SecurityException) {
            Log.w(TAG, "Could not restore phone media volume", e)
        }
    }

    private companion object {
        const val TAG = "PhoneMediaVolume"
        const val PREVIOUS_VOLUME = "previous_media_volume"
    }
}
