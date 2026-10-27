package com.chess.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.chess.ports.ChessBoard;

public class MoveSearcher {

    private static final int MATE = 100_000;
    private static final int INFINITY = 1_000_000;
    private static final int QUIESCENCE_LIMIT = 4;

    private final GameManager gameManager;
    private final ChessBoard board;
    private final CheckDetector checkDetector;
    private final Random random;

    private long deadlineNanos;
    private boolean timeUp;
    private int nodeCounter;

    public MoveSearcher(GameManager gameManager, ChessBoard board) {
        this(gameManager, board, new Random());
    }

    public MoveSearcher(GameManager gameManager, ChessBoard board, Random random) {
        this(gameManager, board, new CheckDetector(), random);
    }

    public MoveSearcher(GameManager gameManager, ChessBoard board, CheckDetector checkDetector, Random random) {
        this.gameManager = gameManager;
        this.board = board;
        this.checkDetector = checkDetector;
        this.random = random;
    }

    public Move findBestMove(int maxDepth, long timeLimitMs, int toleranceCp) {
        gameManager.beginSearch();
        try {
            List<Move> rootMoves = generateMoves();
            if (rootMoves.isEmpty()) return null;
            if (rootMoves.size() == 1) return rootMoves.get(0);

            Collections.shuffle(rootMoves, random);
            orderMoves(rootMoves);

            deadlineNanos = System.nanoTime() + timeLimitMs * 1_000_000L;
            timeUp = false;
            nodeCounter = 0;

            int[] bestScores = null;
            List<Move> bestOrder = rootMoves;

            for (int depth = 1; depth <= maxDepth; depth++) {
                int[] scores = new int[bestOrder.size()];
                int alpha = -INFINITY;

                for (int i = 0; i < bestOrder.size(); i++) {
                    Move move = bestOrder.get(i);
                    gameManager.tryMove(move.getFrom(), move.getTo());
                    int score = -negamax(depth - 1, -INFINITY, toleranceCp > 0 ? INFINITY : -alpha, 1);
                    gameManager.undoSearchMove();
                    if (timeUp) break;
                    scores[i] = score;
                    if (score > alpha) alpha = score;
                }

                if (timeUp) break;

                bestScores = scores;
                List<Move> sorted = new ArrayList<>(bestOrder);
                Integer[] index = new Integer[sorted.size()];
                for (int i = 0; i < index.length; i++) index[i] = i;
                final int[] currentScores = scores;
                java.util.Arrays.sort(index, (a, b) -> Integer.compare(currentScores[b], currentScores[a]));
                List<Move> reordered = new ArrayList<>();
                int[] reorderedScores = new int[scores.length];
                for (int i = 0; i < index.length; i++) {
                    reordered.add(sorted.get(index[i]));
                    reorderedScores[i] = scores[index[i]];
                }
                bestOrder = reordered;
                bestScores = reorderedScores;

                if (bestScores[0] >= MATE - 100) break;
            }

            if (bestScores == null) {
                return bestOrder.get(0);
            }

            if (toleranceCp <= 0) return bestOrder.get(0);

            int best = bestScores[0];
            List<Move> candidates = new ArrayList<>();
            for (int i = 0; i < bestOrder.size() && i < bestScores.length; i++) {
                if (bestScores[i] >= best - toleranceCp) candidates.add(bestOrder.get(i));
            }
            return candidates.get(random.nextInt(candidates.size()));
        } finally {
            gameManager.endSearch();
        }
    }

    private int negamax(int depth, int alpha, int beta, int ply) {
        if (outOfTime()) return 0;

        List<Move> moves = generateMoves();
        if (moves.isEmpty()) {
            return checkDetector.isInCheck(gameManager.getCurrentTurn(), board) ? -MATE + ply : 0;
        }
        if (depth <= 0) {
            return quiescence(moves, alpha, beta, ply, 0);
        }

        orderMoves(moves);
        for (Move move : moves) {
            gameManager.tryMove(move.getFrom(), move.getTo());
            int score = -negamax(depth - 1, -beta, -alpha, ply + 1);
            gameManager.undoSearchMove();
            if (timeUp) return 0;

            if (score >= beta) return beta;
            if (score > alpha) alpha = score;
        }
        return alpha;
    }

