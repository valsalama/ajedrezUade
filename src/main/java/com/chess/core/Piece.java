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

    /**
     * Identificador único y estable de esta pieza en particular, para que el
     * frontend pueda reconocer "esta es la misma pieza que estaba en otra
     * casilla" entre una jugada y la siguiente, y así animar el deslizamiento
     * en vez de hacerla desaparecer de un lado y aparecer en el otro. Se
     * genera solo una vez, al crear la pieza, y no cambia aunque se mueva.
     * La única excepción es la coronación (ver GameManager.promoteIfNeeded):
     * ahí la Dama nueva toma prestado el id del peón que corona, para que
     * el frontend vea "la misma pieza, pero ahora es otra cosa" en vez de
     * "una pieza desapareció y otra apareció de la nada".
     */
    private String id = java.util.UUID.randomUUID().toString();

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
    public String getId() { return id; }

    /** Solo para la coronación: la Dama nueva hereda el id del peón original. */
    void setId(String id) { this.id = id; }
}