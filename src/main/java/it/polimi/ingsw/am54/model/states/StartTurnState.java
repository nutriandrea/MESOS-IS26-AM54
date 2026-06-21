package it.polimi.ingsw.am54.model.states;

import it.polimi.ingsw.am54.model.Game;
import it.polimi.ingsw.am54.model.GameState;
import it.polimi.ingsw.am54.model.Player;
import it.polimi.ingsw.am54.model.OfferingTile;

public class StartTurnState extends GameState {
    @Override
    public void placeTotem(Game game, String nickname, int offerPosition) throws Exception {
        Player player = game.getPlayerByNickname(nickname);

        if (!game.getCurrentPlayer().getNickname().equalsIgnoreCase(player.getNickname())) {
            throw new Exception("Non è il tuo turno per piazzare il totem!");
        }
        if (player.hasPlacedTotem()) {
            throw new Exception("Hai già piazzato il tuo totem in questo round!");
        }

        if (offerPosition < 0 || offerPosition >= game.getBoard().getOfferingTrack().size()) {
            throw new Exception("Posizione offerta non valida.");
        }
        OfferingTile targetTile = game.getBoard().getOfferingTrack().get(offerPosition);

        if (!targetTile.isAvailable()) {
            throw new Exception("Questa tessera offerta è già occupata!");
        }

        game.getBoard().addTotemToPosition(player, targetTile);
        player.setHasPlacedTotem(true);
        player.setCurrentTile(targetTile);

        game.notifyTotemPlaced(player, offerPosition);

        game.getBoard().getTurnOrderTile().stream()
                .filter(slot -> slot.getOccupiedBy() != null &&
                        slot.getOccupiedBy().getNickname().equalsIgnoreCase(player.getNickname()))
                .findFirst()
                .ifPresent(slot -> slot.freeSlot());

        if (game.getBoard().allTotemsPlaced(game.getActivePlayerCount())) {
            game.setState(new PickingState());
            Player firstPicker = game.getBoard().getFirstPlayerFromOfferingTrack();
            game.setCurrentPlayer(firstPicker);
        } else {
            game.nextPlayer();
        }
    }

    @Override
    public void skipTurn(Game game, String nickname) throws Exception {
        Player player = game.getPlayerByNickname(nickname);
        if (!game.getCurrentPlayer().equals(player)) {
            throw new Exception("Non e' il turno di " + nickname);
        }
        if (player.hasPlacedTotem()) {
            throw new Exception(nickname + " ha gia' piazzato il totem");
        }
        game.moveToLastSlot(player);
        player.setHasPlacedTotem(true);
        game.nextPlayer();
    }

    @Override
    public Player recoverActivePlayer(Game game) {
        for (it.polimi.ingsw.am54.model.TurnOrderSlot slot : game.getBoard().getTurnOrderTile()) {
            Player p = slot.getOccupiedBy();
            if (p != null && !p.hasPlacedTotem()) {
                return p;
            }
        }
        return null;
    }

    @Override
    public void handleDisconnection(Game game, Player p, boolean isCurrentPlayer) throws Exception {
        // 1. Se aveva già piazzato (es. crash subito dopo), libera la tessera Offerta
        if (p.hasPlacedTotem() && p.getCurrentTile() != null) {
            p.getCurrentTile().freeSlot();
            p.setCurrentTile(null);
            game.notifyTotemReturned(p, -1);
        }

        // Non forziamo più p.setHasPlacedTotem(true)! Lasciamo che il gioco prosegua basandosi sugli attivi

        // 2. Rimuove il giocatore dalla Turn Order e fa scalare tutti gli altri verso l'alto (Shift Up)
        game.getBoard().removePlayerFromTurnOrderAndShiftUp(p);

        // 3. Se stava scegliendo lui, passiamo al prossimo
        if (isCurrentPlayer) {
            game.nextPlayer();
        }

        // 4. Verifica immediata: la fase è finita grazie all'abbassamento dei player attivi?
        if (game.getBoard().allTotemsPlaced(game.getActivePlayerCount())) {
            game.setState(new PickingState());
            Player first = game.getBoard().getFirstPlayerFromOfferingTrack();
            if (first != null) game.setCurrentPlayer(first);
        }
    }
}