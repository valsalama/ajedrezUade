package com.chess.core;

public class Queen extends Piece {
    public Queen(Color color, Position position) {
        this.color = color;
        this.position = position;
        this.type = PieceType.QUEEN;
        this.hasMoved = false;
        this.movementStrategy = new QueenMovementStrategy();
    }
}