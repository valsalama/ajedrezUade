package com.chess.core;

public class Pawn extends Piece {
    public Pawn(Color color, Position position) {
        this.color = color;
        this.position = position;
        this.type = PieceType.PAWN;
        this.hasMoved = false;
        this.movementStrategy = new PawnMovementStrategy();
    }
}