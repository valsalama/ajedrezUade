package com.chess.core;

public class Bishop extends Piece {
    public Bishop(Color color, Position position) {
        this.color = color;
        this.position = position;
        this.type = PieceType.BISHOP;
        this.movementStrategy = null; // Se completa en A4
    }
}