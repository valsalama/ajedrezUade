package com.chess;

import com.chess.adapters.*;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        // Inicialización lista para cuando descomenten el bucle de juego
        // ChessBoard board = new ArrayChessBoard();
        // GameManager gameManager = new GameManager(board);
        
        ConsoleRenderer renderer = new ConsoleRenderer();
        AnimalCrossingSoundAdapter sound = new AnimalCrossingSoundAdapter();
        AIStrategy ai = new MediumAIStrategy(); 
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== TPO Ajedrez: Humano (Blancas) vs IA (Negras) ===");

        // El loop de juego va aquí (CHESS-E7)
        
        scanner.close(); // Evita el "Resource leak"
    }
}