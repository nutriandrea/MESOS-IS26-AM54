package it.polimi.ingsw.am54.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.polimi.ingsw.am54.model.effects.buildings.*;
import it.polimi.ingsw.am54.model.effects.characters.*;
import it.polimi.ingsw.am54.model.effects.events.*;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;


/**
 * Game decks factory. Reads the cards from the configuration JSON files, filters/shuffles
 * them by Era according to the number of players and associates each with its own effect.
 * The lowest Era cards end up at the top of the deck (drawn first).
 */
public class DeckFactory {

    /**
     * Creates the Buildings deck for the specified number of players: loads the cards
     * from the JSON, groups them by Era, shuffles them and stacks the expected amount from
     * {@code EraCounts.json} (Era 1 at the top, Era 3 at the bottom).
     *
     * @param filePath   path of the buildings JSON
     * @param numPlayers number of players (2-5)
     * @return the ready-to-use Buildings deck
     * @throws IllegalArgumentException if the number of players is not valid
     */
    public static Deck<BuildingCard> createBuildingDeck(String filePath, int numPlayers) {

        if (numPlayers < 2 || numPlayers > 5) {
            throw new IllegalArgumentException("Impossibile creare il mazzo: numero di giocatori (" + numPlayers + ") non valido!");
        }

        int countE1 = 0, countE2 = 0, countE3 = 0;

        // read the quantities setup from EraCounts.json
        try {
            InputStream is = DeckFactory.class.getResourceAsStream("/data/EraCounts.json");
            JsonObject countsRoot = JsonParser.parseReader(new InputStreamReader(is)).getAsJsonObject();
            JsonArray buildingCounts = countsRoot.getAsJsonObject("buildingCounts").getAsJsonArray(String.valueOf(numPlayers));

            if (buildingCounts != null) {
                countE1 = buildingCounts.get(0).getAsInt();
                countE2 = buildingCounts.get(1).getAsInt();
                countE3 = buildingCounts.get(2).getAsInt();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Errore critico durante la lettura del file EraCounts.json", e);
        }

        List<BuildingCard> e1 = new ArrayList<>();
        List<BuildingCard> e2 = new ArrayList<>();
        List<BuildingCard> e3 = new ArrayList<>();

        try {
            InputStream is = DeckFactory.class.getResourceAsStream(filePath);
            JsonArray root = JsonParser.parseReader(new InputStreamReader(is)).getAsJsonArray();

            for (JsonElement elem : root) {
                JsonObject node = elem.getAsJsonObject();
                JobType type = JobType.valueOf(node.get("JobType").getAsString());
                Phases phase = Phases.valueOf(node.get("phase").getAsString());
                int cost = node.get("cost").getAsInt();
                int finalAura = node.get("finalAura").getAsInt();
                String nomeLambda = node.get("nomeLambda").getAsString();
                int era = node.get("era").getAsInt();
                JsonElement paramsElem = node.get("effectParams");
                JsonObject params = paramsElem != null ? paramsElem.getAsJsonObject() : null;

                List<IEffect<BuildingCard>> effects = new ArrayList<>();
                IEffect<BuildingCard> effect = createBuildingEffect(node.get("phase").getAsString(), nomeLambda, params);
                if (effect != null) {
                    effects.add(effect);
                }

                BuildingCard card = new BuildingCard(type, cost, finalAura, phase, effects, era);

                card.setName(nomeLambda);

                if (node.has("imageId")) {
                    card.setImageId(node.get("imageId").getAsString());
                }

                if (era == 1) e1.add(card);
                else if (era == 2) e2.add(card);
                else if (era == 3) e3.add(card);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Errore critico durante la generazione delle carte dal file: " + filePath, e);
        }

        Collections.shuffle(e1);
        Collections.shuffle(e2);
        Collections.shuffle(e3);

        Stack<BuildingCard> finalDeck = new Stack<>();

        // Insert era 3 first (so it goes to the bottom of the deck)
        for (int i = 0; i < Math.min(countE3, e3.size()); i++) {
            finalDeck.push(e3.get(i));
        }
        // Then era 2
        for (int i = 0; i < Math.min(countE2, e2.size()); i++) {
            finalDeck.push(e2.get(i));
        }
        // Finally era 1 (so it remains at the top of the stack and is drawn first)
        for (int i = 0; i < Math.min(countE1, e1.size()); i++) {
            finalDeck.push(e1.get(i));
        }

        return new Deck<>(finalDeck);
    }

    /**
     * Creates the Tribe deck joining Characters and Events. Characters are filtered
     * by minimum number of players, shuffled and "cut" to the quantities per Era;
     * events are joined to the characters of the same Era and the set is reshuffled (so
     * it is not predictable when an event comes out). The final events go to the bottom of the deck.
     *
     * @param charPath   path of the characters JSON
     * @param eventPath  path of the events JSON
     * @param numPlayers number of players (2-5)
     * @return the ready-to-use Tribe deck
     * @throws IllegalArgumentException if the number of players is not valid
     */
    public static Deck<TribeCard> createTribeDeck(String charPath, String eventPath, int numPlayers) {

        if (numPlayers < 2 || numPlayers > 5) {
            throw new IllegalArgumentException("Impossibile creare il mazzo: numero di giocatori (" + numPlayers + ") non valido!");
        }

        int countE1 = 0, countE2 = 0, countE3 = 0;

        // Read the number of characters to draw per Era
        try {
            InputStream is = DeckFactory.class.getResourceAsStream("/data/EraCounts.json");
            JsonObject countsRoot = JsonParser.parseReader(new InputStreamReader(is)).getAsJsonObject();
            JsonObject charCounts = countsRoot.getAsJsonObject("characterCounts").getAsJsonObject(String.valueOf(numPlayers));

            if (charCounts != null) {
                countE1 = charCounts.get("era1").getAsInt();
                countE2 = charCounts.get("era2").getAsInt();
                countE3 = charCounts.get("era3").getAsInt();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Errore critico durante la lettura del file EraCounts.json", e);
        }

        // Separate lists for Characters and Events
        List<TribeCard> charE1 = new ArrayList<>();
        List<TribeCard> charE2 = new ArrayList<>();
        List<TribeCard> charE3 = new ArrayList<>();

        List<TribeCard> eventE1 = new ArrayList<>();
        List<TribeCard> eventE2 = new ArrayList<>();
        List<TribeCard> eventE3 = new ArrayList<>();
        List<TribeCard> finalEvents = new ArrayList<>();

        // Character loading (inserted in charE)
        try {
            InputStream is = DeckFactory.class.getResourceAsStream(charPath);
            JsonArray root = JsonParser.parseReader(new InputStreamReader(is)).getAsJsonArray();
            for (JsonElement elem : root) {
                JsonObject node = elem.getAsJsonObject();
                int minPlayers = node.get("minPlayers").getAsInt();
                if (numPlayers >= minPlayers) {
                    String characterType = node.get("characterType").getAsString();
                    JobType type = JobType.valueOf(characterType);
                    Phases phase = Phases.valueOf(node.get("phase").getAsString());
                    int level = node.get("level").getAsInt();
                    int aura = node.get("aura").getAsInt();
                    int era = node.get("era").getAsInt();

                    List<IEffect<CharacterCard>> effects = new ArrayList<>();
                    effects.add(createCharacterEffect(characterType));

                    CharacterCard card = new CharacterCard(level, type, aura, phase, effects, era);
                    card.setMinPlayers(minPlayers);
                    if (node.has("imageId")) {
                        card.setImageId(node.get("imageId").getAsString());
                    }
                    if (era == 1) charE1.add(card);
                    else if (era == 2) charE2.add(card);
                    else charE3.add(card);
                }
            }
        } catch (Exception e) {
            System.err.println("\n\n!!! VERO ERRORE CATTURATO !!!");
            e.printStackTrace(); // Questo forzerà la console a dirti ESATTAMENTE a che riga è esploso
            System.err.println("!!! FINE ERRORE !!!\n\n");
            throw new IllegalStateException("Errore critico durante la lettura del file dei Personaggi: " + charPath, e);
        }

        // Event loading (inserted in eventE)
        try {
            InputStream is = DeckFactory.class.getResourceAsStream(eventPath);
            JsonArray root = JsonParser.parseReader(new InputStreamReader(is)).getAsJsonArray();
            for (JsonElement elem : root) {
                JsonObject node = elem.getAsJsonObject();
                String etichetta = node.get("etichettaSottoclasse").getAsString();
                int era = node.get("era").getAsInt();
                boolean finalEvent = node.get("finalEvent").getAsBoolean();

                List<IEffect<EventCard>> effects = new ArrayList<>();
                effects.add(createEventEffect(etichetta));

                EventCard event = new EventCard(
                        era,
                        finalEvent,
                        effects,
                        node.has("baseFood") ? node.get("baseFood").getAsInt() : 0,
                        node.has("auraPerCharacter") ? node.get("auraPerCharacter").getAsInt() : 0,
                        node.has("threshold") ? node.get("threshold").getAsInt() : 0,
                        node.has("bonusMultiplier") ? node.get("bonusMultiplier").getAsInt() : 0,
                        node.has("penalty") ? node.get("penalty").getAsInt() : 0,
                        node.has("bonus") ? node.get("bonus").getAsInt() : 0,
                        node.has("malus") ? node.get("malus").getAsInt() : 0
                );

                event.setName(etichetta);

                if (node.has("imageId")) {
                    event.setImageId(node.get("imageId").getAsString());
                }

                if (finalEvent) {
                    finalEvents.add(event);
                } else {
                    if (era == 1) eventE1.add(event);
                    else if (era == 2) eventE2.add(event);
                    else eventE3.add(event);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Errore critico durante la lettura del file degli Eventi: " + eventPath, e);
        }

        // we shuffle only the characters and "cut" them based on the JSON number
        Collections.shuffle(charE1);
        Collections.shuffle(charE2);
        Collections.shuffle(charE3);

        List<TribeCard> era1 = new ArrayList<>(charE1.subList(0, Math.min(countE1, charE1.size())));
        List<TribeCard> era2 = new ArrayList<>(charE2.subList(0, Math.min(countE2, charE2.size())));
        List<TribeCard> era3 = new ArrayList<>(charE3.subList(0, Math.min(countE3, charE3.size())));

        // join all Events to the newly selected characters
        era1.addAll(eventE1);
        era2.addAll(eventE2);
        era3.addAll(eventE3);

        // we shuffle the complete mini-deck for each Era (so you never know when the event comes out)
        Collections.shuffle(era1);
        Collections.shuffle(era2);
        Collections.shuffle(era3);
        Collections.shuffle(finalEvents); // shuffle the finals as well

        // let's stack everything in the final Stack (LIFO)
        Stack<TribeCard> finalDeck = new Stack<>();
        for (TribeCard c : finalEvents) finalDeck.push(c);
        for (TribeCard c : era3) finalDeck.push(c);
        for (TribeCard c : era2) finalDeck.push(c);
        for (TribeCard c : era1) finalDeck.push(c);

        // return the final and complete Stack
        return new Deck<>(finalDeck);
    }

    private static final Map<String, Function<JsonObject, IEffect<BuildingCard>>> BUILDING_EFFECTS = Map.ofEntries(
            Map.entry("SHAMAN_PASSIVE", BuildingShamanPassive::new),
            Map.entry("EVENT_COST_MODIFIER", BuildingEventCostModifier::new),
            Map.entry("JOB_AURA_BONUS", BuildingJobAuraBonus::new),
            Map.entry("IMMEDIATE_RESOURCE", BuildingImmediateResource::new),
            Map.entry("EXTRA_PICK", BuildingExtraPick::new),
            Map.entry("ENDGAME_AURA", BuildingEndgameAura::new),
            Map.entry("BONUS_FOOD", p -> new BuildingTurnOrderBonus()),
            Map.entry("SIX_DIFFERENT_AURA", p -> new BuildingSixDifferentAura()),
            Map.entry("SIX_OF_TYPE", p -> new BuildingSixOfType()),
            Map.entry("DOUBLE_INVENTOR", p -> new BuildingDoubleInventor())
    );

    // creates the right effect for the buildings
    private static IEffect<BuildingCard> createBuildingEffect(String phase, String nomeLambda, JsonObject params) {
        if (nomeLambda == null || nomeLambda.isEmpty() || nomeLambda.equals("null")) return null;
        return BUILDING_EFFECTS.getOrDefault(nomeLambda, p -> {
            throw new IllegalArgumentException("Effetto edificio non gestito dal JSON: " + nomeLambda);
        }).apply(params);
    }

    private static final Map<String, Supplier<IEffect<CharacterCard>>> CHARACTER_EFFECTS = Map.of(
            "HUNTER", CharacterHunter::new,
            "CONSTRUCTOR", CharacterConstructor::new,
            "SHAMAN", CharacterShaman::new,
            "ARTIST", CharacterPassive::new,
            "INVENTOR", CharacterPassive::new,
            "FARMER", CharacterPassive::new
    );

    private static IEffect<CharacterCard> createCharacterEffect(String effectName) {
        return CHARACTER_EFFECTS.getOrDefault(effectName, () -> {
            throw new IllegalArgumentException("Effetto personaggio sconosciuto: " + effectName);
        }).get();
    }

    private static final Map<String, Supplier<IEffect<EventCard>>> EVENT_EFFECTS = Map.of(
            "HUNTER", EventHunter::new,
            "FARMER", EventFarmer::new,
            "ARTIST", EventArtist::new,
            "SHAMAN", EventShaman::new
    );

    private static IEffect<EventCard> createEventEffect(String effectName) {
        return EVENT_EFFECTS.getOrDefault(effectName, () -> {
            throw new IllegalArgumentException("Effetto evento sconosciuto: " + effectName);
        }).get();
    }
}