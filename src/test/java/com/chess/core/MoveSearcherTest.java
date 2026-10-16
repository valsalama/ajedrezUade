package com.chess.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.chess.ports.ChessBoard;

public class MoveSearcherTest {
    private ChessBoard board;
    private GameManager gm;

    @BeforeEach
    public void setUp() {
        board = new ArrayChessBoard();
        gm = new GameManager(board);
    }

    private void put(Piece piece, int r, int c) { board.placePiece(piece, new Position(r, c)); }
    private Position p(int r, int c) { return new Position(r, c); }

    private void standardSetup() {
        Color[] sides = {Color.WHITE, Color.BLACK};
        for (Color color : sides) {
            int back = color == Color.WHITE ? 0 : 7;
            int pawns = color == Color.WHITE ? 1 : 6;
            put(new Rook(color, p(back, 0)), back, 0);
            put(new Knight(color, p(back, 1)), back, 1);
            put(new Bishop(color, p(back, 2)), back, 2);
            put(new Queen(color, p(back, 3)), back, 3);
            put(new King(color, p(back, 4)), back, 4);
            put(new Bishop(color, p(back, 5)), back, 5);
            put(new Knight(color, p(back, 6)), back, 6);
            put(new Rook(color, p(back, 7)), back, 7);
            for (int i = 0; i < 8; i++) put(new Pawn(color, p(pawns, i)), pawns, i);
        }
    }

