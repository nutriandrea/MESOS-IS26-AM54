package it.polimi.ingsw.am54.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.ingsw.am54.model.effects.buildings.*;
import it.polimi.ingsw.am54.model.effects.characters.*;
import it.polimi.ingsw.am54.model.effects.events.*;

import java.io.*;
import java.util.*;


public class DeckFactory {

    private static final ObjectMapper mapper = new ObjectMapper();


    // crea il mazzo BUILDINGS
    public static Deck<BuildingCard> createBuildingDeck(String filePath, int numPlayers) {

        if (numPlayers < 2 || numPlayers > 5) {
            throw new IllegalArgumentException("Impossibile creare il mazzo: numero di giocatori (" + numPlayers + ") non valido!");
        }

        int countE1 = 0, countE2 = 0, countE3 = 0;

        // leggiamo il setup delle quantità da EraCounts.json
        try {
            JsonNode countsRoot = mapper.readTree(new File("data/EraCounts.json"));
            JsonNode buildingCounts = countsRoot.get("buildingCounts").get(String.valueOf(numPlayers));

            if (buildingCounts != null && buildingCounts.isArray()) {
                // L'array nel JSON è [era1, era2, era3]
                countE1 = buildingCounts.get(0).asInt();
                countE2 = buildingCounts.get(1).asInt();
                countE3 = buildingCounts.get(2).asInt();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Errore critico durante la lettura del file EraCounts.json", e);
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

                card.setName(nomeLambda);

                if (node.has("imageId")) {
                    card.setImageId(node.get("imageId").asText());
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

        // Inseriamo prima l'era 3 (così va in fondo al mazzo)
        for (int i = 0; i < Math.min(countE3, e3.size()); i++) {
            finalDeck.push(e3.get(i));
        }
        // Poi l'era 2
        for (int i = 0; i < Math.min(countE2, e2.size()); i++) {
            finalDeck.push(e2.get(i));
        }
        // Infine l'era 1 (così rimane in cima allo stack e viene pescata per prima)
        for (int i = 0; i < Math.min(countE1, e1.size()); i++) {
            finalDeck.push(e1.get(i));
        }

        return new Deck<>(finalDeck);
    }

   //crea il mazzo tribu coi PERSONAGGI ed EVENTI
    public static Deck<TribeCard> createTribeDeck(String charPath, String eventPath, int numPlayers) {

        if (numPlayers < 2 || numPlayers > 5) {
            throw new IllegalArgumentException("Impossibile creare il mazzo: numero di giocatori (" + numPlayers + ") non valido!");
        }

        int countE1 = 0, countE2 = 0, countE3 = 0;

        // Leggiamo il numero di personaggi da pescare per Era
        try {
            JsonNode countsRoot = mapper.readTree(new File("data/EraCounts.json"));
            JsonNode charCounts = countsRoot.get("characterCounts").get(String.valueOf(numPlayers));

            if (charCounts != null) {
                countE1 = charCounts.get("era1").asInt();
                countE2 = charCounts.get("era2").asInt();
                countE3 = charCounts.get("era3").asInt();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Errore critico durante la lettura del file EraCounts.json", e);
        }

        // Liste separate per Personaggi ed Eventi
        List<TribeCard> charE1 = new ArrayList<>();
        List<TribeCard> charE2 = new ArrayList<>();
        List<TribeCard> charE3 = new ArrayList<>();

        List<TribeCard> eventE1 = new ArrayList<>();
        List<TribeCard> eventE2 = new ArrayList<>();
        List<TribeCard> eventE3 = new ArrayList<>();
        List<TribeCard> finalEvents = new ArrayList<>();

        // caricamento Personaggi (inseriti in charE)
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
                    effects.add(createCharacterEffect(characterType));

                    CharacterCard card = new CharacterCard(level, type, aura, phase, effects, era);
                    card.setMinPlayers(minPlayers);
                    if (node.has("imageId")) {
                        card.setImageId(node.get("imageId").asText());
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

        // caricamento Eventi (inseriti in eventE)
        try {
            JsonNode root = mapper.readTree(new File(eventPath));
            for (JsonNode node : root) {
                String etichetta = node.get("etichettaSottoclasse").asText();
                int era = node.get("era").asInt();
                boolean finalEvent = node.get("finalEvent").asBoolean();

                List<IEffect<EventCard>> effects = new ArrayList<>();
                effects.add(createEventEffect(etichetta));

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

                event.setName(etichetta);

                if (node.has("imageId")) {
                    event.setImageId(node.get("imageId").asText());
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

        //mescoliamo solo i personaggi e li "tagliamo" in base al numero del JSON
        Collections.shuffle(charE1);
        Collections.shuffle(charE2);
        Collections.shuffle(charE3);

        List<TribeCard> era1 = new ArrayList<>(charE1.subList(0, Math.min(countE1, charE1.size())));
        List<TribeCard> era2 = new ArrayList<>(charE2.subList(0, Math.min(countE2, charE2.size())));
        List<TribeCard> era3 = new ArrayList<>(charE3.subList(0, Math.min(countE3, charE3.size())));

        // uniamo tutti gli Eventi ai personaggi appena selezionati
        era1.addAll(eventE1);
        era2.addAll(eventE2);
        era3.addAll(eventE3);

        // mischiamo il mazzetto completo per ogni Era (così non sai mai quando esce l'evento)
        Collections.shuffle(era1);
        Collections.shuffle(era2);
        Collections.shuffle(era3);
        Collections.shuffle(finalEvents); // mischiamo anche i finali

        // impiliamo tutto nello Stack finale (LIFO)
        Stack<TribeCard> finalDeck = new Stack<>();
        for (TribeCard c : finalEvents) finalDeck.push(c);
        for (TribeCard c : era3) finalDeck.push(c);
        for (TribeCard c : era2) finalDeck.push(c);
        for (TribeCard c : era1) finalDeck.push(c);

        //ritorniamo lo Stack finale e completo
        return new Deck<>(finalDeck);
    }

    // crea l'effetto giusto per gli edifici
    private static IEffect<BuildingCard> createBuildingEffect(String phase, String nomeLambda, JsonNode params) {
        if (nomeLambda == null || nomeLambda.isEmpty() || nomeLambda.equals("null")) return null;

        return switch (nomeLambda) {
            // 1 classe per + Buildings
            case "SHAMAN_PASSIVE"      -> new BuildingShamanPassive(params);
            case "EVENT_COST_MODIFIER" -> new BuildingEventCostModifier(params);
            case "FLAT_BONUS"          -> new BuildingFlatBonus(params);
            case "JOB_AURA_BONUS"      -> new BuildingJobAuraBonus(params);

            // 1 classe per Building
            case "BONUS_FOOD"          -> new BuildingTurnOrderBonus();
            case "SIX_DIFFERENT_AURA"  -> new BuildingSixDifferentAura();
            case "SIX_OF_TYPE"         -> new BuildingSixOfType();
            case "DOUBLE_INVENTOR"     -> new BuildingDoubleInventor();

            default -> throw new IllegalArgumentException("Effetto edificio non gestito dal JSON: " + nomeLambda);
        };
    }

    private static IEffect<CharacterCard> createCharacterEffect(String effectName) {
        return switch (effectName) {
            case "HUNTER" -> new CharacterHunter();
            case "CONSTRUCTOR" -> new CharacterConstructor();
            case "SHAMAN" -> new CharacterShaman();
            case "ARTIST", "INVENTOR", "FARMER" -> new CharacterPassive();
            default -> throw new IllegalArgumentException("Effetto personaggio sconosciuto: " + effectName);
        };
    }

    private static IEffect<EventCard> createEventEffect(String effectName) {
        return switch (effectName) {
            case "HUNTER" -> new EventHunter();
            case "FARMER" -> new EventFarmer();
            case "ARTIST" -> new EventArtist();
            case "SHAMAN" -> new EventShaman();
            default -> throw new IllegalArgumentException("Effetto evento sconosciuto: " + effectName);
        };
    }
}