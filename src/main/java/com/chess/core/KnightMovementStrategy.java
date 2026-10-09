package com.chess.core;

import com.chess.ports.ChessBoard;
import com.chess.ports.MovementStrategy;
import java.util.ArrayList;
import java.util.List;

public class KnightMovementStrategy implements MovementStrategy {

    private static final int[][] OFFSETS = {
        {2, 1}, {2, -1}, {-2, 1}, {-2, -1},
        {1, 2}, {1, -2}, {-1, 2}, {-1, -2}
    };

    @Override
    public List<Position> getValidMoves(Position from, ChessBoard board) {
        List<Position> moves = new ArrayList<>();
        Piece movingPiece = board.getPieceAt(from);

        for (int[] offset : OFFSETS) {
            int row = from.getRow() + offset[0];
            int col = from.getColumn() + offset[1];

            if (row < 0 || row > 7 || col < 0 || col > 7) {
                continue;
            }

            Position candidate = new Position(row, col);

            if (board.isEmpty(candidate) || board.getPieceAt(candidate).getColor() != movingPiece.getColor()) {
                moves.add(candidate);
            }
        }
        return moves;
    }
}