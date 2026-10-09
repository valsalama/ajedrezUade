
package com.chess.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PieceTest {

    class DummyPiece extends Piece {
        public DummyPiece(Color color, Position position) {
            this.color = color;
            this.position = position;
            this.type = PieceType.PAWN;
            this.hasMoved = false;
        }
    }

    @Test
    public void testAtributosYPosicion() {
        Piece piece = new DummyPiece(Color.WHITE, new Position(1, 1));
        
        assertEquals(Color.WHITE, piece.getColor());
        assertEquals(PieceType.PAWN, piece.getType());
        assertFalse(piece.hasMoved());
        
        piece.setPosition(new Position(2, 2));
        assertTrue(piece.hasMoved());
        assertEquals(2, piece.getPosition().getRow());
    }
}