package com.chess.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.chess.ports.ChessBoard;

/** Elección de la pieza al coronar: Dama, Torre, Alfil o Caballo (nunca Rey ni Peón). */
public class PromotionChoiceTest {
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

    /** Rey blanco en a1, rey negro en a7 y un peón blanco listo para coronar en f7 (sin dar jaque en f8). */
    private Piece setUpWhitePromotion() {
        put(new King(Color.WHITE, p(0, 0)), 0, 0);
        put(new King(Color.BLACK, p(6, 0)), 6, 0);
        return put(new Pawn(Color.WHITE, p(6, 5)), 6, 5);
    }

    @Test
    public void everyAllowedPieceCanBeChosenAndKeepsThePawnId() {
        PieceType[] options = { PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT };
        for (PieceType option : options) {
            board = new ArrayChessBoard();
            gm = new GameManager(board);
            Piece pawn = setUpWhitePromotion();

            assertEquals(MoveResult.SUCCESS, gm.tryMove(p(6, 5), p(7, 5), option), option.name());
            Piece promoted = board.getPieceAt(p(7, 5));
            assertEquals(option, promoted.getType());
            assertEquals(Color.WHITE, promoted.getColor());
            assertEquals(pawn.getId(), promoted.getId());
        }
    }

    @Test
    public void defaultPromotionIsStillQueen() {
        setUpWhitePromotion();
        assertEquals(MoveResult.SUCCESS, gm.tryMove(p(6, 5), p(7, 5)));
        assertEquals(PieceType.QUEEN, board.getPieceAt(p(7, 5)).getType());
    }

    @Test
    public void kingAndPawnAreNotValidChoices() {
        setUpWhitePromotion();
        assertEquals(MoveResult.INVALID_MOVE, gm.tryMove(p(6, 5), p(7, 5), PieceType.KING));
        assertEquals(MoveResult.INVALID_MOVE, gm.tryMove(p(6, 5), p(7, 5), PieceType.PAWN));

        // La jugada rechazada no toca el tablero ni el turno
        assertEquals(PieceType.PAWN, board.getPieceAt(p(6, 5)).getType());
        assertTrue(board.isEmpty(p(7, 5)));
        assertEquals(Color.WHITE, gm.getCurrentTurn());
        assertTrue(gm.getMoveHistory().isEmpty());
    }

    @Test
    public void undoRestoresThePawnAndAnotherChoiceCanBeMade() {
        Piece pawn = setUpWhitePromotion();
        assertEquals(MoveResult.SUCCESS, gm.tryMove(p(6, 5), p(7, 5), PieceType.KNIGHT));
        gm.undoLastMove();

        assertSame(pawn, board.getPieceAt(p(6, 5)));
        assertEquals(PieceType.PAWN, board.getPieceAt(p(6, 5)).getType());
        assertTrue(board.isEmpty(p(7, 5)));

        assertEquals(MoveResult.SUCCESS, gm.tryMove(p(6, 5), p(7, 5), PieceType.ROOK));
        assertEquals(PieceType.ROOK, board.getPieceAt(p(7, 5)).getType());
    }

    @Test
    public void notationShowsTheChosenPiece() {
        String[] expected = { "f8=D", "f8=T", "f8=A", "f8=C" };
        PieceType[] options = { PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT };
        for (int i = 0; i < options.length; i++) {
            board = new ArrayChessBoard();
            gm = new GameManager(board);
            setUpWhitePromotion();
            gm.tryMove(p(6, 5), p(7, 5), options[i]);
            List<String> history = gm.getMoveHistory();
            assertEquals(expected[i], history.get(history.size() - 1));
        }
    }

    @Test
    public void knightPromotionCanGiveCheck() {
        put(new King(Color.WHITE, p(0, 0)), 0, 0);
        put(new King(Color.BLACK, p(6, 3)), 6, 3); // d7: el caballo en f8 lo ataca
        put(new Pawn(Color.BLACK, p(4, 7)), 4, 7); // evita que sea "rey y caballo contra rey" (tablas)
        put(new Pawn(Color.WHITE, p(6, 5)), 6, 5);

        assertEquals(MoveResult.SUCCESS, gm.tryMove(p(6, 5), p(7, 5), PieceType.KNIGHT));
        assertEquals("f8=C+", gm.getMoveHistory().get(0));
        assertEquals(GameState.CHECK, gm.getGameState());
    }

    @Test
    public void theChosenPieceDecidesWhetherItIsCheckmate() {
        // En f8 la Dama da jaque mate al rey de h8; el Caballo, en cambio, no
        put(new King(Color.WHITE, p(5, 6)), 5, 6);   // g6
        put(new King(Color.BLACK, p(7, 7)), 7, 7);   // h8
        put(new Pawn(Color.BLACK, p(4, 0)), 4, 0);   // a5, para que haya material suficiente
        put(new Pawn(Color.WHITE, p(6, 5)), 6, 5);   // f7

        assertEquals(MoveResult.SUCCESS, gm.tryMove(p(6, 5), p(7, 5), PieceType.QUEEN));
        assertEquals(GameState.CHECKMATE, gm.getGameState());
        gm.undoLastMove();

        assertEquals(MoveResult.SUCCESS, gm.tryMove(p(6, 5), p(7, 5), PieceType.KNIGHT));
        assertEquals(PieceType.KNIGHT, board.getPieceAt(p(7, 5)).getType());
        assertEquals(GameState.IN_PROGRESS, gm.getGameState());
    }

    @Test
    public void nullChoiceMeansQueen() {
        setUpWhitePromotion();
        assertEquals(MoveResult.SUCCESS, gm.tryMove(p(6, 5), p(7, 5), null));
        assertEquals(PieceType.QUEEN, board.getPieceAt(p(7, 5)).getType());
    }

    @Test
    public void blackCanChooseToo() {
        put(new King(Color.WHITE, p(0, 7)), 0, 7);
        put(new King(Color.BLACK, p(7, 0)), 7, 0);
        put(new Pawn(Color.BLACK, p(1, 3)), 1, 3);
        put(new Pawn(Color.WHITE, p(3, 3)), 3, 3);

        gm.tryMove(p(3, 3), p(4, 3));
        assertEquals(MoveResult.SUCCESS, gm.tryMove(p(1, 3), p(0, 3), PieceType.BISHOP));
        Piece promoted = board.getPieceAt(p(0, 3));
        assertEquals(PieceType.BISHOP, promoted.getType());
        assertEquals(Color.BLACK, promoted.getColor());
    }

    @Test
    public void choiceIsIgnoredWhenTheMoveIsNotAPromotion() {
        put(new King(Color.WHITE, p(0, 0)), 0, 0);
        put(new King(Color.BLACK, p(7, 7)), 7, 7);
        put(new Pawn(Color.WHITE, p(1, 4)), 1, 4);

        assertEquals(MoveResult.SUCCESS, gm.tryMove(p(1, 4), p(2, 4), PieceType.KNIGHT));
        assertEquals(PieceType.PAWN, board.getPieceAt(p(2, 4)).getType());
        assertEquals("e3", gm.getMoveHistory().get(0));
    }
}
