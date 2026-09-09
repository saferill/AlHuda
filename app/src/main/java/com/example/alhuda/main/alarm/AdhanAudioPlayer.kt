package com.example.alhuda.main.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import com.example.alhuda.R

object AdhanAudioPlayer {

    private var mediaPlayer: MediaPlayer? = null
    private var previewPlayer: MediaPlayer? = null

    @Synchronized
    fun play(context: Context, prayerName: String = "Sholat", customSoundUri: String? = null) {
        stop()

        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            var playerStarted = false

            if (!customSoundUri.isNullOrBlank()) {
                try {
                    val uri = Uri.parse(customSoundUri)
                    mediaPlayer = MediaPlayer().apply {
                        setAudioAttributes(audioAttributes)
                        setDataSource(context.applicationContext, uri)
                        prepare()
                        isLooping = true
                        start()
                    }
                    playerStarted = true
                    Log.d("AdhanAudioPlayer", "Playing custom adhan audio URI: $customSoundUri")
                } catch (e: Exception) {
                    Log.e("AdhanAudioPlayer", "Failed to play custom adhan URI, falling back to default: ${e.message}")
                }
            }

            if (!playerStarted) {
                val isFajr = prayerName.contains("subuh", ignoreCase = true) ||
                             prayerName.contains("fajr", ignoreCase = true)

                val audioResId = if (isFajr) {
                    Log.d("AdhanAudioPlayer", "Memutar suara khusus Adzan Subuh (Fajr)")
                    R.raw.adhan_fajr
                } else {
                    Log.d("AdhanAudioPlayer", "Memutar suara Adzan reguler ($prayerName)")
                    R.raw.adhan_sound
                }

                mediaPlayer = MediaPlayer.create(context.applicationContext, audioResId).apply {
                    setAudioAttributes(audioAttributes)
                    isLooping = true
                    start()
                }
                Log.d("AdhanAudioPlayer", "Adhan audio started looping for $prayerName.")
            }
        } catch (e: Exception) {
            Log.e("AdhanAudioPlayer", "Error playing adhan audio: ${e.message}")
        }
    }

    @Synchronized
    fun playPreview(context: Context, customSoundUri: String?, onCompletion: () -> Unit) {
        stopPreview()

        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            previewPlayer = if (!customSoundUri.isNullOrBlank()) {
                MediaPlayer().apply {
                    setAudioAttributes(audioAttributes)
                    setDataSource(context.applicationContext, Uri.parse(customSoundUri))
                    prepare()
                }
            } else {
                MediaPlayer.create(context.applicationContext, R.raw.adhan_sound).apply {
                    setAudioAttributes(audioAttributes)
                }
            }

            previewPlayer?.setOnCompletionListener {
                stopPreview()
                onCompletion()
            }
            previewPlayer?.start()
            Log.d("AdhanAudioPlayer", "Preview audio started.")
        } catch (e: Exception) {
            Log.e("AdhanAudioPlayer", "Error playing preview audio: ${e.message}")
            stopPreview()
            onCompletion()
        }
    }

    @Synchronized
    fun stopPreview() {
        try {
            previewPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            }
            previewPlayer = null
        } catch (e: Exception) {
            Log.e("AdhanAudioPlayer", "Error stopping preview audio: ${e.message}")
            previewPlayer = null
        }
    }

    @Synchronized
    fun isPreviewPlaying(): Boolean {
        return previewPlayer?.isPlaying == true
    }

    @Synchronized
    fun stop() {
        stopPreview()
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
