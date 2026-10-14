package com.chess.core;

import com.chess.ports.ChessBoard;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameManager {
    private ChessBoard board;
    private Color currentTurn;
    private CheckDetector checkDetector;
    private MoveValidator moveValidator;
    private Deque<Command> history;
    private Map<Color, List<Piece>> capturedPieces; // Añadido para los tests de AgosL

    public GameManager(ChessBoard board) {
        this.board = board;
        this.currentTurn = Color.WHITE;
        this.checkDetector = new CheckDetector();
        this.moveValidator = new MoveValidator();
        this.history = new ArrayDeque<>();
        this.capturedPieces = new HashMap<>();
        this.capturedPieces.put(Color.WHITE, new ArrayList<>());
        this.capturedPieces.put(Color.BLACK, new ArrayList<>());
    }

    public Color getCurrentTurn() { return currentTurn; }

    public List<Piece> getCapturedPieces(Color color) { 
        return capturedPieces.get(color); 
    }

    public MoveResult tryMove(Position from, Position to) {
        Piece piece = board.getPieceAt(from);
        if (piece == null) return MoveResult.INVALID_MOVE;
        if (piece.getColor() != currentTurn) return MoveResult.NOT_YOUR_TURN;
        if (!moveValidator.isValidMove(board, piece, to)) return MoveResult.INVALID_MOVE;

        ChessBoard clonedBoard = board.clone();
        Piece clonedPiece = clonedBoard.getPieceAt(from);
        clonedBoard.removePiece(from);
        clonedBoard.placePiece(clonedPiece, to);

        if (checkDetector.isInCheck(currentTurn, clonedBoard)) {
            return MoveResult.INVALID_MOVE;
        }

        // Si hay una pieza enemiga en el destino, registrarla como capturada
        Piece targetPiece = board.getPieceAt(to);
        if (targetPiece != null) {
            capturedPieces.get(currentTurn).add(targetPiece);
        }

        // El movimiento ahora se hace a través de un Command, para que quede
        // en el historial y se pueda deshacer con undoLastMove().
        MoveCommand command = new MoveCommand(board, from, to, piece, targetPiece);
        command.execute();
        history.push(command);

        // Coronación: un peón que llega a la última fila se convierte en Dama.
        promoteIfNeeded(piece, to);

        currentTurn = (currentTurn == Color.WHITE) ? Color.BLACK : Color.WHITE;
        return MoveResult.SUCCESS;
    }

    /**
     * Si la pieza que acaba de moverse es un peón y llegó a la última fila
     * (7 para Blancas, 0 para Negras), lo reemplaza por una Dama. Es la
     * regla de coronación simplificada: siempre corona a Dama, que es la
     * pieza más fuerte y la elección casi universal en la práctica. Si en
     * algún momento se quiere dejar elegir la pieza (Torre/Alfil/Caballo),
     * este es el único lugar que habría que tocar para eso.
     */
    private void promoteIfNeeded(Piece piece, Position to) {
        if (piece.getType() != PieceType.PAWN) return;

        int lastRow = (piece.getColor() == Color.WHITE) ? 7 : 0;
        if (to.getRow() != lastRow) return;

        Queen promoted = new Queen(piece.getColor(), to);
        promoted.setId(piece.getId());
        board.removePiece(to);
        board.placePiece(promoted, to);
    }

    public MoveResult tryCastle(Position kingFrom, Position rookFrom) {
        Piece king = board.getPieceAt(kingFrom);
        Piece rook = board.getPieceAt(rookFrom);

        if (king == null || rook == null || king.getType() != PieceType.KING || rook.getType() != PieceType.ROOK) 
            return MoveResult.INVALID_MOVE;
        if (king.getColor() != currentTurn || rook.getColor() != currentTurn) 
            return MoveResult.NOT_YOUR_TURN;
        if (king.hasMoved() || rook.hasMoved() || checkDetector.isInCheck(currentTurn, board)) 
            return MoveResult.INVALID_MOVE;

        int direction = kingFrom.getColumn() < rookFrom.getColumn() ? 1 : -1;
        for (int col = kingFrom.getColumn() + direction; col != rookFrom.getColumn(); col += direction) {
            if (!board.isEmpty(new Position(kingFrom.getRow(), col))) return MoveResult.INVALID_MOVE;
        }

        board.removePiece(kingFrom);
        board.removePiece(rookFrom);
        
        Position newKingPos = new Position(kingFrom.getRow(), kingFrom.getColumn() + (direction * 2));
        Position newRookPos = new Position(kingFrom.getRow(), newKingPos.getColumn() - direction);
        
        king.setPosition(newKingPos);
        rook.setPosition(newRookPos);
        board.placePiece(king, newKingPos);
        board.placePiece(rook, newRookPos);

        currentTurn = (currentTurn == Color.WHITE) ? Color.BLACK : Color.WHITE;
        return MoveResult.SUCCESS;
    }
    /** Para saber desde afuera si hay algo para deshacer (ej. deshabilitar el botón en la UI). */
    public boolean canUndo() {
        return !history.isEmpty();
    }

    public void undoLastMove() {
        if (history.isEmpty()) return;

        Command lastCommand = history.pop();
        lastCommand.undo();

        // Si el movimiento deshecho había capturado una pieza, hay que
        // sacarla de la lista de capturadas: si no, queda figurando como
        // comida a pesar de que el undo la devolvió al tablero.
        if (lastCommand instanceof MoveCommand) {
            MoveCommand moveCommand = (MoveCommand) lastCommand;
            Piece captured = moveCommand.getCapturedPiece();
            if (captured != null) {
                capturedPieces.get(moveCommand.getMovedPieceColor()).remove(captured);
            }
        }

        currentTurn = (currentTurn == Color.WHITE) ? Color.BLACK : Color.WHITE;
    }
}