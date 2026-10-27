package com.chess.core;

import com.chess.ports.MovementStrategy;

public class King extends Piece {
    public King(Color color, Position position) {
        this(color, position, new KingMovementStrategy());
    }

    public King(Color color, Position position, MovementStrategy movementStrategy) {
        this.color = color;
        this.position = position;
        this.type = PieceType.KING;
        this.hasMoved = false;
        this.movementStrategy = movementStrategy;
    }
}
