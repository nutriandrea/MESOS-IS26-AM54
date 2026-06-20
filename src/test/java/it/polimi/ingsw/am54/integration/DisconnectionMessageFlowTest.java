package it.polimi.ingsw.am54.integration;

import it.polimi.ingsw.am54.controller.Controller;
import it.polimi.ingsw.am54.model.*;
import it.polimi.ingsw.am54.model.states.PickingState;
import it.polimi.ingsw.am54.model.states.StartTurnState;
import it.polimi.ingsw.am54.network.messages.*;
import it.polimi.ingsw.am54.network.server.ClientConnection;
import it.polimi.ingsw.am54.network.server.VirtualView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test di integrazione: verifica il flusso COMPLETO dei messaggi
 * server → client durante una disconnessione.
 *
 * Setup: 3 players in PickingState with totems placed.
 * Mock ClientConnection cattura tutti i messaggi inviati.
 * Disconnects a player and verifies the others' messages.
 */
public class DisconnectionMessageFlowTest {

    private Game game;
    private Board board;
    private Controller controller;

    private Player alice, bob, charlie;

    private TestClientConnection connAlice, connBob, connCharlie;
    private VirtualView vvAlice, vvBob, vvCharlie;

    @BeforeEach
    public void setUp() throws Exception {
        game = new Game();
        board = new Board();
        game.setBoard(board);

        // --- Players ---
        alice = new Player(); alice.setNickname("Alice");
        bob   = new Player(); bob.setNickname("Bob");
        charlie = new Player(); charlie.setNickname("Charlie");
        game.getPlayers().add(alice);
        game.getPlayers().add(bob);
        game.getPlayers().add(charlie);

        // --- Turn order (A-B-C) ---
        TurnOrderSlot s1 = new TurnOrderSlot(1, 0); s1.occupy(alice);
        TurnOrderSlot s2 = new TurnOrderSlot(2, 0); s2.occupy(bob);
        TurnOrderSlot s3 = new TurnOrderSlot(3, 0); s3.occupy(charlie);
        board.getTurnOrderTile().add(s1);
        board.getTurnOrderTile().add(s2);
        board.getTurnOrderTile().add(s3);

        // --- Offering tiles ---
        OfferingTile tile0 = new OfferingTile('A', 2, 1, 0);
        OfferingTile tile1 = new OfferingTile('B', 2, 1, 0);
        OfferingTile tile2 = new OfferingTile('C', 2, 1, 0);
        board.getOfferingTrack().add(tile0);
        board.getOfferingTrack().add(tile1);
        board.getOfferingTrack().add(tile2);

        // all place the totems
        tile0.occupy(alice); alice.setCurrentTile(tile0); alice.setHasPlacedTotem(true);
        tile1.occupy(bob);   bob.setCurrentTile(tile1);   bob.setHasPlacedTotem(true);
        tile2.occupy(charlie); charlie.setCurrentTile(tile2); charlie.setHasPlacedTotem(true);

        // frees the turn order slots (like in StartTurnState.placeTotem)
        s1.freeSlot();
        s2.freeSlot();
        s3.freeSlot();

        game.setCurrentPlayer(charlie); // Charlie è il current picker
        game.setState(new PickingState());

        // --- Controller + connessioni mock ---
        controller = new Controller(game);

        connAlice = new TestClientConnection("Alice");
        connBob   = new TestClientConnection("Bob");
        connCharlie = new TestClientConnection("Charlie");

        vvAlice = new VirtualView(connAlice, "Alice");
        vvBob   = new VirtualView(connBob, "Bob");
        vvCharlie = new VirtualView(connCharlie, "Charlie");

        // Registers le VirtualView nel Controller (simula handleJoinGame)
        // Use reflection to access the private virtualViews map
        java.lang.reflect.Field vvField = Controller.class.getDeclaredField("virtualViews");
        vvField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, VirtualView> vvMap = (Map<String, VirtualView>) vvField.get(controller);
        vvMap.put("Alice", vvAlice);
        vvMap.put("Bob", vvBob);
        vvMap.put("Charlie", vvCharlie);

        // Registers VirtualViews as observers of the Game
        game.addObserver(vvAlice);
        game.addObserver(vvBob);
        game.addObserver(vvCharlie);

        // Controller set up via VirtualView — no longer needed
        // (setController removed because VirtualView no longer uses the controller)

        // The Controller constructor adds ALL players to disconnectedPlayers
        // (to handle recovery from persistence). In a fresh test setup,
        // no one is disconnected at the start; clear the set.
        java.lang.reflect.Field dpField = Controller.class.getDeclaredField("disconnectedPlayers");
        dpField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<String> dp = (Set<String>) dpField.get(controller);
        dp.clear();
    }

