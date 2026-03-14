package com.example.badmintonscore

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity

class SetupActivity : AppCompatActivity() {

    private lateinit var playerNameInput: EditText
    private lateinit var opponentNameInput: EditText
    private lateinit var targetScoreGroup: RadioGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        playerNameInput = findViewById(R.id.playerNameInput)
        opponentNameInput = findViewById(R.id.opponentNameInput)
        targetScoreGroup = findViewById(R.id.targetScoreGroup)
        val startGameBtn = findViewById<Button>(R.id.startGameBtn)

        startGameBtn.setOnClickListener {
            val playerName = playerNameInput.text.toString().trim().ifEmpty { "You" }
            val opponentName = opponentNameInput.text.toString().trim().ifEmpty { "Opponent" }

            val targetScore = when (targetScoreGroup.checkedRadioButtonId) {
                R.id.score11 -> 11
                R.id.score15 -> 15
                else -> 21
            }

            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("PLAYER_NAME", playerName)
                putExtra("OPPONENT_NAME", opponentName)
                putExtra("TARGET_SCORE", targetScore)
            }
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        playerNameInput.text.clear()
        opponentNameInput.text.clear()
        targetScoreGroup.check(R.id.score21)
    }
}