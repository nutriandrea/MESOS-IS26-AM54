package it.polimi.ingsw.am54.model;

public interface IEffect<T extends Card> {

    // 1. FASE PICK: Chiamato istantaneamente quando la carta viene comprata/pescata
    default void applyOnPurchase(Player p, Game g, T card) {
        // Di default non fa nulla
    }

    // 2. FASE CHIUSURA TURNO: Chiamato quando il singolo giocatore restituisce il totem
    // e prende il bonus dalla Turn Order Track
    default void applyOnTurnEnd(Player p, Game g, T card) {
        // Di default non fa nulla
    }

    // 3. FASE EVENTI: Chiamato un attimo prima di risolvere una carta Evento globale
    default void applyOnEvent(Player p, Game g, T card, EventCard currentEvent) {
        // Di default non fa nulla
    }

    // 4. FASE FINE PARTITA: Chiamato alla fine del gioco per il calcolo dei punti vittoria
    default void applyOnEndGame(Player p, Game g, T card) {
        // Di default non fa nulla
    }

    // 5. FASE PASSIVA: Chiamato ogni volta che un Personaggio viene aggiunto alla tribù
    default void applyOnCharacterPicked(Player p, Game g, T card, CharacterCard newCharacter) {
        // Di default non fa nulla
    }
}