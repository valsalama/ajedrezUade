package com.chess.core;

import com.chess.ports.ChessBoard;
import com.chess.ports.MovementStrategy;
import java.util.ArrayList;
import java.util.List;

public class RookMovementStrategy implements MovementStrategy {

    private static final int[][] DIRECTIONS = {
        {1, 0}, {-1, 0}, {0, 1}, {0, -1}
    };

    @Override
    public List<Position> getValidMoves(Position from, ChessBoard board) {
        List<Position> moves = new ArrayList<>();
        Piece movingPiece = board.getPieceAt(from);

        for (int[] direction : DIRECTIONS) {
            int row = from.getRow();
            int col = from.getColumn();

            while (true) {
                row += direction[0];
                col += direction[1];

                if (row < 0 || row > 7 || col < 0 || col > 7) {
                    break;
                }

                Position candidate = new Position(row, col);

                if (board.isEmpty(candidate)) {
                    moves.add(candidate);
                    continue;
                }

                Piece pieceThere = board.getPieceAt(candidate);
                if (pieceThere.getColor() != movingPiece.getColor()) {
                    moves.add(candidate);
                }
                break;
            }
        }
        return moves;
    }
}