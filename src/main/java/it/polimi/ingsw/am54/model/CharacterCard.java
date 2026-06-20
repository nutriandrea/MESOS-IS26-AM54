package it.polimi.ingsw.am54.model;

import java.util.*;


/**
 * Character Card representing a tribe member. Has a level, a job type,
 * aura points, an activation phase and a list of effects. Activation occurs
 * in the character phase of the turn. Extends TribeCard and implements IDrawable.
 * @author AM54
 */
public class CharacterCard extends TribeCard implements IDrawable {

    private int level;
    private JobType type;
    private int aura;
    private Phases phase;
    private List<IEffect<CharacterCard>> effects;
    private int minPlayers;

    /**
     * Returns the minimum number of players required for this character to appear in the game.
     *
     * @return the minimum player count
     */
    public int getMinPlayers() {
        return minPlayers;
    }

    /**
     * Sets the minimum number of players required for this character to appear.
     *
     * @param minPlayers the minimum player count
     */
    public void setMinPlayers(int minPlayers) {
        this.minPlayers = minPlayers;
    }
    /**
     * Returns the aura points contributed by this character.
     *
     * @return the aura value
     */
    public int getAura() {
        return aura;
    }
    /**
     * Returns the job type of this character.
     *
     * @return the job type
     */
    public JobType getJobType() {
        return type;
    }
    /**
     * Returns the level of this character.
     *
     * @return the character level
     */
    public int getLevel() {
        return level;
    }

    /**
     * Returns the food cost to purchase this character.
     * Character cards have no cost.
     *
     * @return always 0
     */
    public int getCost() {
        return 0;
    }

    /**
     * Returns the name of this character, derived from its job type.
     *
     * @return the job type name, or "Unknown" if the type is not set
     */
    public String getName() {
        return type != null ? type.name() : "Unknown";
    }

    /**
     * Constructs a CharacterCard with the specified level, job type, aura, phase, effects and era.
     *
     * @param level   the character level
     * @param type    the job type
     * @param aura    the aura points contributed by the character
     * @param phase   the activation phase of the character effects
     * @param effects the list of effects associated with the character
     * @param era     the era to which the character belongs (1-3)
     */
    public CharacterCard(int level, JobType type, int aura, Phases phase, List<IEffect<CharacterCard>>effects, int era) {
        super(era);
        this.level = level;
        this.type = type;
        this.aura = aura;
        this.phase = phase;
        this.effects = effects;
    }

    /**
     * Resolves all purchase-time effects of this character card on the specified player.
     *
     * @param p the player who picked the character
     * @param g the current game instance
     */
    public void resolveEffects(Player p, Game g) {
        for (IEffect<CharacterCard> effect : this.effects) {
            effect.applyOnPurchase(p, g, this);
        }
    }

    /**
     * Reveals this character card by placing it on the board in the specified row.
     *
     * @param board the game board
     * @param row   the target row ("upper" or "lower")
     */
    @Override
    public void reveal(Board board, String row) {
        // The card says: "Hey Board, I'm a Character, put me in the Character row!"
        board.addCharacterCard(this, row);
    }

    /**
     * Returns a pipe-separated textual representation of this character for the client.
     * Format: {@code imageId|era|minPlayers|name|level|aura}
     *
     * @return the client-facing string representation
     */
    @Override
    public String toClientString() {
        String result = getImageId() + "|" + getEra() + "|" + getMinPlayers() + "|" + getName() + "|" + getLevel() + "|" + getAura();
        System.out.println("[DEBUG-CHAR] toClientString() called: imageId=" + getImageId() + " era=" + getEra() + " minP=" + getMinPlayers() + " name=" + getName() + " result=" + result);
        return result;
    }

    /**
     * Picks this character: adds it to the player's tribe, resolves its effects,
     * and notifies all owned buildings of the new tribe member.
     *
     * @param player the player picking the character
     * @param game   the current game instance
     * @throws GameException if the character cannot be picked
     */
    @Override
    public void pick(Player player, Game game) throws GameException {
        player.modifyMyTribe(this);
        this.resolveEffects(player, game);

        // NEW TRIGGER: Notify all buildings that the tribe has expanded!
        for (BuildingCard building : player.getBuildings()) {
            if (building.getEffects() != null) {
                for (IEffect<BuildingCard> effect : building.getEffects()) {
                    // Pass 'this' which is the character just purchased
                    effect.applyOnCharacterPicked(player, game, building, this);
                }
            }
        }
    }
}