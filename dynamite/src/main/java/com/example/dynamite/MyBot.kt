package com.example.dynamite

import com.softwire.dynamite.bot.Bot
import com.softwire.dynamite.game.Gamestate
import com.softwire.dynamite.game.Move
import com.sun.org.apache.xpath.internal.operations.Bool
import kotlin.random.Random

class MyBot : Bot {
    var opponentMoves = listOf<Move>()
    var opponentDynamite = 100
    var myDynamite = 100
    var turnCount = 0
    var probabilityList = mutableMapOf<Move, MutableMap<Move, Int>>(
        Move.R to mutableMapOf(Move.R to 0, Move.P to 0, Move.S to 0, Move.D to 0, Move.W to 0),
        Move.P to mutableMapOf(Move.R to 0, Move.P to 0, Move.S to 0, Move.D to 0, Move.W to 0),
        Move.S to mutableMapOf(Move.R to 0, Move.P to 0, Move.S to 0, Move.D to 0, Move.W to 0),
        Move.D to mutableMapOf(Move.R to 0, Move.P to 0, Move.S to 0, Move.D to 0, Move.W to 0),
        Move.W to mutableMapOf(Move.R to 0, Move.P to 0, Move.S to 0, Move.D to 0, Move.W to 0)
    )
    var moveCountMap =
        mutableMapOf<Move, Int>(Move.R to 0, Move.P to 0, Move.S to 0, Move.D to 0, Move.W to 0)

    override fun makeMove(gamestate: Gamestate): Move {
        roundProcessor(gamestate)
        turnCount += 1
        val move = opponentPredictor(opponentMoves)
        if (move == Move.D) {
            myDynamite -= 1
        }
        if (myDynamite == 0) {
            print("Out of Dynamite")
            myDynamite -= 1
        }
        return move
        // Are you debugging?
        // Put a breakpoint in this method to see when we make a move

    }

    fun roundProcessor(gamestate: Gamestate) {
        if (gamestate.rounds.isNotEmpty()) {
            val previousMove: Move = gamestate.rounds[gamestate.rounds.size - 1].p2
            if (previousMove == Move.D) {
                opponentDynamite -= 1
            }
            opponentMoves = opponentMoves+ previousMove
            moveCountMap[previousMove] = moveCountMap[previousMove]?.plus(1)!!
            if (opponentMoves.size > 1) {
                val penultimateMove: Move = gamestate.rounds[gamestate.rounds.size - 2].p2
                val penultimateMoveMap: MutableMap<Move, Int>? =
                    probabilityList.get(penultimateMove)
                var moveValue = penultimateMoveMap?.get(previousMove)
                moveValue = moveValue?.plus(1)

                if (moveValue != null) penultimateMoveMap?.put(previousMove, moveValue)
            }
        }
    }

    fun opponentPredictor(opponentMoves: List<Move>): Move {
        return if (opponentMoves.isNotEmpty()) {
            if (oneMoveStrat(opponentMoves)) {
                winningMove(opponentMoves[0])
            } else {
                statsthing(opponentMoves)
            }
        } else {
            randomMove()
        }
    }

    fun randomMove(): Move {
        val moves = listOf<Move>(Move.R, Move.D, Move.W, Move.S, Move.P)
        return moves.shuffled().first()
    }

    fun statsthing(opponentMoves: List<Move>): Move {
        val previousMove = opponentMoves[opponentMoves.size - 1]
        val previousMoveMap = probabilityList[previousMove]
        var moveCount = moveCountMap[previousMove]
        if (opponentDynamite == 0){
            moveCount = moveCount?.minus(previousMoveMap?.get(Move.D)!!)
            moveCount = moveCount?.minus(previousMoveMap?.get(Move.W)!!)
            previousMoveMap?.set(Move.D, 0)
        }
        val predictedMove = moveSelector(previousMoveMap!!, moveCount!!)
        return if (previousMoveMap[predictedMove]?.div(moveCount.toFloat())!! > 0.4) {
            winningMove(predictedMove)
        } else {
            if (myDynamite > 0){
                val value = Random.nextDouble()
                when {
                    value < 0.5 -> winningMove(predictedMove)
                    else -> Move.D
                }
            } else {
                winningMove(predictedMove)
            }
        }

    }



    fun oneMoveStrat(opponentMoves: List<Move>): Boolean {
        val firstMove = opponentMoves[0]
        var oneMove = true
        for (move in opponentMoves) {
            if (move != firstMove) {
                oneMove = false
            }
        }
        return oneMove
    }

    // I hate this, returns move based on probability distribution
    fun moveSelector(probabilityMap: MutableMap<Move, Int>, totalCount: Int): Move {
        var selection = Random.nextInt(totalCount)
        var move = Move.R
        while (selection >= 0) {
            selection -= probabilityMap[move]!!
            if (selection > 0) {
                move = nextMove(move)
            } else {
                break
            }
        }
        return move
    }

    // I hate this bit more
    fun nextMove(move: Move): Move {
        return when (move) {
            Move.R -> Move.P
            Move.P -> Move.S
            Move.S -> Move.D
            Move.D -> Move.W
            else -> Move.R
        }
    }

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