package com.chess.core;

import com.chess.ports.ChessBoard;

public class ArrayChessBoard implements ChessBoard {
    private Piece[][] board = new Piece[8][8];

    @Override
    public Piece getPieceAt(Position position) {
        return board[position.getRow()][position.getColumn()];
    }

    @Override
    public void placePiece(Piece piece, Position position) {
        board[position.getRow()][position.getColumn()] = piece;
    }

    @Override
    public void removePiece(Position position) {
        board[position.getRow()][position.getColumn()] = null;
    }

    @Override
    public boolean isEmpty(Position position) {
        return getPieceAt(position) == null;
    }

    @Override
    public ChessBoard clone() {
        ArrayChessBoard newBoard = new ArrayChessBoard();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board[r][c];
                if (p != null) {
                    newBoard.placePiece(p, new Position(r, c));
                }
            }
        }
        return newBoard;
    }
}