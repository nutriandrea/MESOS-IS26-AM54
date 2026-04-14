package it.polimi.ingsw.am54.network.messages;

/**
 * Il primo messaggio inviato dal Client per presentarsi al Server.
 * È l'unico Request che DEVE contenere il nickname!
 */
public class JoinGameRequest implements Request {

    private final String nickname;

    public JoinGameRequest(String nickname) {
        this.nickname = nickname;
    }

    public String getNickname() {
        return nickname;
    }
}