package it.polimi.ingsw.am54.network.server;

import it.polimi.ingsw.am54.network.messages.EventUpdate;
import it.polimi.ingsw.am54.network.messages.StateChangedUpdate;
import it.polimi.ingsw.am54.network.messages.Update;
import it.polimi.ingsw.am54.model.TotemColors;

import it.polimi.ingsw.am54.network.messages.LeaderboardEntry;
import java.util.List;
import java.util.Map;

/**
 * ============================================================================
 * ClientConnection — Astrazione del canale di comunicazione server -> client
 * ============================================================================
 *
 * RUOLO:
 * Interfaccia che astrae il trasporto di rete (Socket TCP o RMI).
 * Il Controller e la VirtualView usano questa interfaccia senza sapere
 * se stanno comunicando via Socket o RMI.
 *
 * IMPLEMENTAZIONI:
 * - SocketClientHandler: usa ObjectOutputStream per inviare Update
 * - RMIClientHandler: usa clientStub.receiveUpdate() per inviare Update
 *
 * METODI:
 * - sendUpdate(Update): invia un messaggio di update al client
 * - disconnect(): chiude la connessione e pulisce le risorse
 * - startHeartbeat(): avvia il sistema di ping/pong (default = no-op)
 *
 * NOTA SU startHeartbeat():
 * Il metodo ha un'implementazione di default vuota per retrocompatibilita'.
 * SocketClientHandler e RMIClientHandler lo sovrascrivono per avviare
 * il ping scheduler. Se una futura implementazione non ha bisogno di
 * heartbeat, puo' semplicemente non sovrascriverlo.
 * ============================================================================
 */
public interface ClientConnection {

    /**
     * Metodi per mandare al client gli update
     */

    // metodi specifici (non più il generico sendUpdate(Update))
    void notifyPlayerJoined(String nickname);
    void notifyTotemColor(String nickanme, TotemColors color);
    void notifyTotemPlaced(String nickname, int position);
    void notifyCardPicked(String nickname, String row, int index, int food, Map<Integer, String> offering);
    void notifyTotemReturned(String nickname, int position, int food, int aura);
    void notifyEventResolved(Map<String, EventUpdate.PlayerSnapshot> stats,
                             List<String> charUp, List<String> charLow,
                             List<String> eventUp, List<String> eventLow,
                             List<String> buildUp, List<String> buildLow);
    void notifyTurnChanged(String nickname);
    void notifyStateChanged( String stateName,
                            int currentEra,
                            Map<String, StateChangedUpdate.PlayerSnapshot> playersStats,
                            List<String> turnOrderTrack,
                            Map<Integer, String> offeringTrack,
                            List<String> charUpper, List<String> charLower,
                            List<String> eventUpper, List<String> eventLower,
                            List<String> buildUpper, List<String> buildLower);
    void notifyEndGame(List<String> winnerNames);
    void notifyEndGame(List<String> winnerNames, Map<String, Integer> allScores, int numPlayers);
    void notifyLeaderboard(int playerPosition, List<LeaderboardEntry> entries, int numPlayers);
    void notifyError(String message);
    void sendPing();

    /**
     * Metodi per gestire giocatori che si sconnettono e riconettono
     */
    void notifyPlayerDisconnected(String nickname);
    void notifyPlayerReconnected(String nickname);

    /**
     * Notifica al client che il gioco e' stato sospeso (1 solo giocatore attivo).
     * Implementazione di default vuota per retrocompatibilita'.
     */
    default void notifyGameSuspended() {}

    /**
     * Notifica al client che il gioco e' stato ripreso.
     * Implementazione di default vuota per retrocompatibilita'.
     */
    default void notifyGameResumed() {}

    /**
     * Chiude la connessione e pulisce le risorse.
     * Chiama anche controller.removeClient() per deregistrare il giocatore.
     */
    void disconnect();

    /**
     * Avvia il sistema di heartbeat (ping/pong) per questa connessione.
     * Implementazione di default vuota per retrocompatibilita'.
     *
     * SocketClientHandler e RMIClientHandler sovrascrivono questo metodo
     * per avviare un ScheduledExecutorService che invia ping ogni 5 secondi.
     */
    default void startHeartbeat() {}
}
