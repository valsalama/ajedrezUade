package com.chess.core;

import com.chess.ports.ChessBoard;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class PieceModelTest {

    // Se asume la existencia de un ArrayChessBoard funcional. Si se llama diferente, ajustarlo.
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
        Piece pawn = new Pawn(Color.WHITE, new Position(6, 0));
        board.placePiece(pawn, new Position(6, 0));
        List<Position> moves = pawn.getPossibleMoves(board);
        // Se espera que el peón blanco sin mover en tablero vacío retorne 2 movimientos (avance simple y doble).
        assertEquals(2, moves.size());
    }

    @Test
    public void testKingDelegatesToStrategy() {
        Piece king = new King(Color.WHITE, new Position(4, 4));
        List<Position> moves = king.getPossibleMoves(createEmptyBoard());
        // Se espera que el rey en el centro de un tablero vacío retorne 8 movimientos.
        assertEquals(8, moves.size());
    }
}