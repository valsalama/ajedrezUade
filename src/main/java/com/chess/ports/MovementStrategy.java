package com.chess.ports;

import com.chess.core.Position;
import java.util.List;

public interface MovementStrategy {
    List<Position> getValidMoves(Position from, ChessBoard board);
}