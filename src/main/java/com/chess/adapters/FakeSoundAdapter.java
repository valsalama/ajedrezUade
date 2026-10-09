package com.chess.adapters;

import com.chess.core.PieceType;
import com.chess.ports.SoundPort;

public class FakeSoundAdapter implements SoundPort {
    public int moveSoundCalls = 0;
    public int captureSoundCalls = 0;
    public int checkSoundCalls = 0;
    public int checkmateJingleCalls = 0;

    @Override
    public void playMoveSound(PieceType type) { moveSoundCalls++; }

    @Override
    public void playCaptureSound() { captureSoundCalls++; }

    @Override
    public void playCheckSound() { checkSoundCalls++; }

    @Override
    public void playCheckmateJingle() { checkmateJingleCalls++; }
}