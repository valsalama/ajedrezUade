package com.chess.core;

import com.chess.ports.ChessBoard;
import com.chess.ports.MovementStrategy;
import java.util.ArrayList;
import java.util.List;

public class QueenMovementStrategy implements MovementStrategy {

    private final MovementStrategy rookStrategy = new RookMovementStrategy();
    private final MovementStrategy bishopStrategy = new BishopMovementStrategy();

    @Override
    public List<Position> getValidMoves(Position from, ChessBoard board) {
        List<Position> moves = new ArrayList<>();
        moves.addAll(rookStrategy.getValidMoves(from, board));
        moves.addAll(bishopStrategy.getValidMoves(from, board));
        return moves;
    }
}