package it.polimi.ingsw.am54.model;

import java.util.*;


/**
 * Carta Personaggio: appartiene a uno dei mestieri ({@link JobType}) e, una volta
 * pescata, entra stabilmente nella tribù del giocatore applicando il proprio effetto
 * immediato e notificando gli edifici posseduti.
 */
public class CharacterCard extends TribeCard implements IDrawable {

    private int level;
    private JobType type;
    private int aura;
    private Phases phase;
    private List<IEffect<CharacterCard>> effects;
    private int minPlayers;

    public int getMinPlayers() {
        return minPlayers;
    }

    public void setMinPlayers(int minPlayers) {
        this.minPlayers = minPlayers;
    }
    public int getAura() {
        return aura;
    }
    public JobType getJobType() {
        return type;
    }
    public int getLevel() {
        return level;
    }

    public int getCost() {
        return 0;
    }

    public String getName() {
        return type != null ? type.name() : "Unknown";
    }

    public CharacterCard(int level, JobType type, int aura, Phases phase, List<IEffect<CharacterCard>>effects, int era) {
        super(era);
        this.level = level;
        this.type = type;
        this.aura = aura;
        this.phase = phase;
        this.effects = effects;
    }

    /**
     * Applica tutti gli effetti "on purchase" della carta sul giocatore.
     *
     * @param p il giocatore che ha acquistato la carta
     * @param g la partita corrente
     */
    public void resolveEffects(Player p, Game g) {
        for (IEffect<CharacterCard> effect : this.effects) {
            effect.applyOnPurchase(p, g, this);
        }
    }

    /**
     * Rivela il personaggio inserendolo nella fila Personaggi del tabellone.
     *
     * @param board la plancia di gioco
     * @param row   la fila di destinazione ("upper" o "lower")
     */
    @Override
    public void reveal(Board board, String row) {
        // La carta dice: "Ehi Board, sono un Personaggio, mettimi nella fila dei Personaggi!"
        board.addCharacterCard(this, row);
    }

    @Override
    public String toClientString() {
        String result = getImageId() + "|" + getEra() + "|" + getMinPlayers() + "|" + getName() + "|" + getLevel() + "|" + getAura();
        return result;
    }

    /**
     * Pesca il personaggio: lo aggiunge alla tribù del giocatore, ne risolve gli
     * effetti immediati e avvisa tutti gli edifici posseduti che la tribù si è
     * espansa (alcuni edifici reagiscono all'arrivo di nuovi personaggi).
     *
     * @param player il giocatore che pesca la carta
     * @param game   la partita corrente
     * @throws Exception se la risoluzione di un effetto fallisce
     */
    @Override
    public void pick(Player player, Game game) throws Exception {
        player.modifyMyTribe(this);
        this.resolveEffects(player, game);

        // NUOVO TRIGGER: Avvisiamo tutti gli edifici che la tribù si è espansa!
        for (BuildingCard building : player.getBuildings()) {
            if (building.getEffects() != null) {
                for (IEffect<BuildingCard> effect : building.getEffects()) {
                    // Passiamo 'this' che è il personaggio appena acquistato
                    effect.applyOnCharacterPicked(player, game, building, this);
                }
            }
        }
    }
}
