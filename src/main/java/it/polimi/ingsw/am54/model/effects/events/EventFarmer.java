package it.polimi.ingsw.am54.model.effects.events;

import it.polimi.ingsw.am54.model.*;

/**
 * Effect of the Farmer event (Sustenance): imposes a food cost
 * for each character in the player&rsquo;s tribe. Discounts applied
 * by buildings reduce the total cost. If the player does not have enough
 * food, pays as much as possible and suffers a penalty in Aura points.
 *
 * @author AM54
 */
public class EventFarmer implements IEffect<EventCard> {

    // The isFarmerEffect() and shouldResolveLast() flags have been REMOVED!

    // UPDATED SIGNATURE FOR THE EVENT PHASE
    /**
     * Resolves the Sustenance event by charging each player 1 food per
     * character in their tribe, reduced by farmer and building discounts.
     * Players who cannot pay in full suffer an Aura point penalty.
     *
     * @param p            the player who triggered the event (unused, affects all)
     * @param g            the current game instance
     * @param ec           the event card being resolved
     * @param currentEvent the event currently being processed
     */
    @Override
    public void applyOnEvent(Player p, Game g, EventCard ec, EventCard currentEvent) {
        g.getPlayers().forEach(pl -> {
            int playerFood = pl.getFood();

            // 1. FIXED RULE: Count Farmers and multiply by 3
            int farmerDiscount = (int) pl.getMyTribe().stream()
                    .filter(c -> c.getJobType() == JobType.FARMER)
                    .count() * 3;

            // 2. Use Math.abs to turn the -1 from the building into a +1 to add!
            int buildingDiscount = Math.abs(pl.getEventCostFood());
            int totalDiscount = farmerDiscount + buildingDiscount;

            // 3. The base cost is 1 food for EVERY card in the tribe (including Farmers themselves)
            int baseCost = pl.getMyTribe().size();

            // 4. Calculate the net cost
            int finalCost = baseCost - totalDiscount;

            if (finalCost > 0) {
                if (finalCost > playerFood) {
                    // Not enough food: pay what you have and take the Aura penalty
                    pl.modifyFood(-playerFood);

                    int missingFood = finalCost - playerFood;
                    int auraPenalty = Math.abs(ec.getAuraPerCharacter());
                    if (auraPenalty == 0) auraPenalty = 2; // Safety parachute

                    pl.modifyAuraPoints(-(missingFood * auraPenalty));
                } else {
                    // Pay normally
                    pl.modifyFood(-finalCost);
                }
            }

            // At the end of event resolution, reset the temporary modifiers
            pl.resetEventAttributes();
        });
    }
}