package com.chess.core;

public class King extends Piece {
    public King(Color color, Position position) {
        this.color = color;
        this.position = position;
        this.type = PieceType.KING;
        this.hasMoved = false;
        this.movementStrategy = new KingMovementStrategy();
    }
}