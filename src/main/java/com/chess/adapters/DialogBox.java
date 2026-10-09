package com.chess.adapters;

public class DialogBox {
    public void showMessage(String message, char bipCharacter) {
        for (char c : message.toCharArray()) {
            System.out.print(c);
            try {
                Thread.sleep(40); // Pausa estilo Animal Crossing
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        System.out.println();
    }
}