    // ================================================================
    // TEST 1: NON-current player (Bob) disconnected in PickingState
    // ================================================================
    @Test
    public void testNonCurrentDisconnect_bobDisconnects_aliceAndCharlieStillHaveTile() throws Exception {
        // Bob is NOT the current player (Charlie is). Bob has a tile placed.
        assertNotEquals(bob, game.getCurrentPlayer());
        assertNotNull(bob.getCurrentTile());

        // Simulates Bob's disconnection
        controller.removeClient("Bob");

        // VERIFICA: Alice e Charlie hanno ANCORA il tile
        assertNotNull(alice.getCurrentTile(),
                "Alice deve ancora avere il suo tile (Bob disconnesso)");
        assertNotNull(charlie.getCurrentTile(),
                "Charlie deve ancora avere il suo tile (Bob disconnesso)");
        assertNull(bob.getCurrentTile(),
                "Bob ha perso il suo tile (disconnesso)");

        // VERIFY: Alice received the correct messages
        List<Class<?>> aliceMsgs = connAlice.getReceivedMessageTypes();
        assertTrue(aliceMsgs.contains(PlayerDisconnectedUpdate.class),
                "Alice deve ricevere PlayerDisconnectedUpdate");
        assertTrue(aliceMsgs.contains(TotemReturnedUpdate.class),
                "Alice deve ricevere TotemReturnedUpdate per Bob");

        // Alice must NOT receive TotemReturnedUpdate for herself or Charlie
        List<String> aliceTotemReturns = connAlice.getTotemReturnedNicknames();
        assertFalse(aliceTotemReturns.contains("Alice"),
                "Alice NON deve ricevere TotemReturned per se stessa");
        assertFalse(aliceTotemReturns.contains("Charlie"),
                "Charlie NON deve essere rimosso dall'offering");

        // VERIFY: Alice received ONLY PlayerDisconnected + TotemReturned
        // Non deve ricevere StateChangedUpdate (il gioco NON cambia stato)
        assertFalse(aliceMsgs.contains(StateChangedUpdate.class),
                "Alice NON deve ricevere StateChangedUpdate (nessuna transizione)");
    }

    // ================================================================
    // TEST 2: Current player (Charlie) disconnected in PickingState
    // ================================================================
    @Test
    public void testCurrentPlayerDisconnect_charlieDisconnects_aliceAndBobAffected() throws Exception {
        // Charlie is the current player
        assertEquals(charlie, game.getCurrentPlayer());

        controller.removeClient("Charlie");

        // Il gioco potrebbe essere passato a StartTurnState (tutti hanno pickato)
        // or may have skipped Charlie and passed the turn
        assertNull(charlie.getCurrentTile(),
                "Charlie ha perso il suo tile");
        // Alice and Bob still have the tile if the game has not transitioned
        // (dipende da isPickingComplete)
    }

    // ================================================================
    // TEST 3: Player without tile (already picked) disconnects
    // ================================================================
    @Test
    public void testAlreadyPickedPlayerDisconnect() throws Exception {
        // Alice has already picked: setCurrentTile = null, not in offeringTrack
        alice.setCurrentTile(null);
        // Put Alice in a turn order slot (like after a pick)
        TurnOrderSlot lastSlot = new TurnOrderSlot(4, 0);
        lastSlot.occupy(alice);
        board.getTurnOrderTile().add(lastSlot);

        // Charlie is current, Bob has tile
        game.setCurrentPlayer(charlie);

        controller.removeClient("Alice");

        // Bob e Charlie devono avere ancora i loro tile
        assertNotNull(bob.getCurrentTile(), "Bob deve ancora avere il suo tile");
        assertNotNull(charlie.getCurrentTile(), "Charlie deve ancora avere il suo tile");

        // Alice (already without a tile) should not have caused offering changes
        List<Class<?>> bobMsgs = connBob.getReceivedMessageTypes();
        // Alice had no tile to free → hopefully no TotemReturnedUpdate was sent
        // (o al massimo solo PlayerDisconnectedUpdate)
        assertTrue(bobMsgs.contains(PlayerDisconnectedUpdate.class),
                "Bob deve ricevere PlayerDisconnectedUpdate per Alice");
    }

