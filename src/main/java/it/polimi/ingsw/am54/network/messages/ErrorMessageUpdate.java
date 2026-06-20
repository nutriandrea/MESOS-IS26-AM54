package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.network.client.ClientModel;

/**
 * Message sent by the Server to the Client to report an error
 * (e.g. invalid move or wrong parameters).
 */
public final class ErrorMessageUpdate implements Update {

    private final String message;

    /**
     * Constructs an error message update.
     *
     * @param message the error description to display to the client
     */
    public ErrorMessageUpdate(String message) {
        this.message = message;
    }

    /** @return the error message */
    public String getMessage() {
        return message;
    }

    /**
     * Applies this update to the client model, setting the last error message.
     *
     * @param model the client-side model to update
     */
    @Override
    public void applyTo(ClientModel model) {
        model.updateError(this.message);
    }
}