package it.polimi.ingsw.am54.network;

import it.polimi.ingsw.am54.model.TotemColors;
import it.polimi.ingsw.am54.network.messages.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Update message classes.
 */
public class UpdateTest {

    @Test
    public void testPlayerJoinedUpdate() {
        PlayerJoinedUpdate update = new PlayerJoinedUpdate("Player1");
        assertEquals("Player1", update.getNickname());
        assertTrue(update instanceof Update);
    }

    @Test
    public void testPlayerReconnectedUpdate() {
        PlayerReconnectedUpdate update = new PlayerReconnectedUpdate("Player1");
        assertEquals("Player1", update.getNickname());
        assertTrue(update instanceof Update);
    }

    @Test
    public void testTotemColorUpdate() {
        TotemColorUpdate update = new TotemColorUpdate("Player1", TotemColors.RED);
        assertEquals("Player1", update.getNickname());
        assertEquals(TotemColors.RED, update.getColor());
        assertTrue(update instanceof Update);
    }

    @Test
    public void testTotemPlacedUpdate() {
        TotemPlacedUpdate update = new TotemPlacedUpdate("Player1", 1);
        assertEquals("Player1", update.getNickname());
        assertEquals(1, update.getOfferPosition());
        assertTrue(update instanceof Update);
    }

    @Test
    public void testCardPickedUpdate() {
        CardPickedUpdate update = new CardPickedUpdate("Player1", "UPPER", 0);
        assertEquals("Player1", update.getNickname());
        assertEquals("UPPER", update.getRow());
        assertEquals(0, update.getCardIndex());
        assertTrue(update instanceof Update);
    }

    @Test
    public void testTurnChangedUpdate() {
        TurnChangedUpdate update = new TurnChangedUpdate("Player1");
        assertEquals("Player1", update.getNickname());
        assertTrue(update instanceof Update);
    }

    @Test
    public void testStateChangedUpdate() {
        StateChangedUpdate update = new StateChangedUpdate("SetUpGameState");
        assertEquals("SetUpGameState", update.getStateName());
        assertTrue(update instanceof Update);
    }

    @Test
    public void testEndGameUpdate() {
        List<String> winners = Arrays.asList("Player1", "Player2");
        EndGameUpdate update = new EndGameUpdate(winners);
        assertEquals(2, update.getWinners().size());
        assertTrue(update instanceof Update);
    }

    @Test
    public void testErrorMessageUpdate() {
        ErrorMessageUpdate update = new ErrorMessageUpdate("Invalid move");
        assertEquals("Invalid move", update.getMessage());
        assertTrue(update instanceof Update);
    }

    @Test
    public void testPingRequest() {
        PingRequest ping = new PingRequest();
        assertTrue(ping.getTimestamp() > 0);
        assertTrue(ping instanceof Update);
    }
}