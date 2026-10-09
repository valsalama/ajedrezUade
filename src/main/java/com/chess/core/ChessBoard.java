package com.chess.ports;

import com.chess.core.Piece;
import com.chess.core.Position;

public interface ChessBoard {
    Piece getPieceAt(Position position);
    void placePiece(Piece piece, Position position);
    void removePiece(Position position);
    boolean isEmpty(Position position);
    ChessBoard clone();
}