package it.polimi.ingsw.am54.model.effects.events;

import it.polimi.ingsw.am54.model.*;

public class EventShaman implements IEffect<EventCard> {

    @Override
    public void apply(Player p, Game g, EventCard ec) {
        int max = g.getPlayers().stream().mapToInt(Player::getMagicPoints).max().orElse(0);
        int min = g.getPlayers().stream().mapToInt(Player::getMagicPoints).min().orElse(0);

        g.getPlayers().forEach(pl -> {
            boolean isZero = pl.getBuildings().stream()
                    .flatMap(b -> b.getEffects().stream())
                    .anyMatch(IEffect::isNoLossEffect);
            boolean isDouble = pl.getBuildings().stream()
                    .flatMap(b -> b.getEffects().stream())
                    .anyMatch(IEffect::isDoubleAuraShamanEffect);

            if (pl.getMagicPoints() == max) {
                if (isDouble)
                    pl.modifyAuraPoints(2 * ec.getBonus());
                else pl.modifyAuraPoints(ec.getBonus());
            } else if (pl.getMagicPoints() == min) {
                if (!isZero) pl.modifyAuraPoints(-ec.getMalus());
            }
            pl.resetEventAttributes();
        });
    }
}