package com.chess.core;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.chess.ports.ChessBoard;

public class GameManagerTest {
    private ChessBoard board;
    private GameManager gameManager;

    @BeforeEach
    public void setUp() {
        board = new ArrayChessBoard();
        gameManager = new GameManager(board);
    }

    @Test
    public void testTurnAlternationAndCapture() {
        // Fila 1 (blancas) y fila 2 (negras): el layout real de Main.java/WebMain.java
        // tiene a blancas arrancando en las filas 0-1, avanzando hacia filas más grandes.
        Piece whitePawn = new Pawn(Color.WHITE, new Position(1, 0));
        Piece blackPawn = new Pawn(Color.BLACK, new Position(2, 1));
        board.placePiece(whitePawn, new Position(1, 0));
        board.placePiece(blackPawn, new Position(2, 1));

        // Blancas mueven en diagonal y comen el peón negro; cambia el turno y se registra la captura
        MoveResult result = gameManager.tryMove(new Position(1, 0), new Position(2, 1));

        assertEquals(MoveResult.SUCCESS, result);
        assertEquals(Color.BLACK, gameManager.getCurrentTurn());
        assertEquals(1, gameManager.getCapturedPieces(Color.WHITE).size());
    }

    @Test
    public void testPawnCannotMoveDiagonallyWithoutCapturing() {
        board.placePiece(new Pawn(Color.WHITE, new Position(1, 1)), new Position(1, 1));

        MoveResult result = gameManager.tryMove(new Position(1, 1), new Position(2, 0));

        assertEquals(MoveResult.INVALID_MOVE, result);
        assertEquals(Color.WHITE, gameManager.getCurrentTurn());   // el turno no cambia
    }

    @Test
    public void testPawnCanDoubleStepFromStart() {
        board.placePiece(new Pawn(Color.WHITE, new Position(1, 1)), new Position(1, 1));

        assertEquals(MoveResult.SUCCESS, gameManager.tryMove(new Position(1, 1), new Position(3, 1)));
    }

    @Test
    public void testLegalMovesExcludeMovesThatDoNotResolveCheck() {
        board.placePiece(new King(Color.WHITE, new Position(4, 4)), new Position(4, 4));
        board.placePiece(new Rook(Color.WHITE, new Position(0, 5)), new Position(0, 5));
        board.placePiece(new Rook(Color.BLACK, new Position(4, 7)), new Position(4, 7));

        List<Position> legalMoves = gameManager.getLegalMoves(new Position(0, 5));

        assertFalse(legalMoves.contains(new Position(0, 6)));
        assertTrue(legalMoves.contains(new Position(4, 5)));
        assertEquals(MoveResult.INVALID_MOVE,
                gameManager.tryMove(new Position(0, 5), new Position(0, 6)));
    }

    @Test
    public void testTryCastle() {
        Piece king = new King(Color.WHITE, new Position(7, 4));
        Piece rook = new Rook(Color.WHITE, new Position(7, 7));
        board.placePiece(king, new Position(7, 4));
        board.placePiece(rook, new Position(7, 7));

        MoveResult result = gameManager.tryCastle(new Position(7, 4), new Position(7, 7));
        assertEquals(MoveResult.SUCCESS, result);
    }

    @Test
    public void testUndoRestoresBoardTurnAndCapture() {
        Piece whitePawn = new Pawn(Color.WHITE, new Position(1, 0));
        Piece blackPawn = new Pawn(Color.BLACK, new Position(2, 1));
        board.placePiece(whitePawn, new Position(1, 0));
        board.placePiece(blackPawn, new Position(2, 1));

        gameManager.tryMove(new Position(1, 0), new Position(2, 1));
        gameManager.undoLastMove();

        // Todo vuelve a como estaba: las dos piezas en su lugar, turno de blancas y sin capturas
        assertEquals(whitePawn, board.getPieceAt(new Position(1, 0)));
        assertEquals(blackPawn, board.getPieceAt(new Position(2, 1)));
        assertEquals(Color.WHITE, gameManager.getCurrentTurn());
        assertEquals(0, gameManager.getCapturedPieces(Color.WHITE).size());
        assertFalse(gameManager.canUndo());
    }

    @Test
    public void testPawnCanDoubleStepAgainAfterUndo() {
        board.placePiece(new Pawn(Color.WHITE, new Position(1, 1)), new Position(1, 1));

        gameManager.tryMove(new Position(1, 1), new Position(2, 1));
        gameManager.undoLastMove();

        // Si no se restaurara hasMoved, el peón ya no podría avanzar dos casillas
        assertEquals(MoveResult.SUCCESS, gameManager.tryMove(new Position(1, 1), new Position(3, 1)));
    }

    @Test
    public void testUndoCastleMovesKingAndRookBack() {
        Piece king = new King(Color.WHITE, new Position(7, 4));
        Piece rook = new Rook(Color.WHITE, new Position(7, 7));
        board.placePiece(king, new Position(7, 4));
        board.placePiece(rook, new Position(7, 7));

        gameManager.tryCastle(new Position(7, 4), new Position(7, 7));
        gameManager.undoLastMove();

        assertEquals(king, board.getPieceAt(new Position(7, 4)));
        assertEquals(rook, board.getPieceAt(new Position(7, 7)));
        assertFalse(king.hasMoved());
        assertFalse(rook.hasMoved());
    }

    @Test
    public void testResetClearsTurnCapturesAndHistory() {
        board.placePiece(new Pawn(Color.WHITE, new Position(1, 0)), new Position(1, 0));
        board.placePiece(new Pawn(Color.BLACK, new Position(2, 1)), new Position(2, 1));
        gameManager.tryMove(new Position(1, 0), new Position(2, 1));

        gameManager.reset();

        assertEquals(Color.WHITE, gameManager.getCurrentTurn());
        assertEquals(0, gameManager.getCapturedPieces(Color.WHITE).size());
        assertFalse(gameManager.canUndo());
    }
}