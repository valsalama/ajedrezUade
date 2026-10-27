package com.chess;

import com.chess.adapters.ApiServer;
import com.chess.adapters.GameSession;

public class WebMain {
    public static void main(String[] args) throws Exception {
        String envPort = System.getenv("PORT");
        int port = envPort == null || envPort.isBlank() ? 8081 : Integer.parseInt(envPort.trim());

        ApiServer apiServer = new ApiServer(port, GameSession::create);
        apiServer.start();
    }
}
