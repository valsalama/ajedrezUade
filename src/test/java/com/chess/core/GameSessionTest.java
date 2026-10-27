package com.chess.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.chess.adapters.GameSession;

public class GameSessionTest {

    @Test
    public void unaPartidaNuevaArrancaSinJugadas() {
        GameSession session = GameSession.create();
        String state = session.stateJson();
        assertTrue(state.contains("\"turn\":\"WHITE\""));
        assertTrue(state.contains("\"history\":[]"));
    }

    @Test
    public void lasPartidasSonIndependientes() {
        GameSession a = GameSession.create();
        GameSession b = GameSession.create();

        String result = a.moveJson(1, 4, 3, 4, null);
        assertTrue(result.contains("\"result\":\"SUCCESS\""));

        assertFalse(a.stateJson().contains("\"history\":[]"));
        assertTrue(b.stateJson().contains("\"history\":[]"));
    }

    @Test
    public void reiniciarUnaPartidaNoAfectaALaOtra() {
        GameSession a = GameSession.create();
        GameSession b = GameSession.create();
        a.moveJson(1, 4, 3, 4, null);
        b.moveJson(1, 3, 3, 3, null);

        a.resetJson(Difficulty.HARD);

        assertTrue(a.stateJson().contains("\"history\":[]"));
        assertTrue(a.stateJson().contains("\"difficulty\":\"HARD\""));
        assertFalse(b.stateJson().contains("\"history\":[]"));
        assertTrue(b.stateJson().contains("\"difficulty\":\"MEDIUM\""));
    }

    @Test
    public void deshacerSoloRevierteLaPropiaPartida() {
        GameSession a = GameSession.create();
        GameSession b = GameSession.create();
        a.moveJson(1, 4, 3, 4, null);
        b.moveJson(1, 3, 3, 3, null);

        a.undoJson();

        assertTrue(a.stateJson().contains("\"history\":[]"));
        assertFalse(b.stateJson().contains("\"history\":[]"));
    }

    @Test
    public void unaJugadaFueraDelTableroEsInvalida() {
        GameSession session = GameSession.create();
        assertTrue(session.moveJson(9, 4, 3, 4, null).contains("\"result\":\"INVALID_MOVE\""));
        assertTrue(session.moveJson(1, 4, 3, 4, "REY").contains("\"result\":\"INVALID_MOVE\""));
        assertEquals(true, session.getLastUsed() > 0);
    }
}
