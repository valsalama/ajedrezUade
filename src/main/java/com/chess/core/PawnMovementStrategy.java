package com.chess.core;

import java.util.ArrayList;
import java.util.List;

import com.chess.ports.ChessBoard;
import com.chess.ports.MovementStrategy;

public class PawnMovementStrategy implements MovementStrategy {

    @Override
    public List<Position> getValidMoves(Position from, ChessBoard board) {
        List<Position> moves = new ArrayList<>();
        Piece pawn = board.getPieceAt(from);

        // White starts at rows 0-1 and advances toward row 7 (increasing row).
        // Black starts at rows 6-7 and advances toward row 0 (decreasing row).
        int direction = (pawn.getColor() == Color.WHITE) ? 1 : -1;
        int oneStepRow = from.getRow() + direction;

        if (isInBounds(oneStepRow, from.getColumn())) {
            Position oneStep = new Position(oneStepRow, from.getColumn());

            if (board.isEmpty(oneStep)) {
                moves.add(oneStep);

                int twoStepsRow = from.getRow() + (2 * direction);
                if (!pawn.hasMoved() && isInBounds(twoStepsRow, from.getColumn())) {
                    Position twoSteps = new Position(twoStepsRow, from.getColumn());
                    if (board.isEmpty(twoSteps)) {
                        moves.add(twoSteps);
                    }
                }
            }
        }

        int[] diagonalColumns = {from.getColumn() - 1, from.getColumn() + 1};
        for (int col : diagonalColumns) {
            if (isInBounds(oneStepRow, col)) {
                Position diagonal = new Position(oneStepRow, col);
                if (!board.isEmpty(diagonal) && board.getPieceAt(diagonal).getColor() != pawn.getColor()) {
                    moves.add(diagonal);
                }
            }
        }

        return moves;
    }

    private boolean isInBounds(int row, int col) {
        return row >= 0 && row <= 7 && col >= 0 && col <= 7;
    }
}