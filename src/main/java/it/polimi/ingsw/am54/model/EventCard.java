package it.polimi.ingsw.am54.model;

import java.util.List;

/**
 * Event Card: it is resolved globally on all players during the end-of-round events phase.
 * The numerical parameters (base food, aura per character, thresholds,
 * bonus/malus) are interpreted by its own effect according to the event type.
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
     * Returns the name of the event card.
     * The name is generated as "Event-" + era.
     * Used by the PersistenceManager to save the cards in the decks.
     *
     * @return card name (e.g. "Event-1", "Event-2")
     */
    public String getName() {
        return this.name != null ? this.name + " - ERA: " + getEra() : "Event - Era: " + getEra();
    }

    public int getBaseFood() { return baseFood; }
    public int getAuraPerCharacter() { return auraPerCharacter; }
    public int getThreshold() { return threshold; }
    public int getBonusMultiplier() { return bonusMultiplier; }
    public int getPenalty() { return penalty; }
    public int getBonus() { return bonus; }
    public int getMalus() { return malus; }

    public boolean isSustenanceEvent() {
        return "FARMER".equals(name);
    }

    /**
     * Verifies if the event matches a given job (used by buildings that
     * react only to the event of their type, e.g. Hunting or Cave Paintings).
     *
     * @param jobType the job to compare
     * @return true if the event name matches the job
     */
    public boolean matchesJobType(JobType jobType) {
        return jobType != null && jobType.name().equals(this.name);
    }

    /**
     * Resolves the event applying its effects on the entire game. The effects
     * internally iterate over all players, so here the Player is {@code null};
     * the card passes itself both as the owner card and as the current event.
     *
     * @param g the game on which to resolve the event
     */
    public void resolveEvent(Game g) {
        for (IEffect<EventCard> effect : effects) {
            // Call the new hook.
            // We pass 'null' for the Player because classes like EventHunter or EventShaman
            // already iterate over all players inside them!
            // We pass 'this' both as the effect's owning card and as the current event.
            effect.applyOnEvent(null, g, this, this);
        }
    }

    @Override
    public String toClientString() {
        return getImageId() + "|" + getEra() + "|0|EVENT|" + getName();
    }

    /**
     * Reveals the event by inserting it into the Events row of the board.
     *
     * @param board the game board
     * @param row   the target row
     */
    @Override
    public void reveal(Board board, String row) {
        // The card says: "I am an Event, put me in the Events row"
        board.addEventCard(this, row);
    }

    public boolean isFinalEvent() {
        return finalEvent;
    }

    public void setName(String name) {
        this.name = name;
    }
}