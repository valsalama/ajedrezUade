package com.chess.core;

import com.chess.ports.MovementStrategy;

public class Pawn extends Piece {
    public Pawn(Color color, Position position) {
        this(color, position, new PawnMovementStrategy());
    }

    public Pawn(Color color, Position position, MovementStrategy movementStrategy) {
        this.color = color;
        this.position = position;
        this.type = PieceType.PAWN;
        this.hasMoved = false;
        this.movementStrategy = movementStrategy;
    }
}
