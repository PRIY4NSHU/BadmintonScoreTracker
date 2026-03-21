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
import android.widget.TextView
import android.media.session.MediaSession
import android.media.MediaPlayer
import android.view.KeyEvent
import androidx.activity.viewModels
import com.bumptech.glide.Glide
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.xml.KonfettiView
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""

        if (viewModel.history.isEmpty() && viewModel.myScore.value == 0 && viewModel.opponentScore.value == 0) {
            viewModel.playerName = intent.getStringExtra("PLAYER_NAME") ?: "You"
            viewModel.opponentName = intent.getStringExtra("OPPONENT_NAME") ?: "Opponent"
            viewModel.targetScore = intent.getIntExtra("TARGET_SCORE", 21)
        }

        findViewById<TextView>(R.id.playerNameLabel).text = viewModel.playerName
        findViewById<TextView>(R.id.opponentNameLabel).text = viewModel.opponentName

        myScoreView = findViewById(R.id.myScore)
        opponentScoreView = findViewById(R.id.opponentScore)
        myServeIndicator = findViewById(R.id.myServeIndicator)
        opponentServeIndicator = findViewById(R.id.opponentServeIndicator)
        winnerOverlay = findViewById(R.id.winnerOverlay)
        winnerText = findViewById(R.id.winnerText)
        winnerScore = findViewById(R.id.winnerScore)
        konfettiView = findViewById(R.id.konfettiView)

        // Observe ViewModel data
        viewModel.myScore.observe(this) { score ->
            myScoreView.text = score.toString()
            animateScoreChange(myScoreView)
            if (!checkGameEnd()) speakScore()
        }

        viewModel.opponentScore.observe(this) { score ->
            opponentScoreView.text = score.toString()
            animateScoreChange(opponentScoreView)
            if (!checkGameEnd()) speakScore()
        }

        viewModel.isMyServe.observe(this) { updateServeIndicators() }

        viewModel.gameFinished.observe(this) { finished ->
            if (finished) {
                val winnerName = if (viewModel.myScore.value!! > viewModel.opponentScore.value!!) viewModel.playerName else viewModel.opponentName
                showWinnerOverlay(winnerName)
            } else {
                winnerOverlay.visibility = View.GONE
                konfettiView.reset()
            }
        }

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
                    runOnUiThread {
                        when (event.keyCode) {
                            KeyEvent.KEYCODE_MEDIA_NEXT -> viewModel.addMyPoint()
                            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> viewModel.addOpponentPoint()
                            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                            KeyEvent.KEYCODE_MEDIA_PLAY,
                            KeyEvent.KEYCODE_MEDIA_PAUSE,
                            KeyEvent.KEYCODE_HEADSETHOOK -> speakScore()
                        }
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

        findViewById<Button>(R.id.myPointBtn).setOnClickListener { viewModel.addMyPoint() }
        findViewById<Button>(R.id.opponentPointBtn).setOnClickListener { viewModel.addOpponentPoint() }
        findViewById<Button>(R.id.undoBtn).setOnClickListener { viewModel.undo() }
        findViewById<Button>(R.id.resetBtn).setOnClickListener { resetMatch() }

        winnerOverlay.setOnClickListener {
            winnerOverlay.animate().alpha(0f).setDuration(300).withEndAction {
                winnerOverlay.visibility = View.GONE
                winnerOverlay.alpha = 1f
                konfettiView.reset()
            }.start()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun updateServeIndicators() {
        val isMyServe = viewModel.isMyServe.value ?: true
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
        winnerScore.text = "${viewModel.myScore.value} – ${viewModel.opponentScore.value}"

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
        viewModel.reset()
        textToSpeech.speak("Match reset", TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun speakScore() {
        val serveSide = getServeSide()
        textToSpeech.speak(
            "${viewModel.playerName} ${viewModel.myScore.value}. ${viewModel.opponentName} ${viewModel.opponentScore.value}. $serveSide",
            TextToSpeech.QUEUE_FLUSH, null, null
        )
    }

    private fun getServeSide(): String {
        val myScore = viewModel.myScore.value ?: 0
        val opponentScore = viewModel.opponentScore.value ?: 0
        val isMyServe = viewModel.isMyServe.value ?: true

        return if (isMyServe) {
            if (myScore % 2 == 0) "${viewModel.playerName} serves from right"
            else "${viewModel.playerName} serves from left"
        } else {
            if (opponentScore % 2 == 0) "${viewModel.opponentName} serves from right"
            else "${viewModel.opponentName} serves from left"
        }
    }

    private fun checkGameEnd(): Boolean {
        val myScore = viewModel.myScore.value ?: 0
        val opponentScore = viewModel.opponentScore.value ?: 0
        val maxScore = viewModel.targetScore + 9

        if (myScore >= viewModel.targetScore || opponentScore >= viewModel.targetScore) {
            val diff = kotlin.math.abs(myScore - opponentScore)
            if (diff >= 2 || myScore >= maxScore || opponentScore >= maxScore) {
                viewModel.setGameFinished(true)
                val speech = if (myScore > opponentScore)
                    "Game over. ${viewModel.playerName} wins $myScore to $opponentScore"
                else
                    "Game over. ${viewModel.opponentName} wins $opponentScore to $myScore"
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
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.stop()
            mediaPlayer.release()
        }
        if (::mediaSession.isInitialized) {
            mediaSession.isActive = false
            mediaSession.release()
        }
        super.onDestroy()
    }
}