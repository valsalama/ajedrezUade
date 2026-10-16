package com.chess.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.chess.ports.ChessBoard;

public class GameManager {
    private ChessBoard board;
    private Color currentTurn;
    private CheckDetector checkDetector;
    private MoveValidator moveValidator;
    private Deque<Command> history;
    private Map<Color, List<Piece>> capturedPieces; // Añadido para los tests de AgosL
    private boolean undoAvailable;

    public GameManager(ChessBoard board) {
        this.board = board;
        this.currentTurn = Color.WHITE;
        this.checkDetector = new CheckDetector();
        this.moveValidator = new MoveValidator();
        this.history = new ArrayDeque<>();
        this.capturedPieces = new HashMap<>();
        this.capturedPieces.put(Color.WHITE, new ArrayList<>());
        this.capturedPieces.put(Color.BLACK, new ArrayList<>());
        this.undoAvailable = false;
    }

    public Color getCurrentTurn() { return currentTurn; }

    public List<Piece> getCapturedPieces(Color color) { 
        return capturedPieces.get(color); 
    }

    public List<Position> getLegalMoves(Position from) {
        Piece piece = board.getPieceAt(from);
        if (piece == null || piece.getColor() != currentTurn) return new ArrayList<>();

        List<Position> legalMoves = new ArrayList<>();
        for (Position destination : piece.getPossibleMoves(board)) {
            if (moveValidator.isValidMove(board, piece, destination)
                    && !leavesKingInCheck(from, destination, currentTurn)) {
                legalMoves.add(destination);
            }
        }
        return legalMoves;
    }

    public MoveResult tryMove(Position from, Position to) {
        Piece piece = board.getPieceAt(from);
        if (piece == null) return MoveResult.INVALID_MOVE;
        if (piece.getColor() != currentTurn) return MoveResult.NOT_YOUR_TURN;
        if (!moveValidator.isValidMove(board, piece, to)) return MoveResult.INVALID_MOVE;

        if (leavesKingInCheck(from, to, currentTurn)) {
            return MoveResult.INVALID_MOVE;
        }

        // Si hay una pieza enemiga en el destino, registrarla como capturada
        Piece targetPiece = board.getPieceAt(to);
        if (targetPiece != null) {
            capturedPieces.get(currentTurn).add(targetPiece);
        }

        // El movimiento se hace a través de un MoveCommand y se guarda en el
        // historial, así después se puede deshacer con undoLastMove()
        MoveCommand command = new MoveCommand(board, from, to, piece, targetPiece);
        command.execute();
        history.push(command);
        undoAvailable = true;

        currentTurn = (currentTurn == Color.WHITE) ? Color.BLACK : Color.WHITE;
        return MoveResult.SUCCESS;
    }

    private boolean leavesKingInCheck(Position from, Position to, Color color) {
        ChessBoard clonedBoard = board.clone();
        Piece clonedPiece = clonedBoard.getPieceAt(from);
        clonedBoard.removePiece(from);
        clonedBoard.placePiece(clonedPiece, to);
        return checkDetector.isInCheck(color, clonedBoard);
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

        Position newKingPos = new Position(kingFrom.getRow(), kingFrom.getColumn() + (direction * 2));
        Position newRookPos = new Position(kingFrom.getRow(), newKingPos.getColumn() - direction);

        // El enroque son dos movimientos (rey y torre) que se guardan como un
        // solo comando, así un "deshacer" los revierte a los dos juntos
        MoveCommand kingMove = new MoveCommand(board, kingFrom, newKingPos, king, null);
        MoveCommand rookMove = new MoveCommand(board, rookFrom, newRookPos, rook, null);
        kingMove.execute();
        rookMove.execute();
        history.push(new Command() {
            @Override public void execute() { kingMove.execute(); rookMove.execute(); }
            @Override public void undo() { rookMove.undo(); kingMove.undo(); }
        });
        undoAvailable = true;

        currentTurn = (currentTurn == Color.WHITE) ? Color.BLACK : Color.WHITE;
        return MoveResult.SUCCESS;
    }

    public boolean canUndo() {
        return undoAvailable && !history.isEmpty();
    }

    public void undoLastMove() {
        if (canUndo()) {
            Command lastCommand = history.pop();
            lastCommand.undo();
            currentTurn = (currentTurn == Color.WHITE) ? Color.BLACK : Color.WHITE;
            undoAvailable = false;

            // Si ese movimiento había comido una pieza, la sacamos de la lista
            // de capturadas del jugador que la había comido (el del turno actual)
            if (lastCommand instanceof MoveCommand move && move.getCapturedPiece() != null) {
                List<Piece> captured = capturedPieces.get(currentTurn);
                captured.remove(captured.size() - 1);
            }
        }
    }

    /** Deja el manager como al principio de una partida nueva (turno, capturas e historial). */
    public void reset() {
        currentTurn = Color.WHITE;
        history.clear();
        undoAvailable = false;
        capturedPieces.get(Color.WHITE).clear();
        capturedPieces.get(Color.BLACK).clear();
    }
}