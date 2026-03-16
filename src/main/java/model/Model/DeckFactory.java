package model.Model;

import java.io.*;
import java.util.*;

public class DeckFactory {

    private static final Map<String, IEffect> effectRegistry = new HashMap<>();

    // Inizializzazione del registro degli effetti tramite Lambda [cite: 146, 149]
    static {
        effectRegistry.put("DAI_1_CIBO", (p, g) -> p.modifyFood(1));
        effectRegistry.put("GENERA_MAGIA", (p, g) -> p.modifyAuraPoints(1));
        effectRegistry.put("AURA_PUNTI", (p, g) -> p.modifyAuraPoints(5));
        // Aggiungi qui gli altri nomi lambda usati nei file .txt
    }

    /**
     * Crea il mazzo edifici leggendo dal file specifico.
     */
    public static Stack<BuildingCard> createBuildingDeck(String filePath) {
        // Liste separate per ogni era
        List<BuildingCard> e1 = new ArrayList<>();
        List<BuildingCard> e2 = new ArrayList<>();
        List<BuildingCard> e3 = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] d = line.split(",");
                // Formato Txt: buildingType(JobType), cost(int), finalAura(int), Phase(Phases), NomeLambda(String)
                JobType type = JobType.valueOf(d[0].toUpperCase());
                int cost = Integer.parseInt(d[1]);
                int finalAura = Integer.parseInt(d[2]);
                Phases phase = Phases.valueOf(d[3].toUpperCase());
                List<IEffect> effects = new ArrayList<>();
                if (effectRegistry.containsKey(d[4])) {
                    effects.add(effectRegistry.get(d[4]));
                }
                int era = Integer.parseInt(d[5]);
                BuildingCard card = new BuildingCard(type, cost, finalAura, phase, effects, era);

                // Smistamento per Era
                if (era == 1) e1.add(card);
                else if (era == 2) e2.add(card);
                else if (era == 3) e3.add(card);
            }
        } catch (IOException e) { e.printStackTrace(); }

        // Mischiamo ogni era individualmente
        Collections.shuffle(e1);
        Collections.shuffle(e2);
        Collections.shuffle(e3);

        Stack<BuildingCard> finalStack = new Stack<>();
        // Push in ordine inverso per avere Era 1 in cima
        for (BuildingCard c : e3) finalStack.push(c);
        for (BuildingCard c : e2) finalStack.push(c);
        for (BuildingCard c : e1) finalStack.push(c);

        return finalStack;
    }

    /**
     * Crea il mazzo Tribù unendo Personaggi ed Eventi.
     * Gestisce la logica dei Final Event in fondo al mazzo.
     */
    public static Stack<TribeCard> createTribeDeck(String charPath, String eventPath) {
        List<TribeCard> finalEvents = new ArrayList<>();
        List<TribeCard> era1 = new ArrayList<>();
        List<TribeCard> era2 = new ArrayList<>();
        List<TribeCard> era3 = new ArrayList<>();

        // 1. Caricamento Personaggi (Hanno IEffects)
        try (BufferedReader br = new BufferedReader(new FileReader(charPath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] d = line.split(",");
                // Formato Txt: level(int), characterType(JobType), aura(int), Phase(Phases), NomeLambda(String)
                int level = Integer.parseInt(d[0]);
                JobType type = JobType.valueOf(d[1].toUpperCase());
                int aura = Integer.parseInt(d[2]);
                Phases phase = Phases.valueOf(d[3].toUpperCase());
                List<IEffect> effects = new ArrayList<>();
                if (effectRegistry.containsKey(d[4])) {
                    effects.add(effectRegistry.get(d[4]));
                }
                int era = Integer.parseInt(d[5]);

                CharacterCard card = new CharacterCard(level, type, aura, phase, effects, era);
                if (era == 1) era1.add(card);
                else if (era == 2) era2.add(card);
                else era3.add(card);
            }
        } catch (IOException e) { e.printStackTrace(); }

        // 2. Caricamento Eventi (NON hanno IEffects, usano resolveEvent)
        try (BufferedReader br = new BufferedReader(new FileReader(eventPath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] d = line.split(",");
                // Formato Txt: finalEvent(bool), etichettaSottoclasse(String)
                boolean isFinal = Boolean.parseBoolean(d[0]);
                String label = d[1].toUpperCase();
                int era = Integer.parseInt(d[2]);

                EventCard event = switch (label) {
                    case "HUNT" -> new HuntEvent(era);
                    case "PAINTING" -> new PaintingEvent(era);
                    case "SHAMAN" -> new ShamanEvent(era, isFinal);
                    case "SUSTENANCE" -> new SustenanceEvent(era, isFinal);
                    default -> null;
                };

                if (event != null) {
                    if (isFinal) finalEvents.add(event);
                    else {
                        if (era == 1) era1.add(event);
                        else if (era == 2) era2.add(event);
                        else era3.add(event);
                    }
                }
            }
        } catch (IOException e) { e.printStackTrace(); }

        // Mischiamo tutto per blocchi
        Collections.shuffle(finalEvents);
        Collections.shuffle(era1);
        Collections.shuffle(era2);
        Collections.shuffle(era3);

        Stack<TribeCard> finalStack = new Stack<>();

        // ORDINE DI PUSH (Bottom to Top):
        // 1. Final Events (Abs bottom)
        for (TribeCard c : finalEvents) finalStack.push(c);
        // 2. Era 3
        for (TribeCard c : era3) finalStack.push(c);
        // 3. Era 2
        for (TribeCard c : era2) finalStack.push(c);
        // 4. Era 1 (Top)
        for (TribeCard c : era1) finalStack.push(c);

        return finalStack;
    }
}