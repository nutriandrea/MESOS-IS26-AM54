package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.network.client.ClientModel;

/**
 * Broadcast notification sent by the server when a new player joins the game.
 * SERVER -> CLIENT
 */
public final class PlayerJoinedUpdate implements Update {

    private final String nickname;

    /**
     * Constructs a notification for the given nickname.
     *
     * @param nickname the nickname of the player who joined
     */
    public PlayerJoinedUpdate(String nickname) {
        this.nickname = nickname;
    }

    /**
     * Returns the nickname of the player who joined.
     *
     * @return the joined player's nickname
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * Applies this update to the client model, registering the new player.
     *
     * @param model the client-side model to update
     */
    @Override
    public void applyTo(ClientModel model) {
        model.updatePlayerJoined(this.nickname);
    }
}
