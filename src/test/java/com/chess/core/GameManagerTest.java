package com.chess.core;

import com.chess.ports.ChessBoard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

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
        Piece whitePawn = new Pawn(Color.WHITE, new Position(6, 0));
        Piece blackPawn = new Pawn(Color.BLACK, new Position(5, 1));
        board.placePiece(whitePawn, new Position(6, 0));
        board.placePiece(blackPawn, new Position(5, 1));

        // Blancas mueven en diagonal y comen el peón negro; cambia el turno y se registra la captura
        MoveResult result = gameManager.tryMove(new Position(6, 0), new Position(5, 1));

        assertEquals(MoveResult.SUCCESS, result);
        assertEquals(Color.BLACK, gameManager.getCurrentTurn());
        assertEquals(1, gameManager.getCapturedPieces(Color.WHITE).size());
    }

    @Test
    public void testPawnCannotMoveDiagonallyWithoutCapturing() {
        board.placePiece(new Pawn(Color.WHITE, new Position(6, 1)), new Position(6, 1));

        MoveResult result = gameManager.tryMove(new Position(6, 1), new Position(5, 0));

        assertEquals(MoveResult.INVALID_MOVE, result);
        assertEquals(Color.WHITE, gameManager.getCurrentTurn());   // el turno no cambia
    }

    @Test
    public void testPawnCanDoubleStepFromStart() {
        board.placePiece(new Pawn(Color.WHITE, new Position(6, 1)), new Position(6, 1));

        assertEquals(MoveResult.SUCCESS, gameManager.tryMove(new Position(6, 1), new Position(4, 1)));
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