package com.chess.core;

public class Knight extends Piece {
    public Knight(Color color, Position position) {
        this.color = color;
        this.position = position;
        this.type = PieceType.KNIGHT;
        this.hasMoved = false;
        this.movementStrategy = new KnightMovementStrategy();
    }
}