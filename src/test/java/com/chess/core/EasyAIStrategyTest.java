package com.chess.core;

import com.chess.adapters.EasyAIStrategy;
import com.chess.ports.ChessBoard;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class EasyAIStrategyTest {
    @Test
    public void testEasyAIAlwaysReturnsValidMove() {
        ChessBoard board = new ArrayChessBoard();
        board.placePiece(new Pawn(Color.BLACK, new Position(6, 0)), new Position(6, 0));
        EasyAIStrategy ai = new EasyAIStrategy();
        
        // Se espera que tras 100 iteraciones, siempre encuentre un movimiento válido
        for (int i = 0; i < 100; i++) {
            Move move = ai.getNextMove(board, Color.BLACK);
            assertNotNull(move);
            assertTrue(move.getFrom().getRow() == 6);
        }
    }
}