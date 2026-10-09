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
        Piece whitePawn = new Pawn(Color.WHITE, new Position(1, 0));
        board.placePiece(whitePawn, new Position(1, 0));

        // Resultado esperado: Integración básica del motor responde SUCCESS al mover un peón
        MoveResult result = gameManager.tryMove(new Position(1, 0), new Position(2, 0));
        assertEquals(MoveResult.SUCCESS, result);
    }
}