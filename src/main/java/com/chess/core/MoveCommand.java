package com.chess.core;

public class MoveCommand implements Command {
    private final ChessBoard board;
    private final Position from;
    private final Position to;
    private final Piece movedPiece;
    private final Piece capturedPiece;

    public MoveCommand(ChessBoard board, Position from, Position to) {
        this.board = board;
        this.from = from;
        this.to = to;
        this.movedPiece = board.getPieceAt(from);
        this.capturedPiece = board.getPieceAt(to);
    }

    @Override
    public void execute() {
        if (movedPiece == null) {
            return;
        }

        board.removePiece(from);

        movedPiece.setPosition(to);

        board.placePiece(movedPiece, to);
    }

    @Override
    public void undo() {
        if (movedPiece == null) {
            return;
        }

        board.removePiece(to);

        movedPiece.setPosition(from);
        board.placePiece(movedPiece, from);

        if (capturedPiece != null) {
            board.placePiece(capturedPiece, to);
        }
    }

    public Position getFrom() {
        return from;
    }

    public Position getTo() {
        return to;
    }

    public Piece getMovedPiece() {
        return movedPiece;
    }

    public Piece getCapturedPiece() {
        return capturedPiece;
    }
}