package com.example.badmintonscore

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.speech.tts.TextToSpeech
import java.util.Locale
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.os.Bundle
import android.view.View
import android.view.animation.OvershootInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.media.session.MediaSession
import android.media.MediaPlayer
import android.view.KeyEvent
import com.bumptech.glide.Glide
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.xml.KonfettiView
import java.util.concurrent.TimeUnit

data class GameState(
    val myScore: Int,
    val opponentScore: Int,
    val isMyServe: Boolean
)

class MainActivity : AppCompatActivity() {

    private var myScore = 0
    private var opponentScore = 0
    private var isMyServe = true
    private var targetScore = 21
    private var gameFinished = false

    private lateinit var myScoreView: TextView
    private lateinit var opponentScoreView: TextView
    private lateinit var myServeIndicator: TextView
    private lateinit var opponentServeIndicator: TextView
    private lateinit var winnerOverlay: FrameLayout
    private lateinit var winnerText: TextView
    private lateinit var winnerScore: TextView
    private lateinit var konfettiView: KonfettiView
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var mediaSession: MediaSession
    private lateinit var mediaPlayer: MediaPlayer

    private var playerName = "You"
    private var opponentName = "Opponent"

    private val history = mutableListOf<GameState>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""

        playerName = intent.getStringExtra("PLAYER_NAME") ?: "You"
        opponentName = intent.getStringExtra("OPPONENT_NAME") ?: "Opponent"
        targetScore = intent.getIntExtra("TARGET_SCORE", 21)

        findViewById<TextView>(R.id.playerNameLabel).text = playerName
        findViewById<TextView>(R.id.opponentNameLabel).text = opponentName

