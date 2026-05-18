package it.polimi.ingsw.am54.model;

import java.util.*;


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

    public void resolveEffects(Player p, Game g) {
        // Scorri tutti gli effetti assegnati a questo personaggio
        for (IEffect effect : this.effects) {
            // Applica l'effetto. Passi 'this' perché la lambda potrebbe aver bisogno di leggere il livello o il tipo di QUESTA carta
            effect.apply(p, g, this);
        }
    }

    @Override
    public void reveal(Board board, String row) {
        // La carta dice: "Ehi Board, sono un Personaggio, mettimi nella fila dei Personaggi!"
        board.addCharacterCard(this, row);
    }

    @Override
    public void pick(Player player, Game game) throws Exception {
        player.modifyMyTribe(this);
        this.resolveEffects(player, game);
    }
}