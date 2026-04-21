package it.polimi.ingsw.am54.network;

import it.polimi.ingsw.am54.network.messages.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Request message classes.
 */
public class RequestTest {

    @Test
    public void testJoinGameRequest() {
        JoinGameRequest request = new JoinGameRequest("Player1");
        assertEquals("Player1", request.getNickname());
        assertTrue(request instanceof Request);
    }

    @Test
    public void testChooseTotemRequest() {
        ChooseTotemRequest request = new ChooseTotemRequest(it.polimi.ingsw.am54.model.TotemColors.RED);
        assertEquals(it.polimi.ingsw.am54.model.TotemColors.RED, request.getColor());
        assertTrue(request instanceof Request);
    }

    @Test
    public void testSetupGameRequest() {
        SetupGameRequest request = new SetupGameRequest();
        assertTrue(request instanceof Request);
    }

    @Test
    public void testPlaceTotemRequest() {
        PlaceTotemRequest request = new PlaceTotemRequest(1);
        assertEquals(1, request.getOfferPosition());
        assertTrue(request instanceof Request);
    }

    @Test
    public void testPickCardRequest() {
        PickCardRequest request = new PickCardRequest("UPPER", 0);
        assertEquals("UPPER", request.getRow());
        assertEquals(0, request.getCardIndex());
        assertTrue(request instanceof Request);
    }

    @Test
    public void testReconnectRequest() {
        ReconnectRequest request = new ReconnectRequest("Player1");
        assertEquals("Player1", request.getNickname());
        assertTrue(request instanceof Request);
    }
}