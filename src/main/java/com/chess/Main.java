package com.chess;

import com.chess.adapters.*;
import com.chess.core.*;
import com.chess.ports.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        ChessBoard board = new ArrayChessBoard();
        setupInitialPosition(board);

        GameManager gameManager = new GameManager(board);
        ConsoleRenderer renderer = new ConsoleRenderer();
        SoundPort sound = new AnimalCrossingSoundAdapter();
        AIStrategy ai = new MediumAIStrategy();
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== TPO Ajedrez: Humano (Blancas, MAYÚSCULAS, abajo) vs IA (Negras, minúsculas, arriba) ===");
        System.out.println("Para mover escribí fila y columna de origen y de destino. Ejemplo: 6 0 5 0");
        System.out.println("Escribí 'salir' para terminar.");

        boolean gameRunning = true;
        while (gameRunning) {
            System.out.println();
            renderer.render(board);

            if (gameManager.getCurrentTurn() == Color.WHITE) {
                System.out.print("\nTu turno (Blancas) > ");
                String line = scanner.hasNextLine() ? scanner.nextLine().trim() : "salir";
                if (line.equalsIgnoreCase("salir")) break;

                int[] n = parseMove(line);
                if (n == null) {
                    System.out.println("Entrada no válida. Escribí 4 números del 0 al 7, por ejemplo: 6 0 5 0");
                    continue;                                   // no se pierde el turno
                }
                Position from = new Position(n[0], n[1]);
                Position to = new Position(n[2], n[3]);

                MoveResult result = gameManager.tryMove(from, to);
                if (result == MoveResult.SUCCESS) {
                    playMoveSound(sound, board.getPieceAt(to));
                } else if (result == MoveResult.NOT_YOUR_TURN) {
                    System.out.println("Esa pieza no es tuya (vos jugás con las MAYÚSCULAS).");
                } else {
                    System.out.println("Movimiento inválido para esa pieza.");
                }
            } else {
                System.out.println("\nTurno de la IA (Negras)...");
                if (!playAiTurn(gameManager, board, ai, sound)) {
                    System.out.println("La IA no tiene movimientos legales. ¡Fin del juego!");
                    gameRunning = false;
                }
            }
        }
        scanner.close();
    }

    /** Juega el turno de la IA. Si su jugada preferida es ilegal (deja al rey en jaque), prueba las demás. */
    private static boolean playAiTurn(GameManager gm, ChessBoard board, AIStrategy ai, SoundPort sound) {
        Move preferred = ai.getNextMove(board, Color.BLACK);
        if (preferred != null && gm.tryMove(preferred.getFrom(), preferred.getTo()) == MoveResult.SUCCESS) {
            playMoveSound(sound, board.getPieceAt(preferred.getTo()));
            return true;
        }
        for (Move m : allMoves(board, Color.BLACK)) {
            if (gm.tryMove(m.getFrom(), m.getTo()) == MoveResult.SUCCESS) {
                playMoveSound(sound, board.getPieceAt(m.getTo()));
                return true;
            }
        }
        return false;
    }

    private static List<Move> allMoves(ChessBoard board, Color color) {
        List<Move> moves = new ArrayList<>();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Position pos = new Position(r, c);
                Piece piece = board.getPieceAt(pos);
                if (piece != null && piece.getColor() == color) {
                    for (Position dest : piece.getPossibleMoves(board)) moves.add(new Move(pos, dest));
                }
            }
        }
        return moves;
    }

    /** Devuelve {filaOrigen, colOrigen, filaDestino, colDestino} o null si la entrada no es válida. */
    private static int[] parseMove(String line) {
        String[] parts = line.split("\\s+");
        if (parts.length != 4) return null;
        int[] n = new int[4];
        try {
            for (int i = 0; i < 4; i++) {
                n[i] = Integer.parseInt(parts[i]);
                if (n[i] < 0 || n[i] > 7) return null;
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return n;
    }

    /** El sonido nunca debería tumbar el juego (por ejemplo, si no hay placa de audio). */
    private static void playMoveSound(SoundPort sound, Piece movedPiece) {
        try {
            if (movedPiece != null) sound.playMoveSound(movedPiece.getType());
        } catch (Exception e) {
            // sin audio: se ignora
        }
    }

    /** Blancas abajo (fila 7 piezas, fila 6 peones). Negras arriba (fila 0 piezas, fila 1 peones). */
    private static void setupInitialPosition(ChessBoard board) {
        placeBackRank(board, Color.WHITE, 7);
        placeBackRank(board, Color.BLACK, 0);
        for (int col = 0; col < 8; col++) {
            board.placePiece(new Pawn(Color.WHITE, new Position(6, col)), new Position(6, col));
            board.placePiece(new Pawn(Color.BLACK, new Position(1, col)), new Position(1, col));
        }
    }

    private static void placeBackRank(ChessBoard board, Color color, int row) {
        Piece[] pieces = {
            new Rook(color, new Position(row, 0)),   new Knight(color, new Position(row, 1)),
            new Bishop(color, new Position(row, 2)), new Queen(color, new Position(row, 3)),
            new King(color, new Position(row, 4)),   new Bishop(color, new Position(row, 5)),
            new Knight(color, new Position(row, 6)), new Rook(color, new Position(row, 7))
        };
        for (int col = 0; col < 8; col++) board.placePiece(pieces[col], new Position(row, col));
    }
}