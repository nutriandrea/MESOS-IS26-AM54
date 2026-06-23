package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.network.client.ClientModel;

/**
 * Message sent by the Server to the Client to report an error
 * (e.g., invalid move or wrong parameters).
 */
public final class ErrorMessageUpdate implements Update {

    private final String message;

    public ErrorMessageUpdate(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public void applyTo(ClientModel model) {
        model.updateError(this.message);
    }
}