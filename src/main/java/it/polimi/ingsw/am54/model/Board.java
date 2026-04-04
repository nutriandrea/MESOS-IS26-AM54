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

    public void generateBoard(Game game) {
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

        // controlli di sicurezza
        if (numPlayers < 2 || numPlayers > 5) {
            throw new IllegalArgumentException("Errore critico: Numero di giocatori non valido (" + numPlayers + ")");
        }

        List<Character> neededTiles = new ArrayList<>();

        try {

            //leggiamo il file
            JsonNode rootNode = mapper.readTree(new File("data/OfferingTilesSetup.json"));

            // prendiamo solo le lettere necessarie (A,B,C,...)
            String playerKey = String.valueOf(numPlayers);
            JsonNode arrayTessere = rootNode.get("requiredTiles").get(playerKey);

            if (arrayTessere != null && arrayTessere.isArray()) {
                for (JsonNode nodoLettera : arrayTessere) {
                    neededTiles.add(nodoLettera.asText().charAt(0));
                }
            }

            // prendiamo dalll'array le statistiche delle tessere
            JsonNode arrayTiles = rootNode.get("tiles");

            if (arrayTiles != null && arrayTiles.isArray()) {

                for (JsonNode node : arrayTiles) { //per ogni tessera
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
            throw new RuntimeException("Fatal error: Could not initialize Offering Tiles from JSON configuration.", e);
        }

        // ordine alfabetico finale per sicurezza
        offeringTrack.sort(Comparator.comparing(OfferingTile::getId));
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
            throw new RuntimeException("Fatal error during Offering Tiles initialization", e);
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


    public void populateLowerRow(Game game) {
        int numPlayers = game.getPlayersNumber();
        int requiredCharacters = numPlayers + 1;

        Deck<TribeCard> tribeDeck = game.getTribeDeck();

        // Continua a pescare finché la fila in basso non ha il numero giusto di Personaggi
        while (characterLowerRow.size() < requiredCharacters) {

            TribeCard card = tribeDeck.drawCard();


            // Diciamo alla carta: "Rivelati e vai nella fila in basso!"
            // - Se è un Personaggio, andrà in characterLowerRow.
            // - Se è un Evento, lo sportello della Board lo devierà in eventUpperRow!
            card.reveal(this, "lower");
        }
    }

    public void populateUpperRow(Game game) {
        int numPlayers = game.getPlayersNumber();
        int count = numPlayers + 4;

        Deck<TribeCard> tribeDeck = game.getTribeDeck();

        while (characterUpperRow.size() + eventUpperRow.size() < count) {
            TribeCard card = tribeDeck.drawCard();

            if (card != null) {
                // Se è un Personaggio andrà da solo in characterUpperRow.
                // Se è un Evento andrà da solo in eventUpperRow.
                card.reveal(this, "upper");
            }
        }
    }

    public void populateUpperRowBuildings(Game game) {
        int numPlayers = game.getPlayersNumber();
        int currentEra = game.getCurrentEra();

        // controlli di sicurezza
        if (numPlayers < 2 || numPlayers > 5) {
            throw new IllegalArgumentException("Errore critico: Numero di giocatori non valido (" + numPlayers + ")");
        }

        if (currentEra < 1 || currentEra > 3) {
            throw new IllegalArgumentException("Errore critico: Era non valida (" + currentEra + ")");
        }

        int cardsToDraw = 0;

        // leggo dal JSON
        try {
            JsonNode countsRoot = mapper.readTree(new File("data/EraCounts.json"));
            JsonNode buildingCounts = countsRoot.get("buildingCounts").get(String.valueOf(numPlayers));

            if (buildingCounts != null && buildingCounts.isArray()) { //solito controllo che quello che mi arriva sia come mi aspetto
                // currentEra parte da 1, ma gli array partono da 0. Quindi currentEra - 1
                cardsToDraw = buildingCounts.get(currentEra - 1).asInt();
            } else {
                throw new IllegalStateException("Nodo 'buildingCounts' per " + numPlayers + " giocatori mancante o non valido nel JSON.");
            }
        } catch (Exception e) {
            // Se il file non si trova o è scritto male, fermiamo tutto!
            throw new IllegalStateException("Impossibile leggere il file EraCounts.json: " + e.getMessage(), e);
        }

        // Controllo sulle carte da pescare
        if (cardsToDraw <= 0) {
            throw new IllegalStateException("Errore: Il JSON dice di pescare " + cardsToDraw + " carte. Deve essere > 0.");
        }

        // Pescaggio e riempimento
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
    public void resolveAllEventsInOrder(Game game) {
        eventLowerRow.sort((a, b) -> {
            if (a.shouldResolveLast() == b.shouldResolveLast()) return 0;
            return a.shouldResolveLast() ? 1 : -1;
        });
        while (!eventLowerRow.isEmpty()) {
            eventLowerRow.get(0).resolveEvent(game); //risolve evento
            eventLowerRow.remove(0); //lo rimuove
        }
    }

    // Risolviamo tutti gli eventi visibili (upper + lower), sostentamento per ultimo
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

