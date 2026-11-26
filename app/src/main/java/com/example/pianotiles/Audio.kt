package com.example.pianotiles

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

const val SAMPLE_RATE = 44100

/**
 * Renders piano-like notes with simple additive synthesis (no audio files needed)
 * and caches them so a tap only has to hand a ready buffer to the mixer.
 */
object NoteBank {
    private val cache = HashMap<Int, ShortArray>()
    private var errClip: ShortArray? = null

    @Synchronized
    fun get(midi: Int, beats: Float): ShortArray {
        if (cache.size > 80) cache.clear()
        val key = midi * 1000 + (beats * 100).toInt()
        return cache.getOrPut(key) { render(midi, beats) }
    }

    @Synchronized
    fun errorClip(): ShortArray = errClip ?: renderError().also { errClip = it }

    private fun render(midi: Int, beats: Float): ShortArray {
        val f = 440.0 * 2.0.pow((midi - 69) / 12.0)
        val d = beats.coerceIn(0.25f, 4f).toDouble()
        // Higher notes die away faster; longer written notes ring longer.
        val k = (1.6 + f / 700.0) / (0.7 + 0.3 * d)
        val seconds = minOf(3.2, 5.0 / k)
        val n = (seconds * SAMPLE_RATE).toInt()

        val amps = doubleArrayOf(1.0, 0.55, 0.32, 0.2, 0.12, 0.08)
        var count = 0
        for (h in 1..6) if (f * h < 12000.0) count++

        val w = DoubleArray(count) {
            2.0 * PI * f * (it + 1) * sqrt(1.0 + 0.0003 * (it + 1) * (it + 1)) / SAMPLE_RATE
        }
        val env = DoubleArray(count) { 1.0 }
        val step = DoubleArray(count) { exp(-k * (1.0 + 0.45 * it) / SAMPLE_RATE) }
        var ampSum = 0.0
        for (h in 0 until count) ampSum += amps[h]
        val scale = 14000.0 / ampSum

        val attack = (0.004 * SAMPLE_RATE).toInt()
        val tail = (0.03 * SAMPLE_RATE).toInt()
        val out = ShortArray(n)
        for (i in 0 until n) {
            var s = 0.0
            for (h in 0 until count) {
                s += amps[h] * env[h] * sin(w[h] * i)
                env[h] *= step[h]
            }
            var g = scale
            if (i < attack) g *= i.toDouble() / attack
            if (i > n - tail) g *= (n - i).toDouble() / tail
            out[i] = (s * g).toInt().toShort()
        }
        return out
    }

    private fun renderError(): ShortArray {
        val n = (0.4 * SAMPLE_RATE).toInt()
        val out = ShortArray(n)
        for (i in 0 until n) {
            val t = i.toDouble() / SAMPLE_RATE
            val s = (sin(2 * PI * 98.0 * t) + 0.9 * sin(2 * PI * 103.8 * t) +
                0.5 * sin(2 * PI * 196.0 * t)) * exp(-t * 8.0)
            out[i] = (s * 6000).toInt().toShort()
        }
        return out
    }
}

/** Tiny low-latency mixer: a background thread streams all active notes to one AudioTrack. */
class AudioOut {
    private class Voice(val data: ShortArray) {
        var pos = 0
    }

    private val pending = ConcurrentLinkedQueue<Voice>()

    @Volatile
    private var running = true
    private var track: AudioTrack? = null
    private var thread: Thread? = null

    init {
        try {
            val min = AudioTrack.getMinBufferSize(
                SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
            )
            val t = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(min, CHUNK * 4))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                .build()
            track = t
            thread = Thread { loop(t) }.apply { isDaemon = true; start() }
        } catch (e: Exception) {
            track = null
        }
    }

    fun play(clip: ShortArray) {
        if (track != null) pending.add(Voice(clip))
    }

    fun release() {
        running = false
        try { thread?.join(400) } catch (e: InterruptedException) { }
        try { track?.stop() } catch (e: Exception) { }
        try { track?.release() } catch (e: Exception) { }
        track = null
    }

    private fun loop(t: AudioTrack) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
        val mix = IntArray(CHUNK)
        val out = ShortArray(CHUNK)
        val active = ArrayList<Voice>()
        try {
            t.play()
            while (running) {
                while (true) {
                    val v = pending.poll() ?: break
                    active.add(v)
                    if (active.size > 24) active.removeAt(0)
                }
                java.util.Arrays.fill(mix, 0)
                var k = 0
                while (k < active.size) {
                    val v = active[k]
                    val n = minOf(CHUNK, v.data.size - v.pos)
                    for (j in 0 until n) mix[j] = mix[j] + v.data[v.pos + j].toInt()
                    v.pos += n
                    if (v.pos >= v.data.size) active.removeAt(k) else k++
                }
                for (j in 0 until CHUNK) {
                    val s = mix[j]
                    out[j] = (if (s > 32767) 32767 else if (s < -32768) -32768 else s).toShort()
                }
                if (t.write(out, 0, CHUNK) < 0) break
            }
        } catch (e: Exception) {
            // audio device went away - nothing to do
        }
    }

    private companion object {
        const val CHUNK = 256
    }
}
