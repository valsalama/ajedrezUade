package com.chess.core;

import com.chess.ports.ChessBoard;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CheckDetectorTest {
    @Test
    public void testTorreRivalDaJaqueEnMismaColumna() {
        ChessBoard board = new ArrayChessBoard();
        CheckDetector detector = new CheckDetector();
        
        Piece whiteKing = new King(Color.WHITE, new Position(0, 0));
        Piece blackRook = new Rook(Color.BLACK, new Position(7, 0));
        
        board.placePiece(whiteKing, new Position(0, 0));
        board.placePiece(blackRook, new Position(7, 0));
        
        // Se espera que la torre negra en (7,0) amenace al rey blanco en (0,0)
        assertTrue(detector.isInCheck(Color.WHITE, board));
    }
}