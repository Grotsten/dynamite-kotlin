package com.example.dynamite

import com.softwire.dynamite.bot.Bot
import com.softwire.dynamite.game.Gamestate
import com.softwire.dynamite.game.Move
import com.softwire.dynamite.game.Round
import com.sun.org.apache.xpath.internal.operations.Bool
import sun.security.ec.point.ProjectivePoint
import kotlin.random.Random

// Attempts to use 2 heuristics, one based on previous outcomes, one based on previous moves in
// order to predict the opponent
class MyBot : Bot {
    private var opponentMoves = listOf<Move>()
    private var myMoves = listOf<Move>()
    private var opponentDynamite = 100
    private var myDynamite = 100
    private var turnCount = 0
    private var probabilityList = mutableMapOf<Move, MutableMap<Move, Int>>(
        Move.R to mutableMapOf(Move.R to 0, Move.P to 0, Move.S to 0, Move.D to 0, Move.W to 0),
        Move.P to mutableMapOf(Move.R to 0, Move.P to 0, Move.S to 0, Move.D to 0, Move.W to 0),
        Move.S to mutableMapOf(Move.R to 0, Move.P to 0, Move.S to 0, Move.D to 0, Move.W to 0),
        Move.D to mutableMapOf(Move.R to 0, Move.P to 0, Move.S to 0, Move.D to 0, Move.W to 0),
        Move.W to mutableMapOf(Move.R to 0, Move.P to 0, Move.S to 0, Move.D to 0, Move.W to 0)
    )
    private var moveCountMap =
        mutableMapOf<Move, Int>(Move.R to 0, Move.P to 0, Move.S to 0, Move.D to 0, Move.W to 0)
    var outcomeProbabilityMap = mutableMapOf<Outcome, MutableMap<Move, Int>>(
        Outcome.W to mutableMapOf(Move.R to 0, Move.P to 0, Move.S to 0, Move.D to 0, Move.W to 0),
        Outcome.D to mutableMapOf(Move.R to 0, Move.P to 0, Move.S to 0, Move.D to 0, Move.W to 0),
        Outcome.L to mutableMapOf(Move.R to 0, Move.P to 0, Move.S to 0, Move.D to 0, Move.W to 0)
    )
    var outcomeCountMap = mutableMapOf<Outcome, Int>(Outcome.W to 0, Outcome.D to 0, Outcome.L to 0)


    override fun makeMove(gamestate: Gamestate): Move {
        roundProcessor(gamestate)
        turnCount += 1
        var move = opponentPredictor(opponentMoves, gamestate)
        if (move == Move.D) {
            myDynamite -= 1
        }
        if (myDynamite == 0) {
            print("Out of Dynamite")
            myDynamite -= 1
            if (move == Move.D) {
                move = Move.R
            }
        }
        myMoves = myMoves + move
        return move
        // Are you debugging?
        // Put a breakpoint in this method to see when we make a move

    }

    fun roundProcessor(gamestate: Gamestate) {
        if (gamestate.rounds.isNotEmpty()) {
            val previousMove: Move = gamestate.rounds[gamestate.rounds.size - 1].p2
            val previousOutcome = roundWinner(gamestate.rounds[gamestate.rounds.size - 1])
            if (previousMove == Move.D) {
                opponentDynamite -= 1
            }
            opponentMoves = opponentMoves + previousMove
            moveCountMap[previousMove] = moveCountMap[previousMove]?.plus(1)!!
            outcomeCountMap[previousOutcome] = outcomeCountMap[previousOutcome]?.plus(1)!!
            if (opponentMoves.size > 1) {
                val penultimateMove: Move = gamestate.rounds[gamestate.rounds.size - 2].p2
                val penultimateMoveMap: MutableMap<Move, Int>? =
                    probabilityList.get(penultimateMove)
                var moveValue = penultimateMoveMap?.get(previousMove)
                moveValue = moveValue?.plus(1)

                if (moveValue != null) penultimateMoveMap?.set(previousMove, moveValue)
                val penultimateOutcome = roundWinner(gamestate.rounds[gamestate.rounds.size - 2])
                val penultimateOutcomeMap = outcomeProbabilityMap.get(penultimateOutcome)
                var outcomeValue = penultimateOutcomeMap?.get(previousMove)
                outcomeValue = outcomeValue?.plus(1)
                if (outcomeValue != null) penultimateOutcomeMap?.set(previousMove, outcomeValue)
            }
        }
    }

