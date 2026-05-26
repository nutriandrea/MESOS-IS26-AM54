package it.polimi.ingsw.am54.network.messages;

/**
 * Messaggio inviato dal Server al Client per segnalare un errore
 * (es. mossa non valida o parametri errati).
 */
public class ErrorMessageUpdate implements Update {

    private final String message;

    public ErrorMessageUpdate(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public static final String TYPE = "error_message";
    @Override public String getType() { return TYPE; }
}