    private String fingerprint() {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < 8; r++) for (int c = 0; c < 8; c++) {
            Piece x = board.getPieceAt(p(r, c));
            sb.append(x == null ? "." : x.getId().substring(0, 4) + x.getType().name().charAt(0) + x.getColor().name().charAt(0) + (x.hasMoved() ? "m" : "u"));
            sb.append(',');
        }
        sb.append(gm.getCurrentTurn()).append(gm.getMoveHistory()).append(gm.canUndo())
          .append(gm.getCapturedPieces(Color.WHITE).size()).append(gm.getCapturedPieces(Color.BLACK).size());
        return sb.toString();
    }

    @Test
    public void searchLeavesTheGameExactlyAsItWas() {
        standardSetup();
        gm.tryMove(p(1, 4), p(3, 4));
        gm.tryMove(p(6, 3), p(4, 3));
        gm.tryMove(p(3, 4), p(4, 3));       // exd5: hay una captura en el historial
        String before = fingerprint();
        boolean couldUndo = gm.canUndo();

        Move move = new MoveSearcher(gm, board, new Random(1)).findBestMove(3, 3000, 0);

        assertNotNull(move);
        assertEquals(before, fingerprint());
        assertEquals(couldUndo, gm.canUndo());
        assertEquals(4, gm.getPositions().size());   // inicial + 3 jugadas, sin rastro de lo probado
    }

    @Test
    public void searchDoesNotEnableUndoWhenThereWasNothingToUndo() {
        standardSetup();
        assertFalse(gm.canUndo());
        new MoveSearcher(gm, board, new Random(2)).findBestMove(2, 2000, 0);
        assertFalse(gm.canUndo());
        assertEquals(0, gm.getMoveHistory().size());
    }

    @Test
    public void findsMateInOne() {
        put(new King(Color.BLACK, p(7, 7)), 7, 7);
        put(new King(Color.WHITE, p(0, 0)), 0, 0);
        put(new Rook(Color.WHITE, p(6, 0)), 6, 0);   // cubre la fila 7
        put(new Rook(Color.WHITE, p(0, 5)), 0, 5);   // puede dar el mate en f8
        Move move = new MoveSearcher(gm, board, new Random(3)).findBestMove(2, 2000, 0);
        assertEquals(p(0, 5), move.getFrom());
        assertEquals(p(7, 5), move.getTo());
    }

    @Test
    public void takesAFreeQueenAndDoesNotWalkIntoOne() {
        put(new King(Color.WHITE, p(0, 0)), 0, 0);
        put(new King(Color.BLACK, p(7, 7)), 7, 7);
        put(new Rook(Color.WHITE, p(0, 4)), 0, 4);
        put(new Queen(Color.BLACK, p(5, 4)), 5, 4);   // dama negra sin defensa en la misma columna
        Move move = new MoveSearcher(gm, board, new Random(4)).findBestMove(3, 2000, 0);
        assertEquals(p(5, 4), move.getTo());
    }

    @Test
    public void promotesAPawnAboutToCrown() {
        put(new King(Color.WHITE, p(0, 0)), 0, 0);
        put(new King(Color.BLACK, p(7, 0)), 7, 0);
        put(new Pawn(Color.WHITE, p(6, 5)), 6, 5);
        Move move = new MoveSearcher(gm, board, new Random(5)).findBestMove(2, 2000, 0);
        assertEquals(p(7, 5), move.getTo());
        // la coronación probada durante la búsqueda no deja la dama en el tablero
        assertEquals(PieceType.PAWN, board.getPieceAt(p(6, 5)).getType());
    }

    @Test
    public void returnsNullWhenThereAreNoLegalMoves() {
        put(new King(Color.BLACK, p(7, 0)), 7, 0);
        put(new King(Color.WHITE, p(5, 1)), 5, 1);
        put(new Queen(Color.WHITE, p(5, 2)), 5, 2);
        gm.tryMove(p(5, 2), p(6, 2));                // negras ahogadas
        assertNull(new MoveSearcher(gm, board).findBestMove(3, 1000, 0));
    }

    @Test
    public void hardLevelRespondsInReasonableTimeFromTheStart() {
        standardSetup();
        long start = System.currentTimeMillis();
        Move move = new MoveSearcher(gm, board).findBestMove(4, 2500, 0);
        long took = System.currentTimeMillis() - start;
        assertNotNull(move);
        assertTrue(took < 4500, "tardó " + took + " ms");
    }

    @Test
    public void toleranceGivesVarietyAndOnlyPicksLegalMoves() {
        standardSetup();
        java.util.Set<String> picked = new java.util.HashSet<>();
        for (int i = 0; i < 12; i++) {
            Move move = new MoveSearcher(gm, board, new Random(i)).findBestMove(2, 1500, 30);
            assertTrue(gm.getLegalMoves(move.getFrom()).contains(move.getTo()));
            picked.add(move.getFrom() + ">" + move.getTo());
        }
        assertTrue(picked.size() > 1, "siempre la misma jugada: " + picked);
    }

    @Test
    public void snapshotsAllowReviewingEveryMomentOfTheGame() {
        standardSetup();
        Piece pawn = board.getPieceAt(p(1, 4));
        gm.tryMove(p(1, 4), p(3, 4));
        gm.tryMove(p(6, 4), p(4, 4));
        List<Piece[][]> positions = gm.getPositions();
        assertEquals(3, positions.size());
        assertEquals(pawn, positions.get(0)[1][4]);          // al principio el peón estaba en e2
        assertNull(positions.get(1)[1][4]);
        assertEquals(pawn, positions.get(1)[3][4]);          // después de 1. e4
        assertNotNull(positions.get(2)[4][4]);               // después de 1... e5
        assertEquals(2, gm.getMovesPlayed().size());

        gm.undoLastRound(Color.WHITE);
        assertEquals(1, gm.getPositions().size());           // de vuelta al principio
        assertNotNull(gm.getPositions().get(0)[1][4]);
    }

    @Test
    public void aiVersusAiFinishesWithoutErrorsAndKeepsHistoryConsistent() {
        standardSetup();
        int plies = 0;
        while (plies < 60 && gm.getGameState() != GameState.CHECKMATE && gm.getGameState() != GameState.DRAW) {
            Move move = new MoveSearcher(gm, board, new Random(plies)).findBestMove(2, 800, 20);
            assertNotNull(move);
            assertEquals(MoveResult.SUCCESS, gm.tryMove(move.getFrom(), move.getTo()));
            plies++;
        }
        assertEquals(plies, gm.getMoveHistory().size());
        assertEquals(plies + 1, gm.getPositions().size());
    }
}
