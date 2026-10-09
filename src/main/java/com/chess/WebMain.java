package com.chess;

import com.chess.adapters.ApiServer;
import com.chess.core.Bishop;
import com.chess.core.Color;
import com.chess.core.GameManager;
import com.chess.core.King;
import com.chess.core.Knight;
import com.chess.core.Pawn;
import com.chess.core.Position;
import com.chess.core.Queen;
import com.chess.core.Rook;
import com.chess.ports.ChessBoard;

public class WebMain {
    public static void main(String[] args) throws Exception {
        ChessBoard board = new com.chess.core.ArrayChessBoard();

        board.placePiece(new Rook(Color.WHITE, new Position(0, 0)), new Position(0, 0));
        board.placePiece(new Knight(Color.WHITE, new Position(0, 1)), new Position(0, 1));
        board.placePiece(new Bishop(Color.WHITE, new Position(0, 2)), new Position(0, 2));
        board.placePiece(new Queen(Color.WHITE, new Position(0, 3)), new Position(0, 3));
        board.placePiece(new King(Color.WHITE, new Position(0, 4)), new Position(0, 4));
        board.placePiece(new Bishop(Color.WHITE, new Position(0, 5)), new Position(0, 5));
        board.placePiece(new Knight(Color.WHITE, new Position(0, 6)), new Position(0, 6));
        board.placePiece(new Rook(Color.WHITE, new Position(0, 7)), new Position(0, 7));
        for (int i = 0; i < 8; i++) board.placePiece(new Pawn(Color.WHITE, new Position(1, i)), new Position(1, i));

        board.placePiece(new Rook(Color.BLACK, new Position(7, 0)), new Position(7, 0));
        board.placePiece(new Knight(Color.BLACK, new Position(7, 1)), new Position(7, 1));
        board.placePiece(new Bishop(Color.BLACK, new Position(7, 2)), new Position(7, 2));
        board.placePiece(new Queen(Color.BLACK, new Position(7, 3)), new Position(7, 3));
        board.placePiece(new King(Color.BLACK, new Position(7, 4)), new Position(7, 4));
        board.placePiece(new Bishop(Color.BLACK, new Position(7, 5)), new Position(7, 5));
        board.placePiece(new Knight(Color.BLACK, new Position(7, 6)), new Position(7, 6));
        board.placePiece(new Rook(Color.BLACK, new Position(7, 7)), new Position(7, 7));
        for (int i = 0; i < 8; i++) board.placePiece(new Pawn(Color.BLACK, new Position(6, i)), new Position(6, i));

        GameManager gameManager = new GameManager(board);

        ApiServer apiServer = new ApiServer(gameManager, board, 8080);
        apiServer.start();
    }
}