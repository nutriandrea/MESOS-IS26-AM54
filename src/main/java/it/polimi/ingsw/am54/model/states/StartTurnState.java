package it.polimi.ingsw.am54.model.states;

import it.polimi.ingsw.am54.model.Game;
import it.polimi.ingsw.am54.model.GameState;
import it.polimi.ingsw.am54.model.Player;
import it.polimi.ingsw.am54.model.OfferingTile;

public class StartTurnState extends GameState {

    @Override
    public void placeTotem(Game game, String nickname, int offerPosition) throws Exception {
        Player player = game.getPlayerByNickname(nickname);

        // 1. Controlli di sicurezza
        if (!game.getCurrentPlayer().equals(player)) {
            throw new Exception("Non è il tuo turno per piazzare il totem!");
        }
        if (player.hasPlacedTotem()) {
            throw new Exception("Hai già piazzato il tuo totem in questo round!");
        }

        // 2. Validazione e ricerca della tessera
        if (offerPosition < 0 || offerPosition >= game.getBoard().getOfferingTrack().size()) {
            throw new Exception("Posizione offerta non valida.");
        }
        OfferingTile targetTile = game.getBoard().getOfferingTrack().get(offerPosition);

        if (!targetTile.isAvailable()) {
            throw new Exception("Questa tessera offerta è già occupata!");
        }

        // 3. Piazzamento effettivo
        game.getBoard().addTotemToPosition(player, targetTile);
        player.setHasPlacedTotem(true);
        player.setCurrentTile(targetTile);

        // 4. Liberiamo il giocatore dallo slot dell'ordine di turno precedente
        game.getBoard().getTurnOrderTile().stream()
                .filter(slot -> player.equals(slot.getOccupiedBy()))
                .findFirst()
                .ifPresent(slot -> slot.freeSlot());

        // 5. GESTIONE DEL FLUSSO: Controlliamo se la fase è finita
        if (game.getBoard().allTotemsPlaced(game.getPlayersNumber())) {

            // Tutti hanno piazzato, si passa alla fase di pesca
            game.setState(new PickingState());

            // Regola: Dal giocatore con Totem più a sinistra verso destra
            Player firstPicker = game.getBoard().getFirstPlayerFromOfferingTrack();
            game.setCurrentPlayer(firstPicker);

        } else {
            // C'è ancora qualcuno che deve piazzare, passiamo al prossimo in base all'ordine
            game.nextPlayer();
        }
    }
}