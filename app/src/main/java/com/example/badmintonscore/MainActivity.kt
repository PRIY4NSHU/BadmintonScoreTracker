package com.example.badmintonscore

import android.speech.tts.TextToSpeech
import java.util.Locale
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.media.session.MediaSession
import android.media.MediaPlayer
import android.view.KeyEvent

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

        playerName = intent.getStringExtra("PLAYER_NAME") ?: "You"
        opponentName = intent.getStringExtra("OPPONENT_NAME") ?: "Opponent"
        targetScore = intent.getIntExtra("TARGET_SCORE", 21)

        findViewById<TextView>(R.id.playerNameLabel).text = playerName
        findViewById<TextView>(R.id.opponentNameLabel).text = opponentName

        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.language = Locale.US
            }
        }

        mediaSession = MediaSession(this, "BadmintonScoreSession")
        mediaPlayer = MediaPlayer.create(this, R.raw.silence)
        mediaPlayer.isLooping = true
        mediaPlayer.start()
        mediaSession.setCallback(object : MediaSession.Callback() {
            override fun onMediaButtonEvent(mediaButtonIntent: android.content.Intent): Boolean {
                val event =
                    mediaButtonIntent.getParcelableExtra<KeyEvent>(android.content.Intent.EXTRA_KEY_EVENT)

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

        myScoreView = findViewById(R.id.myScore)
        opponentScoreView = findViewById(R.id.opponentScore)

        val myPointBtn = findViewById<Button>(R.id.myPointBtn)
        val opponentPointBtn = findViewById<Button>(R.id.opponentPointBtn)
        val undoBtn = findViewById<Button>(R.id.undoBtn)
        val resetBtn = findViewById<Button>(R.id.resetBtn)

        myPointBtn.setOnClickListener { addMyPoint() }
        opponentPointBtn.setOnClickListener { addOpponentPoint() }

        undoBtn.setOnClickListener {
            if (history.isNotEmpty()) {
                val previousState = history.removeAt(history.size - 1)
                myScore = previousState.myScore
                opponentScore = previousState.opponentScore
                isMyServe = previousState.isMyServe
                gameFinished = false
                myScoreView.text = myScore.toString()
                opponentScoreView.text = opponentScore.toString()
                speakScore()
            }
        }

        resetBtn.setOnClickListener { resetMatch() }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun resetMatch() {
        myScore = 0
        opponentScore = 0
        isMyServe = true
        gameFinished = false
        history.clear()
        myScoreView.text = "0"
        opponentScoreView.text = "0"
        textToSpeech.speak("Match reset", TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun speakScore() {
        val serveSide = getServeSide()
        val scoreText = "$playerName $myScore. $opponentName $opponentScore. $serveSide"
        textToSpeech.speak(scoreText, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onDestroy() {
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }
        super.onDestroy()
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
        if (!checkGameEnd()) speakScore()
    }

    private fun addOpponentPoint() {
        if (gameFinished) return
        history.add(GameState(myScore, opponentScore, isMyServe))
        opponentScore++
        isMyServe = false
        opponentScoreView.text = opponentScore.toString()
        if (!checkGameEnd()) speakScore()
    }

    private fun checkGameEnd(): Boolean {
        val maxScore = targetScore + 9

        if (myScore >= targetScore || opponentScore >= targetScore) {
            val diff = kotlin.math.abs(myScore - opponentScore)
            if (diff >= 2 || myScore >= maxScore || opponentScore >= maxScore) {
                gameFinished = true
                if (myScore > opponentScore) {
                    textToSpeech.speak(
                        "Game over. $playerName wins $myScore to $opponentScore",
                        TextToSpeech.QUEUE_FLUSH, null, null
                    )
                } else {
                    textToSpeech.speak(
                        "Game over. $opponentName wins $opponentScore to $myScore",
                        TextToSpeech.QUEUE_FLUSH, null, null
                    )
                }
                return true
            }
        }
        return false
    }
}