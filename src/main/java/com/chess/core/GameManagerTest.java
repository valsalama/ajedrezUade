package chess;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GameManagerTest {

    private ChessBoard board;
    private GameManager game;

    @BeforeEach
    void setUp() {
        board = new ArrayChessBoard();
        game = new GameManager(board, new MoveValidator());
    }

    private void put(Piece p) {
        board.placePiece(p, p.getPosition());
    }

    @Test
    void turnAlternatesAfterEachMove() {
        put(new Rook(Color.WHITE, new Position(4, 0)));
        put(new Rook(Color.BLACK, new Position(0, 0)));

        assertEquals(Color.WHITE, game.getCurrentTurn());
        assertEquals(MoveResult.SUCCESS, game.tryMove(new Position(4, 0), new Position(3, 0)));
        assertEquals(Color.BLACK, game.getCurrentTurn());

        // las blancas no pueden mover de nuevo
        assertEquals(MoveResult.NOT_YOUR_TURN, game.tryMove(new Position(3, 0), new Position(2, 0)));
        assertEquals(Color.BLACK, game.getCurrentTurn());          // el turno no cambia si falla

        assertEquals(MoveResult.SUCCESS, game.tryMove(new Position(0, 0), new Position(1, 0)));
        assertEquals(Color.WHITE, game.getCurrentTurn());
    }

    @Test
    void invalidMoveDoesNotChangeTurn() {
        put(new Rook(Color.WHITE, new Position(4, 0)));

        assertEquals(MoveResult.INVALID_MOVE, game.tryMove(new Position(4, 0), new Position(3, 1))); // diagonal
        assertEquals(MoveResult.INVALID_MOVE, game.tryMove(new Position(2, 2), new Position(2, 3))); // casillero vacío
        assertEquals(Color.WHITE, game.getCurrentTurn());
    }

    @Test
    void captureIsRecordedAndPieceLeavesTheBoard() {
        Piece whiteRook = new Rook(Color.WHITE, new Position(4, 0));
        Piece blackRook = new Rook(Color.BLACK, new Position(4, 5));
        put(whiteRook);
        put(blackRook);

        assertEquals(MoveResult.SUCCESS, game.tryMove(new Position(4, 0), new Position(4, 5)));

        assertEquals(1, game.getCapturedPieces(Color.BLACK).size());
        assertSame(blackRook, game.getCapturedPieces(Color.BLACK).get(0));
        assertTrue(game.getCapturedPieces(Color.WHITE).isEmpty());

        assertSame(whiteRook, board.getPieceAt(new Position(4, 5)));   // la capturada ya no está
        assertTrue(board.isEmpty(new Position(4, 0)));
    }
}