    // ================================================================
    // TEST 4: Verify that the TotemReturnedUpdate message contains
    //         ONLY the disconnected player's nickname (not others)
    // ================================================================
    @Test
    public void testTotemReturnedMessageCarriesOnlyDisconnectedNickname() throws Exception {
        connAlice.clear();
        connBob.clear();
        connCharlie.clear();

        controller.removeClient("Charlie");

        // Alice and Bob must receive TotemReturnedUpdate for Charlie
        List<String> aliceReturns = connAlice.getTotemReturnedNicknames();
        assertEquals(1, aliceReturns.size(),
                "Alice deve ricevere esattamente 1 TotemReturnedUpdate");
        assertTrue(aliceReturns.contains("Charlie"),
                "Il TotemReturnedUpdate di Alice deve essere per Charlie");

        List<String> bobReturns = connBob.getTotemReturnedNicknames();
        assertEquals(1, bobReturns.size(),
                "Bob deve ricevere esattamente 1 TotemReturnedUpdate");
        assertTrue(bobReturns.contains("Charlie"),
                "Il TotemReturnedUpdate di Bob deve essere per Charlie");

        // Charlie receives nothing (connection closed)
        assertEquals(0, connCharlie.getReceivedMessages().size(),
                "Charlie non deve ricevere messaggi (disconnesso)");
    }

    // ================================================================
    // Helper: Mock ClientConnection
    // ================================================================
    private static class TestClientConnection implements ClientConnection {
        private final String nickname;
        private final List<Object> receivedMessages = new CopyOnWriteArrayList<>();

        TestClientConnection(String nickname) { this.nickname = nickname; }

        void clear() { receivedMessages.clear(); }
        List<Object> getReceivedMessages() { return receivedMessages; }

        List<Class<?>> getReceivedMessageTypes() {
            return receivedMessages.stream().map(Object::getClass).toList();
        }

        List<String> getTotemReturnedNicknames() {
            return receivedMessages.stream()
                .filter(m -> m instanceof TotemReturnedUpdate)
                .map(m -> ((TotemReturnedUpdate) m).getNickname())
                .toList();
        }

        @Override public void notifyPlayerJoined(String nickname) {
            receivedMessages.add(new PlayerJoinedUpdate(nickname));
        }
        @Override public void notifyTotemColor(String nickanme, TotemColors color) {
            receivedMessages.add(new TotemColorUpdate(nickanme, color));
        }
        @Override public void notifyTotemPlaced(String nickname, int position) {
            receivedMessages.add(new TotemPlacedUpdate(nickname, position));
        }
        @Override
        public void notifyCardPicked(String nickname, String row, int index, int food, int magic, Map<Integer, String> offering) {
            receivedMessages.add(new CardPickedUpdate(nickname, row, index, food, magic, offering));
        }
        @Override
        public void notifyTotemReturned(String nickname, int position, int food, int aura) {
            receivedMessages.add(new TotemReturnedUpdate(nickname, position, food, aura));
        }
        @Override public void notifyEventResolved(String name, Map<String, EventUpdate.PlayerSnapshot> stats, List<String> charUp, List<String> charLow, List<String> eventUp, List<String> eventLow, List<String> buildUp, List<String> buildLow) {}
        @Override public void notifyTurnChanged(String nickname) {
            receivedMessages.add(new TurnChangedUpdate(nickname));
        }
        @Override
        public void notifyStateChanged(String stateName, int era, int round,
                Map<String, StateChangedUpdate.PlayerSnapshot> stats,
                List<String> turnOrder, Map<Integer, String> offering,
                List<String> charUp, List<String> charLow,
                List<String> eventUp, List<String> eventLow,
                List<String> buildUp, List<String> buildLow) {
            receivedMessages.add(new StateChangedUpdate(stateName, era, round, stats, turnOrder, offering,
                    charUp, charLow, eventUp, eventLow, buildUp, buildLow));
        }
        @Override public void notifyPlayerDisconnected(String nickname) {
            receivedMessages.add(new PlayerDisconnectedUpdate(nickname));
        }
        @Override public void notifyPlayerReconnected(String nickname) {
            receivedMessages.add(new PlayerReconnectedUpdate(nickname));
        }
        @Override public void notifyError(String message) {
            receivedMessages.add(new ErrorMessageUpdate(message));
        }
        @Override
        public void notifyEndGame(List<String> winners, Map<String, Integer> allScores, int numPlayers) {
            receivedMessages.add(new EndGameUpdate(winners, allScores, numPlayers));
        }
        @Override public void sendLeaderboardUpdate(LeaderboardUpdate update) {}
        @Override public void notifyGameSuspended() {
            receivedMessages.add("GAME_SUSPENDED");
        }
        @Override public void notifyGameResumed() {
            receivedMessages.add("GAME_RESUMED");
        }
        @Override public void disconnect() {
            receivedMessages.add("DISCONNECTED");
        }
        @Override public void sendPing() {}
        @Override public void startHeartbeat() {}
    }
}
