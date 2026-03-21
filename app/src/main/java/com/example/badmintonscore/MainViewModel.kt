package com.example.badmintonscore

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {
    private val _myScore = MutableLiveData(0)
    val myScore: LiveData<Int> = _myScore

    private val _opponentScore = MutableLiveData(0)
    val opponentScore: LiveData<Int> = _opponentScore

    private val _isMyServe = MutableLiveData(true)
    val isMyServe: LiveData<Boolean> = _isMyServe

    private val _gameFinished = MutableLiveData(false)
    val gameFinished: LiveData<Boolean> = _gameFinished

    var targetScore = 21
    var playerName = "You"
    var opponentName = "Opponent"
    val history = mutableListOf<GameState>()

    fun addMyPoint() {
        if (_gameFinished.value == true) return
        history.add(GameState(_myScore.value!!, _opponentScore.value!!, _isMyServe.value!!))
        _isMyServe.value = true
        _myScore.value = (_myScore.value ?: 0) + 1
    }

    fun addOpponentPoint() {
        if (_gameFinished.value == true) return
        history.add(GameState(_myScore.value!!, _opponentScore.value!!, _isMyServe.value!!))
        _isMyServe.value = false
        _opponentScore.value = (_opponentScore.value ?: 0) + 1
    }

    fun undo() {
        if (history.isNotEmpty()) {
            val prev = history.removeAt(history.size - 1)
            _myScore.value = prev.myScore
            _opponentScore.value = prev.opponentScore
            _isMyServe.value = prev.isMyServe
            _gameFinished.value = false
        }
    }

    fun setGameFinished(finished: Boolean) {
        _gameFinished.value = finished
    }

    fun reset() {
        _myScore.value = 0
        _opponentScore.value = 0
        _isMyServe.value = true
        _gameFinished.value = false
        history.clear()
    }
}