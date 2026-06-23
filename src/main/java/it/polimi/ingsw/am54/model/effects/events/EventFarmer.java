package it.polimi.ingsw.am54.model.effects.events;

import it.polimi.ingsw.am54.model.*;

/**
 * Effetto dell'evento Sostentamento: ogni giocatore deve "nutrire" la propria tribù
 * pagando 1 cibo per carta, scontato dai raccoglitori (e dagli edifici); se il cibo
 * non basta, paga la differenza in aura.
 */
public class EventFarmer implements IEffect<EventCard> {

    // I flag isFarmerEffect() e shouldResolveLast() sono stati ELIMINATI!

    /**
     * Per ogni giocatore calcola il costo di sostentamento (1 per carta in tribù),
     * sottrae lo sconto dei raccoglitori (×3) e degli edifici, e applica il costo:
     * se il cibo è insufficiente, converte la parte mancante in perdita di aura.
     * Infine azzera i modificatori temporanei.
     *
     * @param p            non usato (l'effetto itera su tutti i giocatori)
     * @param g            la partita corrente
     * @param ec           la carta evento proprietaria dell'effetto
     * @param currentEvent l'evento in risoluzione
     */
    @Override
    public void applyOnEvent(Player p, Game g, EventCard ec, EventCard currentEvent) {
        g.getPlayers().forEach(pl -> {
            int playerFood = pl.getFood();

            // 1. REGOLA FISSA: Conta i Farmer e moltiplica per 3
            int scontoContadini = (int) pl.getMyTribe().stream()
                    .filter(c -> c.getJobType() == JobType.FARMER)
                    .count() * 3;

            // 2. Usiamo Math.abs per trasformare il -1 dell'edificio in un +1 da sommare!
            int scontoEdifici = Math.abs(pl.getEventCostFood());
            int scontoTotale = scontoContadini + scontoEdifici;

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

            // A fine risoluzione evento, resettiamo i modificatori temporanei
            pl.resetEventAttributes();
        });
    }
}
