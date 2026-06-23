package it.polimi.ingsw.am54.model;

import java.util.*;

/**
 * Character Card: belongs to one of the jobs ({@link JobType}) and, once
 * picked, permanently enters the player's tribe, applying its immediate effect
 * and notifying owned buildings.
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
     * Applies all the "on purchase" effects of the card on the player.
     *
     * @param p the player who purchased the card
     * @param g the current game
     */
    public void resolveEffects(Player p, Game g) {
        for (IEffect<CharacterCard> effect : this.effects) {
            effect.applyOnPurchase(p, g, this);
        }
    }

    /**
     * Reveals the character by placing it in the Characters row of the board.
     *
     * @param board the game board
     * @param row   the target row ("upper" or "lower")
     */
    @Override
    public void reveal(Board board, String row) {
        // The card says: "Hey Board, I'm a Character, put me in the Characters row!"
        board.addCharacterCard(this, row);
    }

    @Override
    public String toClientString() {
        String result = getImageId() + "|" + getEra() + "|" + getMinPlayers() + "|" + getName() + "|" + getLevel() + "|" + getAura();
        return result;
    }

    /**
     * Picks the character: adds it to the player's tribe, resolves its
     * immediate effects and notifies all owned buildings that the tribe has expanded
     * (some buildings react to the arrival of new characters).
     *
     * @param player the player who picks the card
     * @param game   the current game
     * @throws Exception if an effect resolution fails
     */
    @Override
    public void pick(Player player, Game game) throws Exception {
        player.modifyMyTribe(this);
        this.resolveEffects(player, game);

        // NEW TRIGGER: Let's notify all buildings that the tribe has expanded!
        for (BuildingCard building : player.getBuildings()) {
            if (building.getEffects() != null) {
                for (IEffect<BuildingCard> effect : building.getEffects()) {
                    // We pass 'this' which is the newly purchased character
                    effect.applyOnCharacterPicked(player, game, building, this);
                }
            }
        }
    }
}