package model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.effects.buildings.*;
import model.effects.characters.CharacterEffect;
import model.effects.events.EventEffect;
import java.io.*;
import java.util.*;


public class DeckFactory {

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Crea il mazzo edifici leggendo dal file JSON.
     */
    public static Deck<BuildingCard> createBuildingDeck(String filePath, int numPlayers) {

        int[][] eraCounts = {
                {0, 0, 0},
                {0, 0, 0},
                {1, 2, 3},
                {2, 2, 4},
                {2, 3, 4},
                {2, 3, 5}
        };

        List<BuildingCard> e1 = new ArrayList<>();
        List<BuildingCard> e2 = new ArrayList<>();
        List<BuildingCard> e3 = new ArrayList<>();

        try {
            JsonNode root = mapper.readTree(new File(filePath));

            for (JsonNode node : root) {
                JobType type = JobType.valueOf(node.get("JobType").asText());
                Phases phase = Phases.valueOf(node.get("phase").asText());
                int cost = node.get("cost").asInt();
                int finalAura = node.get("finalAura").asInt();
                String nomeLambda = node.get("nomeLambda").asText();
                int era = node.get("era").asInt();
                JsonNode params = node.get("effectParams");

                List<IEffect<BuildingCard>> effects = new ArrayList<>();
                IEffect<BuildingCard> effect = createBuildingEffect(node.get("phase").asText(), nomeLambda, params);
                if (effect != null) {
                    effects.add(effect);
                }

                BuildingCard card = new BuildingCard(type, cost, finalAura, phase, effects, era);
                if (era == 1) e1.add(card);
                else if (era == 2) e2.add(card);
                else if (era == 3) e3.add(card);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Collections.shuffle(e1);
        Collections.shuffle(e2);
        Collections.shuffle(e3);

        Stack<BuildingCard> finalDeck = new Stack<>();

        int countE3 = eraCounts[numPlayers][2];
        for (int i = 0; i < Math.min(countE3, e3.size()); i++) {
            finalDeck.push(e3.get(i));
        }
        int countE2 = eraCounts[numPlayers][1];
        for (int i = 0; i < Math.min(countE2, e2.size()); i++) {
            finalDeck.push(e2.get(i));
        }
        int countE1 = eraCounts[numPlayers][0];
        for (int i = 0; i < Math.min(countE1, e1.size()); i++) {
            finalDeck.push(e1.get(i));
        }

        return new Deck<>(finalDeck);
    }

    /**
     * Crea il mazzo Tribu unendo Personaggi ed Eventi.
     */
    public static Deck<TribeCard> createTribeDeck(String charPath, String eventPath, int numPlayers) {

        List<TribeCard> finalEvents = new ArrayList<>();
        List<TribeCard> era1 = new ArrayList<>();
        List<TribeCard> era2 = new ArrayList<>();
        List<TribeCard> era3 = new ArrayList<>();

        // 1. Caricamento Personaggi
        try {
            JsonNode root = mapper.readTree(new File(charPath));

            for (JsonNode node : root) {
                int minPlayers = node.get("minPlayers").asInt();
                if (numPlayers >= minPlayers) {
                    String characterType = node.get("characterType").asText();
                    JobType type = JobType.valueOf(characterType);
                    Phases phase = Phases.valueOf(node.get("phase").asText());
                    int level = node.get("level").asInt();
                    int aura = node.get("aura").asInt();
                    int era = node.get("era").asInt();

                    List<IEffect<CharacterCard>> effects = new ArrayList<>();
                    effects.add(new CharacterEffect(characterType));

                    CharacterCard card = new CharacterCard(level, type, aura, phase, effects, era);
                    if (era == 1) era1.add(card);
                    else if (era == 2) era2.add(card);
                    else era3.add(card);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 2. Caricamento Eventi
        try {
            JsonNode root = mapper.readTree(new File(eventPath));

            for (JsonNode node : root) {
                String etichetta = node.get("etichettaSottoclasse").asText();
                int era = node.get("era").asInt();
                boolean finalEvent = node.get("finalEvent").asBoolean();

                List<IEffect<EventCard>> effects = new ArrayList<>();
                effects.add(new EventEffect(etichetta));

                EventCard event = new EventCard(
                        era,
                        finalEvent,
                        effects,
                        node.has("baseFood") ? node.get("baseFood").asInt() : 0,
                        node.has("auraPerCharacter") ? node.get("auraPerCharacter").asInt() : 0,
                        node.has("threshold") ? node.get("threshold").asInt() : 0,
                        node.has("bonusMultiplier") ? node.get("bonusMultiplier").asInt() : 0,
                        node.has("penalty") ? node.get("penalty").asInt() : 0,
                        node.has("bonus") ? node.get("bonus").asInt() : 0,
                        node.has("malus") ? node.get("malus").asInt() : 0
                );

                if (finalEvent) {
                    finalEvents.add(event);
                } else {
                    if (era == 1) era1.add(event);
                    else if (era == 2) era2.add(event);
                    else era3.add(event);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Collections.shuffle(era1);
        Collections.shuffle(era2);
        Collections.shuffle(era3);

        Stack<TribeCard> finalDeck = new Stack<>();
        for (TribeCard c : finalEvents) finalDeck.push(c);
        for (TribeCard c : era3) finalDeck.push(c);
        for (TribeCard c : era2) finalDeck.push(c);
        for (TribeCard c : era1) finalDeck.push(c);

        return new Deck<>(finalDeck);
    }

    /**
     * Factory method: crea l'effetto giusto in base alla phase.
     */
    private static IEffect<BuildingCard> createBuildingEffect(String phase, String nomeLambda, JsonNode params) {
        if (nomeLambda == null) return null;

        return switch (phase) {
            case "EVENT", "STARTTURN" -> new EventPhaseEffect(nomeLambda, params);
            case "ENDGAME"            -> new EndgamePhaseEffect(nomeLambda, params);
            case "POSTPICK"           -> new PostpickPhaseEffect(nomeLambda, params);
            case "POSTACTION"         -> new PostactionPhaseEffect(nomeLambda, params);
            default -> null;
        };
    }
}
