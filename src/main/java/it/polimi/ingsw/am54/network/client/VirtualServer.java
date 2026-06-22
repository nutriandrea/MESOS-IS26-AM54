package it.polimi.ingsw.am54.network.client;

import it.polimi.ingsw.am54.network.messages.*;
import it.polimi.ingsw.am54.model.TotemColors;

/**
 * Offre alla View i metodi per interagire con il gioco.
 * Trasforma le chiamate ai metodi in oggetti Request inviati sulla rete.
 */
public class VirtualServer {
    private final String nickname;

    public VirtualServer(ServerConnection connection, String nickname) {
        // NB: la connessione NON viene memorizzata qui. Dopo una riconnessione
        // automatica il Client sostituisce la connessione viva nel singleton; se
        // tenessimo un riferimento "congelato" al costruttore, ogni mossa verrebbe
        // inviata alla vecchia connessione morta e andrebbe persa silenziosamente.
        this.nickname = nickname;
    }

    /** Restituisce SEMPRE la connessione attualmente viva (aggiornata dopo le riconnessioni). */
    private ServerConnection connection() {
        return Client.getInstance().getConnection();
    }

    public void joinGame() {
        connection().joinGame(nickname);
    }

    public void setupGame() {
        connection().setupGame();
    }

    public void chooseTotem(TotemColors color) {
        connection().chooseTotem(color);
    }

    public void placeTotem(int position) {
        connection().placeTotem(position);
    }

    public void pickCard(String row, int index) {
        connection().pickCard(row, index);
    }

    public void endTurn() {
        connection().endTurn();
    }

    public void requestLeaderboard(int numPlayers) {
        connection().requestLeaderboard(numPlayers);
    }

}
