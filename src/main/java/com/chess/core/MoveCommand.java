package com.chess.core;

import com.chess.ports.ChessBoard;

public class MoveCommand implements Command {
    private ChessBoard board;
    private Position from, to;
    private Piece movedPiece, capturedPiece;
    private boolean movedPieceHadMoved;
    private Piece promotedPiece; // la pieza que reemplaza al peón si corona (null si no corona)
    private final PieceType promotionType; // a qué pieza corona: Dama, Torre, Alfil o Caballo

    public MoveCommand(ChessBoard board, Position from, Position to, Piece movedPiece, Piece capturedPiece) {
        this(board, from, to, movedPiece, capturedPiece, PieceType.QUEEN);
    }

    public MoveCommand(ChessBoard board, Position from, Position to, Piece movedPiece, Piece capturedPiece,
                       PieceType promotionType) {
        this.promotionType = promotionType == null ? PieceType.QUEEN : promotionType;
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

        // Coronación: un peón que llega al final del tablero se convierte en la
        // pieza elegida (Dama por defecto). La pieza nueva conserva el id del peón,
        // así el frontend sabe que es "la misma" pieza que cambió de tipo y puede
        // animar la coronación.
        if (isPromotion()) {
            promotedPiece = createPromotedPiece();
            promotedPiece.setId(movedPiece.getId());
            promotedPiece.setPosition(to);
            board.placePiece(promotedPiece, to);
        }
    }

    @Override 
    public void undo() {
        board.removePiece(to);
        promotedPiece = null;
        board.placePiece(movedPiece, from);
        movedPiece.restore(from, movedPieceHadMoved);
        if (capturedPiece != null) {
            board.placePiece(capturedPiece, to);
            capturedPiece.restore(to, capturedPiece.hasMoved());
        }
    }

    private Piece createPromotedPiece() {
        Color color = movedPiece.getColor();
        switch (promotionType) {
            case ROOK:   return new Rook(color, to);
            case BISHOP: return new Bishop(color, to);
            case KNIGHT: return new Knight(color, to);
            default:     return new Queen(color, to);
        }
    }

    /** Piezas a las que puede coronar un peón: todas menos el Rey y el Peón. */
    public static boolean isValidPromotionType(PieceType type) {
        return type == PieceType.QUEEN || type == PieceType.ROOK
                || type == PieceType.BISHOP || type == PieceType.KNIGHT;
    }

    private boolean isPromotion() {
        if (movedPiece.getType() != PieceType.PAWN) return false;
        int lastRow = movedPiece.getColor() == Color.WHITE ? 7 : 0;
        return to.getRow() == lastRow;
    }

    public Piece getCapturedPiece() { return capturedPiece; }
    public boolean isPromotionMove() { return promotedPiece != null; }
}