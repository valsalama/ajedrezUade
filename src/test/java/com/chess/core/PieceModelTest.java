package com.chess.core;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import com.chess.ports.ChessBoard;

public class PieceModelTest {

    private ChessBoard createEmptyBoard() {
        return new ChessBoard() {
            @Override public Piece getPieceAt(Position p) { return null; }
            @Override public void placePiece(Piece p, Position pos) {}
            @Override public void removePiece(Position p) {}
            @Override public boolean isEmpty(Position p) { return true; }
            @Override public ChessBoard clone() { return this; }
        };
    }

    @Test
    public void testPawnDelegatesToStrategy() {
        ChessBoard board = new ArrayChessBoard();
        Piece pawn = new Pawn(Color.WHITE, new Position(1, 0));
        board.placePiece(pawn, new Position(1, 0));
        List<Position> moves = pawn.getPossibleMoves(board);
        assertEquals(2, moves.size());
    }

    @Test
    public void testKingDelegatesToStrategy() {
        Piece king = new King(Color.WHITE, new Position(4, 4));
        List<Position> moves = king.getPossibleMoves(createEmptyBoard());
        assertEquals(8, moves.size());
    }
}