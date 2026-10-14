package com.chess.core;

import com.chess.ports.ChessBoard;

public class CheckmateDetector {
    private final CheckDetector checkDetector = new CheckDetector();

    public boolean isCheckmate(Color color, ChessBoard board) {
        return checkDetector.isInCheck(color, board) && !hasAnyLegalMove(color, board);
    }

    /**
     * Ahogado (stalemate): a ese color NO le toca estar en jaque ahora mismo,
     * pero no tiene ningún movimiento legal con ninguna pieza. Es tablas
     * (empate), no derrota. Antes de este método, esta situación no se
     * distinguía de una partida normal en curso: el turno quedaba trabado
     * para siempre porque cualquier movimiento que se intentara era
     * rechazado, sin que nada le avisara al jugador que la partida había
     * terminado en empate.
     */
    public boolean isStalemate(Color color, ChessBoard board) {
        return !checkDetector.isInCheck(color, board) && !hasAnyLegalMove(color, board);
    }

    /**
     * Recorre todas las piezas de ese color y, para cada movimiento que
     * "sobre el papel" podrían hacer, simula la jugada en un tablero clonado
     * y comprueba si eso deja al propio rey en jaque. Devuelve true apenas
     * encuentra UN movimiento que de verdad se pueda jugar.
     */
    public boolean hasAnyLegalMove(Color color, ChessBoard board) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Position pos = new Position(r, c);
                Piece piece = board.getPieceAt(pos);
                if (piece != null && piece.getColor() == color) {
                    for (Position dest : piece.getPossibleMoves(board)) {
                        ChessBoard clone = board.clone();
                        Piece clonePiece = clone.getPieceAt(pos);
                        clone.removePiece(pos);
                        clone.placePiece(clonePiece, dest);
                        if (!checkDetector.isInCheck(color, clone)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}