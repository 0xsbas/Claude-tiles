package com.example.pianotiles

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.random.Random

/**
 * The whole game lives in this one custom View: 4 columns x 4 visible rows.
 * Row i is stacked upwards from the bottom; `scroll` = how many rows the stack has moved down.
 * Each black tile is bound to the i-th note of the song and plays it when tapped.
 */
class GameView(context: Context) : View(context) {

    var audio: AudioOut? = null
    var onExit: (() -> Unit)? = null
    var onResult: ((songIndex: Int, tiles: Int) -> Unit)? = null

    private enum class State { IDLE, LOADING, READY, PLAYING, LOST, WON }

    private var state = State.IDLE
    private var song: Song? = null
    private var songIndex = 0
    private var speedMul = 1f
    private var clips: Array<ShortArray> = emptyArray()
    private var cols = IntArray(0)
    private var next = 0          // tiles tapped so far == index of the tile that must be tapped now
    private var scroll = 0f       // rows scrolled
    private var lastNs = 0L
    private var endTime = 0f
    private var wrongRow = -1
    private var wrongCol = -1
    private var best = 0
    private var newBest = false
    private var loadToken = 0

    private val accent = 0xFF26A69A.toInt()
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val grid = Paint().apply {
        color = 0xFFD9DDE1.toInt()
        strokeWidth = 2f
    }
    private val tile = RectF()
    private val btnRetry = RectF()
    private val btnMenu = RectF()

    fun start(index: Int, s: Song, speed: Float, bestSoFar: Int) {
        songIndex = index
        song = s
        speedMul = speed
        best = bestSoFar
        state = State.LOADING
        val token = ++loadToken
        val notes = s.notes
        Thread {
            // Pre-render every note off the UI thread so taps never stall.
            val built = Array(notes.size) { NoteBank.get(notes[it].midi, notes[it].dur) }
            post {
                if (token == loadToken) {
                    clips = built
                    reset()
                }
            }
        }.start()
        lastNs = System.nanoTime()
        invalidate()
    }

    fun stop() {
        loadToken++
        state = State.IDLE
    }

    private fun reset() {
        val rnd = Random(System.nanoTime())
        cols = IntArray(clips.size) { rnd.nextInt(4) }
        next = 0
        scroll = 0f
        wrongRow = -1
        wrongCol = -1
        endTime = 0f
        newBest = false
        state = State.READY
        lastNs = System.nanoTime()
        invalidate()
    }

    private fun finish(won: Boolean, snapToMiss: Boolean) {
        if (snapToMiss) scroll = next.toFloat()   // slide back so the missed tile is visible
        state = if (won) State.WON else State.LOST
        endTime = 0f
        newBest = next > best
        if (newBest) best = next
        if (!won) audio?.play(NoteBank.errorClip())
        onResult?.invoke(songIndex, next)
    }

