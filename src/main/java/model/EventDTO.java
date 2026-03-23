package model;

public class EventDTO {
    public boolean finalEvent;
    public String etichettaSottoclasse; // HUNTER, FARMER, FARMER, SHAMAN
    public int era;

    // per HUNTER e FARMER
    public int baseFood;          // quantità di cibo da dare (HUNTER)
    public int auraPerCharacter;  // moltiplicatore di aura

    // per FARMER
    public int threshold;         // soglia per penalità
    public int bonusMultiplier;   // moltiplicatore aura se sopra soglia
    public int penalty;           // penalità se sotto soglia

    // per SHAMAN
    public int bonus;             // bonus per player con più aura
    public int malus;             // malus per player con meno aura
}