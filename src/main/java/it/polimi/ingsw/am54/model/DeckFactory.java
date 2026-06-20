package it.polimi.ingsw.am54.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.polimi.ingsw.am54.model.effects.buildings.*;
import it.polimi.ingsw.am54.model.effects.characters.*;
import it.polimi.ingsw.am54.model.effects.events.*;

import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;


/**
 * Factory for creating game decks from JSON files.
 * @author AM54
 */
public class DeckFactory {



    /**
     * Creates the building card deck from a JSON configuration file.
     * Cards are read, grouped by era, shuffled, and stacked with Era 3 at the bottom
     * and Era 1 on top (drawn first). The number of cards per era is determined by
     * the player count from {@code data/EraCounts.json}.
     *
     * @param filePath   path to the building cards JSON configuration file
     * @param numPlayers the number of players (2-5)
     * @return the assembled building deck
     * @throws IllegalArgumentException if the number of players is not between 2 and 5
     * @throws IllegalStateException    if the JSON files cannot be read or parsed
     */
    public static Deck<BuildingCard> createBuildingDeck(String filePath, int numPlayers) {

        if (numPlayers < 2 || numPlayers > 5) {
            throw new IllegalArgumentException("Unable to create deck: invalid number of players (" + numPlayers + ")!");
        }

        int countE1 = 0, countE2 = 0, countE3 = 0;

        // read the quantity setup from EraCounts.json
        try {
            JsonObject countsRoot = JsonParser.parseReader(new FileReader("data/EraCounts.json")).getAsJsonObject();
            JsonArray buildingCounts = countsRoot.getAsJsonObject("buildingCounts").getAsJsonArray(String.valueOf(numPlayers));

            if (buildingCounts != null) {
                countE1 = buildingCounts.get(0).getAsInt();
                countE2 = buildingCounts.get(1).getAsInt();
                countE3 = buildingCounts.get(2).getAsInt();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Critical error while reading file EraCounts.json", e);
        }

        List<BuildingCard> e1 = new ArrayList<>();
        List<BuildingCard> e2 = new ArrayList<>();
        List<BuildingCard> e3 = new ArrayList<>();

        try {
            JsonArray root = JsonParser.parseReader(new FileReader(filePath)).getAsJsonArray();

            for (JsonElement elem : root) {
                JsonObject node = elem.getAsJsonObject();
                JobType type = JobType.valueOf(node.get("JobType").getAsString());
                Phases phase = Phases.valueOf(node.get("phase").getAsString());
                int cost = node.get("cost").getAsInt();
                int finalAura = node.get("finalAura").getAsInt();
                String lambdaName = node.get("lambdaName").getAsString();
                int era = node.get("era").getAsInt();
                JsonElement paramsElem = node.get("effectParams");
                JsonObject params = paramsElem != null ? paramsElem.getAsJsonObject() : null;

                List<IEffect<BuildingCard>> effects = new ArrayList<>();
                IEffect<BuildingCard> effect = createBuildingEffect(node.get("phase").getAsString(), lambdaName, params);
                if (effect != null) {
                    effects.add(effect);
                }

                BuildingCard card = new BuildingCard(type, cost, finalAura, phase, effects, era);

                card.setName(lambdaName);

                if (node.has("imageId")) {
                    card.setImageId(node.get("imageId").getAsString());
                }

                if (era == 1) e1.add(card);
                else if (era == 2) e2.add(card);
                else if (era == 3) e3.add(card);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Critical error while generating cards from file: " + filePath, e);
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
        // Finally era 1 (so it stays on top of the stack and is drawn first)
        for (int i = 0; i < Math.min(countE1, e1.size()); i++) {
            finalDeck.push(e1.get(i));
        }

        return new Deck<>(finalDeck);
    }

    /**
     * Creates the tribe card deck containing both character and event cards.
     * Characters are read from {@code charPath}, events from {@code eventPath}.
     * Cards are grouped by era, shuffled together, and stacked with final events
     * at the bottom and Era 1 on top (drawn first). The number of character cards
     * per era is determined by the player count from {@code data/EraCounts.json}.
     *
     * @param charPath   path to the character cards JSON configuration file
     * @param eventPath  path to the event cards JSON configuration file
     * @param numPlayers the number of players (2-5)
     * @return the assembled tribe deck
     * @throws IllegalArgumentException if the number of players is not between 2 and 5
     * @throws IllegalStateException    if the JSON files cannot be read or parsed
     */
    public static Deck<TribeCard> createTribeDeck(String charPath, String eventPath, int numPlayers) {

        if (numPlayers < 2 || numPlayers > 5) {
            throw new IllegalArgumentException("Unable to create deck: invalid number of players (" + numPlayers + ")!");
        }

        int countE1 = 0, countE2 = 0, countE3 = 0;

        // Read the number of characters to draw per Era
        try {
            JsonObject countsRoot = JsonParser.parseReader(new FileReader("data/EraCounts.json")).getAsJsonObject();
            JsonObject charCounts = countsRoot.getAsJsonObject("characterCounts").getAsJsonObject(String.valueOf(numPlayers));

            if (charCounts != null) {
                countE1 = charCounts.get("era1").getAsInt();
                countE2 = charCounts.get("era2").getAsInt();
                countE3 = charCounts.get("era3").getAsInt();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Critical error while reading file EraCounts.json", e);
        }

        // Separate lists for Characters and Events
        List<TribeCard> charE1 = new ArrayList<>();
        List<TribeCard> charE2 = new ArrayList<>();
        List<TribeCard> charE3 = new ArrayList<>();

        List<TribeCard> eventE1 = new ArrayList<>();
        List<TribeCard> eventE2 = new ArrayList<>();
        List<TribeCard> eventE3 = new ArrayList<>();
        List<TribeCard> finalEvents = new ArrayList<>();

        // loading Characters (inserted into charE)
        try {
            JsonArray root = JsonParser.parseReader(new FileReader(charPath)).getAsJsonArray();
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
            System.err.println("\n\n!!! REAL ERROR CAUGHT !!!");
            e.printStackTrace(); // This will force the console to tell you EXACTLY which line broke
            System.err.println("!!! END ERROR !!!\n\n");
            throw new IllegalStateException("Critical error while reading Characters file: " + charPath, e);
        }

        // loading Events (inserted into eventE)
        try {
            JsonArray root = JsonParser.parseReader(new FileReader(eventPath)).getAsJsonArray();
            for (JsonElement elem : root) {
                JsonObject node = elem.getAsJsonObject();
                String label = node.get("subclassLabel").getAsString();
                int era = node.get("era").getAsInt();
                boolean finalEvent = node.get("finalEvent").getAsBoolean();

                List<IEffect<EventCard>> effects = new ArrayList<>();
                effects.add(createEventEffect(label));

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

                event.setName(label);

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
            throw new IllegalStateException("Critical error while reading Events file: " + eventPath, e);
        }

        //shuffle only the characters and "cut" them based on the JSON number
        Collections.shuffle(charE1);
        Collections.shuffle(charE2);
        Collections.shuffle(charE3);

        List<TribeCard> era1 = new ArrayList<>(charE1.subList(0, Math.min(countE1, charE1.size())));
        List<TribeCard> era2 = new ArrayList<>(charE2.subList(0, Math.min(countE2, charE2.size())));
        List<TribeCard> era3 = new ArrayList<>(charE3.subList(0, Math.min(countE3, charE3.size())));

        // merge all Events with the just-selected characters
        era1.addAll(eventE1);
        era2.addAll(eventE2);
        era3.addAll(eventE3);

        // shuffle the complete deck for each Era (so you never know when the event comes out)
        Collections.shuffle(era1);
        Collections.shuffle(era2);
        Collections.shuffle(era3);
        Collections.shuffle(finalEvents); // also shuffle the final events

        // stack everything into the final Stack (LIFO)
        Stack<TribeCard> finalDeck = new Stack<>();
        for (TribeCard c : finalEvents) finalDeck.push(c);
        for (TribeCard c : era3) finalDeck.push(c);
        for (TribeCard c : era2) finalDeck.push(c);
        for (TribeCard c : era1) finalDeck.push(c);

        //return the final complete Stack
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

    // create the right effect for buildings
    private static IEffect<BuildingCard> createBuildingEffect(String phase, String lambdaName, JsonObject params) {
        if (lambdaName == null || lambdaName.isEmpty() || lambdaName.equals("null")) return null;
        return BUILDING_EFFECTS.getOrDefault(lambdaName, p -> {
            throw new IllegalArgumentException("Building effect not handled by JSON: " + lambdaName);
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
            throw new IllegalArgumentException("Unknown character effect: " + effectName);
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
            throw new IllegalArgumentException("Unknown event effect: " + effectName);
        }).get();
    }
}