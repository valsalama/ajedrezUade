package com.chess.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.chess.ports.ChessBoard;

public class GameManager {

    private static class HistoryEntry {
        Command command;
        Color mover;
        Piece captured;
        int previousHalfmoveClock;
        Position previousEnPassantTarget;
        Position previousEnPassantVictim;
        String positionKey;
        Piece[][] snapshot;
        String notation;
        Position from;
        Position to;
    }

    private static String pieceLetter(PieceType type) {
        switch (type) {
            case KING:   return "R";
            case QUEEN:  return "D";
            case ROOK:   return "T";
            case BISHOP: return "A";
            case KNIGHT: return "C";
            default:     return "";
        }
    }

    private ChessBoard board;
    private Color currentTurn;
    private CheckDetector checkDetector;
    private MoveValidator moveValidator;
    private Deque<HistoryEntry> history;
    private Map<Color, List<Piece>> capturedPieces;

    private Position enPassantTarget;
    private Position enPassantVictim;

    private int halfmoveClock;
    private Map<String, Integer> positionCounts;

    private boolean undoAvailable;

    private Piece[][] initialSnapshot;

    private boolean searchMode;
    private boolean undoAvailableBeforeSearch;

    public GameManager(ChessBoard board) {
        this(board, new CheckDetector(), new MoveValidator());
    }

