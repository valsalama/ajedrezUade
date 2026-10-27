package com.chess.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.chess.ports.ChessBoard;

public class SpecialRulesTest {
    private ChessBoard board;
    private GameManager gm;

    @BeforeEach
    public void setUp() {
        board = new ArrayChessBoard();
        gm = new GameManager(board);
    }

    private Piece put(Piece piece, int r, int c) {
        board.placePiece(piece, new Position(r, c));
        return piece;
    }

    private Position p(int r, int c) { return new Position(r, c); }

    // ---------- Enroque ----------

    @Test
    public void castlingAppearsAsKingMoveAndMovesRook() {
        put(new King(Color.WHITE, p(0, 4)), 0, 4);
        put(new Rook(Color.WHITE, p(0, 7)), 0, 7);
        put(new Rook(Color.WHITE, p(0, 0)), 0, 0);
        put(new King(Color.BLACK, p(7, 4)), 7, 4);

        List<Position> moves = gm.getLegalMoves(p(0, 4));
        assertTrue(moves.contains(p(0, 6)));
        assertTrue(moves.contains(p(0, 2)));

        assertEquals(MoveResult.SUCCESS, gm.tryMove(p(0, 4), p(0, 6)));
        assertEquals(PieceType.KING, board.getPieceAt(p(0, 6)).getType());
        assertEquals(PieceType.ROOK, board.getPieceAt(p(0, 5)).getType());
        assertTrue(board.isEmpty(p(0, 7)));
    }

    @Test
    public void cannotCastleThroughAttackedSquare() {
        put(new King(Color.WHITE, p(0, 4)), 0, 4);
        put(new Rook(Color.WHITE, p(0, 7)), 0, 7);
        put(new King(Color.BLACK, p(7, 0)), 7, 0);
        put(new Rook(Color.BLACK, p(7, 5)), 7, 5); // ataca (0,5)

        assertFalse(gm.getLegalMoves(p(0, 4)).contains(p(0, 6)));
        assertEquals(MoveResult.INVALID_MOVE, gm.tryMove(p(0, 4), p(0, 6)));
    }

    @Test
    public void cannotCastleAfterRookMoved() {
        Piece rook = put(new Rook(Color.WHITE, p(0, 7)), 0, 7);
        put(new King(Color.WHITE, p(0, 4)), 0, 4);
        put(new King(Color.BLACK, p(7, 4)), 7, 4);
        gm.tryMove(p(0, 7), p(0, 6));
        gm.tryMove(p(7, 4), p(7, 3));
        gm.tryMove(p(0, 6), p(0, 7));
        gm.tryMove(p(7, 3), p(7, 4));
        assertTrue(rook.hasMoved());
        assertFalse(gm.getLegalMoves(p(0, 4)).contains(p(0, 6)));
    }

    // ---------- Peón al paso ----------

    @Test
    public void enPassantCaptureAndUndo() {
        put(new King(Color.WHITE, p(0, 0)), 0, 0);
        put(new King(Color.BLACK, p(7, 7)), 7, 7);
        Piece white = put(new Pawn(Color.WHITE, p(4, 4)), 4, 4);
        Piece black = put(new Pawn(Color.BLACK, p(6, 3)), 6, 3);
        gm.tryMove(p(0, 0), p(0, 1));            // blancas esperan
        assertEquals(MoveResult.SUCCESS, gm.tryMove(p(6, 3), p(4, 3))); // negras avanzan dos

        assertTrue(gm.getLegalMoves(p(4, 4)).contains(p(5, 3)));
        assertEquals(MoveResult.SUCCESS, gm.tryMove(p(4, 4), p(5, 3)));
        assertTrue(board.isEmpty(p(4, 3)));
        assertEquals(white, board.getPieceAt(p(5, 3)));
        assertEquals(1, gm.getCapturedPieces(Color.WHITE).size());

        gm.undoLastMove();
        assertEquals(black, board.getPieceAt(p(4, 3)));
        assertEquals(white, board.getPieceAt(p(4, 4)));
        assertEquals(0, gm.getCapturedPieces(Color.WHITE).size());
    }

    @Test
    public void enPassantOnlyRightAfterDoubleStep() {
        put(new King(Color.WHITE, p(0, 0)), 0, 0);
        put(new King(Color.BLACK, p(7, 7)), 7, 7);
        put(new Pawn(Color.WHITE, p(4, 4)), 4, 4);
        put(new Pawn(Color.BLACK, p(6, 3)), 6, 3);
        gm.tryMove(p(0, 0), p(0, 1));
        gm.tryMove(p(6, 3), p(4, 3));
        gm.tryMove(p(0, 1), p(0, 0));   // blancas no capturan al paso
        gm.tryMove(p(7, 7), p(7, 6));
        assertFalse(gm.getLegalMoves(p(4, 4)).contains(p(5, 3)));
    }

