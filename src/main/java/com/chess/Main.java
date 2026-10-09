package com.chess;

import com.chess.adapters.*;
import com.chess.core.*;
import com.chess.ports.*; // <-- Agrega esta línea
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        ChessBoard board = new ArrayChessBoard();

        // 1. Setup inicial de piezas
        // Blancas
        board.placePiece(new Rook(Color.WHITE, new Position(0, 0)), new Position(0, 0));
        board.placePiece(new Knight(Color.WHITE, new Position(0, 1)), new Position(0, 1));
        board.placePiece(new Bishop(Color.WHITE, new Position(0, 2)), new Position(0, 2));
        board.placePiece(new Queen(Color.WHITE, new Position(0, 3)), new Position(0, 3));
        board.placePiece(new King(Color.WHITE, new Position(0, 4)), new Position(0, 4));
        board.placePiece(new Bishop(Color.WHITE, new Position(0, 5)), new Position(0, 5));
        board.placePiece(new Knight(Color.WHITE, new Position(0, 6)), new Position(0, 6));
        board.placePiece(new Rook(Color.WHITE, new Position(0, 7)), new Position(0, 7));
        for (int i = 0; i < 8; i++) board.placePiece(new Pawn(Color.WHITE, new Position(1, i)), new Position(1, i));

        // Negras
        board.placePiece(new Rook(Color.BLACK, new Position(7, 0)), new Position(7, 0));
        board.placePiece(new Knight(Color.BLACK, new Position(7, 1)), new Position(7, 1));
        board.placePiece(new Bishop(Color.BLACK, new Position(7, 2)), new Position(7, 2));
        board.placePiece(new Queen(Color.BLACK, new Position(7, 3)), new Position(7, 3));
        board.placePiece(new King(Color.BLACK, new Position(7, 4)), new Position(7, 4));
        board.placePiece(new Bishop(Color.BLACK, new Position(7, 5)), new Position(7, 5));
        board.placePiece(new Knight(Color.BLACK, new Position(7, 6)), new Position(7, 6));
        board.placePiece(new Rook(Color.BLACK, new Position(7, 7)), new Position(7, 7));
        for (int i = 0; i < 8; i++) board.placePiece(new Pawn(Color.BLACK, new Position(6, i)), new Position(6, i));

        // 2. Inicialización de dependencias
        GameManager gameManager = new GameManager(board);
        ConsoleRenderer renderer = new ConsoleRenderer();
        AnimalCrossingSoundAdapter sound = new AnimalCrossingSoundAdapter();
        AIStrategy ai = new MediumAIStrategy(); 
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== TPO Ajedrez: Humano (Blancas) vs IA (Negras) ===");

        // 3. Bucle interactivo de juego
        boolean gameRunning = true;
        
        while (gameRunning) {
            renderer.render(board);
            
            if (gameManager.getCurrentTurn() == Color.WHITE) {
                System.out.println("\nTu turno (Blancas). Ingresa origen y destino separando con espacios (ej: '1 0 2 0'):");
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
                System.out.println("\nTurno de la IA (Negras)...");
                Move aiMove = ai.getNextMove(board, Color.BLACK);
                if (aiMove != null) {
                    gameManager.tryMove(aiMove.getFrom(), aiMove.getTo());
                    sound.playMoveSound(board.getPieceAt(aiMove.getTo()).getType());
                } else {
                    System.out.println("La IA no tiene movimientos válidos. ¡Fin del juego!");
                    gameRunning = false;
                }
            }
        }
        scanner.close(); 
    }
}