    public GameManager(ChessBoard board, CheckDetector checkDetector, MoveValidator moveValidator) {
        this.board = board;
        this.currentTurn = Color.WHITE;
        this.checkDetector = checkDetector;
        this.moveValidator = moveValidator;
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

    public boolean hasAnyLegalMove() {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (!getLegalMoves(new Position(r, c)).isEmpty()) return true;
            }
        }
        return false;
    }

    public MoveResult tryMove(Position from, Position to) {
        return tryMove(from, to, PieceType.QUEEN);
    }

    public MoveResult tryMove(Position from, Position to, PieceType promotion) {
        if (promotion == null) promotion = PieceType.QUEEN;
        if (!MoveCommand.isValidPromotionType(promotion)) return MoveResult.INVALID_MOVE;

        Piece piece = board.getPieceAt(from);
        if (piece == null) return MoveResult.INVALID_MOVE;
        if (piece.getColor() != currentTurn) return MoveResult.NOT_YOUR_TURN;

        if (piece.getType() == PieceType.KING
                && from.getRow() == to.getRow()
                && Math.abs(to.getColumn() - from.getColumn()) == 2) {
            int rookCol = to.getColumn() > from.getColumn() ? 7 : 0;
            return tryCastle(from, new Position(from.getRow(), rookCol));
        }

        if (piece.getType() == PieceType.PAWN && to.equals(enPassantTarget) && canCaptureEnPassant(piece, from)) {
            return executeEnPassant(piece, from, to);
        }

        if (!moveValidator.isValidMove(board, piece, to)) return MoveResult.INVALID_MOVE;

        if (leavesKingInCheck(from, to, currentTurn)) {
            return MoveResult.INVALID_MOVE;
        }

        Piece targetPiece = board.getPieceAt(to);

        String notation = searchMode ? null : notationFor(piece, from, to, targetPiece != null, promotion);

        MoveCommand command = new MoveCommand(board, from, to, piece, targetPiece, promotion);
        HistoryEntry entry = beginEntry(command, targetPiece, from, to);
        command.execute();

        boolean pawnMove = piece.getType() == PieceType.PAWN;
        boolean doubleStep = pawnMove && Math.abs(to.getRow() - from.getRow()) == 2;
        finishMove(entry, notation, pawnMove || targetPiece != null,
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

        MoveCommand kingMove = new MoveCommand(board, kingFrom, newKingPos, king, null);
        MoveCommand rookMove = new MoveCommand(board, rookFrom, newRookPos, rook, null);
        Command castle = new Command() {
            @Override public void execute() { kingMove.execute(); rookMove.execute(); }
            @Override public void undo() { rookMove.undo(); kingMove.undo(); }
        };
        HistoryEntry entry = beginEntry(castle, null, kingFrom, newKingPos);
        castle.execute();
        finishMove(entry, direction > 0 ? "O-O" : "O-O-O", false, null, null);
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
        String notation = searchMode ? null : notationFor(pawn, from, to, true, PieceType.QUEEN);
        HistoryEntry entry = beginEntry(enPassant, victim, from, to);
        enPassant.execute();
        finishMove(entry, notation, true, null, null);
        return MoveResult.SUCCESS;
    }

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

        if (from.getRow() != enPassantVictim.getRow()) return false;
        if (Math.abs(from.getColumn() - enPassantVictim.getColumn()) != 1) return false;

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

    private HistoryEntry beginEntry(Command command, Piece captured, Position from, Position to) {
        if (!searchMode && positionCounts.isEmpty()) {
            positionCounts.merge(positionKey(), 1, Integer::sum);
        }
        if (!searchMode && history.isEmpty()) {
            initialSnapshot = snapshotBoard();
        }

        HistoryEntry entry = new HistoryEntry();
        entry.command = command;
        entry.mover = currentTurn;
        entry.captured = captured;
        entry.from = from;
        entry.to = to;
        entry.previousHalfmoveClock = halfmoveClock;
        entry.previousEnPassantTarget = enPassantTarget;
        entry.previousEnPassantVictim = enPassantVictim;
        if (captured != null) {
            capturedPieces.get(currentTurn).add(captured);
        }
        return entry;
    }

    private void finishMove(HistoryEntry entry, String notation, boolean resetsHalfmoveClock,
                            Position newEnPassantTarget, Position newEnPassantVictim) {
        halfmoveClock = resetsHalfmoveClock ? 0 : halfmoveClock + 1;
        enPassantTarget = newEnPassantTarget;
        enPassantVictim = newEnPassantVictim;
        currentTurn = (currentTurn == Color.WHITE) ? Color.BLACK : Color.WHITE;

        if (searchMode) {
            history.push(entry);
            return;
        }

        String suffix = "";
        if (checkDetector.isInCheck(currentTurn, board)) {
            suffix = hasAnyLegalMove() ? "+" : "#";
        }
        entry.notation = notation + suffix;
        entry.snapshot = snapshotBoard();

        entry.positionKey = positionKey();
        positionCounts.merge(entry.positionKey, 1, Integer::sum);
        history.push(entry);
        undoAvailable = true;
    }

    private static String squareName(Position position) {
        return "" + (char) ('a' + position.getColumn()) + (position.getRow() + 1);
    }

    private String notationFor(Piece piece, Position from, Position to, boolean capture, PieceType promotion) {
        StringBuilder san = new StringBuilder();

        if (piece.getType() == PieceType.PAWN) {
            if (capture) san.append((char) ('a' + from.getColumn())).append('x');
            san.append(squareName(to));
            int lastRow = piece.getColor() == Color.WHITE ? 7 : 0;
            if (to.getRow() == lastRow) san.append('=').append(pieceLetter(promotion));
            return san.toString();
        }

        san.append(pieceLetter(piece.getType()));

        if (piece.getType() != PieceType.KING) {
            boolean ambiguous = false;
            boolean sameColumn = false;
            boolean sameRow = false;
            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 8; c++) {
                    Position other = new Position(r, c);
                    if (other.equals(from)) continue;
                    Piece candidate = board.getPieceAt(other);
                    if (candidate == null || candidate.getType() != piece.getType()
                            || candidate.getColor() != piece.getColor()) continue;
                    if (getLegalMoves(other).contains(to)) {
                        ambiguous = true;
                        if (other.getColumn() == from.getColumn()) sameColumn = true;
                        if (other.getRow() == from.getRow()) sameRow = true;
                    }
                }
            }
            if (ambiguous) {
                if (!sameColumn) {
                    san.append((char) ('a' + from.getColumn()));
                } else if (!sameRow) {
                    san.append(from.getRow() + 1);
                } else {
                    san.append(squareName(from));
                }
            }
        }

        if (capture) san.append('x');
        san.append(squareName(to));
        return san.toString();
    }

    public List<String> getMoveHistory() {
        List<String> moves = new ArrayList<>();
        java.util.Iterator<HistoryEntry> oldestFirst = history.descendingIterator();
        while (oldestFirst.hasNext()) {
            moves.add(oldestFirst.next().notation);
        }
        return moves;
    }

    private Piece[][] snapshotBoard() {
        Piece[][] copy = new Piece[8][8];
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                copy[r][c] = board.getPieceAt(new Position(r, c));
            }
        }
        return copy;
    }

    public List<Piece[][]> getPositions() {
        List<Piece[][]> positions = new ArrayList<>();
        if (history.isEmpty()) {
            positions.add(snapshotBoard());
            return positions;
        }
        positions.add(initialSnapshot);
        java.util.Iterator<HistoryEntry> oldestFirst = history.descendingIterator();
        while (oldestFirst.hasNext()) {
            positions.add(oldestFirst.next().snapshot);
        }
        return positions;
    }

    public List<Move> getMovesPlayed() {
        List<Move> moves = new ArrayList<>();
        java.util.Iterator<HistoryEntry> oldestFirst = history.descendingIterator();
        while (oldestFirst.hasNext()) {
            HistoryEntry entry = oldestFirst.next();
            moves.add(new Move(entry.from, entry.to));
        }
        return moves;
    }

    void beginSearch() {
        searchMode = true;
        undoAvailableBeforeSearch = undoAvailable;
    }

    void endSearch() {
        searchMode = false;
        undoAvailable = undoAvailableBeforeSearch;
    }

    void undoSearchMove() {
        revertTopEntry();
    }

    public Move getLastMove() {
        HistoryEntry last = history.peek();
        return last == null ? null : new Move(last.from, last.to);
    }

    public boolean canUndo() {
        return undoAvailable && !history.isEmpty();
    }

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

        if (entry.captured != null) {
            capturedPieces.get(entry.mover).remove(entry.captured);
        }

        if (entry.positionKey != null) {
            positionCounts.computeIfPresent(entry.positionKey, (key, count) -> count > 1 ? count - 1 : null);
        }
    }

    public boolean undoLastRound(Color humanColor) {
        if (!canUndo()) return false;
        revertTopEntry();
        if (currentTurn != humanColor && !history.isEmpty()) {
            revertTopEntry();
        }
        undoAvailable = false;
        return true;
    }

    public void reset() {
        currentTurn = Color.WHITE;
        history.clear();
        capturedPieces.get(Color.WHITE).clear();
        capturedPieces.get(Color.BLACK).clear();
        enPassantTarget = null;
        enPassantVictim = null;
        halfmoveClock = 0;
        positionCounts.clear();
        initialSnapshot = null;
        undoAvailable = false;
    }

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
        if (inCheck && !hasAnyLegalMove()) return DrawReason.NONE;
        if (hasInsufficientMaterial()) return DrawReason.INSUFFICIENT_MATERIAL;
        if (positionCounts.getOrDefault(positionKey(), 0) >= 3) return DrawReason.THREEFOLD_REPETITION;
        if (halfmoveClock >= 100) return DrawReason.FIFTY_MOVE_RULE;
        return DrawReason.NONE;
    }

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
                        return false;
                }
            }
        }

        if (minorPieces <= 1) return true;
        return knights == 0 && (bishopsOnLight == 0 || bishopsOnDark == 0);
    }

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
