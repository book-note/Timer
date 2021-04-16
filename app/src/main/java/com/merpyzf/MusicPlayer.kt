package com.merpyzf

import android.animation.ValueAnimator
import android.app.Service
import android.media.MediaPlayer
import android.os.PowerManager
import android.util.Log
import androidx.core.animation.addListener
import kotlin.math.abs

/**
 *
 * Description: music player
 * Date: 4/16/21
 * @author wangke
 *
 */
class MusicPlayer(private val context: Service) : MediaPlayer.OnPreparedListener {
    private val TAG = MusicPlayer::class.java.simpleName
    private var mediaPlayer: MediaPlayer? = null
    private var valueAnimator: ValueAnimator? = null
    private lateinit var musicPath: String
    private val fadeDuration = 800L
    private var isPausing = false
    private var isResuming = false
    private var isFadingOut = false
    private var volume = 1F

    fun playMusic(source: String) {
        this.musicPath = source

        if (isFadingOut) return
        if (valueAnimator?.isRunning == true) valueAnimator?.cancel()
        if (mediaPlayer == null) {
            mediaPlayer = prepareMediaPlayer(musicPath)
            return
        }
        if (mediaPlayer?.isPlaying == true) {
            if (volume == 0F) volume = 1F
            valueAnimator = ValueAnimator.ofFloat(volume, 0F)
                .apply {
                    duration = calculateFadeDuration(volume, fadeDuration)
                    addListener(
                        onStart = {
                            isFadingOut = true
                        },
                        onEnd = {
                            isFadingOut = false
                            mediaPlayer = changeMusic(source)
                        },
                        onCancel = {
                            isFadingOut = false
                        }
                    )
                    addUpdateListener { animator ->
                        volume = animator.animatedValue as Float
                        mediaPlayer!!.setVolume(volume, volume)
                    }
                    start()
                }
            return
        }
        mediaPlayer = changeMusic(source)
    }

    override fun onPrepared(mp: MediaPlayer?) {
        mp?.let {
            it.start()

            if (volume == 1F) volume = 0F
            valueAnimator = ValueAnimator.ofFloat(volume, 1F)
                .apply {
                    duration = calculateFadeDuration(volume - 1F, fadeDuration)
                    addUpdateListener { animator ->
                        volume = animator.animatedValue as Float
                        setVolume(mp, volume)
                    }
                    start()
                }
        }
    }

    fun pauseMusic() {
        if (isPausing) return
        if (valueAnimator?.isRunning == true) valueAnimator?.cancel()

        mediaPlayer?.let { mp ->
            if (!mp.isPlaying) return
            valueAnimator = ValueAnimator.ofFloat(volume, 0F).apply {
                duration = calculateFadeDuration(volume, fadeDuration)
                addListener(
                    onStart = {
                        isPausing = true
                    },
                    onEnd = {
                        if (mp.isPlaying) {
                            mp.pause()
                            isPausing = false
                        }
                    },
                    onCancel = {
                        isPausing = false
                    }
                )
                addUpdateListener { animator ->
                    volume = animator.animatedValue as Float
                    setVolume(mp, volume)
                }
                start()
            }
        }
    }

    fun resumeMusic() {
        if (isResuming) return
        if (isPausing) valueAnimator?.cancel()

        mediaPlayer?.let { mp ->
            if (mp.isPlaying) return
            if (volume == 1F) volume = 0F
            valueAnimator = ValueAnimator.ofFloat(volume, 1F)
                .apply {
                    duration = calculateFadeDuration(volume - 1F, fadeDuration)
                    addListener(
                        onStart = {
                            isResuming = true
                            if (!mp.isPlaying) mp.start()
                        },
                        onEnd = {
                            isResuming = false
                        },
                        onCancel = {
                            isResuming = false
                        }
                    )
                    addUpdateListener { animator ->
                        volume = animator.animatedValue as Float
                        setVolume(mp, volume)
                    }
                    start()
                }
        }
    }

    fun release() {
        mediaPlayer?.let {
            it.stop()
            it.release()
            mediaPlayer = null
        }
    }

    private fun prepareMediaPlayer(musicPath: String): MediaPlayer {
        return MediaPlayer().apply {
            isLooping = true
            setDataSource(musicPath)
            setOnPreparedListener(this@MusicPlayer)
            prepareAsync()
            setWakeMode(
                context,
                PowerManager.PARTIAL_WAKE_LOCK
            )
        }
    }

    private fun changeMusic(newMusicPath: String): MediaPlayer {
        release()
        return prepareMediaPlayer(newMusicPath)
    }

    private fun calculateFadeDuration(volumeValue: Float, fadeDuration: Long): Long {
        return (abs(volumeValue) * fadeDuration).toLong()
    }

    private fun setVolume(mp: MediaPlayer, volume: Float) {
        try {
            mp.setVolume(volume, volume)
        } catch (e: Exception) {
            Log.e(TAG, "setVolume: ${e.message}")
        }
    }
}