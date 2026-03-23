package model;

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

    public int getBaseFood() {
        return baseFood;
    }
    public int getAuraPerCharacter() {
        return auraPerCharacter;
    }
    public int getThreshold() {
        return threshold;
    }
    public int getBonusMultiplier() {
        return bonusMultiplier;
    }
    public int getPenalty() {
        return penalty;
    }
    public int getBonus() {
        return bonus;
    }
    public int getMalus() {
        return malus;
    }

    public void resolveEvent(Game g) {
        for (IEffect<EventCard> effect : effects) {
            effect.apply(null, g, this); // il player può essere null se l'effetto riguarda tutti
        }
    }

    public boolean isFinalEvent() {
        return finalEvent;
    }

}