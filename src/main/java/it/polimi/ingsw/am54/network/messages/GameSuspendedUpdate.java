package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.network.client.ClientModel;

/**
 * Notifica broadcast a tutti i client che il gioco e' stato sospeso
 * perche' e' rimasto un solo giocatore attivo.
 * SERVER -> CLIENT
 */
public final class GameSuspendedUpdate implements Update {

    @Override
    public void applyTo(ClientModel model) {
        model.updateGameSuspended();
    }
}
