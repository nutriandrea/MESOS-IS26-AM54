package it.polimi.ingsw.am54.model.effects.events;

import it.polimi.ingsw.am54.model.*;

/**
 * Sustenance event effect: each player must "feed" their tribe
 * by paying 1 food per card, discounted by gatherers (and buildings); if the food
 * is not enough, they pay the difference in aura.
 */
public class EventFarmer implements IEffect<EventCard> {

    // The isFarmerEffect() and shouldResolveLast() flags have been REMOVED!

    /**
     * For each player calculates the sustenance cost (1 per card in tribe),
     * subtracts the gatherers discount (×3) and the buildings discount, and applies the cost:
     * if the food is insufficient, converts the missing part into aura loss.
     * Finally resets temporary modifiers.
     *
     * @param p            not used (the effect iterates over all players)
     * @param g            the current game
     * @param ec           the event card owning the effect
     * @param currentEvent the event being resolved
     */
    @Override
    public void applyOnEvent(Player p, Game g, EventCard ec, EventCard currentEvent) {
        g.getPlayers().forEach(pl -> {
            int playerFood = pl.getFood();

            // 1. FIXED RULE: Count the Farmers and multiply by 3
            int scontoContadini = (int) pl.getMyTribe().stream()
                    .filter(c -> c.getJobType() == JobType.FARMER)
                    .count() * 3;

            // 2. We use Math.abs to transform the -1 of the building into a +1 to add!
            int scontoEdifici = Math.abs(pl.getEventCostFood());
            int scontoTotale = scontoContadini + scontoEdifici;

            // 3. The base cost is 1 food for EVERY card in the tribe (including the Farmers themselves)
            int costoBase = pl.getMyTribe().size();

            // 4. Let's calculate the net cost
            int costoFinale = costoBase - scontoTotale;

            if (costoFinale > 0) {
                if (costoFinale > playerFood) {
                    // Not enough food: pays what they have and takes the Aura malus
                    pl.modifyFood(-playerFood);

                    int ciboMancante = costoFinale - playerFood;
                    int auraPenalty = Math.abs(ec.getAuraPerCharacter());
                    if (auraPenalty == 0) auraPenalty = 2; // Safety parachute

                    pl.modifyAuraPoints(-(ciboMancante * auraPenalty));
                } else {
                    // Pays normally
                    pl.modifyFood(-costoFinale);
                }
            }

            // At the end of the event resolution, we reset the temporary modifiers
            pl.resetEventAttributes();
        });
    }
}