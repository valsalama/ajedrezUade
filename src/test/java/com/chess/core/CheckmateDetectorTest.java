package com.chess.core;

import com.chess.ports.ChessBoard;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CheckmateDetectorTest {
    @Test
    public void testMatePastor() {
        ChessBoard board = new ArrayChessBoard();
        CheckmateDetector checkmateDetector = new CheckmateDetector();
        
        board.placePiece(new King(Color.BLACK, new Position(0, 4)), new Position(0, 4));
        board.placePiece(new Queen(Color.WHITE, new Position(1, 4)), new Position(1, 4));
        board.placePiece(new Bishop(Color.WHITE, new Position(4, 1)), new Position(4, 1));
        
        assertTrue(checkmateDetector.isCheckmate(Color.BLACK, board));
    }
}