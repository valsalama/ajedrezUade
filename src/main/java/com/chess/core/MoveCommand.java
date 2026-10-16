package com.chess.core;

import com.chess.ports.ChessBoard;

public class MoveCommand implements Command {
    private ChessBoard board;
    private Position from, to;
    private Piece movedPiece, capturedPiece;
    private boolean movedPieceHadMoved;

    public MoveCommand(ChessBoard board, Position from, Position to, Piece movedPiece, Piece capturedPiece) {
        this.board = board;
        this.from = from;
        this.to = to;
        this.movedPiece = movedPiece;
        this.capturedPiece = capturedPiece;
    }

    @Override 
    public void execute() {
        // Se guarda antes de mover, para poder dejarlo igual al deshacer
        movedPieceHadMoved = movedPiece.hasMoved();
        board.removePiece(from);
        board.placePiece(movedPiece, to);
        movedPiece.setPosition(to);
    }

    @Override 
    public void undo() {
        board.removePiece(to);
        board.placePiece(movedPiece, from);
        movedPiece.restore(from, movedPieceHadMoved);
        if (capturedPiece != null) {
            board.placePiece(capturedPiece, to);
            capturedPiece.restore(to, capturedPiece.hasMoved());
        }
    }

    public Piece getCapturedPiece() { return capturedPiece; }
}