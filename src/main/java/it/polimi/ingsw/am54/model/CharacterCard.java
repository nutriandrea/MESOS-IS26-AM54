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
        for (IEffect<CharacterCard> effect : this.effects) {
            effect.applyOnPurchase(p, g, this);
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