        myScoreView = findViewById(R.id.myScore)
        opponentScoreView = findViewById(R.id.opponentScore)
        myServeIndicator = findViewById(R.id.myServeIndicator)
        opponentServeIndicator = findViewById(R.id.opponentServeIndicator)
        winnerOverlay = findViewById(R.id.winnerOverlay)
        winnerText = findViewById(R.id.winnerText)
        winnerScore = findViewById(R.id.winnerScore)
        konfettiView = findViewById(R.id.konfettiView)

        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) textToSpeech.language = Locale.US
        }

        mediaSession = MediaSession(this, "BadmintonScoreSession")
        mediaPlayer = MediaPlayer.create(this, R.raw.silence)
        mediaPlayer.isLooping = true
        mediaPlayer.start()

        mediaSession.setCallback(object : MediaSession.Callback() {
            override fun onMediaButtonEvent(mediaButtonIntent: android.content.Intent): Boolean {
                val event = mediaButtonIntent.getParcelableExtra<KeyEvent>(android.content.Intent.EXTRA_KEY_EVENT)
                if (event != null && event.action == KeyEvent.ACTION_DOWN) {
                    when (event.keyCode) {
                        KeyEvent.KEYCODE_MEDIA_NEXT -> addMyPoint()
                        KeyEvent.KEYCODE_MEDIA_PREVIOUS -> addOpponentPoint()
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                        KeyEvent.KEYCODE_MEDIA_PLAY,
                        KeyEvent.KEYCODE_MEDIA_PAUSE,
                        KeyEvent.KEYCODE_HEADSETHOOK -> speakScore()
                    }
                }
                return true
            }
        })
        mediaSession.setFlags(
            MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS
        )
        mediaSession.isActive = true

        findViewById<Button>(R.id.myPointBtn).setOnClickListener { addMyPoint() }
        findViewById<Button>(R.id.opponentPointBtn).setOnClickListener { addOpponentPoint() }

        findViewById<Button>(R.id.undoBtn).setOnClickListener {
            if (history.isNotEmpty()) {
                val prev = history.removeAt(history.size - 1)
                myScore = prev.myScore
                opponentScore = prev.opponentScore
                isMyServe = prev.isMyServe
                gameFinished = false
                winnerOverlay.visibility = View.GONE
                konfettiView.reset()
                myScoreView.text = myScore.toString()
                opponentScoreView.text = opponentScore.toString()
                updateServeIndicators()
                speakScore()
            }
        }

        findViewById<Button>(R.id.resetBtn).setOnClickListener { resetMatch() }

        winnerOverlay.setOnClickListener {
            winnerOverlay.animate().alpha(0f).setDuration(300).withEndAction {
                winnerOverlay.visibility = View.GONE
                winnerOverlay.alpha = 1f
                konfettiView.reset()
            }.start()
        }

        updateServeIndicators()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun updateServeIndicators() {
        myServeIndicator.visibility = if (isMyServe) View.VISIBLE else View.INVISIBLE
        opponentServeIndicator.visibility = if (!isMyServe) View.VISIBLE else View.INVISIBLE
    }

    private fun animateScoreChange(scoreView: TextView) {
        val scaleX = ObjectAnimator.ofFloat(scoreView, "scaleX", 1f, 1.25f, 1f)
        val scaleY = ObjectAnimator.ofFloat(scoreView, "scaleY", 1f, 1.25f, 1f)
        scaleX.duration = 350
        scaleY.duration = 350
        scaleX.interpolator = OvershootInterpolator(3f)
        scaleY.interpolator = OvershootInterpolator(3f)
        AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            start()
        }
    }

    private fun launchConfetti() {
        val colors = listOf(
            0xFF4A7C59.toInt(),
            0xFFC0604A.toInt(),
            0xFFFFD166.toInt(),
            0xFF5ECEC8.toInt(),
            0xFFA8C8F0.toInt(),
            0xFFFF8FAF.toInt()
        )
        val parties = listOf(
            Party(
                speed = 2f,
                maxSpeed = 25f,
                damping = 0.9f,
                spread = 180,
                colors = colors,
                emitter = Emitter(duration = 3L, timeUnit = TimeUnit.SECONDS).max(300),
                position = Position.Relative(0.5, 0.0)
            ),
            Party(
                speed = 2f,
                maxSpeed = 25f,
                damping = 0.9f,
                spread = 90,
                colors = colors,
                emitter = Emitter(duration = 3L, timeUnit = TimeUnit.SECONDS).max(150),
                position = Position.Relative(0.0, 0.0)
            ),
            Party(
                speed = 2f,
                maxSpeed = 25f,
                damping = 0.9f,
                spread = 90,
                colors = colors,
                emitter = Emitter(duration = 3L, timeUnit = TimeUnit.SECONDS).max(150),
                position = Position.Relative(1.0, 0.0)
            )
        )
        parties.forEach { konfettiView.start(it) }
    }

    private fun showWinnerOverlay(winnerName: String) {
        winnerText.text = "$winnerName wins!"
        winnerScore.text = "$myScore – $opponentScore"

        Glide.with(this)
            .asGif()
            .load(R.drawable.trophy)
            .into(findViewById(R.id.trophyIcon))

        winnerOverlay.alpha = 0f
        winnerOverlay.visibility = View.VISIBLE
        winnerOverlay.animate()
            .alpha(1f)
            .setDuration(400)
            .setInterpolator(DecelerateInterpolator())
            .start()

        winnerText.scaleX = 0.5f
        winnerText.scaleY = 0.5f
        winnerText.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(500)
            .setStartDelay(200)
            .setInterpolator(OvershootInterpolator(2f))
            .start()

        launchConfetti()
    }

    private fun resetMatch() {
        myScore = 0
        opponentScore = 0
        isMyServe = true
        gameFinished = false
        history.clear()
        myScoreView.text = "0"
        opponentScoreView.text = "0"
        winnerOverlay.visibility = View.GONE
        konfettiView.reset()
        updateServeIndicators()
        textToSpeech.speak("Match reset", TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun speakScore() {
        val serveSide = getServeSide()
        textToSpeech.speak(
            "$playerName $myScore. $opponentName $opponentScore. $serveSide",
            TextToSpeech.QUEUE_FLUSH, null, null
        )
    }

    private fun getServeSide(): String {
        return if (isMyServe) {
            if (myScore % 2 == 0) "$playerName serves from right"
            else "$playerName serves from left"
        } else {
            if (opponentScore % 2 == 0) "$opponentName serves from right"
            else "$opponentName serves from left"
        }
    }

    private fun addMyPoint() {
        if (gameFinished) return
        history.add(GameState(myScore, opponentScore, isMyServe))
        myScore++
        isMyServe = true
        myScoreView.text = myScore.toString()
        updateServeIndicators()
        animateScoreChange(myScoreView)
        if (!checkGameEnd()) speakScore()
    }

    private fun addOpponentPoint() {
        if (gameFinished) return
        history.add(GameState(myScore, opponentScore, isMyServe))
        opponentScore++
        isMyServe = false
        opponentScoreView.text = opponentScore.toString()
        updateServeIndicators()
        animateScoreChange(opponentScoreView)
        if (!checkGameEnd()) speakScore()
    }

    private fun checkGameEnd(): Boolean {
        val maxScore = targetScore + 9
        if (myScore >= targetScore || opponentScore >= targetScore) {
            val diff = kotlin.math.abs(myScore - opponentScore)
            if (diff >= 2 || myScore >= maxScore || opponentScore >= maxScore) {
                gameFinished = true
                val winnerName = if (myScore > opponentScore) playerName else opponentName
                showWinnerOverlay(winnerName)
                val speech = if (myScore > opponentScore)
                    "Game over. $playerName wins $myScore to $opponentScore"
                else
                    "Game over. $opponentName wins $opponentScore to $myScore"
                textToSpeech.speak(speech, TextToSpeech.QUEUE_FLUSH, null, null)
                return true
            }
        }
        return false
    }

    override fun onDestroy() {
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        if (::mediaPlayer.isInitialized) mediaPlayer.release()
        super.onDestroy()
    }
}