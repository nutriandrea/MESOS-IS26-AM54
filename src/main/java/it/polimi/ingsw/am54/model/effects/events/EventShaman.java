package it.polimi.ingsw.am54.model.effects.events;

import it.polimi.ingsw.am54.model.*;

public class EventShaman implements IEffect<EventCard> {

    // FIRMA AGGIORNATA PER LA FASE EVENTI
    @Override
    public void applyOnEvent(Player p, Game g, EventCard ec, EventCard currentEvent) {
        int max = g.getPlayers().stream().mapToInt(Player::getMagicPoints).max().orElse(0);
        int min = g.getPlayers().stream().mapToInt(Player::getMagicPoints).min().orElse(0);

        g.getPlayers().forEach(pl -> {
            // IL REFACTORING IN AZIONE: Non scorriamo più gli edifici!
            // Leggiamo i modificatori temporanei direttamente dal giocatore
            boolean isZero = pl.hasShamanNoLoss();
            boolean isDouble = pl.hasShamanDoubleAura();

            if (pl.getMagicPoints() == max) {
                if (isDouble)
                    pl.modifyAuraPoints(2 * ec.getBonus());
                else pl.modifyAuraPoints(ec.getBonus());
            }
            // se c'è un pareggio tra tutti i giocatori, tutti prima guadagnano PP e poi perdono PP
            if (pl.getMagicPoints() == min) {
                if (!isZero) pl.modifyAuraPoints(-ec.getMalus());
            }

            // Resetta tutti i modificatori temporanei (inclusi gli scudi dello sciamano) a fine evento
            pl.resetEventAttributes();
        });
    }
}