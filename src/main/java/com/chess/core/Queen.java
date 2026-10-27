package com.chess.core;

import com.chess.ports.MovementStrategy;

public class Queen extends Piece {
    public Queen(Color color, Position position) {
        this(color, position, new QueenMovementStrategy());
    }

    public Queen(Color color, Position position, MovementStrategy movementStrategy) {
        this.color = color;
        this.position = position;
        this.type = PieceType.QUEEN;
        this.hasMoved = false;
        this.movementStrategy = movementStrategy;
    }
}
