package com.chess.ports;
import com.chess.core.PieceType;

public interface SoundPort {
    void playMoveSound(PieceType type);
    void playCaptureSound();
    void playCheckSound();
    void playCheckmateJingle();
}