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
        Piece whitePawn = new Pawn(Color.WHITE, new Position(1, 0));
        Piece blackPawn = new Pawn(Color.BLACK, new Position(2, 1));
        board.placePiece(whitePawn, new Position(1, 0));
        board.placePiece(blackPawn, new Position(2, 1));

        // Resultado esperado: Blancas mueven y comen peón negro, cambia el turno y se registra la captura
        MoveResult result = gameManager.tryMove(new Position(1, 0), new Position(2, 1));
        
        assertEquals(MoveResult.SUCCESS, result);
        assertEquals(Color.BLACK, gameManager.getCurrentTurn());
        assertEquals(1, gameManager.getCapturedPieces(Color.WHITE).size());
    }

    @Test
    public void testTryCastle() {
        Piece king = new King(Color.WHITE, new Position(7, 4));
        Piece rook = new Rook(Color.WHITE, new Position(7, 7));
        board.placePiece(king, new Position(7, 4));
        board.placePiece(rook, new Position(7, 7));

        // Resultado esperado: El enroque es exitoso pasando las posiciones del Rey y la Torre
        MoveResult result = gameManager.tryCastle(new Position(7, 4), new Position(7, 7));
        assertEquals(MoveResult.SUCCESS, result);
    }
}