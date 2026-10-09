package com.chess.core;

public class Rook extends Piece {
    public Rook(Color color, Position position) {
        this.color = color;
        this.position = position;
        this.type = PieceType.ROOK;
        this.movementStrategy = null; // Se completa en A4
    }
}