package it.polimi.ingsw.am54.model;

import java.util.List;

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
     * Restituisce il nome della carta evento.
     * Il nome è generato come "Event-" + era.
     * Utilizzato dal PersistenceManager per salvare le carte nei mazzi.
     *
     * @return nome della carta (es. "Event-1", "Event-2")
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

    public boolean matchesJobType(JobType jobType) {
        return jobType != null && jobType.name().equals(this.name);
    }

    // ==========================================
    // RISOLUZIONE DELL'EVENTO AGGIORNATA
    // ==========================================
    public void resolveEvent(Game g) {
        for (IEffect<EventCard> effect : effects) {
            // Chiamiamo il nuovo gancio.
            // Passiamo 'null' per il Player perché classi come EventHunter o EventShaman
            // fanno già un ciclo su tutti i giocatori al loro interno!
            // Passiamo 'this' sia come carta proprietaria dell'effetto, sia come evento corrente.
            effect.applyOnEvent(null, g, this, this);
        }
    }

    @Override
    public String toClientString() {
        return getImageId() + "|" + getEra() + "|0|EVENT|" + getName();
    }

    @Override
    public void reveal(Board board, String row) {
        // La carta dice: "sono un Evento, mettimi nella fila degli Eventi"
        board.addEventCard(this, row);
    }

    public boolean isFinalEvent() {
        return finalEvent;
    }

    public void setName(String name) {
        this.name = name;
    }
}