    // ---------- Coronación ----------

    @Test
    public void promotionCreatesQueenWithSameIdAndUndoRestoresPawn() {
        put(new King(Color.WHITE, p(0, 0)), 0, 0);
        put(new King(Color.BLACK, p(7, 0)), 7, 0);
        Piece pawn = put(new Pawn(Color.WHITE, p(6, 5)), 6, 5);

        assertEquals(MoveResult.SUCCESS, gm.tryMove(p(6, 5), p(7, 5)));
        Piece promoted = board.getPieceAt(p(7, 5));
        assertEquals(PieceType.QUEEN, promoted.getType());
        assertEquals(pawn.getId(), promoted.getId());

        gm.undoLastMove();
        assertEquals(pawn, board.getPieceAt(p(6, 5)));
        assertEquals(PieceType.PAWN, board.getPieceAt(p(6, 5)).getType());
        assertTrue(board.isEmpty(p(7, 5)));
    }

    @Test
    public void blackPromotesOnRowZero() {
        put(new King(Color.WHITE, p(0, 7)), 0, 7);
        put(new King(Color.BLACK, p(7, 0)), 7, 0);
        put(new Pawn(Color.BLACK, p(1, 3)), 1, 3);
        put(new Pawn(Color.WHITE, p(3, 3)), 3, 3);
        gm.tryMove(p(3, 3), p(4, 3));
        gm.tryMove(p(1, 3), p(0, 3));
        assertEquals(PieceType.QUEEN, board.getPieceAt(p(0, 3)).getType());
    }

    // ---------- Tablas ----------

    @Test
    public void kingVsKingIsInsufficientMaterial() {
        put(new King(Color.WHITE, p(0, 0)), 0, 0);
        put(new King(Color.BLACK, p(7, 7)), 7, 7);
        assertEquals(GameState.DRAW, gm.getGameState());
        assertEquals(DrawReason.INSUFFICIENT_MATERIAL, gm.getDrawReason());
    }

    @Test
    public void kingAndKnightVsKingIsInsufficientButRookIsNot() {
        put(new King(Color.WHITE, p(0, 0)), 0, 0);
        put(new King(Color.BLACK, p(7, 7)), 7, 7);
        put(new Knight(Color.WHITE, p(3, 3)), 3, 3);
        assertEquals(DrawReason.INSUFFICIENT_MATERIAL, gm.getDrawReason());
        put(new Rook(Color.WHITE, p(3, 5)), 3, 5);
        assertEquals(GameState.IN_PROGRESS, gm.getGameState());
    }

    @Test
    public void stalemateIsDrawWithReason() {
        put(new King(Color.BLACK, p(7, 0)), 7, 0);
        put(new King(Color.WHITE, p(5, 1)), 5, 1);
        put(new Queen(Color.WHITE, p(5, 2)), 5, 2);
        gm.tryMove(p(5, 2), p(6, 2)); // deja a negras ahogadas
        assertEquals(GameState.DRAW, gm.getGameState());
        assertEquals(DrawReason.STALEMATE, gm.getDrawReason());
    }

    @Test
    public void threefoldRepetition() {
        put(new King(Color.WHITE, p(0, 0)), 0, 0);
        put(new King(Color.BLACK, p(7, 7)), 7, 7);
        put(new Rook(Color.WHITE, p(0, 5)), 0, 5);
        put(new Rook(Color.BLACK, p(7, 5)), 7, 5);
        // Los reyes ya movidos pierden el derecho a enrocar, así que la posición
        // "de referencia" se alcanza por primera vez tras la primera vuelta.
        gm.tryMove(p(0, 0), p(0, 1));
        gm.tryMove(p(7, 7), p(7, 6));
        gm.tryMove(p(0, 1), p(0, 0));
        gm.tryMove(p(7, 6), p(7, 7));           // 1ª vez
        assertEquals(GameState.IN_PROGRESS, gm.getGameState());
        for (int i = 0; i < 2; i++) {
            assertEquals(GameState.IN_PROGRESS, gm.getGameState());
            gm.tryMove(p(0, 0), p(0, 1));
            gm.tryMove(p(7, 7), p(7, 6));
            gm.tryMove(p(0, 1), p(0, 0));
            gm.tryMove(p(7, 6), p(7, 7));       // 2ª y 3ª vez
        }
        assertEquals(GameState.DRAW, gm.getGameState());
        assertEquals(DrawReason.THREEFOLD_REPETITION, gm.getDrawReason());
    }

