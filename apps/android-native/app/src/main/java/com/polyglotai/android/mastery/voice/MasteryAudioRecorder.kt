package com.polyglotai.android.mastery.voice

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/**
 * Local microphone capture for the mastery loop (spec §3.5, Milestone 2 half). Records to the cache
 * and hands back the file; the grader seam decides what to do with it. Deliberately minimal — VAD,
 * silence trimming, and noise detection are later refinements. Runtime RECORD_AUDIO permission is
 * requested by the screen before this is used.
 */
@Suppress("DEPRECATION") // no-arg MediaRecorder() ctor deprecated on API 31+, needed for minSdk 24
class MasteryAudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    val isRecording: Boolean get() = recorder != null

    fun start(): Boolean {
        stopQuietly()
        val file = File(context.cacheDir, "mastery_take_${System.currentTimeMillis()}.m4a")
        return try {
            val rec = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else MediaRecorder()
            rec.setAudioSource(MediaRecorder.AudioSource.MIC)
            rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            rec.setOutputFile(file.absolutePath)
            rec.prepare()
            rec.start()
            recorder = rec
            outputFile = file
            true
        } catch (e: Exception) {
            stopQuietly()
            false
        }
    }

    /** Stop and return the captured file, or null if capture failed. */
    fun stop(): File? {
        val rec = recorder ?: return null
        runCatching { rec.stop() }
        rec.release()
        recorder = null
        val file = outputFile
        outputFile = null
        return file?.takeIf { it.exists() && it.length() > 0 }
    }

    fun stopQuietly() {
        recorder?.let { runCatching { it.stop() }; runCatching { it.release() } }
        recorder = null
        outputFile = null
    }
}
