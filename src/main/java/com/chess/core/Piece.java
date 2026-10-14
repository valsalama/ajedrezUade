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

    public List<Position> getPossibleMoves(ChessBoard board) {
        return movementStrategy.getValidMoves(position, board);
    }

    public void setPosition(Position position) {
        this.position = position;
        this.hasMoved = true;
    }

    /**
     * Pone la pieza en una posición sin tocar hasMoved. Solo lo usa
     * MoveCommand.undo() para devolver la pieza a como estaba antes del
     * movimiento que se deshace (si no se había movido antes, tiene que
     * seguir sin haberse movido después del undo).
     */
    void restorePosition(Position position, boolean hasMovedBefore) {
        this.position = position;
        this.hasMoved = hasMovedBefore;
    }

    public MovementStrategy getMovementStrategy() { return movementStrategy; }
    public boolean hasMoved() { return hasMoved; }
    public Color getColor() { return color; }
    public Position getPosition() { return position; }
    public PieceType getType() { return type; }
}