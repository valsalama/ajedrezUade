package com.chess.core;

import com.chess.adapters.FakeSoundAdapter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class FakeSoundAdapterTest {
    @Test
    public void testFakeSoundAdapterCountsCalls() {
        FakeSoundAdapter adapter = new FakeSoundAdapter();
        
        adapter.playMoveSound(PieceType.PAWN);
        adapter.playMoveSound(PieceType.ROOK);
        adapter.playCaptureSound();
        
        assertEquals(2, adapter.moveSoundCalls);
        assertEquals(1, adapter.captureSoundCalls);
        assertEquals(0, adapter.checkmateJingleCalls);
    }
}