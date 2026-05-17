package it.polimi.ingsw.am54.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.ingsw.am54.model.effects.buildings.EndgamePhaseEffect;
import it.polimi.ingsw.am54.model.effects.buildings.EventPhaseEffect;
import it.polimi.ingsw.am54.model.effects.buildings.PostactionPhaseEffect;
import it.polimi.ingsw.am54.model.effects.buildings.PostpickPhaseEffect;
import it.polimi.ingsw.am54.model.effects.buildings.*;
import it.polimi.ingsw.am54.model.effects.characters.CharacterEffect;
import it.polimi.ingsw.am54.model.effects.events.EventEffect;
import java.io.*;
import java.util.*;


public class DeckFactory {

    private static final ObjectMapper mapper = new ObjectMapper();


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

   //crea il mazzo tribu coi personaggi ed eventi
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
                    effects.add(new CharacterEffect(characterType));

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

    //crea l'effetto giusto in base alla fase
    private static IEffect<BuildingCard> createBuildingEffect(String phase, String nomeLambda, JsonNode params) {
        if (nomeLambda == null) return null;

        //questa factory prende la stringa della fase dal JSON, istanzia la classe corretta passandole
        //il nome specifico dell'effetto e i suoi parametri,
        //restituisce infine l'oggetto pronto per essere iniettato nella BuildingCard
        return switch (phase) {
            case "EVENT", "STARTTURN" -> new EventPhaseEffect(nomeLambda, params);
            case "ENDGAME"            -> new EndgamePhaseEffect(nomeLambda, params);
            case "POSTPICK"           -> new PostpickPhaseEffect(nomeLambda, params);
            case "POSTACTION"         -> new PostactionPhaseEffect(nomeLambda, params);
            default -> null;
        };
    }
}
