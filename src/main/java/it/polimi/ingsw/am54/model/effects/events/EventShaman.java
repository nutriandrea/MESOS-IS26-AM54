package it.polimi.ingsw.am54.model.effects.events;

import it.polimi.ingsw.am54.model.*;

public class EventShaman implements IEffect<EventCard> {

    @Override
    public void applyOnEvent(Player p, Game g, EventCard ec, EventCard currentEvent) {
        int max = g.getPlayers().stream().mapToInt(Player::getMagicPoints).max().orElse(0);
        int min = g.getPlayers().stream().mapToInt(Player::getMagicPoints).min().orElse(0);

        // IL FIX: Contiamo quanti giocatori hanno il punteggio massimo per gestire i pareggi
        long maxPlayersCount = g.getPlayers().stream().filter(pl -> pl.getMagicPoints() == max).count();

        g.getPlayers().forEach(pl -> {
            boolean isZero = pl.hasShamanNoLoss();
            boolean isDouble = pl.hasShamanDoubleAura();

            if (pl.getMagicPoints() == max) {
                // Raddoppia SOLO se ha il bonus E non ci sono pareggi (è l'unico ad avere il max)
                if (isDouble && maxPlayersCount == 1) {
                    pl.modifyAuraPoints(2 * ec.getBonus());
                } else {
                    pl.modifyAuraPoints(ec.getBonus());
                }
            }

            // se c'è un pareggio tra tutti i giocatori, tutti prima guadagnano PP e poi perdono PP
            if (pl.getMagicPoints() == min) {
                if (!isZero) pl.modifyAuraPoints(-ec.getMalus());
            }

            // Resetta tutti i modificatori temporanei a fine evento
            pl.resetEventAttributes();
        });
    }
}