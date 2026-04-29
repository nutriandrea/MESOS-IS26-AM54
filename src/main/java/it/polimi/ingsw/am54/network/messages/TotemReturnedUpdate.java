package it.polimi.ingsw.am54.network.messages;

/**
 * Messaggio di update inviato dal Server quando un giocatore finisce
 * il suo turno di Pick. Notifica ai Client di togliere il Totem
 * dalla Offering Track e rimetterlo nella Turn Order Track.
 */
public class TotemReturnedUpdate implements Update {

    private final String nickname;

    public TotemReturnedUpdate(String nickname) {
        this.nickname = nickname;
    }

    public String getNickname() {
        return nickname;
    }
}