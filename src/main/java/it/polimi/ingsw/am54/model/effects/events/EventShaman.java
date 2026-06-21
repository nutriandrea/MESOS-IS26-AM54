package it.polimi.ingsw.am54.model.effects.events;

import it.polimi.ingsw.am54.model.*;

/**
 * Effect of the Shaman event (Shamanic Ritual): compares the Magic points
 * of all players. The player with the highest score gains
 * bonus Aura points; the one with the lowest score suffers a
 * penalty. Buildings with shamanic synergy can provide
 * immunity to loss or doubling of gain.
 *
 * @author AM54
 */
public class EventShaman implements IEffect<EventCard> {

    /**
     * Resolves the Shamanic Ritual event by comparing Magic points across
     * all players. The player(s) with the highest score gain Aura points;
     * those with the lowest lose Aura points. Shamanic building flags can
     * grant loss immunity or double the gain.
     *
     * @param p            the player who triggered the event (unused, affects all)
     * @param g            the current game instance
     * @param ec           the event card being resolved
     * @param currentEvent the event currently being processed
     */
    @Override
    public void applyOnEvent(Player p, Game g, EventCard ec, EventCard currentEvent) {
        int max = g.getPlayers().stream().mapToInt(Player::getMagicPoints).max().orElse(0);
        int min = g.getPlayers().stream().mapToInt(Player::getMagicPoints).min().orElse(0);

        // THE FIX: Count how many players have the maximum score to handle ties
        long maxPlayersCount = g.getPlayers().stream().filter(pl -> pl.getMagicPoints() == max).count();

        g.getPlayers().forEach(pl -> {
            boolean isZero = pl.hasShamanNoLoss();
            boolean isDouble = pl.hasShamanDoubleAura();

            if (pl.getMagicPoints() == max) {
                // Double ONLY if they have the bonus AND no ties (they are the only one with max)
                if (isDouble && maxPlayersCount == 1) {
                    pl.modifyAuraPoints(2 * ec.getBonus());
                } else {
                    pl.modifyAuraPoints(ec.getBonus());
                }
            }

            // if there is a tie between all players, everyone first gains PP and then loses PP
            if (pl.getMagicPoints() == min) {
                if (!isZero) pl.modifyAuraPoints(-ec.getMalus());
            }

            // Resets all temporary modifiers at end of event
            pl.resetEventAttributes();
        });
    }
}