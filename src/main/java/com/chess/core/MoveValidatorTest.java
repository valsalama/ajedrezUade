package chess;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class MoveValidatorTest {

    private final MoveValidator validator = new MoveValidator();

    @Test
    void acceptsValidMove() {
        ChessBoard board = new ArrayChessBoard();
        Piece rook = new Rook(Color.WHITE, new Position(4, 4));
        board.placePiece(rook, new Position(4, 4));

        assertTrue(validator.isValidMove(board, rook, new Position(4, 7)));
    }

    @Test
    void rejectsCaptureOfOwnPiece() {
        ChessBoard board = new ArrayChessBoard();
        Piece rook = new Rook(Color.WHITE, new Position(4, 4));
        Piece ownPawn = new Pawn(Color.WHITE, new Position(4, 6));
        board.placePiece(rook, new Position(4, 4));
        board.placePiece(ownPawn, new Position(4, 6));

        assertFalse(validator.isValidMove(board, rook, new Position(4, 6)));
    }

    @Test
    void rejectsMoveOutsidePossibleMoves() {
        ChessBoard board = new ArrayChessBoard();
        Piece rook = new Rook(Color.WHITE, new Position(4, 4));
        board.placePiece(rook, new Position(4, 4));

        // la torre no se mueve en diagonal
        assertFalse(validator.isValidMove(board, rook, new Position(5, 5)));
    }
package chess;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class MoveValidatorTest {

    private final MoveValidator validator = new MoveValidator();

    @Test
    void acceptsValidMove() {
        ChessBoard board = new ArrayChessBoard();
        Piece rook = new Rook(Color.WHITE, new Position(4, 4));
        board.placePiece(rook, new Position(4, 4));

        assertTrue(validator.isValidMove(board, rook, new Position(4, 7)));
    }

    @Test
    void rejectsCaptureOfOwnPiece() {
        ChessBoard board = new ArrayChessBoard();
        Piece rook = new Rook(Color.WHITE, new Position(4, 4));
        Piece ownPawn = new Pawn(Color.WHITE, new Position(4, 6));
        board.placePiece(rook, new Position(4, 4));
        board.placePiece(ownPawn, new Position(4, 6));

        assertFalse(validator.isValidMove(board, rook, new Position(4, 6)));
    }

    @Test
    void rejectsMoveOutsidePossibleMoves() {
        ChessBoard board = new ArrayChessBoard();
        Piece rook = new Rook(Color.WHITE, new Position(4, 4));
        board.placePiece(rook, new Position(4, 4));

        // la torre no se mueve en diagonal
        assertFalse(validator.isValidMove(board, rook, new Position(5, 5)));
    }
}