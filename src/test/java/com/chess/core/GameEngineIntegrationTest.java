package com.chess.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.chess.ports.ChessBoard;

public class GameEngineIntegrationTest {
    private ChessBoard board;
    private GameManager gameManager;

    @BeforeEach
    public void setUp() {
        board = new ArrayChessBoard();
        gameManager = new GameManager(board);
    }

    @Test
    public void testFullGameIntegration() {
        Piece whitePawn = new Pawn(Color.WHITE, new Position(1, 0));
        board.placePiece(whitePawn, new Position(1, 0));

        // Un paso adelante para las blancas (hacia filas más grandes, como en Main.java)
        MoveResult result = gameManager.tryMove(new Position(1, 0), new Position(2, 0));
        assertEquals(MoveResult.SUCCESS, result);
    }
}