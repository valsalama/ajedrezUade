package com.chess.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    public void testTryCastle() {
        Piece king = new King(Color.WHITE, new Position(7, 4));
        Piece rook = new Rook(Color.WHITE, new Position(7, 7));
        board.placePiece(king, new Position(7, 4));
        board.placePiece(rook, new Position(7, 7));

        MoveResult result = gameManager.tryCastle(new Position(7, 4), new Position(7, 7));
        assertEquals(MoveResult.SUCCESS, result);
    }
}