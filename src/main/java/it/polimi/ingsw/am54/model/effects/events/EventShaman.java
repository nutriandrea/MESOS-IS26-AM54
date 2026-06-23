package it.polimi.ingsw.am54.model.effects.events;

import it.polimi.ingsw.am54.model.*;

/**
 * Effetto dell'evento Rituale Sciamanico: premia in aura chi ha più punti magia e
 * penalizza chi ne ha meno, rispettando gli scudi/raddoppi impostati dagli edifici.
 */
public class EventShaman implements IEffect<EventCard> {

    /**
     * Calcola il massimo e il minimo di punti magia tra i giocatori: chi è al massimo
     * guadagna il bonus (raddoppiato se ha l'edificio "double aura" ed è l'unico al
     * massimo), chi è al minimo subisce il malus (salvo "no loss"); poi azzera i
     * modificatori temporanei.
     *
     * @param p            non usato (l'effetto itera su tutti i giocatori)
     * @param g            la partita corrente
     * @param ec           la carta evento proprietaria dell'effetto
     * @param currentEvent l'evento in risoluzione
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
