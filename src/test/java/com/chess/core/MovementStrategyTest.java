package com.chess.core;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.chess.ports.ChessBoard;

public class MovementStrategyTest {

    static class DummyPiece extends Piece {
        DummyPiece(Color color, Position position) {
            this.color = color;
            this.position = position;
        }
    }

    @Test
    void rookOnEmptyBoardFromCenterHas14Moves() {
        ChessBoard board = new ArrayChessBoard();
        Position center = new Position(3, 3);
        board.placePiece(new DummyPiece(Color.WHITE, center), center);
        assertEquals(14, new RookMovementStrategy().getValidMoves(center, board).size());
    }

    @Test
    void rookStopsBeforeOwnPiece() {
        ChessBoard board = new ArrayChessBoard();
        Position rookPos = new Position(3, 3);
        Position blockerPos = new Position(3, 5);
        board.placePiece(new DummyPiece(Color.WHITE, rookPos), rookPos);
        board.placePiece(new DummyPiece(Color.WHITE, blockerPos), blockerPos);
        List<Position> moves = new RookMovementStrategy().getValidMoves(rookPos, board);
        assertFalse(moves.contains(blockerPos));
        assertTrue(moves.contains(new Position(3, 4)));
    }

    @Test
    void bishopOnEmptyBoardFromCenterHas13Moves() {
        ChessBoard board = new ArrayChessBoard();
        Position center = new Position(3, 3);
        board.placePiece(new DummyPiece(Color.WHITE, center), center);
        assertEquals(13, new BishopMovementStrategy().getValidMoves(center, board).size());
    }

    @Test
    void knightOnEmptyBoardFromCenterHas8Moves() {
        ChessBoard board = new ArrayChessBoard();
        Position center = new Position(3, 3);
        board.placePiece(new DummyPiece(Color.WHITE, center), center);
        assertEquals(8, new KnightMovementStrategy().getValidMoves(center, board).size());
    }

    @Test
    void knightInCornerHas2Moves() {
        ChessBoard board = new ArrayChessBoard();
        Position corner = new Position(0, 0);
        board.placePiece(new DummyPiece(Color.WHITE, corner), corner);
        assertEquals(2, new KnightMovementStrategy().getValidMoves(corner, board).size());
    }

    @Test
    void pawnThatHasNotMovedHas2ForwardMoves() {
        ChessBoard board = new ArrayChessBoard();
        // Fila 1: ahí es donde Main.java/WebMain.java arrancan los peones blancos.
        Position pawnPos = new Position(1, 0);
        board.placePiece(new DummyPiece(Color.WHITE, pawnPos), pawnPos);
        assertEquals(2, new PawnMovementStrategy().getValidMoves(pawnPos, board).size());
    }

    @Test
    void pawnThatAlreadyMovedHas1ForwardMove() {
        ChessBoard board = new ArrayChessBoard();
        Position pawnPos = new Position(1, 0);
        Piece pawn = new DummyPiece(Color.WHITE, pawnPos);
        board.placePiece(pawn, pawnPos);
        pawn.setPosition(pawnPos);
        assertEquals(1, new PawnMovementStrategy().getValidMoves(pawnPos, board).size());
    }

    @Test
    void pawnCanCaptureDiagonally() {
        ChessBoard board = new ArrayChessBoard();
        Position pawnPos = new Position(1, 3);
        Position enemyPos = new Position(2, 4);
        board.placePiece(new DummyPiece(Color.WHITE, pawnPos), pawnPos);
        board.placePiece(new DummyPiece(Color.BLACK, enemyPos), enemyPos);
        assertTrue(new PawnMovementStrategy().getValidMoves(pawnPos, board).contains(enemyPos));
    }

    @Test
    void queenOnEmptyBoardFromCenterHas27Moves() {
        ChessBoard board = new ArrayChessBoard();
        Position center = new Position(3, 3);
        board.placePiece(new DummyPiece(Color.WHITE, center), center);
        assertEquals(27, new QueenMovementStrategy().getValidMoves(center, board).size());
    }

    @Test
    void kingOnEmptyBoardFromCenterHas8Moves() {
        ChessBoard board = new ArrayChessBoard();
        Position center = new Position(3, 3);
        board.placePiece(new DummyPiece(Color.WHITE, center), center);
        assertEquals(8, new KingMovementStrategy().getValidMoves(center, board).size());
    }

    @Test
    void kingInCornerHas3Moves() {
        ChessBoard board = new ArrayChessBoard();
        Position corner = new Position(0, 0);
        board.placePiece(new DummyPiece(Color.WHITE, corner), corner);
        assertEquals(3, new KingMovementStrategy().getValidMoves(corner, board).size());
    }
}