package it.polimi.ingsw.am54.network.messages;

/**
 * Notifica broadcast a tutti i client che un giocatore si e' riconnesso.
 * SERVER -> CLIENT
 */
public class PlayerReconnectedUpdate implements Update {
    private final String nickname;

    public PlayerReconnectedUpdate(String nickname) {
        this.nickname = nickname;
    }

    public String getNickname() {
        return nickname;
    }
}
