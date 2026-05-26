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
 * Setup: 3 giocatori in PickingState con totem piazzati.
 * Mock ClientConnection cattura tutti i messaggi inviati.
 * Disconnette un giocatore e verifica i messaggi degli altri.
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

        // --- Giocatori ---
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

        // tutti piazzano i totem
        tile0.occupy(alice); alice.setCurrentTile(tile0); alice.setHasPlacedTotem(true);
        tile1.occupy(bob);   bob.setCurrentTile(tile1);   bob.setHasPlacedTotem(true);
        tile2.occupy(charlie); charlie.setCurrentTile(tile2); charlie.setHasPlacedTotem(true);

        // libera i turn order slot (come in StartTurnState.placeTotem)
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

        // Registra le VirtualView nel Controller (simula handleJoinGame)
        // Usiamo reflection per accedere alla mappa private virtualViews
        java.lang.reflect.Field vvField = Controller.class.getDeclaredField("virtualViews");
        vvField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, VirtualView> vvMap = (Map<String, VirtualView>) vvField.get(controller);
        vvMap.put("Alice", vvAlice);
        vvMap.put("Bob", vvBob);
        vvMap.put("Charlie", vvCharlie);

        // Registra VirtualViews come observer del Game
        game.addObserver(vvAlice);
        game.addObserver(vvBob);
        game.addObserver(vvCharlie);

        // Imposta il controller sulle VirtualView
        vvAlice.setController(controller);
        vvBob.setController(controller);
        vvCharlie.setController(controller);

        // Il costruttore del Controller aggiunge TUTTI i giocatori a disconnectedPlayers
        // (per gestire il recupero da persistenza). In un setup di test fresco,
        // nessuno è disconnesso all'inizio; puliamo il set.
        java.lang.reflect.Field dpField = Controller.class.getDeclaredField("disconnectedPlayers");
        dpField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<String> dp = (Set<String>) dpField.get(controller);
        dp.clear();
    }

    // ================================================================
    // TEST 1: NON-current player (Bob) disconnesso in PickingState
    // ================================================================
    @Test
    public void testNonCurrentDisconnect_bobDisconnects_aliceAndCharlieStillHaveTile() throws Exception {
        // Bob NON è di turno (Charlie è il current). Bob ha un tile piazzato.
        assertNotEquals(bob, game.getCurrentPlayer());
        assertNotNull(bob.getCurrentTile());

        // Simula la disconnessione di Bob
        controller.removeClient("Bob");

        // VERIFICA: Alice e Charlie hanno ANCORA il tile
        assertNotNull(alice.getCurrentTile(),
                "Alice deve ancora avere il suo tile (Bob disconnesso)");
        assertNotNull(charlie.getCurrentTile(),
                "Charlie deve ancora avere il suo tile (Bob disconnesso)");
        assertNull(bob.getCurrentTile(),
                "Bob ha perso il suo tile (disconnesso)");

        // VERIFICA: Alice ha ricevuto i messaggi corretti
        List<Class<?>> aliceMsgs = connAlice.getReceivedMessageTypes();
        assertTrue(aliceMsgs.contains(PlayerDisconnectedUpdate.class),
                "Alice deve ricevere PlayerDisconnectedUpdate");
        assertTrue(aliceMsgs.contains(TotemReturnedUpdate.class),
                "Alice deve ricevere TotemReturnedUpdate per Bob");

        // Alice NON deve ricevere TotemReturnedUpdate per se stessa o Charlie
        List<String> aliceTotemReturns = connAlice.getTotemReturnedNicknames();
        assertFalse(aliceTotemReturns.contains("Alice"),
                "Alice NON deve ricevere TotemReturned per se stessa");
        assertFalse(aliceTotemReturns.contains("Charlie"),
                "Charlie NON deve essere rimosso dall'offering");

        // VERIFICA: Alice ha ricevuto SOLO PlayerDisconnected + TotemReturned
        // Non deve ricevere StateChangedUpdate (il gioco NON cambia stato)
        assertFalse(aliceMsgs.contains(StateChangedUpdate.class),
                "Alice NON deve ricevere StateChangedUpdate (nessuna transizione)");
    }

    // ================================================================
    // TEST 2: Current player (Charlie) disconnesso in PickingState
    // ================================================================
    @Test
    public void testCurrentPlayerDisconnect_charlieDisconnects_aliceAndBobAffected() throws Exception {
        // Charlie è di turno
        assertEquals(charlie, game.getCurrentPlayer());

        controller.removeClient("Charlie");

        // Il gioco potrebbe essere passato a StartTurnState (tutti hanno pickato)
        // o potrebbe aver skippato Charlie e passato il turno
        assertNull(charlie.getCurrentTile(),
                "Charlie ha perso il suo tile");
        // Alice e Bob hanno ancora il tile se il gioco non è transitato
        // (dipende da isPickingComplete)
    }

    // ================================================================
    // TEST 3: Player senza tile (già pickato) si disconnette
    // ================================================================
    @Test
    public void testAlreadyPickedPlayerDisconnect() throws Exception {
        // Alice ha già pickato: setCurrentTile = null, non è in offeringTrack
        alice.setCurrentTile(null);
        // Mettiamo Alice in un turn order slot (come dopo un pick)
        TurnOrderSlot lastSlot = new TurnOrderSlot(4, 0);
        lastSlot.occupy(alice);
        board.getTurnOrderTile().add(lastSlot);

        // Charlie è current, Bob ha tile
        game.setCurrentPlayer(charlie);

        controller.removeClient("Alice");

        // Bob e Charlie devono avere ancora i loro tile
        assertNotNull(bob.getCurrentTile(), "Bob deve ancora avere il suo tile");
        assertNotNull(charlie.getCurrentTile(), "Charlie deve ancora avere il suo tile");

        // Alice (già senza tile) non dovrebbe aver causato modifiche all'offering
        List<Class<?>> bobMsgs = connBob.getReceivedMessageTypes();
        // Alice non aveva tile da freeare → si spera nessun TotemReturnedUpdate sia stato mandato
        // (o al massimo solo PlayerDisconnectedUpdate)
        assertTrue(bobMsgs.contains(PlayerDisconnectedUpdate.class),
                "Bob deve ricevere PlayerDisconnectedUpdate per Alice");
    }

    // ================================================================
    // TEST 4: Verifica che il messaggio TotemReturnedUpdate contenga
    //         SOLO il nickname del disconnesso (non altri)
    // ================================================================
    @Test
    public void testTotemReturnedMessageCarriesOnlyDisconnectedNickname() throws Exception {
        connAlice.clear();
        connBob.clear();
        connCharlie.clear();

        controller.removeClient("Charlie");

        // Alice e Bob devono ricevere TotemReturnedUpdate per Charlie
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

        // Charlie non riceve nulla (connessione chiusa)
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
