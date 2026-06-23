package it.polimi.ingsw.am54.model.effects.events;

import it.polimi.ingsw.am54.model.*;

/**
 * Shamanic Ritual event effect: rewards in aura the one with the most magic points and
 * penalizes the one with the least, respecting the shields/doublings set by buildings.
 */
public class EventShaman implements IEffect<EventCard> {

    /**
     * Calculates the maximum and minimum magic points among the players: the one at the maximum
     * gains the bonus (doubled if they have the "double aura" building and are the only one at the
     * maximum), the one at the minimum suffers the malus (unless "no loss"); then resets the
     * temporary modifiers.
     *
     * @param p            not used (the effect iterates over all players)
     * @param g            the current game
     * @param ec           the event card owning the effect
     * @param currentEvent the event being resolved
     */
    @Override
    public void applyOnEvent(Player p, Game g, EventCard ec, EventCard currentEvent) {
        int max = g.getPlayers().stream().mapToInt(Player::getMagicPoints).max().orElse(0);
        int min = g.getPlayers().stream().mapToInt(Player::getMagicPoints).min().orElse(0);

        long maxPlayersCount = g.getPlayers().stream().filter(pl -> pl.getMagicPoints() == max).count();

        g.getPlayers().forEach(pl -> {
            boolean isZero = pl.hasShamanNoLoss();
            boolean isDouble = pl.hasShamanDoubleAura();

            if (pl.getMagicPoints() == max) {
                // Doubles ONLY if they have the bonus AND there are no ties (they are the only one having the max)
                if (isDouble && maxPlayersCount == 1) {
                    pl.modifyAuraPoints(2 * ec.getBonus());
                } else {
                    pl.modifyAuraPoints(ec.getBonus());
                }
            }

            // if there is a tie among all players, everyone first gains PP and then loses PP
            if (pl.getMagicPoints() == min) {
                if (!isZero) pl.modifyAuraPoints(-ec.getMalus());
            }

            // Resets all temporary modifiers at the end of the event
            pl.resetEventAttributes();
        });
    }
}