package it.polimi.ingsw.am54.model.states;

import it.polimi.ingsw.am54.model.Game;
import it.polimi.ingsw.am54.model.GameException;
import it.polimi.ingsw.am54.model.GameState;
import it.polimi.ingsw.am54.model.Player;
import it.polimi.ingsw.am54.model.OfferingTile;

public class StartTurnState extends GameState {
    @Override
    public void placeTotem(Game game, String nickname, int offerPosition) throws GameException {
        Player player = game.getPlayerByNickname(nickname);

        if (!game.getCurrentPlayer().getNickname().equalsIgnoreCase(player.getNickname())) {
            throw new GameException("Non è il tuo turno per piazzare il totem!");
        }
        if (player.hasPlacedTotem()) {
            throw new GameException("Hai già piazzato il tuo totem in questo round!");
        }

        if (offerPosition < 0 || offerPosition >= game.getBoard().getOfferingTrack().size()) {
            throw new GameException("Posizione offerta non valida.");
        }
        OfferingTile targetTile = game.getBoard().getOfferingTrack().get(offerPosition);

        if (!targetTile.isAvailable()) {
            throw new GameException("Questa tessera offerta è già occupata!");
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

        // FIX: La fase passa quando tutti gli ATTIVI hanno piazzato
        if (game.getBoard().allTotemsPlaced(game.getActivePlayerCount())) {
            game.setState(new PickingState());
            Player firstPicker = game.getBoard().getFirstPlayerFromOfferingTrack();
            game.setCurrentPlayer(firstPicker);
        } else {
            game.nextPlayer();
        }
    }

    @Override
    public void skipTurn(Game game, String nickname) throws GameException {
        Player player = game.getPlayerByNickname(nickname);
        if (!game.getCurrentPlayer().equals(player)) {
            throw new GameException("Non e' il turno di " + nickname);
        }
        if (player.hasPlacedTotem()) {
            throw new GameException(nickname + " ha gia' piazzato il totem");
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
    public void handleDisconnection(Game game, Player p, boolean isCurrentPlayer) throws GameException {
        if (p.hasPlacedTotem() && p.getCurrentTile() != null) {
            p.getCurrentTile().freeSlot();
            p.setCurrentTile(null);
            game.notifyTotemReturned(p, -1);
        }

        game.getBoard().removePlayerFromTurnOrderAndShiftUp(p);

        if (isCurrentPlayer) {
            game.nextPlayer();
        }

        if (game.getBoard().allTotemsPlaced(game.getActivePlayerCount())) {
            game.setState(new PickingState());
            Player first = game.getBoard().getFirstPlayerFromOfferingTrack();
            if (first != null) game.setCurrentPlayer(first);
        }
    }
}