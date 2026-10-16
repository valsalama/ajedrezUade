package com.chess.core;

/** Motivo por el que una partida terminó en tablas (NONE si no hay tablas). */
public enum DrawReason {
    NONE,
    STALEMATE,
    THREEFOLD_REPETITION,
    INSUFFICIENT_MATERIAL,
    FIFTY_MOVE_RULE
}
