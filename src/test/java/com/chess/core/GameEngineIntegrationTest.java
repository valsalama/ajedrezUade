package com.chess.core;

import com.chess.ports.ChessBoard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

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
        Piece whitePawn = new Pawn(Color.WHITE, new Position(6, 0));
        board.placePiece(whitePawn, new Position(6, 0));

        // Un paso adelante para las blancas (hacia filas más chicas)
        MoveResult result = gameManager.tryMove(new Position(6, 0), new Position(5, 0));
        assertEquals(MoveResult.SUCCESS, result);
    }
}