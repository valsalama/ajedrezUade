package com.chess.core;

import com.chess.ports.MovementStrategy;

public class Rook extends Piece {
    public Rook(Color color, Position position) {
        this(color, position, new RookMovementStrategy());
    }

    public Rook(Color color, Position position, MovementStrategy movementStrategy) {
        this.color = color;
        this.position = position;
        this.type = PieceType.ROOK;
        this.hasMoved = false;
        this.movementStrategy = movementStrategy;
    }
}
