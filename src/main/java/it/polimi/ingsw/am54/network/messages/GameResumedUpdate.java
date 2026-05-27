package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.network.client.ClientModel;

/**
 * Notifica broadcast a tutti i client che il gioco e' stato ripreso
 * dopo che un secondo giocatore si e' riconnesso.
 * SERVER -> CLIENT
 */
public final class GameResumedUpdate implements Update {

    @Override
    public void applyTo(ClientModel model) {
        model.updateGameResumed();
    }
}
