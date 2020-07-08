package com.example.dynamite

import com.softwire.dynamite.bot.Bot
import com.softwire.dynamite.game.Gamestate
import com.softwire.dynamite.game.Move
import com.sun.org.apache.xpath.internal.operations.Bool

class MyBot : Bot {
    var opponentMoves = listOf<Move>()
    var opponentDynamite = 100
    var myDynamite = 100
    var turnCount = 0
    var probabilityList = mutableMapOf<Move,MutableMap<Move,Int>>(
        Move.R to mutableMapOf(Move.R to 0, Move.P to 0, Move.S to 0, Move.D to 0, Move.W to 0),
        Move.P to mutableMapOf(Move.R to 0, Move.P to 0, Move.S to 0, Move.D to 0, Move.W to 0),
        Move.S to mutableMapOf(Move.R to 0, Move.P to 0, Move.S to 0, Move.D to 0, Move.W to 0),
        Move.D to mutableMapOf(Move.R to 0, Move.P to 0, Move.S to 0, Move.D to 0, Move.W to 0),
        Move.W to mutableMapOf(Move.R to 0, Move.P to 0, Move.S to 0, Move.D to 0, Move.W to 0))
    var moveCountMap = mutableMapOf<Move, Int>(Move.R to 0, Move.P to 0, Move.S to 0, Move.D to 0, Move.W to 0)

    override fun makeMove(gamestate: Gamestate): Move {
        roundProcessor(gamestate)
        turnCount += 1
        return opponentPredictor(opponentMoves)
        // Are you debugging?
        // Put a breakpoint in this method to see when we make a move

    }

    fun roundProcessor(gamestate: Gamestate) {
        if (gamestate.rounds.isNotEmpty()) {
            var previousMove: Move = gamestate.rounds[gamestate.rounds.size - 1].p2
            opponentMoves += previousMove
            moveCountMap[previousMove] = moveCountMap[previousMove]?.plus(1)!!
            if (opponentMoves.size > 1) {
                var penultimateMove: Move = gamestate.rounds[gamestate.rounds.size - 2].p2
                var penultimateMoveMap: MutableMap<Move, Int>? = probabilityList.get(penultimateMove)
                var moveValue = penultimateMoveMap?.get(previousMove)
                moveValue = moveValue?.plus(1)

                if (moveValue != null) penultimateMoveMap?.put(previousMove,moveValue)
            }
        }
    }
    fun opponentPredictor(opponentMoves: List<Move>): Move {
        return if (opponentMoves.isNotEmpty()) {
            if (oneMoveStrat(opponentMoves)){
                winningMove(opponentMoves[0])
            } else {
                statsthing(opponentMoves)
            }
        } else {
            randomMove()
        }
    }

    fun randomMove(): Move{
        var moves = listOf<Move>(Move.R,Move.D,Move.W,Move.S,Move.P)
        return moves.shuffled().first()
    }
    fun statsthing(opponentMoves: List<Move>): Move {
        var previousMove = opponentMoves[opponentMoves.size - 1]
        var previousMoveMap = probabilityList[previousMove]
        var moveCount = moveCountMap[previousMove]
        var probabilityMap = mutableMapOf<Move,Double>(Move.R to previousMoveMap?.get(Move.R)!! / moveCount!!,
            Move.P to previousMoveMap?.get(Move.P)!! / moveCount!!,
            Move.S to previousMoveMap?.get(Move.S)!! / moveCount!!,
            Move.D to previousMoveMap?.get(Move.D)!! / moveCount!!,
            Move.W to previousMoveMap?.get(Move.W)!! / moveCount!!)


        return Move.R
    }
    fun oneMoveStrat(opponentMoves: List<Move>): Boolean {
        var firstMove = opponentMoves[0]
        var oneMove = true
        for (move in opponentMoves) {
            if (move != firstMove) {
                oneMove = false
            }
        }
        return oneMove
    }

    fun moveSelector() {}
    fun winningMove(move: Move): Move {
        return when {
            move == Move.R -> Move.P
            move == Move.P -> Move.S
            move == Move.S -> Move.R
            move == Move.D -> Move.W
            move == Move.W -> Move.R
            else -> Move.R
        }
    }

    init {
        // Are you debugging?
        // Put a breakpoint on the line below to see when we start a new match
        println("Started new match")
    }
}