package com.chess.core;

import com.chess.adapters.MediumAIStrategy;
import com.chess.ports.ChessBoard;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MediumAIStrategyTest {
    @Test
    public void testMediumAIPrioritizesCapture() {
        ChessBoard board = new ArrayChessBoard();
        // IA con blancas, rey en esquina
        board.placePiece(new King(Color.WHITE, new Position(0, 0)), new Position(0, 0));
        // Peón enemigo servido para ser capturado en (1,1)
        board.placePiece(new Pawn(Color.BLACK, new Position(1, 1)), new Position(1, 1));
        
        MediumAIStrategy ai = new MediumAIStrategy();
        Move move = ai.getNextMove(board, Color.WHITE);
        
        // Se espera que la IA elija comer el peón negro en lugar de moverse a casillas vacías como (0,1) o (1,0)
        assertNotNull(move);
        assertEquals(1, move.getTo().getRow());
        assertEquals(1, move.getTo().getColumn());
    }
}