    fun opponentPredictor(opponentMoves: List<Move>, gamestate: Gamestate): Move {
        return if (opponentMoves.isNotEmpty()) {
            if (oneMoveStrat(opponentMoves)) {
                winningMove(opponentMoves[0])
            } else {
                heuristicCalc(opponentMoves, gamestate)
            }
        } else {
            randomMove()
        }
    }

    fun randomMove(): Move {
        val moves = listOf<Move>(Move.R, Move.D, Move.W, Move.S, Move.P)
        return moves.shuffled().first()
    }

    fun heuristicCalc(opponentMoves: List<Move>, gamestate: Gamestate): Move {
        val move = moveHeuristic(opponentMoves)
        val turn = turnHeuristic(gamestate)
        var out = if (move.probability >= turn.probability) {
            Choice(winningMove(move.move), move.probability)
        } else {
            Choice(winningMove(turn.move), turn.probability)
        }
        if (out.probability < 0.45 && myDynamite > 0) {
            val rand = Random.nextDouble()
            if (rand > 0.5) {
                out = Choice(Move.D, 1f)
            }
        }
        return out.move
    }

    fun moveHeuristic(opponentMoves: List<Move>): Choice {
        val previousMove = opponentMoves[opponentMoves.size - 1]
        val previousMoveMap = probabilityList[previousMove]
        var moveCount = moveCountMap[previousMove]
        if (opponentDynamite == 0) {
            moveCount = moveCount?.minus(previousMoveMap?.get(Move.D)!!)
            moveCount = moveCount?.minus(previousMoveMap?.get(Move.W)!!)
            previousMoveMap?.set(Move.D, 0)
        }
        return moveSelector(previousMoveMap!!, moveCount!!)


    }

    fun turnHeuristic(gamestate: Gamestate): Choice {
        val previousTurn = roundWinner(gamestate.rounds.last())
        val previousTurnMap = outcomeProbabilityMap[previousTurn]
        var outcomeCount = outcomeCountMap[previousTurn]
        if (opponentDynamite == 0) {
            outcomeCount = outcomeCount?.minus(previousTurnMap?.get(Move.D)!!)
            previousTurnMap?.set(Move.D, 0)
        }
        return moveSelector(previousTurnMap!!, outcomeCount!!)
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

    fun moveSelector(probabilityMap: MutableMap<Move, Int>, totalCount: Int): Choice {
        val move = probabilityMap.maxBy { x -> x.value }
        val prob = (move?.value)?.div(totalCount.toFloat())
        return if (move != null) {
            Choice(move.key, prob!!)
        } else Choice(Move.R, 0.0f)
    }
    // I hate this, returns move based on probability distribution
    /*   fun moveSelector(probabilityMap: MutableMap<Move, Int>, totalCount: Int): Move {
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
       }*/

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

    fun roundWinner(round: Round): Outcome {
        val p1 = round.p1
        val p2 = round.p2
        return when {
            p1 == p2 -> Outcome.D
            p1 == Move.D -> Outcome.W
            p1 != Move.D && p2 == Move.W -> Outcome.W
            p1 == winningMove(p2) -> Outcome.W
            else -> Outcome.L
        }
    }

    init {
        // Are you debugging?
        // Put a breakpoint on the line below to see when we start a new match
        println("Started new match")
    }
}

