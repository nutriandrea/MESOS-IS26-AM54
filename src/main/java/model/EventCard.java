package model;

import java.util.function.Consumer;

public class EventCard extends TribeCard {

    private boolean finalEvent;
    private Consumer<Game> effect;

    public int baseFood;
    public int auraPerCharacter;
    public int threshold;
    public int bonusMultiplier;
    public int penalty;
    public int bonus;
    public int malus;

    public EventCard(int era, boolean finalEvent) {
        super(era);
        this.finalEvent = finalEvent;
    }

    public void setEffect(Consumer<Game> effect) {
        this.effect = effect;
    }

    public void resolveEvent(Game g) {
        effect.accept(g);
    }

    public boolean isFinalEvent() {
        return finalEvent;
    }
}