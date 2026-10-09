package com.chess.core;

import com.chess.ports.ChessBoard;

public class MoveCommand implements Command {
    private ChessBoard board;
    private Position from, to;
    private Piece movedPiece, capturedPiece;

    public MoveCommand(ChessBoard board, Position from, Position to, Piece movedPiece, Piece capturedPiece) {
        this.board = board;
        this.from = from;
        this.to = to;
        this.movedPiece = movedPiece;
        this.capturedPiece = capturedPiece;
    }

    @Override 
    public void execute() {
        board.removePiece(from);
        board.placePiece(movedPiece, to);
        movedPiece.setPosition(to);
    }

    @Override 
    public void undo() {
        board.removePiece(to);
        board.placePiece(movedPiece, from);
        movedPiece.setPosition(from);
        if (capturedPiece != null) {
            board.placePiece(capturedPiece, to);
            capturedPiece.setPosition(to);
        }
    }
}