package com.chess.core;

import com.chess.ports.ChessBoard;
import com.chess.ports.MovementStrategy;
import java.util.ArrayList;
import java.util.List;

public class KingMovementStrategy implements MovementStrategy {

    private final QueenMovementStrategy queenLikeStrategy = new QueenMovementStrategy();

    @Override
    public List<Position> getValidMoves(Position from, ChessBoard board) {
        List<Position> onlyAdjacent = new ArrayList<>();

        for (Position candidate : queenLikeStrategy.getValidMoves(from, board)) {
            int rowDistance = Math.abs(candidate.getRow() - from.getRow());
            int colDistance = Math.abs(candidate.getColumn() - from.getColumn());

            if (Math.max(rowDistance, colDistance) == 1) {
                onlyAdjacent.add(candidate);
            }
        }
        return onlyAdjacent;
    }
}