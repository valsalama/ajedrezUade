package com.chess.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.chess.ports.ChessBoard;

public class NotationTest {
    private ChessBoard board;
    private GameManager gm;

    @BeforeEach
    public void setUp() {
        board = new ArrayChessBoard();
        gm = new GameManager(board);
    }

    private void put(Piece piece, int r, int c) { board.placePiece(piece, new Position(r, c)); }
    private Position p(int r, int c) { return new Position(r, c); }

    @Test
    public void pawnAndKnightMovesUseSquareNames() {
        put(new Pawn(Color.WHITE, p(1, 4)), 1, 4);
        put(new Knight(Color.BLACK, p(7, 6)), 7, 6);
        gm.tryMove(p(1, 4), p(3, 4));   // e2-e4
        gm.tryMove(p(7, 6), p(5, 5));   // caballo g8-f6
        assertEquals(List.of("e4", "Cf6"), gm.getMoveHistory());
    }

    @Test
    public void captureUsesX() {
        put(new Pawn(Color.WHITE, p(3, 4)), 3, 4);
        put(new Pawn(Color.BLACK, p(4, 3)), 4, 3);
        put(new Rook(Color.BLACK, p(7, 0)), 7, 0);
        put(new Rook(Color.WHITE, p(0, 0)), 0, 0);
        gm.tryMove(p(3, 4), p(4, 3));   // peón e4 come en d5: exd5
        gm.tryMove(p(7, 0), p(0, 0));   // torre negra come torre blanca: Txa1
        assertEquals(List.of("exd5", "Txa1"), gm.getMoveHistory());
    }

    @Test
    public void castlingAndPromotion() {
        put(new King(Color.WHITE, p(0, 4)), 0, 4);
        put(new Rook(Color.WHITE, p(0, 7)), 0, 7);
        put(new Rook(Color.WHITE, p(0, 0)), 0, 0);
        put(new King(Color.BLACK, p(7, 4)), 7, 4);
        put(new Pawn(Color.BLACK, p(1, 0)), 1, 0);  // peón negro a punto de coronar... en a1 hay torre blanca
        gm.tryMove(p(0, 4), p(0, 6));
        assertEquals("O-O", gm.getMoveHistory().get(0));

        ChessBoard b2 = new ArrayChessBoard();
        GameManager g2 = new GameManager(b2);
        b2.placePiece(new King(Color.WHITE, p(0, 4)), p(0, 4));
        b2.placePiece(new Rook(Color.WHITE, p(0, 0)), p(0, 0));
        b2.placePiece(new King(Color.BLACK, p(7, 7)), p(7, 7));
        g2.tryMove(p(0, 4), p(0, 2));
        assertEquals("O-O-O", g2.getMoveHistory().get(0));
    }

    @Test
    public void promotionIsMarked() {
        put(new King(Color.WHITE, p(0, 0)), 0, 0);
        put(new King(Color.BLACK, p(7, 0)), 7, 0);
        put(new Pawn(Color.WHITE, p(6, 5)), 6, 5);
        gm.tryMove(p(6, 5), p(7, 5));
        assertEquals("f8=D+", gm.getMoveHistory().get(0)); // la dama coronada da jaque al rey en a8
    }

    @Test
    public void checkAndCheckmateSuffixes() {
        put(new King(Color.BLACK, p(7, 7)), 7, 7);
        put(new King(Color.WHITE, p(0, 0)), 0, 0);
        put(new Rook(Color.WHITE, p(0, 5)), 0, 5);
        put(new Rook(Color.WHITE, p(1, 0)), 1, 0);
        gm.tryMove(p(0, 5), p(0, 7));   // Th1+  (jaque en columna h, el rey negro puede ir a g8...)
        assertTrue(gm.getMoveHistory().get(0).endsWith("+"), gm.getMoveHistory().get(0));

        ChessBoard b2 = new ArrayChessBoard();
        GameManager g2 = new GameManager(b2);
        b2.placePiece(new King(Color.BLACK, p(7, 7)), p(7, 7));
        b2.placePiece(new King(Color.WHITE, p(0, 0)), p(0, 0));
        b2.placePiece(new Rook(Color.WHITE, p(6, 0)), p(6, 0));
        b2.placePiece(new Rook(Color.WHITE, p(0, 5)), p(0, 5));
        g2.tryMove(p(0, 5), p(7, 5));   // Tf8#: mate de pasillo con la otra torre cubriendo la fila 7
        assertEquals("Tf8#", g2.getMoveHistory().get(0));
    }

    @Test
    public void ambiguousMovesAreDisambiguated() {
        put(new Rook(Color.WHITE, p(0, 0)), 0, 0);   // Ta1
        put(new Rook(Color.WHITE, p(0, 7)), 0, 7);   // Th1
        put(new King(Color.WHITE, p(3, 3)), 3, 3);
        put(new King(Color.BLACK, p(7, 3)), 7, 3);
        gm.tryMove(p(0, 0), p(0, 3));                // las dos torres pueden ir a d1 -> "Tad1"
        assertEquals("Tad1", gm.getMoveHistory().get(0));
    }

    @Test
    public void ambiguousByRowUsesRankNumber() {
        put(new Rook(Color.WHITE, p(0, 0)), 0, 0);   // Ta1
        put(new Rook(Color.WHITE, p(6, 0)), 6, 0);   // Ta7
        put(new King(Color.WHITE, p(3, 7)), 3, 7);
        put(new King(Color.BLACK, p(7, 7)), 7, 7);
        gm.tryMove(p(0, 0), p(3, 0));                // ambas pueden ir a a4, misma columna -> "T1a4"
        assertEquals("T1a4", gm.getMoveHistory().get(0));
    }

    @Test
    public void enPassantUsesPawnCaptureNotation() {
        put(new King(Color.WHITE, p(0, 0)), 0, 0);
        put(new King(Color.BLACK, p(7, 7)), 7, 7);
        put(new Pawn(Color.WHITE, p(4, 4)), 4, 4);
        put(new Pawn(Color.BLACK, p(6, 3)), 6, 3);
        gm.tryMove(p(0, 0), p(0, 1));
        gm.tryMove(p(6, 3), p(4, 3));
        gm.tryMove(p(4, 4), p(5, 3));
        assertEquals("exd6", gm.getMoveHistory().get(2));
    }

    @Test
    public void lastMoveAndHistoryFollowUndo() {
        put(new King(Color.WHITE, p(0, 0)), 0, 0);
        put(new King(Color.BLACK, p(7, 7)), 7, 7);
        put(new Pawn(Color.WHITE, p(1, 3)), 1, 3);
        put(new Pawn(Color.BLACK, p(6, 3)), 6, 3);
        assertNull(gm.getLastMove());

        gm.tryMove(p(1, 3), p(3, 3));
        gm.tryMove(p(6, 3), p(4, 3));
        gm.tryMove(p(0, 0), p(0, 1));
        gm.tryMove(p(7, 7), p(7, 6));
        assertEquals(p(7, 6), gm.getLastMove().getTo());
        assertEquals(p(7, 7), gm.getLastMove().getFrom());
        assertEquals(4, gm.getMoveHistory().size());

        gm.undoLastRound(Color.WHITE);
        assertEquals(2, gm.getMoveHistory().size());
        assertEquals(p(4, 3), gm.getLastMove().getTo());

        gm.reset();
        assertEquals(0, gm.getMoveHistory().size());
        assertNull(gm.getLastMove());
    }
}
