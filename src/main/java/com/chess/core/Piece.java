package com.chess.core;

import com.chess.ports.ChessBoard;
import com.chess.ports.MovementStrategy;
import java.util.List;

public abstract class Piece {
    protected Color color;
    protected Position position;
    protected boolean hasMoved;
    protected PieceType type;
    protected MovementStrategy movementStrategy;

    private String id = java.util.UUID.randomUUID().toString();

    public List<Position> getPossibleMoves(ChessBoard board) {
        return movementStrategy.getValidMoves(position, board);
    }

    public void setPosition(Position position) {
        this.position = position;
        this.hasMoved = true;
    }

    void restore(Position position, boolean hasMovedBefore) {
        this.position = position;
        this.hasMoved = hasMovedBefore;
    }

    public MovementStrategy getMovementStrategy() { return movementStrategy; }
    public boolean hasMoved() { return hasMoved; }
    public Color getColor() { return color; }
    public Position getPosition() { return position; }
    public PieceType getType() { return type; }
    public String getId() { return id; }

    void setId(String id) { this.id = id; }
}