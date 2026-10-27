package com.chess.core;

import com.chess.adapters.MediumAIStrategy;
import com.chess.ports.ChessBoard;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MediumAIStrategyTest {
    @Test
    public void testMediumAIPrioritizesCapture() {
        ChessBoard board = new ArrayChessBoard();
        board.placePiece(new King(Color.WHITE, new Position(0, 0)), new Position(0, 0));
        board.placePiece(new Pawn(Color.BLACK, new Position(1, 1)), new Position(1, 1));
        
        MediumAIStrategy ai = new MediumAIStrategy();
        Move move = ai.getNextMove(board, Color.WHITE);
        
        assertNotNull(move);
        assertEquals(1, move.getTo().getRow());
        assertEquals(1, move.getTo().getColumn());
    }
}