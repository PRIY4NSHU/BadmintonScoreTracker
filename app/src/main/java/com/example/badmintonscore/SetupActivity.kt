package com.example.badmintonscore

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class SetupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        val playerNameInput = findViewById<EditText>(R.id.playerNameInput)
        val opponentNameInput = findViewById<EditText>(R.id.opponentNameInput)
        val startGameBtn = findViewById<Button>(R.id.startGameBtn)

        startGameBtn.setOnClickListener {
            val playerName = playerNameInput.text.toString().trim().ifEmpty { "You" }
            val opponentName = opponentNameInput.text.toString().trim().ifEmpty { "Opponent" }

            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("PLAYER_NAME", playerName)
                putExtra("OPPONENT_NAME", opponentName)
            }
            startActivity(intent)
            finish()
        }
    }
}
