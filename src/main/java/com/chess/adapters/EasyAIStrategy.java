package com.chess.adapters;

import com.chess.core.*;
import com.chess.adapters.AIStrategy;
import com.chess.ports.ChessBoard;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class EasyAIStrategy implements AIStrategy {
    private Random random = new Random();

    @Override
    public Move getNextMove(ChessBoard board, Color color) {
        List<Move> validMoves = new ArrayList<>();
        
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Position pos = new Position(r, c);
                Piece piece = board.getPieceAt(pos);
                if (piece != null && piece.getColor() == color) {
                    for (Position dest : piece.getPossibleMoves(board)) {
                        validMoves.add(new Move(pos, dest));
                    }
                }
            }
        }
        if (validMoves.isEmpty()) return null;
        return validMoves.get(random.nextInt(validMoves.size()));
    }
}