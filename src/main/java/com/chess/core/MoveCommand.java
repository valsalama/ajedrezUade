package com.chess.core;

import com.chess.ports.ChessBoard;

public class MoveCommand implements Command {
    private ChessBoard board;
    private Position from, to;
    private Piece movedPiece, capturedPiece;
    // Se guarda ACÁ, al construir el comando (antes de mover nada), si la
    // pieza ya se había movido previamente. Así el undo puede devolverle
    // ese mismo valor en vez de dejarla siempre en "true".
    private final boolean movedPieceHadMovedBefore;

    public MoveCommand(ChessBoard board, Position from, Position to, Piece movedPiece, Piece capturedPiece) {
        this.board = board;
        this.from = from;
        this.to = to;
        this.movedPiece = movedPiece;
        this.capturedPiece = capturedPiece;
        this.movedPieceHadMovedBefore = movedPiece.hasMoved();
    }

    @Override
    public void execute() {
        board.removePiece(from);
        board.placePiece(movedPiece, to);
        movedPiece.setPosition(to);
    }

    @Override
    public void undo() {
        // Se saca lo que haya en "to" ahora mismo: puede ser la pieza que se
        // movió, o -si hubo coronación- la Dama que la reemplazó. En ambos
        // casos hay que sacarla, porque lo que va a volver a "from" es la
        // pieza ORIGINAL (movedPiece), no lo que terminó en el tablero.
        board.removePiece(to);
        board.placePiece(movedPiece, from);
        movedPiece.restorePosition(from, movedPieceHadMovedBefore);

        if (capturedPiece != null) {
            // La pieza capturada ya estaba parada en "to" antes de ser
            // comida: su posición y su hasMoved no cambiaron en ningún
            // momento, así que alcanza con devolverla al tablero tal cual
            // está, sin tocarle ningún campo.
            board.placePiece(capturedPiece, to);
        }
    }

    /** Para que GameManager pueda sacar esta pieza de la lista de capturadas al deshacer. */
    public Piece getCapturedPiece() { return capturedPiece; }

    /** Color de quien hizo este movimiento (para saber en qué lista buscar la pieza capturada). */
    public Color getMovedPieceColor() { return movedPiece.getColor(); }
}