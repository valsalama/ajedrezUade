package com.chess.ports;
import com.chess.core.Color;
import com.chess.core.Move;

public interface AIStrategy {
    Move getNextMove(ChessBoard board, Color color);
}