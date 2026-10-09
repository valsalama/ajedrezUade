package com.chess.core;

import com.chess.ports.ChessBoard;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MoveCommandTest {
    @Test
    public void testExecuteAndUndoMove() {
        ChessBoard board = new ArrayChessBoard();
        Piece whitePawn = new Pawn(Color.WHITE, new Position(1, 0));
        board.placePiece(whitePawn, new Position(1, 0));
        
        MoveCommand command = new MoveCommand(board, new Position(1, 0), new Position(3, 0), whitePawn, null);
        
        command.execute();
        assertNull(board.getPieceAt(new Position(1, 0)));
        assertNotNull(board.getPieceAt(new Position(3, 0)));
        
        command.undo();
        // Se espera que al deshacer, el peón vuelva a su posición original
        assertNotNull(board.getPieceAt(new Position(1, 0)));
        assertNull(board.getPieceAt(new Position(3, 0)));
    }
}