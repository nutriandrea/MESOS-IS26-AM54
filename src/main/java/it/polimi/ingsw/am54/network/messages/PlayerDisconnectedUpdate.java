package it.polimi.ingsw.am54.network.messages;

/**
 * Notifica broadcast a tutti i client che un giocatore e' stato marcato come disconnesso.
 * SERVER -> CLIENT
 */
public class PlayerDisconnectedUpdate implements Update {
    private final String nickname;

    public PlayerDisconnectedUpdate(String nickname) {
        this.nickname = nickname;
    }

    public String getNickname() {
        return nickname;
    }

    public static final String TYPE = "player_disconnected";
    @Override public String getType() { return TYPE; }
}
