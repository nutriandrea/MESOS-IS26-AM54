package model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.util.*;

public class Board {

    private List<CharacterCard> characterUpperRow;
    private List<CharacterCard> characterLowerRow;
    private List<EventCard> eventUpperRow;
    private List<EventCard> eventLowerRow;
    private List<BuildingCard> buildingCardsUpperRow;
    private List<BuildingCard> buildingCardsLowerRow;

    private List<OfferingTile> offeringTrack;
    private List<TurnOrderSlot> turnOrderTile;

    public Board() {
        this.characterUpperRow = new ArrayList<>();
        this.characterLowerRow = new ArrayList<>();
        this.eventUpperRow = new ArrayList<>();
        this.eventLowerRow = new ArrayList<>();
        this.buildingCardsUpperRow = new ArrayList<>();
        this.buildingCardsLowerRow = new ArrayList<>();
        this.offeringTrack = new ArrayList<>();
        this.turnOrderTile = new ArrayList<>();
    }

    public void generateBoard(Game game) throws Exception {
        int numPlayers = game.getPlayersNumber();
        initializeOfferingTiles(game);
        initializeTurnOrderTile(numPlayers);

        populateLowerRow(game);
        populateUpperRow(game);
        populateUpperRowBuildings(game);
    }

    private void initializeOfferingTiles(Game game) {
        offeringTrack.clear();
        int numPlayers = game.getPlayersNumber();

        // Controllo di sicurezza
        if (numPlayers < 2 || numPlayers > 5) return;

        List<Character> neededTiles = new ArrayList<>();

        try {
            ObjectMapper mapper = new ObjectMapper();

            // 1. Leggiamo tutto il file UNA SOLA VOLTA nell'albero principale
            JsonNode rootNode = mapper.readTree(new File("data/OfferingTilesSetup.json"));

            // 2. Peschiamo l'array delle lettere necessarie ("A", "B", "C"...)
            String playerKey = String.valueOf(numPlayers);
            JsonNode arrayTessere = rootNode.get("requiredTiles").get(playerKey);

            if (arrayTessere != null && arrayTessere.isArray()) {
                for (JsonNode nodoLettera : arrayTessere) {
                    neededTiles.add(nodoLettera.asText().charAt(0));
                }
            }

            // 3. Ora peschiamo l'array con le STATISTICHE delle tessere!
            JsonNode arrayTiles = rootNode.get("tiles"); //

            if (arrayTiles != null && arrayTiles.isArray()) {
                // Cicliamo SOLO dentro le tessere
                for (JsonNode node : arrayTiles) {
                    char idLetto = node.get("id").asText().charAt(0);

                    // Se la tessera corrente ci serve per questa partita, la costruiamo
                    if (neededTiles.contains(idLetto)) {
                        int upper = node.get("cardsFromUpper").asInt();
                        int lower = node.get("cardsFromLower").asInt();
                        int food  = node.get("foodBonus").asInt();

                        OfferingTile tile = new OfferingTile(idLetto, upper, lower, food);
                        offeringTrack.add(tile);
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Errore lettura JSON Offering Tiles: " + e.getMessage());
            e.printStackTrace(); // Ti aiuta a capire subito la riga esatta se qualcosa esplode
        }

        // 4. Ordine alfabetico finale per sicurezza
        offeringTrack.sort(Comparator.comparing(OfferingTile::getId));
    }

    public void initializeTurnOrderTile(int numPlayers) {
        // Svuota la lista se c'era già qualcosa
        this.turnOrderTile.clear();

        // Controllo di sicurezza
        if (numPlayers < 2 || numPlayers > 5) return;

        try {
            ObjectMapper mapper = new ObjectMapper();

            // 1. Legge il file UNA SOLA VOLTA
            JsonNode rootNode = mapper.readTree(new File("data/TurnOrderConfig.json"));
            JsonNode bonusArray = rootNode.get("bonusFood").get(String.valueOf(numPlayers));

            // 2. Se l'array esiste, crea gli slot
            if (bonusArray != null && bonusArray.isArray()) {

                // Se sono 4 giocatori, il ciclo gira 4 volte
                for (int i = 0; i < bonusArray.size(); i++) {
                    int position = i + 1; // La posizione "umana" parte da 1
                    int bonus = bonusArray.get(i).asInt(); // Il cibo da dare (-1, 0, 1, 2...)

                    // 3. Fabbrica lo slot passandogli i dati già pronti
                    TurnOrderSlot slot = new TurnOrderSlot(position, bonus);
                    this.turnOrderTile.add(slot);
                }
            }
        } catch (Exception e) {
            System.err.println("Errore lettura JSON TurnOrderConfig: " + e.getMessage());
        }
    }

    // La Board offre degli "sportelli" specifici, senza fare domande
    public void addCharacterCard(CharacterCard card, String row) {
        if (row.equalsIgnoreCase("upper")) {
            characterUpperRow.add(card);
        } else {
            characterLowerRow.add(card);
        }
    }

    public void addEventCard(EventCard card, String row) {
            eventUpperRow.add(card);
    }


    public void populateLowerRow(Game game) throws Exception {
        int numPlayers = game.getPlayersNumber();
        int requiredCharacters = numPlayers + 1;

        Deck<TribeCard> tribeDeck = game.getTribeDeck();

        while (characterLowerRow.size() < requiredCharacters) {

            TribeCard card = tribeDeck.drawCard();

            if (card == null) {
                throw new Exception("Tribe deck is empty!");
            }

            card.reveal(this, "lower");
        }
    }

    public void populateUpperRow(Game game) throws Exception {
        int numPlayers = game.getPlayersNumber();
        int count = numPlayers + 4;

        Deck<TribeCard> tribeDeck = game.getTribeDeck();

        while (characterUpperRow.size() + eventUpperRow.size() < count) {
            TribeCard card = tribeDeck.drawCard();

            if (card == null) {
                throw new Exception("Tribe deck is empty!");
            }

            card.reveal(this, "upper");
        }
    }

    public void populateUpperRowBuildings(Game game) throws Exception {
        int numPlayers = game.getPlayersNumber();
        int currentEra = game.getCurrentEra();

        if (numPlayers < 2 || numPlayers > 5) {
            throw new Exception("Invalid player count!");
        }

        int[][] eraCounts = {
                {},
                {},
                {1, 2, 3},
                {2, 2, 4},
                {2, 3, 4},
                {2, 3, 5}
        };

        if (currentEra < 1 || currentEra > 3) {
            throw new Exception("Invalid era!");
        }

        int cardsToDraw = eraCounts[numPlayers][currentEra - 1];
        if (cardsToDraw <= 0) {
            throw new Exception("Invalid cards to draw count!");
        }

        Deck<BuildingCard> buildingDeck = game.getBuildingDeck();

        for (int i = 0; i < cardsToDraw; i++) {
            BuildingCard card = buildingDeck.drawCard();
            if (card != null) {
                buildingCardsUpperRow.add(card);
            }
        }
    }


    public void setupInitialTurnOrder(List<Player> randomPlayers) {
        for (int i = 0; i < randomPlayers.size(); i++) {
            Player p = randomPlayers.get(i);
            TurnOrderSlot slot = this.turnOrderTile.get(i);

            // 1. Piazza il giocatore nello slot (usa il metodo che hai in TurnOrderSlot)
            slot.occupy(p);

        }
    }

    public void moveUpperToLower() {
        // 1. Facciamo scivolare giù tutti i Personaggi rimasti
        characterLowerRow.addAll(characterUpperRow);
        characterUpperRow.clear();

        // 2. Facciamo scivolare giù tutti gli Eventi rimasti
        eventLowerRow.addAll(eventUpperRow);
        eventUpperRow.clear();
    }

    public void moveBuildings() {
        buildingCardsLowerRow.clear();
        buildingCardsLowerRow.addAll(buildingCardsUpperRow);
        buildingCardsUpperRow.clear();
    }

    public void refillUpperRow(Game game) throws Exception {
        int numPlayers = game.getPlayersNumber();
        int targetSize = numPlayers + 4;

        Deck<TribeCard> tribeDeck = game.getTribeDeck();

        while ((characterUpperRow.size() + eventUpperRow.size()) < targetSize) {

            TribeCard card = tribeDeck.drawCard();

            if (card == null) {
                throw new Exception("Tribe deck is empty!");
            }

            card.reveal(this, "upper");
        }
    }


    /**
     * Ottiene la fila specificata
     */
    public List<Card> getRow(String row) {
        // Creiamo una nuova lista vuota che conterrà le carte unite
        List<Card> combinedRow = new ArrayList<>();

        if (row.equalsIgnoreCase("upper")) {
            // Uniamo i Personaggi e gli Eventi della riga in alto
            combinedRow.addAll(characterUpperRow);
            combinedRow.addAll(eventUpperRow);
        } else if (row.equalsIgnoreCase("lower")) {
            // Uniamo i Personaggi e gli Eventi della riga in basso
            combinedRow.addAll(characterLowerRow);
            combinedRow.addAll(eventLowerRow);
        }

        // Restituiamo la lista fusa!
        return combinedRow;
    }

    /**
     * Piazza un Totem su una tessera Offerta
     */
    public void addTotemToPosition(Player player, OfferingTile tile) {
            tile.occupy(player);
    }

    public CharacterCard takeCharacterCard(String row, int index) {
        List<CharacterCard> targetRow = row.equalsIgnoreCase("upper") 
            ? characterUpperRow 
            : characterLowerRow;
        
        if (index < 0 || index >= targetRow.size()) {
            return null;
        }
        
        return targetRow.remove(index);
    }

    public BuildingCard takeBuildingCard(String row, int index) {
        List<BuildingCard> targetRow = row.equalsIgnoreCase("upper") 
            ? buildingCardsUpperRow 
            : buildingCardsLowerRow;
        
        if (index < 0 || index >= targetRow.size()) {
            return null;
        }
        
        return targetRow.remove(index);
    }

    public int getCharacterRowSize(String row) {
        return row.equalsIgnoreCase("upper") 
            ? characterUpperRow.size() 
            : characterLowerRow.size();
    }

    public int getBuildingRowSize(String row) {
        return row.equalsIgnoreCase("upper") 
            ? buildingCardsUpperRow.size() 
            : buildingCardsLowerRow.size();
    }


// Ordina eventi: non-Farmer prima, Farmer dopo. Poi risolve e rimuove.
public void resolveAllEventsInOrder(Game game) {
    eventLowerRow.sort((a, b) -> {
        if (a.shouldResolveLast() == b.shouldResolveLast()) return 0;
        return a.shouldResolveLast() ? 1 : -1;
    });
    while (!eventLowerRow.isEmpty()) {
        eventLowerRow.get(0).resolveEvent(game);
        eventLowerRow.remove(0);
    }
}

// Risolvi TUTTI gli eventi visibili (upper + lower), Farmer per ultimo
public void resolveAllVisibleEvents(Game game) {
    List<EventCard> allEvents = new ArrayList<>();
    allEvents.addAll(eventUpperRow);
    allEvents.addAll(eventLowerRow);
    eventUpperRow.clear();
    eventLowerRow.clear();

    allEvents.sort((a, b) -> {
        if (a.shouldResolveLast() == b.shouldResolveLast()) return 0;
        return a.shouldResolveLast() ? 1 : -1;
    });

    allEvents.forEach(e -> e.resolveEvent(game));
}

// PULISCE E BASTA (solo caratteri, eventi gia' rimossi)
public void clearLowerRow() {
    characterLowerRow.clear();
}


public List<OfferingTile> getOfferingTrack() { return offeringTrack; }
public List<TurnOrderSlot> getTurnOrderTile() { return turnOrderTile;}
public List<BuildingCard> getBuildingCardsUpperRow() { return buildingCardsUpperRow; }
public List<BuildingCard> getBuildingCardsLowerRow() { return buildingCardsLowerRow; }

}
