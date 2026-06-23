package it.polimi.ingsw.am54.model.states;

import it.polimi.ingsw.am54.model.EventCard;
import it.polimi.ingsw.am54.model.Game;
import it.polimi.ingsw.am54.model.GameState;

public class EventState extends GameState {
    public String getStateName() { return "EventState"; }

    @Override
    public boolean isAutoResolvable() {
        return true;
    }

    @Override
    public boolean isDisconnectBlocked() {
        return true;
    }

    /**
     * Automatically resolves the end-of-round event phase: applies building triggers and
     * resolves all event cards in order, then reorganizes the board (discards/moves rows,
     * refills, handles possible Era transition), advances the round, resets the totems and
     * finally transitions to {@link StartTurnState}.
     *
     * @param game the game on which to resolve the phase
     */
    @Override
    public void resolvePhase(Game game){

        EventCard currentEvent;
        while ((currentEvent = game.getBoard().popNextEvent()) != null) {

            // ==========================================
            // TRIGGER INJECTION: On Event
            // Before the event hits, give the Buildings a chance to prepare
            // the player's "medical record" (discounts, shaman shields, etc.)
            // ==========================================
            for (it.polimi.ingsw.am54.model.Player p : game.getPlayers()) {
                for (it.polimi.ingsw.am54.model.BuildingCard building : p.getBuildings()) {
                    if (building.getEffects() != null) {
                        for (it.polimi.ingsw.am54.model.IEffect<it.polimi.ingsw.am54.model.BuildingCard> effect : building.getEffects()) {
                            effect.applyOnEvent(p, game, building, currentEvent);
                        }
                    }
                }
            }

            // RESOLVE THE EVENT
            // Now the event will calculate damages/bonuses by reading the temporary modifiers
            // just set by the Buildings, after which it will clean everything (resetEventAttributes).
            currentEvent.resolveEvent(game);

            game.notifyEventResolved(currentEvent);
        }

        // 3. Discard all remaining cards (Characters) in the lower row
        game.getBoard().clearLowerRow();

        // 4. Move Characters and Events from the upper row to the lower row
        game.getBoard().moveUpperToLower();

        // 5. Refill the upper row (delegated to Game)
        game.refillUpperTribeRow();

        // 6. START OF NEW ERA (If the next card in the deck is from the next era)
        if (game.isEraTransition()) {
            game.advanceEra();

            // RULES: If transitioning to Era III -> discard any Buildings from the lower row
            if (game.getCurrentEra() == 3) {
                game.getBoard().discardLowerRowBuildings();
            }

            // Move the Buildings from the upper row to the lower row
            game.getBoard().moveBuildings();

            // Place the new Era Buildings in the upper row
            game.refillUpperBuildings();
        }

        // 8. End Game Condition Check (Rule: The Tribe deck is empty)
        game.advanceRound();

        // A. Reset the totem flag for all players
        for(it.polimi.ingsw.am54.model.Player p : game.getPlayers()) {
            p.setHasPlacedTotem(false);
        }

        // B. The new first player is the one sitting in the first Turn Order slot
        for (it.polimi.ingsw.am54.model.TurnOrderSlot slot : game.getBoard().getTurnOrderTile()) {
            if (slot.getOccupiedBy() != null) {
                game.setCurrentPlayer(slot.getOccupiedBy());
                break; // Found the first one, we stop
            }
        }
        for (it.polimi.ingsw.am54.model.OfferingTile tile : game.getBoard().getOfferingTrack()) {
            tile.freeSlot(); // Free the occupant
            tile.resetPicks();
        }

        game.setState(new StartTurnState());
    }
}