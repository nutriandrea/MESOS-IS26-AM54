package it.polimi.ingsw.am54.model.states;

import it.polimi.ingsw.am54.model.EventCard;
import it.polimi.ingsw.am54.model.Game;
import it.polimi.ingsw.am54.model.GameState;

/**
 * Manages the event resolution phase at the end of each round.
 * Sequentially executes the events in the event deck, applying
 * their effects to all players. Afterwards, manages the transition between eras
 * and prepares the board for the next round.
 *
 * @author AM54
 */
public class EventState extends GameState {
    /**
     * Returns the name of this state.
     *
     * @return the string "EventState"
     */
    public String getStateName() { return "EventState"; }

    /**
     * Indicates whether this state can be resolved automatically without player input.
     *
     * @return {@code true} as event resolution requires no player interaction
     */
    @Override
    public boolean isAutoResolvable() {
        return true;
    }

    /**
     * Indicates whether player disconnection is blocked in this state.
     *
     * @return {@code true} as disconnections are not allowed during event resolution
     */
    @Override
    public boolean isDisconnectBlocked() {
        return true;
    }

    /**
     * Resolves the event phase by executing all pending events, refreshing the board
     * rows, handling era transitions, advancing the round counter, and transitioning
     * to the next turn state.
     *
     * @param game the current game instance
     */
    @Override
    public void resolvePhase(Game game){

        EventCard currentEvent;
        while ((currentEvent = game.getBoard().popNextEvent()) != null) {

            // ==========================================
            // TRIGGER INJECTION: On Event
            // Before the event hits, we give Buildings the chance to prepare
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

            // RESOLVING THE EVENT
            // Now the event will calculate damages/bonuses by reading the temporary modifiers
            // just set by the Buildings, then it will clean everything (resetEventAttributes).
            currentEvent.resolveEvent(game);

            game.notifyEventResolved(currentEvent);
        }

        // 3. Discard all cards (Characters) remaining in the lower row
        game.getBoard().clearLowerRow();

        // 4. Move Characters and Events from the upper row to the lower row
        game.getBoard().moveUpperToLower();

        // 5. Fill the upper row (delegated to Game)
        game.refillUpperTribeRow();

        // 6. NEW ERA START (If the next card in the deck is from the next era)
        if (game.isEraTransition()) {
            game.advanceEra();

            // RULES: If transitioning to Era III -> discard any Buildings from the lower row
            if (game.getCurrentEra() == 3) {
                game.getBoard().discardLowerRowBuildings();
            }

            // Move Buildings from the upper row to the lower row
            game.getBoard().moveBuildings();

            // Place the new Era Buildings in the upper row
            game.refillUpperBuildings();
        }

        // 8. Check End Game Condition (Rule: The Tribe deck is exhausted)
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
            tile.freeSlot(); // Frees the occupant
            tile.resetPicks();
        }

        game.setState(new StartTurnState());
    }
}
