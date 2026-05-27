package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.network.client.ClientModel;

/**
 * Messaggio inviato dal Server al Client per segnalare un errore
 * (es. mossa non valida o parametri errati).
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