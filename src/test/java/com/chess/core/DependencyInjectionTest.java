package com.chess.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.chess.adapters.AIStrategy;
import com.chess.adapters.EasyAIStrategy;
import com.chess.adapters.MediumAIStrategy;
import com.chess.ports.ChessBoard;
import com.chess.ports.MovementStrategy;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class DependencyInjectionTest {

    @Test
    public void pieceUsesTheInjectedMovementStrategy() {
        MovementStrategy fixed = (from, board) -> List.of(new Position(4, 4));
        Knight knight = new Knight(Color.WHITE, new Position(0, 1), fixed);

        assertSame(fixed, knight.getMovementStrategy());
        assertEquals(List.of(new Position(4, 4)), knight.getPossibleMoves(new ArrayChessBoard()));
    }

    @Test
    public void shortConstructorStillUsesTheDefaultStrategy() {
        Knight knight = new Knight(Color.WHITE, new Position(0, 1));

        assertTrue(knight.getMovementStrategy() instanceof KnightMovementStrategy);
    }

    @Test
    public void queenCombinesTheStrategiesItReceives() {
        MovementStrategy rook = (from, board) -> List.of(new Position(1, 1));
        MovementStrategy bishop = (from, board) -> List.of(new Position(2, 2));

        List<Position> moves = new QueenMovementStrategy(rook, bishop)
                .getValidMoves(new Position(0, 0), new ArrayChessBoard());

        assertEquals(List.of(new Position(1, 1), new Position(2, 2)), moves);
    }

    @Test
    public void gameManagerUsesTheInjectedCheckDetector() {
        ChessBoard board = new ArrayChessBoard();
        board.placePiece(new King(Color.WHITE, new Position(0, 4)), new Position(0, 4));
        board.placePiece(new King(Color.BLACK, new Position(7, 4)), new Position(7, 4));
        List<Color> asked = new ArrayList<>();
        CheckDetector spy = new CheckDetector() {
            @Override
            public boolean isInCheck(Color color, ChessBoard b) {
                asked.add(color);
                return super.isInCheck(color, b);
            }
        };

        GameManager manager = new GameManager(board, spy, new MoveValidator());
        manager.getGameState();

        assertTrue(!asked.isEmpty());
    }

    @Test
    public void mediumAiFallsBackToTheInjectedStrategy() {
        Move expected = new Move(new Position(1, 0), new Position(2, 0));
        AIStrategy fallback = (board, color) -> expected;
        ChessBoard board = new ArrayChessBoard();
        board.placePiece(new Pawn(Color.BLACK, new Position(6, 0)), new Position(6, 0));

        Move chosen = new MediumAIStrategy(fallback).getNextMove(board, Color.BLACK);

        assertSame(expected, chosen);
    }

    @Test
    public void easyAiImplementsTheAiPort() {
        AIStrategy ai = new EasyAIStrategy();

        assertTrue(ai instanceof EasyAIStrategy);
    }
}