    @Test
    public void fiftyMoveRule() throws Exception {
        put(new King(Color.WHITE, p(0, 0)), 0, 0);
        put(new King(Color.BLACK, p(7, 7)), 7, 7);
        put(new Rook(Color.WHITE, p(0, 5)), 0, 5);

        java.lang.reflect.Field clock = GameManager.class.getDeclaredField("halfmoveClock");
        clock.setAccessible(true);
        clock.setInt(gm, 99);

        assertEquals(GameState.IN_PROGRESS, gm.getGameState());
        gm.tryMove(p(0, 0), p(0, 1));       // la media jugada 100
        assertEquals(GameState.DRAW, gm.getGameState());
        assertEquals(DrawReason.FIFTY_MOVE_RULE, gm.getDrawReason());

        gm.undoLastMove();                  // al deshacer el contador vuelve
        assertEquals(GameState.IN_PROGRESS, gm.getGameState());
    }

    @Test
    public void capturingResetsFiftyMoveCounter() throws Exception {
        put(new King(Color.WHITE, p(0, 0)), 0, 0);
        put(new King(Color.BLACK, p(7, 7)), 7, 7);
        put(new Rook(Color.WHITE, p(0, 5)), 0, 5);
        put(new Knight(Color.BLACK, p(4, 5)), 4, 5);
        java.lang.reflect.Field clock = GameManager.class.getDeclaredField("halfmoveClock");
        clock.setAccessible(true);
        clock.setInt(gm, 99);
        gm.tryMove(p(0, 5), p(4, 5));       // come el caballo
        assertEquals(0, clock.getInt(gm));
    }

    // ---------- Deshacer por ronda ----------

    @Test
    public void undoRoundRevertsHumanAndAiMoveTogether() {
        put(new King(Color.WHITE, p(0, 0)), 0, 0);
        put(new King(Color.BLACK, p(7, 7)), 7, 7);
        Piece wp = put(new Pawn(Color.WHITE, p(1, 3)), 1, 3);
        Piece bp = put(new Pawn(Color.BLACK, p(6, 3)), 6, 3);
        gm.tryMove(p(1, 3), p(3, 3));   // humano
        gm.tryMove(p(6, 3), p(4, 3));   // IA

        assertTrue(gm.undoLastRound(Color.WHITE));
        assertEquals(wp, board.getPieceAt(p(1, 3)));
        assertEquals(bp, board.getPieceAt(p(6, 3)));
        assertEquals(Color.WHITE, gm.getCurrentTurn());
        assertFalse(gm.canUndo());
    }

    @Test
    public void undoRoundWhenAiDidNotReplyOnlyUndoesHumanMove() {
        put(new King(Color.WHITE, p(0, 0)), 0, 0);
        put(new King(Color.BLACK, p(7, 7)), 7, 7);
        put(new Pawn(Color.WHITE, p(1, 3)), 1, 3);
        put(new Pawn(Color.BLACK, p(6, 3)), 6, 3);
        gm.tryMove(p(1, 3), p(3, 3));
        gm.tryMove(p(6, 3), p(4, 3));
        gm.tryMove(p(0, 0), p(0, 1));   // ronda 2: humano juega y la IA aún no respondió

        gm.undoLastRound(Color.WHITE);
        assertEquals(Color.WHITE, gm.getCurrentTurn());
        assertEquals(PieceType.KING, board.getPieceAt(p(0, 0)).getType());
        assertEquals(PieceType.PAWN, board.getPieceAt(p(3, 3)).getType()); // la ronda 1 sigue hecha
    }

    @Test
    public void undoOnlyGoesBackOneRoundAndThenIsBlockedUntilNextMove() {
        put(new King(Color.WHITE, p(0, 0)), 0, 0);
        put(new King(Color.BLACK, p(7, 7)), 7, 7);
        put(new Pawn(Color.WHITE, p(1, 3)), 1, 3);
        put(new Pawn(Color.BLACK, p(6, 3)), 6, 3);
        gm.tryMove(p(1, 3), p(3, 3));   // ronda 1
        gm.tryMove(p(6, 3), p(4, 3));
        gm.tryMove(p(0, 0), p(0, 1));   // ronda 2
        gm.tryMove(p(7, 7), p(7, 6));

        assertTrue(gm.canUndo());
        gm.undoLastRound(Color.WHITE);                       // vuelve solo la ronda 2
        assertEquals(PieceType.KING, board.getPieceAt(p(0, 0)).getType());
        assertEquals(PieceType.PAWN, board.getPieceAt(p(3, 3)).getType());
        assertFalse(gm.canUndo());                           // no se puede volver más atrás
        assertFalse(gm.undoLastRound(Color.WHITE));
        assertEquals(PieceType.PAWN, board.getPieceAt(p(3, 3)).getType());

        gm.tryMove(p(0, 0), p(0, 1));                        // una jugada nueva lo habilita otra vez
        assertTrue(gm.canUndo());
    }
}
