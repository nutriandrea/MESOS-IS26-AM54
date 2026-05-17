package it.polimi.ingsw.am54.model.effects.events;

import it.polimi.ingsw.am54.model.*;

public class EventFarmer implements IEffect<EventCard> {

    // Effettuiamo l'override dei metodi di default della tua interfaccia
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
            int playerFood = pl.getFood();

            // 1. REGOLA FISSA: Conta i Farmer e moltiplica per 3
            int scontoContadini = (int) pl.getMyTribe().stream()
                    .filter(c -> c.getJobType() == JobType.FARMER)
                    .count() * 3;

            // 2. Sommiamo lo sconto dei Farmer con eventuali sconti degli Edifici
            int scontoTotale = scontoContadini + pl.getEventCostFood();

            // 3. Il costo base è 1 cibo per OGNI carta nella tribù (inclusi i Farmer stessi)
            int costoBase = pl.getMyTribe().size();

            // 4. Calcoliamo il costo netto
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

            pl.resetEventAttributes();
        });
    }
}