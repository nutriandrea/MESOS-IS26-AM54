package it.polimi.ingsw.am54.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.ingsw.am54.model.effects.buildings.EndgamePhaseEffect;
import it.polimi.ingsw.am54.model.effects.buildings.EventPhaseEffect;
import it.polimi.ingsw.am54.model.effects.buildings.PostactionPhaseEffect;
import it.polimi.ingsw.am54.model.effects.buildings.PostpickPhaseEffect;
import model.effects.buildings.*;
import it.polimi.ingsw.am54.model.effects.characters.CharacterEffect;
import it.polimi.ingsw.am54.model.effects.events.EventEffect;
import java.io.*;
import java.util.*;


public class DeckFactory {

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Crea il mazzo edifici leggendo dal file JSON.
     */
    public static Deck<BuildingCard> createBuildingDeck(String filePath, int numPlayers) {

        // Controllo di sicurezza
        if (numPlayers < 2 || numPlayers > 5) {
            System.err.println("Numero di giocatori non valido per creare il mazzo!");
            return new Deck<>(new Stack<>());
        }

        int countE1 = 0, countE2 = 0, countE3 = 0;

        // 1. Leggiamo il setup delle quantità da EraCounts.json
        try {
            JsonNode countsRoot = mapper.readTree(new File("data/EraCounts.json")); // Assicurati del percorso!
            JsonNode buildingCounts = countsRoot.get("buildingCounts").get(String.valueOf(numPlayers));

            if (buildingCounts != null && buildingCounts.isArray()) {
                // L'array nel JSON è [era1, era2, era3]
                countE1 = buildingCounts.get(0).asInt();
                countE2 = buildingCounts.get(1).asInt();
                countE3 = buildingCounts.get(2).asInt();
            }
        } catch (Exception e) {
            System.err.println("Errore lettura EraCounts.json: " + e.getMessage());
        }

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
            System.err.println("Errore lettura file carte: " + e.getMessage());
            e.printStackTrace();
        }

        Collections.shuffle(e1);
        Collections.shuffle(e2);
        Collections.shuffle(e3);

        Stack<BuildingCard> finalDeck = new Stack<>();

        // Inseriamo PRIMA l'Era 3 (così va in fondo al mazzo)
        for (int i = 0; i < Math.min(countE3, e3.size()); i++) {
            finalDeck.push(e3.get(i));
        }
        // Poi l'Era 2
        for (int i = 0; i < Math.min(countE2, e2.size()); i++) {
            finalDeck.push(e2.get(i));
        }
        // Infine l'Era 1 (così rimane in CIMA allo Stack e viene pescata per prima)
        for (int i = 0; i < Math.min(countE1, e1.size()); i++) {
            finalDeck.push(e1.get(i));
        }

        return new Deck<>(finalDeck);
    }

    /**
     * Crea il mazzo Tribu unendo Personaggi (quantità da JSON) e TUTTI gli Eventi.
     */
    public static Deck<TribeCard> createTribeDeck(String charPath, String eventPath, int numPlayers) {

        // Controllo di sicurezza
        if (numPlayers < 2 || numPlayers > 5) {
            System.err.println("Numero di giocatori non valido!");
            return new Deck<>(new Stack<>());
        }

        int countE1 = 0, countE2 = 0, countE3 = 0;

        // 0. Leggiamo il numero di personaggi da pescare per Era
        try {
            JsonNode countsRoot = mapper.readTree(new File("data/EraCounts.json"));
            JsonNode charCounts = countsRoot.get("characterCounts").get(String.valueOf(numPlayers));

            if (charCounts != null) {
                countE1 = charCounts.get("era1").asInt();
                countE2 = charCounts.get("era2").asInt();
                countE3 = charCounts.get("era3").asInt();
            }
        } catch (Exception e) {
            System.err.println("Errore lettura EraCounts: " + e.getMessage());
        }

        // Liste separate per Personaggi ed Eventi
        List<TribeCard> charE1 = new ArrayList<>();
        List<TribeCard> charE2 = new ArrayList<>();
        List<TribeCard> charE3 = new ArrayList<>();

        List<TribeCard> eventE1 = new ArrayList<>();
        List<TribeCard> eventE2 = new ArrayList<>();
        List<TribeCard> eventE3 = new ArrayList<>();
        List<TribeCard> finalEvents = new ArrayList<>();

        // 1. Caricamento Personaggi (Li mettiamo nelle liste charE)
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
                    if (era == 1) charE1.add(card);
                    else if (era == 2) charE2.add(card);
                    else charE3.add(card);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 2. Caricamento Eventi (Li mettiamo nelle liste eventE)
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
                    if (era == 1) eventE1.add(event);
                    else if (era == 2) eventE2.add(event);
                    else eventE3.add(event);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 3. Mescoliamo SOLO i personaggi e li "tagliamo" in base al numero del JSON
        Collections.shuffle(charE1);
        Collections.shuffle(charE2);
        Collections.shuffle(charE3);

        List<TribeCard> era1 = new ArrayList<>(charE1.subList(0, Math.min(countE1, charE1.size())));
        List<TribeCard> era2 = new ArrayList<>(charE2.subList(0, Math.min(countE2, charE2.size())));
        List<TribeCard> era3 = new ArrayList<>(charE3.subList(0, Math.min(countE3, charE3.size())));

        // 4. Uniamo TUTTI gli Eventi ai personaggi appena selezionati
        era1.addAll(eventE1);
        era2.addAll(eventE2);
        era3.addAll(eventE3);

        // 5. Mischiamo il mazzetto completo per ogni Era (così non sai mai quando esce l'evento)
        Collections.shuffle(era1);
        Collections.shuffle(era2);
        Collections.shuffle(era3);
        Collections.shuffle(finalEvents); // Consigliato mischiare anche quelli finali

        // 6. Impiliamo tutto nello Stack finale (LIFO)
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
