package com.chess;

import com.chess.adapters.*;
import com.chess.core.*;
// import com.chess.core.ArrayChessBoard; // Descomentar cuando Track C esté integrado
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        // Se asume que ArrayChessBoard fue creado por AgosL en el Track C
        /*
        ChessBoard board = new ArrayChessBoard();
        // Inicializar piezas en el tablero aquí...
        GameManager gameManager = new GameManager(board);
        */
        
        ConsoleRenderer renderer = new ConsoleRenderer();
        AnimalCrossingSoundAdapter sound = new AnimalCrossingSoundAdapter();
        AIStrategy ai = new MediumAIStrategy(); // Usamos la IA media
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== TPO Ajedrez: Humano (Blancas) vs IA (Negras) ===");

        /* BUCLE DE JUEGO (Descomentar al integrar todo)
        boolean gameRunning = true;
        
        while (gameRunning) {
            renderer.render(board);
            
            if (gameManager.getCurrentTurn() == Color.WHITE) {
                System.out.println("Tu turno (Blancas). Ingresa origen y destino (ej: '6 4 4 4'):");
                int fromRow = scanner.nextInt();
                int fromCol = scanner.nextInt();
                int toRow = scanner.nextInt();
                int toCol = scanner.nextInt();
                
                Position from = new Position(fromRow, fromCol);
                Position to = new Position(toRow, toCol);
                
                MoveResult result = gameManager.tryMove(from, to);
                if (result == MoveResult.SUCCESS) {
                    sound.playMoveSound(board.getPieceAt(to).getType());
                } else {
                    System.out.println("Movimiento inválido: " + result);
                }
            } else {
                System.out.println("Turno de la IA (Negras)...");
                Move aiMove = ai.getNextMove(board, Color.BLACK);
                if (aiMove != null) {
                    gameManager.tryMove(aiMove.getFrom(), aiMove.getTo());
                    sound.playMoveSound(board.getPieceAt(aiMove.getTo()).getType());
                }
            }
        }
        */
    }
}