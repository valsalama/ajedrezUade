package com.chess.adapters;

import com.chess.core.PieceType;
import com.chess.ports.SoundPort;
import javax.sound.sampled.*;

public class AnimalCrossingSoundAdapter implements SoundPort {
    private final DialogBox dialogBox;

    public AnimalCrossingSoundAdapter() {
        this(new DialogBox());
    }

    public AnimalCrossingSoundAdapter(DialogBox dialogBox) {
        this.dialogBox = dialogBox;
    }

    private void generateTone(int hz, int msecs) {
        try {
            float sampleRate = 8000f;
            byte[] buf = new byte[1];
            AudioFormat af = new AudioFormat(sampleRate, 8, 1, true, false);
            SourceDataLine sdl = AudioSystem.getSourceDataLine(af);
            sdl.open(af);
            sdl.start();
            for (int i = 0; i < msecs * 8; i++) {
                double angle = i / (sampleRate / hz) * 2.0 * Math.PI;
                buf[0] = (byte) (Math.sin(angle) * 127.0);
                sdl.write(buf, 0, 1);
            }
            sdl.drain();
            sdl.stop();
            sdl.close();
        } catch (LineUnavailableException e) {
            System.err.println("Audio no disponible");
        }
    }

    @Override
    public void playMoveSound(PieceType type) {
        dialogBox.showMessage("* " + type + " se mueve *", '.');
        int freq = type == PieceType.KING ? 300 : type == PieceType.QUEEN ? 700 : 500;
        generateTone(freq, 150);
    }

    @Override
    public void playCaptureSound() { generateTone(200, 250); }

    @Override
    public void playCheckSound() { generateTone(800, 300); }

    @Override
    public void playCheckmateJingle() { generateTone(1000, 500); }
}
