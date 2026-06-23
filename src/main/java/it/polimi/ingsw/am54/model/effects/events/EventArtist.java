package it.polimi.ingsw.am54.model.effects.events;

import it.polimi.ingsw.am54.model.*;

/**
 * Effetto dell'evento Pitture Rupestri: chi raggiunge la soglia di artisti guadagna
 * aura proporzionale, chi non la raggiunge subisce una penalità.
 */
public class EventArtist implements IEffect<EventCard> {

    /**
     * Per ogni giocatore confronta il numero di artisti con la soglia dell'evento:
     * sotto soglia applica la penalità in aura, da soglia in su accredita
     * {@code artisti * moltiplicatore}; infine azzera i modificatori temporanei.
     *
     * @param p            non usato (l'effetto itera su tutti i giocatori)
     * @param g            la partita corrente
     * @param ec           la carta evento proprietaria dell'effetto
     * @param currentEvent l'evento in risoluzione
     */
    @Override
    public void applyOnEvent(Player p, Game g, EventCard ec, EventCard currentEvent) {
        g.getPlayers().forEach(pl -> {
            long painters = pl.getMyTribe().stream()
                    .filter(c -> c.getJobType() == JobType.ARTIST).count();

            // Legge i modificatori temporanei impostati dagli edifici (se ci sono)
            int buildingFood = pl.getEventCostFood();
            if (buildingFood > 0) pl.modifyFood(buildingFood);

            pl.modifyAuraPoints(painters < ec.getThreshold()
                    ? -ec.getPenalty()
                    : (int) painters * ec.getBonusMultiplier());

            // Resetta i modificatori a fine evento
            pl.resetEventAttributes();
        });
    }
}
