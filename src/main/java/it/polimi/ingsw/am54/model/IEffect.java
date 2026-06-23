package it.polimi.ingsw.am54.model;

public interface IEffect<T extends Card> {

    // 1. PICK PHASE: Called instantly when the card is bought/picked
    default void applyOnPurchase(Player p, Game g, T card) {
        // By default does nothing
    }

    // 2. TURN END PHASE: Called when the single player returns the totem
    // and takes the bonus from the Turn Order Track
    default void applyOnTurnEnd(Player p, Game g, T card) {
        // By default does nothing
    }

    // 3. EVENTS PHASE: Called right before resolving a global Event card
    default void applyOnEvent(Player p, Game g, T card, EventCard currentEvent) {
        // By default does nothing
    }

    // 4. END GAME PHASE: Called at the end of the game for the victory points calculation
    default void applyOnEndGame(Player p, Game g, T card) {
        // By default does nothing
    }

    // 5. PASSIVE PHASE: Called every time a Character is added to the tribe
    default void applyOnCharacterPicked(Player p, Game g, T card, CharacterCard newCharacter) {
        // By default does nothing
    }
}