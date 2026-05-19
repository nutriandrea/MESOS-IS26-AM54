package it.polimi.ingsw.am54.model;

import java.util.ArrayList;

public class Player {

    private String nickname;
    private TotemColors totemColor;

    // Risorse base
    private int food;
    private int aura;
    private int magicPoints;

    // Strutture dati
    private ArrayList<CharacterCard> myTribe;
    private ArrayList<BuildingCard> myBuildings;

    // Sconti permanenti (Acquisto carte)
    private int myBuildingDiscount;
    private int myFoodDiscount;

    // Modificatori temporanei (Fase Eventi)
    private int eventCostFood;
    private int eventCostAura;
    private boolean shamanNoLoss;
    private boolean shamanDoubleAura;

    // Flag di stato
    private boolean extraPick;
    private boolean hasPlacedTotem;
    private OfferingTile currentTile;

    /**
     * Costruttore di default
     */
    public Player() {
        this.food = 0;
        this.aura = 0;
        this.magicPoints = 0;
        this.myTribe = new ArrayList<>();
        this.myBuildings = new ArrayList<>();
        this.hasPlacedTotem = false;
        this.shamanNoLoss = false;
        this.shamanDoubleAura = false;
    }

    // --- GETTER E SETTER BASE ---

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public TotemColors getTotemColors() { return totemColor; }
    public void setTotemColor(TotemColors color) { this.totemColor = color; }

    public int getFood() { return food; }
    public int getFoodTokens() { return food; } // Alias mantenuto per compatibilità
    public int getAuraPoints() { return aura; }
    public int getMagicPoints() { return magicPoints; }

    public ArrayList<CharacterCard> getMyTribe() { return myTribe; }
    public ArrayList<BuildingCard> getBuildings() { return myBuildings; }

    public OfferingTile getCurrentTile() { return currentTile; }
    public void setCurrentTile(OfferingTile tile) { this.currentTile = tile; }

    public boolean hasPlacedTotem() { return hasPlacedTotem; }
    public void setHasPlacedTotem(boolean value) { this.hasPlacedTotem = value; }

    public boolean hasExtraPick() { return extraPick; }
    public void setExtraPick(boolean extraPick) { this.extraPick = extraPick; }

    // --- MODIFICATORI DELLE RISORSE ---

    public void modifyFood(int amount) {
        if (amount > 0) {
            this.food += amount;
        } else {
            this.food = Math.max(0, this.food - (-amount));
        }
    }

    public void modifyAuraPoints(int amount) {
        this.aura += amount;
    }

    public void modifyMagicPoints(int amount) {
        this.magicPoints += amount;
    }

    public void modifyMyTribe(CharacterCard c) {
        myTribe.add(c);
    }

    // --- SCONTI PERMANENTI ---

    public int getMyBuildingDiscount() { return myBuildingDiscount; }
    public void modifyMyBuildingDiscount(int amount) { this.myBuildingDiscount += amount; }

    public int getMyFoodDiscount() { return myFoodDiscount; }
    public void modifyMyFoodDiscount(int amount) { this.myFoodDiscount += amount; }

    // --- MODIFICATORI TEMPORANEI (EVENTI) ---

    public int getEventCostFood() { return eventCostFood; }
    public void setEventCostFood(int eventCost) { this.eventCostFood = eventCost; }

    public int getEventCostAura() { return eventCostAura; }
    public void setEventCostAura(int eventCost) { this.eventCostAura = eventCost; }

    public boolean hasShamanNoLoss() { return shamanNoLoss; }
    public void setShamanNoLoss(boolean shamanNoLoss) { this.shamanNoLoss = shamanNoLoss; }

    public boolean hasShamanDoubleAura() { return shamanDoubleAura; }
    public void setShamanDoubleAura(boolean shamanDoubleAura) { this.shamanDoubleAura = shamanDoubleAura; }

    /**
     * Resetta a zero tutti i modificatori temporanei della fase eventi.
     * Va chiamato alla fine della risoluzione di ogni singolo Evento.
     */
    public void resetEventAttributes() {
        this.eventCostAura = 0;
        this.eventCostFood = 0;
        this.shamanNoLoss = false;
        this.shamanDoubleAura = false;
    }
}