package com.chess.core;

import com.chess.ports.ChessBoard;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MoveValidatorTest {

    @Test
    public void testMoveValidatorRejectsSameColor() {
        ChessBoard board = new ArrayChessBoard();
        MoveValidator validator = new MoveValidator();
        
        Piece whiteRook = new Rook(Color.WHITE, new Position(0,0));
        Piece whitePawn = new Pawn(Color.WHITE, new Position(0,1));
        
        board.placePiece(whiteRook, new Position(0,0));
        board.placePiece(whitePawn, new Position(0,1));

        // Resultado esperado: Retorna falso porque no puede comer a su propia pieza
        assertFalse(validator.isValidMove(board, whiteRook, new Position(0,1)));
    }
}