    private int quiescence(List<Move> allMoves, int alpha, int beta, int ply, int extra) {
        int standPat = evaluate();
        if (standPat >= beta) return beta;
        if (standPat > alpha) alpha = standPat;
        if (extra >= QUIESCENCE_LIMIT) return alpha;

        List<Move> captures = new ArrayList<>();
        for (Move move : allMoves) {
            if (board.getPieceAt(move.getTo()) != null) captures.add(move);
        }
        orderMoves(captures);

        for (Move move : captures) {
            if (outOfTime()) return 0;
            gameManager.tryMove(move.getFrom(), move.getTo());
            List<Move> replies = generateMoves();
            int score;
            if (replies.isEmpty()) {
                score = checkDetector.isInCheck(gameManager.getCurrentTurn(), board) ? MATE - ply : 0;
            } else {
                score = -quiescence(replies, -beta, -alpha, ply + 1, extra + 1);
            }
            gameManager.undoSearchMove();
            if (timeUp) return 0;

            if (score >= beta) return beta;
            if (score > alpha) alpha = score;
        }
        return alpha;
    }

    private boolean outOfTime() {
        if (timeUp) return true;
        nodeCounter++;
        if ((nodeCounter & 127) == 0 && System.nanoTime() > deadlineNanos) {
            timeUp = true;
        }
        return timeUp;
    }

    private List<Move> generateMoves() {
        List<Move> moves = new ArrayList<>();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Position from = new Position(r, c);
                for (Position to : gameManager.getLegalMoves(from)) {
                    moves.add(new Move(from, to));
                }
            }
        }
        return moves;
    }

    private void orderMoves(List<Move> moves) {
        moves.sort((a, b) -> Integer.compare(orderScore(b), orderScore(a)));
    }

    private int orderScore(Move move) {
        Piece mover = board.getPieceAt(move.getFrom());
        Piece target = board.getPieceAt(move.getTo());
        int score = 0;
        if (target != null) score += 10 * value(target.getType()) - value(mover.getType());
        if (mover.getType() == PieceType.PAWN && (move.getTo().getRow() == 0 || move.getTo().getRow() == 7)) score += 800;
        return score;
    }

    static int value(PieceType type) {
        switch (type) {
            case PAWN:   return 100;
            case KNIGHT: return 320;
            case BISHOP: return 330;
            case ROOK:   return 500;
            case QUEEN:  return 900;
            default:     return 0;
        }
    }

    private int evaluate() {
        boolean queensOnBoard = false;
        for (int r = 0; r < 8 && !queensOnBoard; r++) {
            for (int c = 0; c < 8; c++) {
                Piece piece = board.getPieceAt(new Position(r, c));
                if (piece != null && piece.getType() == PieceType.QUEEN) {
                    queensOnBoard = true;
                    break;
                }
            }
        }

        int white = 0;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece piece = board.getPieceAt(new Position(r, c));
                if (piece == null) continue;
                int score = value(piece.getType()) + positional(piece, r, c, queensOnBoard);
                white += piece.getColor() == Color.WHITE ? score : -score;
            }
        }
        return gameManager.getCurrentTurn() == Color.WHITE ? white : -white;
    }

    private int positional(Piece piece, int row, int col, boolean queensOnBoard) {
        int advance = piece.getColor() == Color.WHITE ? row : 7 - row;
        double centerDistance = Math.max(Math.abs(row - 3.5), Math.abs(col - 3.5));
        double centrality = 3.5 - centerDistance;

        switch (piece.getType()) {
            case PAWN: {
                int bonus = advance * 6;
                if (col == 3 || col == 4) bonus += advance * 3;
                return bonus;
            }
            case KNIGHT: return (int) (centrality * 12);
            case BISHOP: return (int) (centrality * 5);
            case QUEEN:  return (int) (centrality * 2);
            case ROOK:   return advance == 6 ? 15 : 0;
            case KING: {
                if (queensOnBoard) {
                    int shelter = advance == 0 ? 20 : 0;
                    if (advance == 0 && (col <= 2 || col >= 6)) shelter += 15;
                    return shelter - (int) (centrality * 6);
                }
                return (int) (centrality * 8);
            }
            default: return 0;
        }
    }
}
