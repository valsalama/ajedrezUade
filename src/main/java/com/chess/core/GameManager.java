package com.chess.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.chess.ports.ChessBoard;

public class GameManager {

    /** Todo lo que hace falta recordar de una jugada para poder deshacerla del todo. */
    private static class HistoryEntry {
        Command command;
        Color mover;                // quién movió
        Piece captured;             // pieza comida (null si no comió)
        int previousHalfmoveClock;
        Position previousEnPassantTarget;
        Position previousEnPassantVictim;
        String positionKey;         // clave de la posición que quedó después de la jugada
    }

    private ChessBoard board;
    private Color currentTurn;
    private CheckDetector checkDetector;
    private MoveValidator moveValidator;
    private Deque<HistoryEntry> history;
    private Map<Color, List<Piece>> capturedPieces; // Añadido para los tests de AgosL

    // Peón al paso: casilla "salteada" por un peón que acaba de avanzar dos, y
    // posición de ese peón (el que se puede comer). Ambos son null si no aplica.
    private Position enPassantTarget;
    private Position enPassantVictim;

    // Regla de los 50 movimientos (se cuenta en medias jugadas) y repetición de posiciones
    private int halfmoveClock;
    private Map<String, Integer> positionCounts;

    // Solo se puede deshacer una vez seguida: después de deshacer, el botón
    // queda bloqueado hasta que se haga una jugada nueva.
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
        this.positionCounts = new HashMap<>();
    }

    public Color getCurrentTurn() { return currentTurn; }

    public List<Piece> getCapturedPieces(Color color) {
        return capturedPieces.get(color);
    }

    // ------------------------------------------------------------------
    // Movimientos legales
    // ------------------------------------------------------------------

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

        if (piece.getType() == PieceType.PAWN && canCaptureEnPassant(piece, from)) {
            legalMoves.add(enPassantTarget);
        }

        if (piece.getType() == PieceType.KING) {
            for (int rookCol : new int[] {0, 7}) {
                Position rookFrom = new Position(from.getRow(), rookCol);
                if (canCastle(from, rookFrom)) {
                    int direction = rookCol > from.getColumn() ? 1 : -1;
                    legalMoves.add(new Position(from.getRow(), from.getColumn() + 2 * direction));
                }
            }
        }
        return legalMoves;
    }

    /** True si el jugador al que le toca tiene al menos una jugada legal. */
    public boolean hasAnyLegalMove() {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (!getLegalMoves(new Position(r, c)).isEmpty()) return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Jugar
    // ------------------------------------------------------------------

    public MoveResult tryMove(Position from, Position to) {
        Piece piece = board.getPieceAt(from);
        if (piece == null) return MoveResult.INVALID_MOVE;
        if (piece.getColor() != currentTurn) return MoveResult.NOT_YOUR_TURN;

        // Enroque: se pide moviendo el rey dos columnas hacia una de sus torres
        if (piece.getType() == PieceType.KING
                && from.getRow() == to.getRow()
                && Math.abs(to.getColumn() - from.getColumn()) == 2) {
            int rookCol = to.getColumn() > from.getColumn() ? 7 : 0;
            return tryCastle(from, new Position(from.getRow(), rookCol));
        }

        // Peón al paso
        if (piece.getType() == PieceType.PAWN && to.equals(enPassantTarget) && canCaptureEnPassant(piece, from)) {
            return executeEnPassant(piece, from, to);
        }

        if (!moveValidator.isValidMove(board, piece, to)) return MoveResult.INVALID_MOVE;

        if (leavesKingInCheck(from, to, currentTurn)) {
            return MoveResult.INVALID_MOVE;
        }

        // Si hay una pieza enemiga en el destino, registrarla como capturada
        Piece targetPiece = board.getPieceAt(to);

        // El movimiento se hace a través de un MoveCommand y se guarda en el
        // historial, así después se puede deshacer con undoLastMove()
        MoveCommand command = new MoveCommand(board, from, to, piece, targetPiece);
        HistoryEntry entry = beginEntry(command, targetPiece);
        command.execute();

        boolean pawnMove = piece.getType() == PieceType.PAWN;
        boolean doubleStep = pawnMove && Math.abs(to.getRow() - from.getRow()) == 2;
        finishMove(entry, pawnMove || targetPiece != null,
                doubleStep ? new Position((from.getRow() + to.getRow()) / 2, from.getColumn()) : null,
                doubleStep ? to : null);
        return MoveResult.SUCCESS;
    }

    public MoveResult tryCastle(Position kingFrom, Position rookFrom) {
        Piece king = board.getPieceAt(kingFrom);
        Piece rook = board.getPieceAt(rookFrom);

        if (king == null || rook == null || king.getType() != PieceType.KING || rook.getType() != PieceType.ROOK)
            return MoveResult.INVALID_MOVE;
        if (king.getColor() != currentTurn || rook.getColor() != currentTurn)
            return MoveResult.NOT_YOUR_TURN;
        if (!canCastle(kingFrom, rookFrom)) return MoveResult.INVALID_MOVE;

        int direction = kingFrom.getColumn() < rookFrom.getColumn() ? 1 : -1;
        Position newKingPos = new Position(kingFrom.getRow(), kingFrom.getColumn() + (direction * 2));
        Position newRookPos = new Position(kingFrom.getRow(), newKingPos.getColumn() - direction);

        // El enroque son dos movimientos (rey y torre) que se guardan como un
        // solo comando, así un "deshacer" los revierte a los dos juntos
        MoveCommand kingMove = new MoveCommand(board, kingFrom, newKingPos, king, null);
        MoveCommand rookMove = new MoveCommand(board, rookFrom, newRookPos, rook, null);
        Command castle = new Command() {
            @Override public void execute() { kingMove.execute(); rookMove.execute(); }
            @Override public void undo() { rookMove.undo(); kingMove.undo(); }
        };
        HistoryEntry entry = beginEntry(castle, null);
        castle.execute();
        finishMove(entry, false, null, null);
        return MoveResult.SUCCESS;
    }

    private MoveResult executeEnPassant(Piece pawn, Position from, Position to) {
        Piece victim = board.getPieceAt(enPassantVictim);
        MoveCommand pawnMove = new MoveCommand(board, from, to, pawn, null);
        Position victimPos = enPassantVictim;
        Command enPassant = new Command() {
            @Override public void execute() {
                board.removePiece(victimPos);
                pawnMove.execute();
            }
            @Override public void undo() {
                pawnMove.undo();
                board.placePiece(victim, victimPos);
            }
        };
        HistoryEntry entry = beginEntry(enPassant, victim);
        enPassant.execute();
        finishMove(entry, true, null, null);
        return MoveResult.SUCCESS;
    }

    // ------------------------------------------------------------------
    // Reglas especiales: enroque y peón al paso
    // ------------------------------------------------------------------

    /**
     * Enroque legal: ni el rey ni la torre se movieron, están en la misma fila,
     * no hay nada entre ellos, el rey no está en jaque y no pasa ni cae en una
     * casilla atacada.
     */
    private boolean canCastle(Position kingFrom, Position rookFrom) {
        Piece king = board.getPieceAt(kingFrom);
        Piece rook = board.getPieceAt(rookFrom);

        if (king == null || rook == null) return false;
        if (king.getType() != PieceType.KING || rook.getType() != PieceType.ROOK) return false;
        if (king.getColor() != rook.getColor()) return false;
        if (kingFrom.getRow() != rookFrom.getRow()) return false;
        if (king.hasMoved() || rook.hasMoved()) return false;
        if (checkDetector.isInCheck(king.getColor(), board)) return false;

        int direction = kingFrom.getColumn() < rookFrom.getColumn() ? 1 : -1;
        for (int col = kingFrom.getColumn() + direction; col != rookFrom.getColumn(); col += direction) {
            if (!board.isEmpty(new Position(kingFrom.getRow(), col))) return false;
        }

        // El rey no puede pasar por una casilla atacada ni terminar en una
        for (int step = 1; step <= 2; step++) {
            Position crossing = new Position(kingFrom.getRow(), kingFrom.getColumn() + direction * step);
            if (leavesKingInCheck(kingFrom, crossing, king.getColor())) return false;
        }
        return true;
    }

    private boolean canCaptureEnPassant(Piece pawn, Position from) {
        if (enPassantTarget == null || enPassantVictim == null) return false;
        if (pawn.getColor() != currentTurn) return false;

        Piece victim = board.getPieceAt(enPassantVictim);
        if (victim == null || victim.getType() != PieceType.PAWN || victim.getColor() == pawn.getColor()) return false;
        if (!board.isEmpty(enPassantTarget)) return false;

        // El peón tiene que estar al lado del que acaba de avanzar dos casillas
        if (from.getRow() != enPassantVictim.getRow()) return false;
        if (Math.abs(from.getColumn() - enPassantVictim.getColumn()) != 1) return false;

        // Y la captura no puede dejar al propio rey en jaque
        ChessBoard clone = board.clone();
        clone.removePiece(from);
        clone.removePiece(enPassantVictim);
        clone.placePiece(pawn, enPassantTarget);
        return !checkDetector.isInCheck(pawn.getColor(), clone);
    }

    private boolean leavesKingInCheck(Position from, Position to, Color color) {
        ChessBoard clonedBoard = board.clone();
        Piece clonedPiece = clonedBoard.getPieceAt(from);
        clonedBoard.removePiece(from);
        clonedBoard.placePiece(clonedPiece, to);
        return checkDetector.isInCheck(color, clonedBoard);
    }

    // ------------------------------------------------------------------
    // Historial y deshacer
    // ------------------------------------------------------------------

    private HistoryEntry beginEntry(Command command, Piece captured) {
        // La primera vez se cuenta la posición inicial (el tablero se arma
        // después de crear el GameManager, por eso no se puede hacer en el constructor)
        if (positionCounts.isEmpty()) {
            positionCounts.merge(positionKey(), 1, Integer::sum);
        }

        HistoryEntry entry = new HistoryEntry();
        entry.command = command;
        entry.mover = currentTurn;
        entry.captured = captured;
        entry.previousHalfmoveClock = halfmoveClock;
        entry.previousEnPassantTarget = enPassantTarget;
        entry.previousEnPassantVictim = enPassantVictim;
        if (captured != null) {
            capturedPieces.get(currentTurn).add(captured);
        }
        return entry;
    }

    /** Se llama justo después de ejecutar la jugada: cambia el turno y actualiza los contadores. */
    private void finishMove(HistoryEntry entry, boolean resetsHalfmoveClock,
                            Position newEnPassantTarget, Position newEnPassantVictim) {
        halfmoveClock = resetsHalfmoveClock ? 0 : halfmoveClock + 1;
        enPassantTarget = newEnPassantTarget;
        enPassantVictim = newEnPassantVictim;
        currentTurn = (currentTurn == Color.WHITE) ? Color.BLACK : Color.WHITE;

        entry.positionKey = positionKey();
        positionCounts.merge(entry.positionKey, 1, Integer::sum);
        history.push(entry);
        undoAvailable = true;
    }

    public boolean canUndo() {
        return undoAvailable && !history.isEmpty();
    }

    /** Deshace una sola jugada (la última que se hizo, de quien sea). */
    public void undoLastMove() {
        if (!canUndo()) return;
        revertTopEntry();
        undoAvailable = false;
    }

    private void revertTopEntry() {
        HistoryEntry entry = history.pop();
        entry.command.undo();

        currentTurn = entry.mover;
        halfmoveClock = entry.previousHalfmoveClock;
        enPassantTarget = entry.previousEnPassantTarget;
        enPassantVictim = entry.previousEnPassantVictim;

        // Si esa jugada había comido una pieza, la sacamos de la bandeja de quien la comió
        if (entry.captured != null) {
            capturedPieces.get(entry.mover).remove(entry.captured);
        }

        positionCounts.computeIfPresent(entry.positionKey, (key, count) -> count > 1 ? count - 1 : null);
    }

    /**
     * Deshace una "ronda" completa contra la IA: la respuesta de la IA y la
     * jugada del humano que la provocó, y nada más. Si la IA todavía no había
     * respondido (por ejemplo, porque la jugada del humano terminó la partida),
     * solo deshace la jugada del humano. Después de deshacer no se puede volver
     * a deshacer hasta que se haga una jugada nueva.
     */
    public boolean undoLastRound(Color humanColor) {
        if (!canUndo()) return false;
        revertTopEntry();
        if (currentTurn != humanColor && !history.isEmpty()) {
            revertTopEntry();
        }
        undoAvailable = false;
        return true;
    }

    /** Deja el manager como al principio de una partida nueva (turno, capturas e historial). */
    public void reset() {
        currentTurn = Color.WHITE;
        history.clear();
        capturedPieces.get(Color.WHITE).clear();
        capturedPieces.get(Color.BLACK).clear();
        enPassantTarget = null;
        enPassantVictim = null;
        halfmoveClock = 0;
        positionCounts.clear();
        undoAvailable = false;
    }

    // ------------------------------------------------------------------
    // Estado de la partida (jaque, jaque mate, tablas)
    // ------------------------------------------------------------------

    public GameState getGameState() {
        boolean inCheck = checkDetector.isInCheck(currentTurn, board);
        boolean hasLegalMove = hasAnyLegalMove();

        if (inCheck && !hasLegalMove) return GameState.CHECKMATE;
        if (getDrawReason() != DrawReason.NONE) return GameState.DRAW;
        return inCheck ? GameState.CHECK : GameState.IN_PROGRESS;
    }

    public DrawReason getDrawReason() {
        boolean inCheck = checkDetector.isInCheck(currentTurn, board);
        if (!inCheck && !hasAnyLegalMove()) return DrawReason.STALEMATE;
        if (inCheck && !hasAnyLegalMove()) return DrawReason.NONE; // jaque mate, no son tablas
        if (hasInsufficientMaterial()) return DrawReason.INSUFFICIENT_MATERIAL;
        if (positionCounts.getOrDefault(positionKey(), 0) >= 3) return DrawReason.THREEFOLD_REPETITION;
        if (halfmoveClock >= 100) return DrawReason.FIFTY_MOVE_RULE;
        return DrawReason.NONE;
    }

    /**
     * Material insuficiente para dar mate: rey solo contra rey, rey y un
     * alfil/caballo contra rey, o solo alfiles todos en casillas del mismo color.
     */
    private boolean hasInsufficientMaterial() {
        int minorPieces = 0;
        int knights = 0;
        int bishopsOnLight = 0;
        int bishopsOnDark = 0;

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece piece = board.getPieceAt(new Position(r, c));
                if (piece == null) continue;
                switch (piece.getType()) {
                    case KING:
                        break;
                    case KNIGHT:
                        knights++;
                        minorPieces++;
                        break;
                    case BISHOP:
                        if ((r + c) % 2 == 0) bishopsOnDark++; else bishopsOnLight++;
                        minorPieces++;
                        break;
                    default:
                        return false; // peón, torre o dama: siempre alcanza para mate
                }
            }
        }

        if (minorPieces <= 1) return true;
        return knights == 0 && (bishopsOnLight == 0 || bishopsOnDark == 0);
    }

    /** Identifica una posición para detectar repeticiones: piezas, turno, derechos de enroque y peón al paso. */
    private String positionKey() {
        StringBuilder key = new StringBuilder();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece piece = board.getPieceAt(new Position(r, c));
                if (piece == null) {
                    key.append('.');
                } else {
                    key.append(piece.getColor() == Color.WHITE ? 'w' : 'b');
                    key.append(piece.getType().name().charAt(0));
                    if (piece.getType() == PieceType.KING || piece.getType() == PieceType.ROOK) {
                        key.append(piece.hasMoved() ? 'm' : 'u');
                    }
                }
            }
        }
        key.append(currentTurn == Color.WHITE ? "|W" : "|B");

        // El peón al paso solo cuenta si de verdad se puede ejecutar
        if (enPassantTarget != null && enPassantCapturePossible()) {
            key.append("|ep").append(enPassantTarget.getRow()).append(enPassantTarget.getColumn());
        }
        return key.toString();
    }

    private boolean enPassantCapturePossible() {
        for (int dc : new int[] {-1, 1}) {
            int col = enPassantVictim.getColumn() + dc;
            if (col < 0 || col > 7) continue;
            Position from = new Position(enPassantVictim.getRow(), col);
            Piece piece = board.getPieceAt(from);
            if (piece != null && piece.getType() == PieceType.PAWN && canCaptureEnPassant(piece, from)) {
                return true;
            }
        }
        return false;
    }
}
