package it.polimi.ingsw.am54.model.states;

import it.polimi.ingsw.am54.model.Game;
import it.polimi.ingsw.am54.model.GameState;
import it.polimi.ingsw.am54.model.Player;
import it.polimi.ingsw.am54.model.OfferingTile;

public class StartTurnState extends GameState {
    /**
     * Places the turn player's totem on a free offering tile. Validates
     * turn, double placement and tile availability; then frees the player's slot
     * in the turn order. When all have placed, transitions to
     * {@link PickingState}, otherwise passes to the next player.
     *
     * @param game          the current game
     * @param nickname      the player placing the totem
     * @param offerPosition the index of the chosen offering tile
     * @throws Exception if it's not their turn, they have already placed, or the tile is occupied/invalid
     */
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

    /**
     * Skips the current player's turn (e.g. disconnected): moves them to the bottom
     * of the turn order, marks them as "has placed" and passes to the next.
     *
     * @param game     the current game
     * @param nickname the player to skip
     * @throws Exception if it's not their turn or they have already placed the totem
     */
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

    /**
     * After a crash recovery, identifies the player who has yet to act in
     * this phase (the first in the turn order who hasn't placed the totem).
     *
     * @param game the current game
     * @return the active player to restore, or {@code null} if none
     */
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

    /**
     * Handles the disconnection of a player during the placement phase: frees the
     * offering tile if they had occupied it, removes them from the turn order compacting
     * the others, and if it was their turn passes to the next. If the exit completes the phase,
     * transitions to {@link PickingState}.
     *
     * @param game            the current game
     * @param p               the disconnected player
     * @param isCurrentPlayer true if they were the turn player
     * @throws Exception in case of an error in the transition
     */
    @Override
    public void handleDisconnection(Game game, Player p, boolean isCurrentPlayer) throws Exception {
        // 1. If they had already placed (e.g. crash right after), frees the Offering tile
        if (p.hasPlacedTotem() && p.getCurrentTile() != null) {
            p.getCurrentTile().freeSlot();
            p.setCurrentTile(null);
            game.notifyTotemReturned(p, -1);
        }

        // We no longer force p.setHasPlacedTotem(true)! We let the game continue based on the active ones

        // 2. Removes the player from the Turn Order and shifts all others upwards (Shift Up)
        game.getBoard().removePlayerFromTurnOrderAndShiftUp(p);

        // 3. If they were choosing, let's move to the next
        if (isCurrentPlayer) {
            game.nextPlayer();
        }

        // 4. Immediate check: is the phase over due to the decrease in active players?
        if (game.getBoard().allTotemsPlaced(game.getActivePlayerCount())) {
            game.setState(new PickingState());
            Player first = game.getBoard().getFirstPlayerFromOfferingTrack();
            if (first != null) game.setCurrentPlayer(first);
        }
    }
}