package com.chess.adapters;

import com.chess.core.*;
import com.chess.adapters.AIStrategy;
import com.chess.ports.ChessBoard;
import java.util.ArrayList;
import java.util.List;

public class MediumAIStrategy implements AIStrategy {
    private final EasyAIStrategy fallbackStrategy = new EasyAIStrategy();

    @Override
    public Move getNextMove(ChessBoard board, Color color) {
        List<Move> captureMoves = new ArrayList<>();

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Position pos = new Position(r, c);
                Piece piece = board.getPieceAt(pos);
                
                if (piece != null && piece.getColor() == color) {
                    for (Position dest : piece.getPossibleMoves(board)) {
                        if (!board.isEmpty(dest)) {
                            captureMoves.add(new Move(pos, dest));
                        }
                    }
                }
            }
        }

        if (!captureMoves.isEmpty()) {
            return captureMoves.get(0); // Prioriza el primer ataque que encuentra
        }
        
        // Si no puede capturar, mueve al azar
        return fallbackStrategy.getNextMove(board, color);
    }
}