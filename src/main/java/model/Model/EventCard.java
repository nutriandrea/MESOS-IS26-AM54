package model.Model;

public abstract class EventCard extends TribeCard { // Deve essere abstract per le sottoclassi [cite: 116]

    public EventCard(int era) {
        super(era);
    }

    public abstract void resolveEvent(Game g);
}