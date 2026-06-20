package it.polimi.ingsw.am54.model;

import java.util.List;

/**
 * Event Card with an instant effect that resolves during the event phase of the round.
 * Can be a sustenance event (FARMER) or a thematic event with thresholds,
 * multipliers, bonuses and penalties. Resolution occurs via resolveEvent().
 * @author AM54
 */
public class EventCard extends TribeCard {

    private boolean finalEvent;
    private List<IEffect<EventCard>> effects;

    private int baseFood;
    private int auraPerCharacter;
    private int threshold;
    private int bonusMultiplier;
    private int penalty;
    private int bonus;
    private int malus;
    private String name;

    /**
     * Constructs an EventCard with the specified era, final flag, effects and event parameters.
     *
     * @param era            the era to which the event belongs (1-3)
     * @param finalEvent     whether this is a final (endgame) event
     * @param effects        the list of effects associated with the event
     * @param baseFood       the base food amount for the event
     * @param auraPerCharacter the aura granted per matching character
     * @param threshold      the threshold for bonus/penalty activation
     * @param bonusMultiplier the multiplier applied to the bonus
     * @param penalty        the penalty value for failing the event
     * @param bonus          the bonus value for succeeding the event
     * @param malus          the malus value applied during the event
     */
    public EventCard(int era, boolean finalEvent, List<IEffect<EventCard>> effects,
                     int baseFood, int auraPerCharacter, int threshold,
                     int bonusMultiplier, int penalty, int bonus, int malus) {
        super(era);
        this.finalEvent = finalEvent;
        this.effects = effects;
        this.baseFood = baseFood;
        this.auraPerCharacter = auraPerCharacter;
        this.threshold = threshold;
        this.bonusMultiplier = bonusMultiplier;
        this.penalty = penalty;
        this.bonus = bonus;
        this.malus = malus;
    }

    /**
     * Returns the event card name.
     * The name is generated as "Event-" + era.
     * Used by PersistenceManager to save cards in decks.
     *
     * @return card name (e.g. "Event-1", "Event-2")
     */
    public String getName() {
        return this.name != null ? this.name + " - ERA: " + getEra() : "Event - Era: " + getEra();
    }

    /**
     * Returns the base food amount for this event.
     *
     * @return the base food value
     */
    public int getBaseFood() { return baseFood; }

    /**
     * Returns the aura granted per matching character during event resolution.
     *
     * @return the aura per character value
     */
    public int getAuraPerCharacter() { return auraPerCharacter; }

    /**
     * Returns the threshold for bonus/penalty activation.
     *
     * @return the threshold value
     */
    public int getThreshold() { return threshold; }

    /**
     * Returns the multiplier applied to the event bonus.
     *
     * @return the bonus multiplier
     */
    public int getBonusMultiplier() { return bonusMultiplier; }

    /**
     * Returns the penalty applied when the event is failed.
     *
     * @return the penalty value
     */
    public int getPenalty() { return penalty; }

    /**
     * Returns the bonus granted when the event is succeeded.
     *
     * @return the bonus value
     */
    public int getBonus() { return bonus; }

    /**
     * Returns the malus applied during the event.
     *
     * @return the malus value
     */
    public int getMalus() { return malus; }

    /**
     * Checks whether this event is a sustenance (FARMER) event.
     *
     * @return {@code true} if the event name is "FARMER"
     */
    public boolean isSustenanceEvent() {
        return "FARMER".equals(name);
    }

    /**
     * Checks whether this event matches the specified job type.
     *
     * @param jobType the job type to compare
     * @return {@code true} if the event name matches the job type name
     */
    public boolean matchesJobType(JobType jobType) {
        return jobType != null && jobType.name().equals(this.name);
    }

    /**
     * Resolves all effects of this event card against the game state.
     *
     * @param g the current game instance
     */
    public void resolveEvent(Game g) {
        for (IEffect<EventCard> effect : effects) {
            // Call the new hook.
            // Pass 'null' for Player because classes like EventHunter or EventShaman
            // already loop over all players internally!
            // Pass 'this' both as the card owning the effect and as the current event.
            effect.applyOnEvent(null, g, this, this);
        }
    }

    /**
     * Returns a pipe-separated textual representation of this event for the client.
     * Format: {@code imageId|era|0|EVENT|name}
     *
     * @return the client-facing string representation
     */
    @Override
    public String toClientString() {
        return getImageId() + "|" + getEra() + "|0|EVENT|" + getName();
    }

    /**
     * Reveals this event card by placing it on the board in the specified row.
     *
     * @param board the game board
     * @param row   the target row
     */
    @Override
    public void reveal(Board board, String row) {
        // The card says: "I'm an Event, put me in the Event row"
        board.addEventCard(this, row);
    }

    /**
     * Returns whether this is a final (endgame) event.
     *
     * @return {@code true} if this is a final event
     */
    public boolean isFinalEvent() {
        return finalEvent;
    }

    /**
     * Sets the name of this event card.
     *
     * @param name the event name
     */
    public void setName(String name) {
        this.name = name;
    }
}