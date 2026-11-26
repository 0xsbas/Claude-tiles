package com.example.pianotiles

import android.app.Activity
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

private val BG = 0xFF101418.toInt()
private val ROW = 0xFF1C232B.toInt()
private val MUTED = 0xFF9AA7B4.toInt()
private val ACCENT = 0xFF4DD0B8.toInt()

class MainActivity : Activity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var root: FrameLayout
    private lateinit var menu: LinearLayout
    private lateinit var songList: LinearLayout
    private lateinit var speedBtn: TextView
    private lateinit var game: GameView
    private val bestViews = ArrayList<TextView>()
    private var audio: AudioOut? = null

    private val speedNames = arrayOf("Easy", "Normal", "Fast")
    private val speedMult = floatArrayOf(0.8f, 1f, 1.3f)
    private var speedIdx = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("tiles", MODE_PRIVATE)
        speedIdx = prefs.getInt("speed", 1).coerceIn(0, 2)
        volumeControlStream = AudioManager.STREAM_MUSIC
        window.statusBarColor = BG
        window.navigationBarColor = BG

        root = FrameLayout(this)
        root.setBackgroundColor(BG)

        game = GameView(this)
        game.onExit = { showMenu() }
        game.onResult = { idx, tiles ->
            if (tiles > prefs.getInt("best_$idx", 0)) {
                prefs.edit().putInt("best_$idx", tiles).apply()
            }
        }

        buildMenu()
        root.addView(menu, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        root.addView(game, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        setContentView(root)
        showMenu()
    }

    override fun onResume() {
        super.onResume()
        audio = AudioOut()
        game.audio = audio
    }

    override fun onPause() {
        super.onPause()
        if (game.visibility == View.VISIBLE) showMenu()
        game.audio = null
        audio?.release()
        audio = null
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        if (game.visibility == View.VISIBLE) showMenu() else super.onBackPressed()
    }

    // ------------------------------------------------------------------ screens

    private fun showMenu() {
        game.stop()
        game.visibility = View.GONE
        menu.visibility = View.VISIBLE
        showSystemUi()
        refreshBests()
    }

    private fun startGame(index: Int) {
        menu.visibility = View.GONE
        game.visibility = View.VISIBLE
        hideSystemUi()
        game.start(index, SONGS[index], speedMult[speedIdx], prefs.getInt("best_$index", 0))
    }

    @Suppress("DEPRECATION")
    private fun hideSystemUi() {
        if (Build.VERSION.SDK_INT >= 30) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.systemBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
    }

    @Suppress("DEPRECATION")
    private fun showSystemUi() {
        if (Build.VERSION.SDK_INT >= 30) {
            window.insetsController?.show(WindowInsets.Type.systemBars())
        } else {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    // ------------------------------------------------------------------ menu

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun buildMenu() {
        menu = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(BG)
            setPadding(dp(16), dp(16), dp(16), 0)
            fitsSystemWindows = true
        }

        menu.addView(TextView(this).apply {
            text = "Classic Tiles"
            setTextColor(Color.WHITE)
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
        })
        menu.addView(TextView(this).apply {
            text = "Tap the black tiles. Every tap plays the next note.\nPublic-domain classics only."
            setTextColor(MUTED)
            textSize = 13f
            setPadding(0, dp(4), 0, dp(12))
        })

        speedBtn = TextView(this).apply {
            setTextColor(ACCENT)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(14), dp(8), dp(14), dp(8))
            background = GradientDrawable().apply {
                setColor(ROW)
                cornerRadius = dp(18).toFloat()
            }
            setOnClickListener {
                speedIdx = (speedIdx + 1) % speedNames.size
                prefs.edit().putInt("speed", speedIdx).apply()
                updateSpeedLabel()
            }
        }
        updateSpeedLabel()
        menu.addView(
            speedBtn,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(12) }
        )

        songList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(24))
        }
        SONGS.forEachIndexed { i, s ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(16), dp(14), dp(16), dp(14))
                background = GradientDrawable().apply {
                    setColor(ROW)
                    cornerRadius = dp(14).toFloat()
                }
                setOnClickListener { startGame(i) }
            }
            val left = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            left.addView(TextView(this).apply {
                text = s.title
                setTextColor(Color.WHITE)
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
            })
            left.addView(TextView(this).apply {
                text = "${s.composer}  ·  ${s.notes.size} tiles"
                setTextColor(MUTED)
                textSize = 12f
            })
            row.addView(left, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            val bestTv = TextView(this).apply {
                setTextColor(ACCENT)
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
            }
            bestViews.add(bestTv)
            row.addView(bestTv)
            songList.addView(
                row,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(10) }
            )
        }

        val scroller = ScrollView(this)
        scroller.addView(songList)
        menu.addView(scroller, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
    }

    private fun updateSpeedLabel() {
        speedBtn.text = "Speed: ${speedNames[speedIdx]}  (tap to change)"
    }

    private fun refreshBests() {
        bestViews.forEachIndexed { i, tv ->
            val b = prefs.getInt("best_$i", 0)
            tv.text = if (b > 0) "Best $b/${SONGS[i].notes.size}" else ""
        }
    }
}
