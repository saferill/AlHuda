package com.example.alhuda.main.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import com.example.alhuda.R

object AdhanAudioPlayer {

    private var mediaPlayer: MediaPlayer? = null

    @Synchronized
    fun play(context: Context, prayerName: String = "Sholat") {
        stop()

        try {
            val isFajr = prayerName.contains("subuh", ignoreCase = true) ||
                         prayerName.contains("fajr", ignoreCase = true)

            val audioResId = if (isFajr) {
                Log.d("AdhanAudioPlayer", "Memutar suara khusus Adzan Subuh (Fajr)")
                R.raw.adhan_fajr
            } else {
                Log.d("AdhanAudioPlayer", "Memutar suara Adzan reguler ($prayerName)")
                R.raw.adhan_sound
            }

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            mediaPlayer = MediaPlayer.create(context.applicationContext, audioResId).apply {
                setAudioAttributes(audioAttributes)
                isLooping = true
                start()
            }
            Log.d("AdhanAudioPlayer", "Adhan audio started looping for $prayerName.")
        } catch (e: Exception) {
            Log.e("AdhanAudioPlayer", "Error playing adhan audio: ${e.message}")
        }
    }

    @Synchronized
    fun stop() {
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            }
            mediaPlayer = null
            Log.d("AdhanAudioPlayer", "Adhan audio stopped.")
        } catch (e: Exception) {
            Log.e("AdhanAudioPlayer", "Error stopping audio: ${e.message}")
            mediaPlayer = null
        }
    }
}
