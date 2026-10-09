package com.chess.adapters;

import com.chess.core.Color;
import com.chess.core.Piece;
import com.chess.core.PieceType;
import com.chess.core.Position;
import com.chess.ports.ChessBoard;

public class ConsoleRenderer {
    public void render(ChessBoard board) {
        System.out.println("  0  1  2  3  4  5  6  7");
        for (int r = 0; r < 8; r++) {
            System.out.print(r + " ");
            for (int c = 0; c < 8; c++) {
                Piece p = board.getPieceAt(new Position(r, c));
                if (p == null) {
                    System.out.print("[ ]");
                } else {
                    // Caballo = N (para no confundirlo con el Rey = K)
                    char letter = (p.getType() == PieceType.KNIGHT) ? 'N' : p.getType().name().charAt(0);
                    // Blancas en mayúscula, negras en minúscula
                    letter = (p.getColor() == Color.WHITE) ? Character.toUpperCase(letter) : Character.toLowerCase(letter);
                    System.out.print("[" + letter + "]");
                }
            }
            System.out.println();
        }
    }
}