package it.polimi.ingsw.am54.model.effects.events;

import it.polimi.ingsw.am54.model.*;
import it.polimi.ingsw.am54.model.effects.buildings.BuildingEventCostModifier; // <-- Import fondamentale!

public class EventFarmer implements IEffect<EventCard> {

    @Override
    public boolean isFarmerEffect() {
        return true;
    }

    @Override
    public boolean shouldResolveLast() {
        return true;
    }

    @Override
    public void apply(Player p, Game g, EventCard ec) {
        g.getPlayers().forEach(pl -> {

            // 1. Puliamo eventuali valori residui prima di calcolare
            pl.resetEventAttributes();

            // 2. RICALCOLO DINAMICO: Applichiamo gli effetti degli edifici in tempo reale.
            // In questo modo lo sconto si basa sul numero ESATTO di carte possedute ORA.
            for (BuildingCard b : pl.getBuildings()) {
                for (IEffect effect : b.getEffects()) {
                    if (effect instanceof BuildingEventCostModifier) {
                        effect.apply(pl, g, b);
                    }
                }
            }

            int playerFood = pl.getFood();

            // 3. REGOLA FISSA: Conta i Farmer e moltiplica per 3
            int scontoContadini = (int) pl.getMyTribe().stream()
                    .filter(c -> c.getJobType() == JobType.FARMER)
                    .count() * 3;

            // 4. Usiamo Math.abs per trasformare il -1 dell'edificio in un +1 da sommare!
            int scontoEdifici = Math.abs(pl.getEventCostFood());
            int scontoTotale = scontoContadini + scontoEdifici;

            // 5. Il costo base è 1 cibo per OGNI carta nella tribù (inclusi i Farmer stessi)
            int costoBase = pl.getMyTribe().size();

            // 6. Calcoliamo il costo netto
            int costoFinale = costoBase - scontoTotale;

            if (costoFinale > 0) {
                if (costoFinale > playerFood) {
                    // Non ha abbastanza cibo: paga quello che ha e prende il malus in Aura
                    pl.modifyFood(-playerFood);

                    int ciboMancante = costoFinale - playerFood;
                    int auraPenalty = Math.abs(ec.getAuraPerCharacter());
                    if (auraPenalty == 0) auraPenalty = 2; // Paracadute di sicurezza

                    pl.modifyAuraPoints(-(ciboMancante * auraPenalty));
                } else {
                    // Paga normalmente
                    pl.modifyFood(-costoFinale);
                }
            }

            // 7. Pulizia finale degli attributi per non "sporcare" gli eventi successivi
            pl.resetEventAttributes();
        });
    }
}