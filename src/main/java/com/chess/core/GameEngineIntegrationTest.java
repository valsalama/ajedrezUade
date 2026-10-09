package chess;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GameEngineIntegrationTest {

    private ChessBoard board;
    private GameManager game;

    @BeforeEach
    void setUp() {
        board = new ArrayChessBoard();
        placeInitialPosition(board);
        game = new GameManager(board, new MoveValidator());
    }

    @Test
    void shortGameAlternatesTurnsAndRecordsCaptures() {
        // 1. e4
        assertEquals(MoveResult.SUCCESS, game.tryMove(pos(6, 4), pos(4, 4)));
        assertEquals(Color.BLACK, game.getCurrentTurn());

        // 1... e5
        assertEquals(MoveResult.SUCCESS, game.tryMove(pos(1, 4), pos(3, 4)));

        // 2. Cf3
        assertEquals(MoveResult.SUCCESS, game.tryMove(pos(7, 6), pos(5, 5)));

        // 2... Cc6
        assertEquals(MoveResult.SUCCESS, game.tryMove(pos(0, 1), pos(2, 2)));

        // 3. Cxe5 (captura el peón negro)
        assertEquals(MoveResult.SUCCESS, game.tryMove(pos(5, 5), pos(3, 4)));
        assertEquals(1, game.getCapturedPieces(Color.BLACK).size());
        assertEquals(PieceType.PAWN, game.getCapturedPieces(Color.BLACK).get(0).getType());
        assertTrue(game.getCapturedPieces(Color.WHITE).isEmpty());

        // las blancas intentan mover de nuevo: no es su turno
        assertEquals(MoveResult.NOT_YOUR_TURN, game.tryMove(pos(6, 0), pos(5, 0)));

        // 3... Cxe5 (las negras recapturan el caballo)
        assertEquals(MoveResult.SUCCESS, game.tryMove(pos(2, 2), pos(3, 4)));
        assertEquals(1, game.getCapturedPieces(Color.WHITE).size());
        assertEquals(PieceType.KNIGHT, game.getCapturedPieces(Color.WHITE).get(0).getType());

        // estado final del tablero
        assertEquals(PieceType.KNIGHT, board.getPieceAt(pos(3, 4)).getType());
        assertEquals(Color.BLACK, board.getPieceAt(pos(3, 4)).getColor());
        assertTrue(board.isEmpty(pos(5, 5)));
        assertEquals(Color.WHITE, game.getCurrentTurn());
    }

    // ---------- helpers del test ----------

    private static Position pos(int row, int col) {
        return new Position(row, col);
    }

    private void put(Piece p) {
        board.placePiece(p, p.getPosition());
    }

    private void placeInitialPosition(ChessBoard b) {
        for (int col = 0; col < 8; col++) {
            put(new Pawn(Color.WHITE, pos(6, col)));
            put(new Pawn(Color.BLACK, pos(1, col)));
        }
        placeBackRank(Color.WHITE, 7);
        placeBackRank(Color.BLACK, 0);
    }

    private void placeBackRank(Color c, int row) {
        put(new Rook(c, pos(row, 0)));
        put(new Knight(c, pos(row, 1)));
        put(new Bishop(c, pos(row, 2)));
        put(new Queen(c, pos(row, 3)));
        put(new King(c, pos(row, 4)));
        put(new Bishop(c, pos(row, 5)));
        put(new Knight(c, pos(row, 6)));
        put(new Rook(c, pos(row, 7)));
    }
}