    // ------------------------------------------------------------------ input

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val a = e.actionMasked
        if (a == MotionEvent.ACTION_DOWN || a == MotionEvent.ACTION_POINTER_DOWN) {
            val i = e.actionIndex
            handleTap(e.getX(i), e.getY(i))
        }
        return true
    }

    private fun handleTap(x: Float, y: Float) {
        when (state) {
            State.READY, State.PLAYING -> tapTile(x, y)
            State.LOST, State.WON -> {
                if (endTime < 0.9f) return
                if (btnRetry.contains(x, y)) reset()
                else if (btnMenu.contains(x, y)) onExit?.invoke()
            }
            else -> {}
        }
    }

    private fun tapTile(x: Float, y: Float) {
        val n = clips.size
        val rowH = height / 4f
        val colW = width / 4f
        val r = ceil(3f + scroll - y / rowH).toInt()
        if (r < next || r >= n) return                 // already played, or no tile there
        val c = (x / colW).toInt().coerceIn(0, 3)
        val black = cols[r] == c
        if (r == next && black) {
            audio?.play(clips[r])
            next++
            if (state == State.READY) state = State.PLAYING
            if (next >= n) finish(won = true, snapToMiss = false)
        } else if (!black && state == State.PLAYING) {
            wrongRow = r
            wrongCol = c
            finish(won = false, snapToMiss = false)
        }
        // tapping a black tile that is not the next one is ignored
    }

    // ------------------------------------------------------------------ drawing

    override fun onDraw(canvas: Canvas) {
        val now = System.nanoTime()
        val dt = ((now - lastNs) / 1_000_000_000f).coerceIn(0f, 0.05f)
        lastNs = now
        val w = width.toFloat()
        val h = height.toFloat()
        canvas.drawColor(Color.WHITE)
        if (state == State.IDLE) return
        val cx = w / 2f

        if (state == State.LOADING) {
            label.color = 0xFF6B7782.toInt()
            label.textSize = w * 0.06f
            canvas.drawText("Loading…", cx, h / 2f, label)
            postInvalidateOnAnimation()
            return
        }

        val n = clips.size
        val rowH = h / 4f
        val colW = w / 4f

        if (state == State.PLAYING) {
            scroll += (2.2f + minOf(next * 0.035f, 3.4f)) * speedMul * dt
            if (next < n && scroll - next >= 1f) {
                wrongRow = next
                wrongCol = cols[next]
                finish(won = false, snapToMiss = true)
            }
        } else if (state == State.LOST || state == State.WON) {
            endTime += dt
        }

        for (k in 1..3) canvas.drawLine(colW * k, 0f, colW * k, h, grid)

        val gap = w * 0.004f
        val f0 = maxOf(0, floor(scroll).toInt() - 1)
        val f1 = minOf(n - 1, floor(scroll).toInt() + 4)
        for (i in f0..f1) {
            val top = rowH * (3f + scroll - i)
            if (top >= h || top + rowH <= 0f) continue
            canvas.drawLine(0f, top, w, top, grid)
            val c = cols[i]
            tile.set(c * colW + gap, top + gap, (c + 1) * colW - gap, top + rowH - gap)
            fill.color = if (i < next) 0xFFC9CED3.toInt() else 0xFF14181D.toInt()
            canvas.drawRect(tile, fill)
            if (i == 0 && state == State.READY) {
                label.color = Color.WHITE
                label.textSize = w * 0.07f
                canvas.drawText("TAP", tile.centerX(), tile.centerY() + w * 0.025f, label)
            }
        }

        if (wrongRow >= 0) {
            val top = rowH * (3f + scroll - wrongRow)
            tile.set(wrongCol * colW + gap, top + gap, (wrongCol + 1) * colW - gap, top + rowH - gap)
            fill.color = 0xFFE53935.toInt()
            canvas.drawRect(tile, fill)
        }

        // progress bar + score
        fill.color = accent
        canvas.drawRect(0f, 0f, w * next / maxOf(1, n), h * 0.008f, fill)
        label.color = 0xAA1E88E5.toInt()
        label.textSize = w * 0.13f
        canvas.drawText(next.toString(), cx, h * 0.11f, label)

        if (state == State.READY) {
            label.color = 0xFF6B7782.toInt()
            label.textSize = w * 0.045f
            canvas.drawText(song?.title ?: "", cx, h * 0.16f, label)
            label.textSize = w * 0.04f
            canvas.drawText("Tap the black tiles - each tap plays the next note", cx, h * 0.195f, label)
        }

        val overlayDelay = if (state == State.WON) 0.6f else 0.5f
        if ((state == State.LOST || state == State.WON) && endTime > overlayDelay) {
            drawOverlay(canvas, w, h, n)
        }

        postInvalidateOnAnimation()
    }

    private fun drawOverlay(canvas: Canvas, w: Float, h: Float, n: Int) {
        val cx = w / 2f
        fill.color = 0xDD101418.toInt()
        canvas.drawRect(0f, 0f, w, h, fill)

        label.color = Color.WHITE
        label.textSize = w * 0.09f
        canvas.drawText(if (state == State.WON) "Song complete!" else "Game over", cx, h * 0.30f, label)
        label.color = 0xFF9AA7B4.toInt()
        label.textSize = w * 0.045f
        canvas.drawText(song?.title ?: "", cx, h * 0.36f, label)
        label.color = Color.WHITE
        label.textSize = w * 0.07f
        canvas.drawText("$next / $n tiles", cx, h * 0.46f, label)
        label.color = accent
        label.textSize = w * 0.045f
        canvas.drawText(if (newBest) "New best!" else "Best: $best", cx, h * 0.52f, label)

        val bw = w * 0.62f
        val bh = h * 0.075f
        val y0 = h * 0.60f
        btnRetry.set(cx - bw / 2, y0, cx + bw / 2, y0 + bh)
        btnMenu.set(cx - bw / 2, y0 + bh * 1.4f, cx + bw / 2, y0 + bh * 2.4f)
        fill.color = accent
        canvas.drawRoundRect(btnRetry, bh / 2, bh / 2, fill)
        fill.color = 0xFF2A323B.toInt()
        canvas.drawRoundRect(btnMenu, bh / 2, bh / 2, fill)
        label.textSize = w * 0.05f
        label.color = 0xFF101418.toInt()
        canvas.drawText("Retry", cx, btnRetry.centerY() + label.textSize * 0.35f, label)
        label.color = Color.WHITE
        canvas.drawText("Menu", cx, btnMenu.centerY() + label.textSize * 0.35f, label)
    }
}
