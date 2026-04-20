package it.polimi.ingsw.am54.model.effects.events;

import it.polimi.ingsw.am54.model.*;


//gestisce tutti gli effetti degli eventi
//l'effetto da applicare è determinato dalla etichettaSottoclasse letta dal JSON
public class EventEffect implements IEffect<EventCard> {

    private final String effectName;

    public EventEffect(String effectName) {
        this.effectName = effectName;
    }

    public String getEffectName() {
        return effectName;
    }

    @Override
    public boolean isFarmerEffect() {
        return "FARMER".equals(effectName);
    }

    // Farmer events are resolved last
    @Override
    public boolean shouldResolveLast() {
        return "FARMER".equals(effectName);
    }

    @Override
    public void apply(Player p, Game g, EventCard ec) {
        switch (effectName) {
            case "HUNTER" -> resolveHunter(g, ec);
            case "FARMER" -> resolveFarmer(g, ec);
            case "ARTIST" -> resolveArtist(g, ec);
            case "SHAMAN" -> resolveShaman(g, ec);
        }
    }

    private void resolveHunter(Game g, EventCard ec) {
        g.getPlayers().forEach(pl -> {
            long cacciatori = pl.getMyTribe().stream()
                    .filter(c -> c.getJobType() == JobType.HUNTER).count();

            int buildingAura = pl.getEventCostAura();

            pl.modifyFood((int) cacciatori * ec.getBaseFood());
            pl.modifyAuraPoints((int) cacciatori * ec.getAuraPerCharacter() + buildingAura);
            pl.resetEventAttributes();
        });
    }

    private void resolveFarmer(Game g, EventCard ec) {
        g.getPlayers().forEach(pl -> {
            int playerMaxFood = pl.getFood();
            int sconto = pl.getEventCostFood();
            int costo = pl.getMyTribe().size() + sconto;

            if (costo > 0) {
                if (costo > playerMaxFood) {
                    pl.modifyFood(-playerMaxFood);
                    pl.modifyAuraPoints((playerMaxFood - costo) * ec.getAuraPerCharacter());
                } else {
                    pl.modifyFood(-costo);
                }
            }
            pl.resetEventAttributes();
        });
    }

    private void resolveArtist(Game g, EventCard ec) {
        g.getPlayers().forEach(pl -> {
            long painters = pl.getMyTribe().stream()
                    .filter(c -> c.getJobType() == JobType.ARTIST).count();
            pl.modifyAuraPoints(painters <= ec.getThreshold()
                    ? -ec.getPenalty()
                    : (int) painters * ec.getBonusMultiplier());

            pl.resetEventAttributes();
        });
    }

    private void resolveShaman(Game g, EventCard ec) {
        int max = g.getPlayers().stream().mapToInt(Player::getMagicPoints).max().orElse(0);
        int min = g.getPlayers().stream().mapToInt(Player::getMagicPoints).min().orElse(0);

        g.getPlayers().forEach(pl -> {
            boolean isZero = pl.getBuildings().stream()
                    .flatMap(b -> b.getEffects().stream())
                    .anyMatch(IEffect::isNoLossEffect);
            boolean isDouble = pl.getBuildings().stream()
                    .flatMap(b -> b.getEffects().stream())
                    .anyMatch(IEffect::isDoubleAuraShamanEffect);

            if (pl.getAuraPoints() == max) {
                if (isDouble)
                    pl.modifyAuraPoints(2 * ec.getBonus());
                else pl.modifyAuraPoints(ec.getBonus());
            }
            if (pl.getAuraPoints() == min) {
                if (!isZero) pl.modifyAuraPoints(-ec.getMalus());
            }
            pl.resetEventAttributes();
        });
    }
}
