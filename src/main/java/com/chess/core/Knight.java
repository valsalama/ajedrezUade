package com.chess.core;

import com.chess.ports.MovementStrategy;

public class Knight extends Piece {
    public Knight(Color color, Position position) {
        this(color, position, new KnightMovementStrategy());
    }

    public Knight(Color color, Position position, MovementStrategy movementStrategy) {
        this.color = color;
        this.position = position;
        this.type = PieceType.KNIGHT;
        this.hasMoved = false;
        this.movementStrategy = movementStrategy;
    }
}
