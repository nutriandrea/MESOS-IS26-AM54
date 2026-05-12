package it.polimi.ingsw.am54.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.util.*;

public class Board {

    private static final ObjectMapper mapper = new ObjectMapper();

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

    // Sostituisce la vecchia generateBoard. Ora la Board riceve solo il numero di giocatori (Dati, non Logica).
    public void initializeLayout(int numPlayers) {
        initializeOfferingTiles(numPlayers);
        initializeTurnOrderTile(numPlayers);
    }

    // Modificato: non prende più "Game", ma solo "numPlayers"
    private void initializeOfferingTiles(int numPlayers) {
        offeringTrack.clear();

        if (numPlayers < 2 || numPlayers > 5) {
            throw new IllegalArgumentException("Errore critico: Numero di giocatori non valido (" + numPlayers + ")");
        }

        List<Character> neededTiles = new ArrayList<>();

        try {
            JsonNode rootNode = mapper.readTree(new File("data/OfferingTilesSetup.json"));
            String playerKey = String.valueOf(numPlayers);
            JsonNode arrayTessere = rootNode.get("requiredTiles").get(playerKey);

            if (arrayTessere != null && arrayTessere.isArray()) {
                for (JsonNode nodoLettera : arrayTessere) {
                    neededTiles.add(nodoLettera.asText().charAt(0));
                }
            }

            JsonNode arrayTiles = rootNode.get("tiles");

            if (arrayTiles != null && arrayTiles.isArray()) {
                for (JsonNode node : arrayTiles) {
                    char idLetto = node.get("id").asText().charAt(0);
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
            throw new RuntimeException("Fatal error: Could not initialize Offering Tiles from JSON configuration.", e);
        }

        offeringTrack.sort(Comparator.comparing(OfferingTile::getId));
    }

    // Aggiungi questo metodo per permettere al Game di inserire gli Edifici in modo pulito
    public void addBuildingCard(BuildingCard card, String row) {
        if (row.equalsIgnoreCase("upper")) {
            buildingCardsUpperRow.add(card);
        } else {
            buildingCardsLowerRow.add(card);
        }
    }

    // Aggiungi questo metodo di utilità per contare gli eventi presenti in una fila (serve al Game per il Setup)
    public int getEventRowSize(String row) {
        return row.equalsIgnoreCase("upper") ? eventUpperRow.size() : eventLowerRow.size();
    }


    public void initializeTurnOrderTile(int numPlayers) {
        // Svuota la lista se c'era già qualcosa
        this.turnOrderTile.clear();

        // controlli di sicurezza
        if (numPlayers < 2 || numPlayers > 5) {
            throw new IllegalArgumentException("Errore critico: Numero di giocatori non valido (" + numPlayers + ")");
        }

        try {

            //legge il file
            JsonNode rootNode = mapper.readTree(new File("data/TurnOrderConfig.json"));
            JsonNode bonusArray = rootNode.get("bonusFood").get(String.valueOf(numPlayers));

            // se l'array esiste, crea gli slot
            if (bonusArray != null && bonusArray.isArray()) {

                for (int i = 0; i < bonusArray.size(); i++) {
                    int position = i + 1; // la posizione "umana" parte da 1
                    int bonus = bonusArray.get(i).asInt(); // Il cibo da dare (-1, 0, 1, 2...)

                    // fabbrica lo slot passandogli i dati già pronti
                    TurnOrderSlot slot = new TurnOrderSlot(position, bonus);
                    this.turnOrderTile.add(slot);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Fatal error during Turn Order initialization", e);
        }
    }


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

    public void setupInitialTurnOrder(List<Player> randomPlayers) {
        for (int i = 0; i < randomPlayers.size(); i++) {
            Player p = randomPlayers.get(i);
            TurnOrderSlot slot = this.turnOrderTile.get(i);

            // Piazzo il giocatore nello slot
            slot.occupy(p);

        }
    }

    public void moveUpperToLower() {
        // facciamo scivolare giù tutti i personaggi rimasti
        characterLowerRow.addAll(characterUpperRow);
        characterUpperRow.clear();

        // facciamo scivolare giù tutti gli eventi rimasti
        eventLowerRow.addAll(eventUpperRow);
        eventUpperRow.clear();
    }

    public void moveBuildings() {
        buildingCardsLowerRow.clear();
        buildingCardsLowerRow.addAll(buildingCardsUpperRow);
        buildingCardsUpperRow.clear();
    }

    public void refillUpperRow(Game game) {
        int numPlayers = game.getPlayersNumber();
        int targetSize = numPlayers + 4;

        Deck<TribeCard> tribeDeck = game.getTribeDeck();

        // Il ciclo controlla la somma di personaggi ed eventi presenti nella riga in alto
        // Finché non arriviamo a numPlayers + 4, continuiamo a pescare
        while ((characterUpperRow.size() + eventUpperRow.size()) < targetSize) {

            TribeCard card = tribeDeck.drawCard();

            // Sicurezza: se il mazzo è finito, ci fermiamo
            if (card == null) { break; }

            // la carta si rivela e andrà in automatico in characterUpperR oppure in eventUpperRow.
            // In entrambi i casi, la somma totale salirà di 1
            card.reveal(this, "upper");
        }
    }

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

        // Restituiamo la lista intera
        return combinedRow;
    }


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


    // Ordina eventi: sostentamento ultimo
    public EventCard popNextEvent() {
        if (eventLowerRow.isEmpty()) {
            return null; // Non ci sono più eventi
        }

        // Troviamo l'evento da risolvere in base alle regole:
        // Sostentamento alla fine, a parità di quello l'Era più bassa
        EventCard nextEvent = eventLowerRow.stream()
                .min((a, b) -> {
                    if (a.shouldResolveLast() != b.shouldResolveLast()) {
                        return a.shouldResolveLast() ? 1 : -1;
                    }
                    return Integer.compare(a.getEra(), b.getEra());
                })
                .orElse(null);

        if (nextEvent != null) {
            eventLowerRow.remove(nextEvent); // Rimuove fisicamente la carta dal tavolo
        }

        return nextEvent;
    }


    public EventCard popNextVisibleEvent() {
        // Prima cerchiamo nella fila inferiore (regola standard)
        EventCard next = popNextEvent(); // Il metodo creato in precedenza per la fila inferiore

        // Se la fila inferiore è vuota, cerchiamo in quella superiore
        if (next == null && !eventUpperRow.isEmpty()) {
            next = eventUpperRow.stream()
                    .min((a, b) -> {
                        // Prima priorità: Eventi Sostentamento (vanno in fondo)
                        if (a.shouldResolveLast() != b.shouldResolveLast()) {
                            return a.shouldResolveLast() ? 1 : -1;
                        }
                        // Seconda priorità: Ordine crescente di Era[cite: 2]
                        return Integer.compare(a.getEra(), b.getEra());
                    })
                    .orElse(null);

            // Se abbiamo trovato un evento, rimuoviamolo fisicamente dal tavolo
            if (next != null) {
                eventUpperRow.remove(next);
            }
        }

        return next;
    }

    /**
     * Controlla se tutti i giocatori attivi hanno piazzato il loro totem.
     * @param expectedCount numero di giocatori che devono piazzare (attivi, non totali)
     */
    public boolean allTotemsPlaced(int expectedCount) {
        int placedCount = 0;
        for (OfferingTile tile : offeringTrack) {
            if (!tile.isAvailable()) {
                placedCount++;
            }
        }
        return placedCount == expectedCount;
    }

    // Trova il giocatore più a sinistra sul tracciato delle offerte
    public Player getFirstPlayerFromOfferingTrack() {
        for (OfferingTile tile : offeringTrack) {
            if (!tile.isAvailable()) {
                return tile.getOccupiedBy();
            }
        }
        return null;
    }

    // Regola specifica per l'ingresso nell'Era III
    public void discardLowerRowBuildings() {
        buildingCardsLowerRow.clear();
    }

    // PULISCE E BASTA (solo caratteri, eventi gia' rimossi)
    public void clearLowerRow() {
        characterLowerRow.clear();
    }


    public List<OfferingTile> getOfferingTrack() { return offeringTrack; }
    public List<TurnOrderSlot> getTurnOrderTile() { return turnOrderTile;}
    public List<BuildingCard> getBuildingCardsUpperRow() { return buildingCardsUpperRow; }
    public List<BuildingCard> getBuildingCardsLowerRow() { return buildingCardsLowerRow; }

    // Getter per le liste di carte personaggio ed eventi
    public List<CharacterCard> getCharacterUpperRowList() { return characterUpperRow; }
    public List<CharacterCard> getCharacterLowerRowList() { return characterLowerRow; }
    public List<EventCard> getEventUpperRowList() { return eventUpperRow; }
    public List<EventCard> getEventLowerRowList() { return eventLowerRow; }

    // Original getters
    public List<CharacterCard> getCharacterUpperRow() { return characterUpperRow; }
    public List<CharacterCard> getCharacterLowerRow() { return characterLowerRow; }
    public List<EventCard> getEventUpperRow() { return eventUpperRow; }
    public List<EventCard> getEventLowerRow() { return eventLowerRow; }
}

