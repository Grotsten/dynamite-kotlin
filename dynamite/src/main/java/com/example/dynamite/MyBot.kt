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
    private var opponentHeuristic = true
    private var opponentMoves = listOf<Move>()
    private var myMoves = listOf<Move>()
    private var opponentDynamite = 100
    private var myDynamite = 100
    private var turnCount = 0
    private var score = 0
    private var losingStreak = 0
    private var winningStreak = 0

    // Distribution of 2 turn patterns
    private var probabilityMap = mutableMapOf<Move, MutableMap<Move, MutableMap<Move, Int>>>(
        Move.R to mutableMapOf(
            Move.R to mutableMapOf(Move.R to 0, Move.P to 0, Move.S to 0, Move.D to 0, Move.W to 0),
            Move.P to mutableMapOf(Move.R to 0, Move.P to 0, Move.S to 0, Move.D to 0, Move.W to 0),
            Move.S to mutableMapOf(Move.R to 0, Move.P to 0, Move.S to 0, Move.D to 0, Move.W to 0),
            Move.D to mutableMapOf(Move.R to 0, Move.P to 0, Move.S to 0, Move.D to 0, Move.W to 0),
            Move.W to mutableMapOf(Move.R to 0, Move.P to 0, Move.S to 0, Move.D to 0, Move.W to 0)
        ),
        Move.P to mutableMapOf(
            Move.R to mutableMapOf(Move.R to 0, Move.P to 0, Move.S to 0, Move.D to 0, Move.W to 0),
            Move.P to mutableMapOf(Move.R to 0, Move.P to 0, Move.S to 0, Move.D to 0, Move.W to 0),
            Move.S to mutableMapOf(Move.R to 0, Move.P to 0, Move.S to 0, Move.D to 0, Move.W to 0),
            Move.D to mutableMapOf(Move.R to 0, Move.P to 0, Move.S to 0, Move.D to 0, Move.W to 0),
            Move.W to mutableMapOf(Move.R to 0, Move.P to 0, Move.S to 0, Move.D to 0, Move.W to 0)
        ),
        Move.S to mutableMapOf(
            Move.R to mutableMapOf(Move.R to 0, Move.P to 0, Move.S to 0, Move.D to 0, Move.W to 0),
            Move.P to mutableMapOf(Move.R to 0, Move.P to 0, Move.S to 0, Move.D to 0, Move.W to 0),
            Move.S to mutableMapOf(Move.R to 0, Move.P to 0, Move.S to 0, Move.D to 0, Move.W to 0),
            Move.D to mutableMapOf(Move.R to 0, Move.P to 0, Move.S to 0, Move.D to 0, Move.W to 0),
            Move.W to mutableMapOf(Move.R to 0, Move.P to 0, Move.S to 0, Move.D to 0, Move.W to 0)
        ),
        Move.D to mutableMapOf(
            Move.R to mutableMapOf(Move.R to 0, Move.P to 0, Move.S to 0, Move.D to 0, Move.W to 0),
            Move.P to mutableMapOf(Move.R to 0, Move.P to 0, Move.S to 0, Move.D to 0, Move.W to 0),
            Move.S to mutableMapOf(Move.R to 0, Move.P to 0, Move.S to 0, Move.D to 0, Move.W to 0),
            Move.D to mutableMapOf(Move.R to 0, Move.P to 0, Move.S to 0, Move.D to 0, Move.W to 0),
            Move.W to mutableMapOf(Move.R to 0, Move.P to 0, Move.S to 0, Move.D to 0, Move.W to 0)
        ),
        Move.W to mutableMapOf(
            Move.R to mutableMapOf(Move.R to 0, Move.P to 0, Move.S to 0, Move.D to 0, Move.W to 0),
            Move.P to mutableMapOf(Move.R to 0, Move.P to 0, Move.S to 0, Move.D to 0, Move.W to 0),
            Move.S to mutableMapOf(Move.R to 0, Move.P to 0, Move.S to 0, Move.D to 0, Move.W to 0),
            Move.D to mutableMapOf(Move.R to 0, Move.P to 0, Move.S to 0, Move.D to 0, Move.W to 0),
            Move.W to mutableMapOf(Move.R to 0, Move.P to 0, Move.S to 0, Move.D to 0, Move.W to 0)
        )
    )

    // Distribution of 1 turn patterns
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

    // for trying to work out the opponents strategy
    private var myProbabilityList = mutableMapOf<Move, MutableMap<Move, Int>>(
        Move.R to mutableMapOf(Move.R to 0, Move.P to 0, Move.S to 0, Move.D to 0, Move.W to 0),
        Move.P to mutableMapOf(Move.R to 0, Move.P to 0, Move.S to 0, Move.D to 0, Move.W to 0),
        Move.S to mutableMapOf(Move.R to 0, Move.P to 0, Move.S to 0, Move.D to 0, Move.W to 0),
        Move.D to mutableMapOf(Move.R to 0, Move.P to 0, Move.S to 0, Move.D to 0, Move.W to 0),
        Move.W to mutableMapOf(Move.R to 0, Move.P to 0, Move.S to 0, Move.D to 0, Move.W to 0)
    )
    private var myMoveCountMap =
        mutableMapOf<Move, Int>(Move.R to 0, Move.P to 0, Move.S to 0, Move.D to 0, Move.W to 0)

    override fun makeMove(gamestate: Gamestate): Move {
        roundProcessor(gamestate)
        turnCount += 1
        var move = opponentPredictor(opponentMoves, myMoves, gamestate)

        if (myDynamite == 0) {
            print("Out of Dynamite")
            myDynamite -= 1
            if (move == Move.D) {
                move = Move.R
            }
        }
        if (gamestate.rounds.isNotEmpty()) {
            if (gamestate.rounds.last().p1 == Move.D && move == Move.D) {
                while (move == Move.D) move = opponentPredictor(opponentMoves, myMoves, gamestate)
            }
        }
        if (move == Move.D) {
            myDynamite -= 1
        }
        myMoves = myMoves + move
        return move
        // Are you debugging?
        // Put a breakpoint in this method to see when we make a move
    }

    fun roundProcessor(gamestate: Gamestate) {
        if (gamestate.rounds.isNotEmpty()) {
            val previousMove: Move = gamestate.rounds.last().p2
            val myPreviousMove: Move = gamestate.rounds.last().p1
            if (opponentMoves.size <= 2) {

                val previousOutcome = roundWinner(gamestate.rounds[gamestate.rounds.size - 1])
                if (previousMove == Move.D) {
                    opponentDynamite -= 1
                }
                if (previousOutcome == Outcome.D) {
                    score += 1
                } else {
                    score = 0
                }
                opponentMoves = opponentMoves + previousMove
                moveCountMap[previousMove] = moveCountMap[previousMove]?.plus(1)!!
                outcomeCountMap[previousOutcome] = outcomeCountMap[previousOutcome]?.plus(1)!!
                if (previousOutcome == Outcome.L) {
                    losingStreak += 1
                } else {
                    losingStreak = 0
                }
                if (previousMove == Outcome.W) {
                    winningStreak += 1
                } else {
                    winningStreak = 0
                }

            }
            if (opponentMoves.size > 1) {
                opponentPenultimateProcessor(gamestate, previousMove)
                myPenultimateProcessor(gamestate, previousMove)
            }
            if (opponentMoves.size > 2) {
                val previousMove: Move = gamestate.rounds.last().p2
                val myPreviousMove: Move = gamestate.rounds.last().p1
                val previousOutcome = roundWinner(gamestate.rounds[gamestate.rounds.size - 1])
                val penultimateMove: Move = gamestate.rounds[gamestate.rounds.size - 2].p2
                val thirdMove: Move = gamestate.rounds[gamestate.rounds.size - 3].p2
                val thirdMoveMap = probabilityMap[thirdMove]
                val penultimateMoveMap = thirdMoveMap?.get(penultimateMove)
                var previousMoveValue = penultimateMoveMap?.get(previousMove)!!
                penultimateMoveMap[previousMove] = previousMoveValue + 1
                if (previousMove == Move.D) {
                    opponentDynamite -= 1
                }
                if (previousOutcome == Outcome.D) {
                    score += 1
                } else {
                    score = 0
                }
                opponentMoves = opponentMoves + previousMove
                moveCountMap[previousMove] = moveCountMap[previousMove]?.plus(1)!!
                outcomeCountMap[previousOutcome] = outcomeCountMap[previousOutcome]?.plus(1)!!
                if (previousOutcome == Outcome.L) {
                    losingStreak += 1
                } else {
                    losingStreak = 0
                }
                if (previousMove == Outcome.W) {
                    winningStreak += 1
                } else {
                    winningStreak = 0
                }
            }
        }
    }

    fun opponentPenultimateProcessor(gamestate: Gamestate, previousMove: Move) {
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
        moveHeuristicStrat(gamestate)
    }

    fun myPenultimateProcessor(gamestate: Gamestate, previousMove: Move) {
        val penultimateMove: Move = gamestate.rounds[gamestate.rounds.size - 2].p1
        val penultimateMoveMap: MutableMap<Move, Int>? =
            myProbabilityList.get(penultimateMove)
        var moveValue = penultimateMoveMap?.get(previousMove)
        moveValue = moveValue?.plus(1)
        if (moveValue != null) penultimateMoveMap?.set(previousMove, moveValue)
    }

    fun opponentPredictor(
        opponentMoves: List<Move>,
        myMoves: List<Move>,
        gamestate: Gamestate
    ): Move {
        if (opponentMoves.isNotEmpty()) {
            return if (opponentMoves.size > 1) {
                when {
                    oneMoveStrat(opponentMoves) -> winningMove(opponentMoves[0])
                    /*opponentHeuristic && turnCount >10 -> winningMove(
                            moveHeuristic(
                                myMoves,
                                myProbabilityList,
                                myMoveCountMap
                            ).move
                        )*/
                    else -> heuristicCalc(opponentMoves, myMoves, gamestate)
                }
            } else {
                randomMove()
            }
        } else {
            return randomMove()
        }
    }

    fun randomMove(): Move {
        val moves = listOf<Move>(Move.R, Move.D, Move.S, Move.P)
        return moves.shuffled().first()
    }

    fun heuristicCalc(opponentMoves: List<Move>, myMoves: List<Move>, gamestate: Gamestate): Move {
        var move: Choice = when {
            turnCount < 50 -> moveHeuristic(myMoves, myProbabilityList, myMoveCountMap)
            else -> moveHeuristic2(opponentMoves)
        }

        val turn = turnHeuristic(gamestate)
        var out = if (move.probability >= turn.probability) {
            Choice(winningMove(move.move), move.probability)
        } else {
            Choice(winningMove(turn.move), turn.probability)
        }
        if (losingStreak > 3) {
            var rand = listOf<Int>(0, 1, 2)
            if (myDynamite > 0) {
                when (rand.shuffled().first()) {
                    0 -> out = Choice(Move.D, 0.3f)
                    1 -> out = Choice(winningMove(winningMove(out.move)), out.probability)
                }
            } else {
                if (rand.shuffled().first() == 0) {
                    out = Choice(winningMove(winningMove(out.move)), out.probability)
                }
            }
        }
        if (myDynamite > 0) {
            if (score > 2) {
                out = Choice(Move.D, 1f)
            } else if (score > 0 && out.probability < 0.35) {
                var rand = listOf<Int>(0, 1, 2)
                if (rand.shuffled().first() == 0) {
                    out = Choice(Move.D, 1f)
                }
            }
        }
        if (winningStreak > 1) {
            if (out.move == Move.D) {
                out = Choice(heuristicCalc(opponentMoves, myMoves, gamestate), 0f)
            }
        }

        return out.move
    }

    fun moveHeuristic(
        myMoves: List<Move>,
        probabilityMap: MutableMap<Move, MutableMap<Move, Int>>?,
        moveCountMap: MutableMap<Move, Int>
    ): Choice {
        val previousMove = myMoves[myMoves.size - 1]
        val previousMoveMap = probabilityMap?.get(previousMove)
        var moveCount = moveCountMap[previousMove]
        if (opponentDynamite == 0) {
            moveCount = moveCount?.minus(previousMoveMap?.get(Move.D)!!)
            moveCount = moveCount?.minus(previousMoveMap?.get(Move.W)!!)
            previousMoveMap?.set(Move.D, 0)
        }
        return moveSelector(previousMoveMap!!, moveCount!!)
    }

    fun moveHeuristic2(opponentMoves: List<Move>): Choice {
        val penultimateMove = opponentMoves[opponentMoves.size - 2]
        val penultimateMoveMap = probabilityMap[penultimateMove]
        val previousMove = opponentMoves.last()
        val previousMoveMap = penultimateMoveMap?.get(previousMove)
        if (opponentDynamite == 0) {
            previousMoveMap?.set(Move.D, 0)
        }

        return moveSelector(
            previousMoveMap!!,
            probabilityList[penultimateMove]?.get(previousMove)!!
        )
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

    fun moveSelector(probabilityMap: MutableMap<Move, Int>, totalCount: Int): Choice {
        val move = probabilityMap.maxBy { x -> x.value }
        val prob = (move?.value)?.div(totalCount.toFloat())
        return if (move != null) {
            Choice(move.key, prob!!)

        } else Choice(Move.R, 0.0f)
    }
    // I hate this, returns move based on probability distribution

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

    fun moveHeuristicStrat(gamestate: Gamestate): Boolean {
        var previousMove = gamestate.rounds.last().p2
        if (opponentHeuristic) {
            opponentHeuristic = previousMove == moveHeuristic(
                myMoves,
                myProbabilityList,
                myMoveCountMap
            ).move || previousMove == Move.D

        }
        return opponentHeuristic
    }

    init {
        // Are you debugging?
        // Put a breakpoint on the line below to see when we start a new match
        println("Started new match")
    }
}

