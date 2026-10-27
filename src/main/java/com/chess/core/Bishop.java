package com.chess.core;

import com.chess.ports.MovementStrategy;

public class Bishop extends Piece {
    public Bishop(Color color, Position position) {
        this(color, position, new BishopMovementStrategy());
    }

    public Bishop(Color color, Position position, MovementStrategy movementStrategy) {
        this.color = color;
        this.position = position;
        this.type = PieceType.BISHOP;
        this.hasMoved = false;
        this.movementStrategy = movementStrategy;
    }
}
