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
     * Crea il mazzo edifici leggendo dal file specifico e filtrando per numero di giocatori.
     * @param filePath il percorso del file
     * @param numPlayers il numero di giocatori nella partita
     */
    public static Stack<BuildingCard> createBuildingDeck(String filePath, int numPlayers) {

        // Tabella dei conteggi [giocatori][era-1]
        int[][] eraCounts = {
                {0, 0, 0},              // indice 0 non usato
                {0, 0, 0},              // indice 1 non usato
                {1, 2, 3},              // 2 giocatori → Era I, II, III
                {2, 2, 4},              // 3 giocatori
                {2, 3, 4},              // 4 giocatori
                {2, 3, 5}               // 5 giocatori
        };

        // Liste separate per ogni era
        List<BuildingCard> e1 = new ArrayList<>();
        List<BuildingCard> e2 = new ArrayList<>();
        List<BuildingCard> e3 = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] d = line.split(",");
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

                // Smistamento per Era (qui carichiamo TUTTE le carte del file)
                if (era == 1) e1.add(card);
                else if (era == 2) e2.add(card);
                else if (era == 3) e3.add(card);
            }
        } catch (IOException e) { e.printStackTrace(); }

        // Mischiamo tutte le carte di ogni era
        Collections.shuffle(e1);
        Collections.shuffle(e2);
        Collections.shuffle(e3);

        Stack<BuildingCard> finalStack = new Stack<>();

        // Push in ordine inverso (Era 3, poi 2, poi 1) limitato dal numero di giocatori
        // Usiamo Math.min per evitare errori se nel file ci sono meno carte del previsto

        int countE3 = eraCounts[numPlayers][2];
        for (int i = 0; i < Math.min(countE3, e3.size()); i++) {
            finalStack.push(e3.get(i));
        }

        int countE2 = eraCounts[numPlayers][1];
        for (int i = 0; i < Math.min(countE2, e2.size()); i++) {
            finalStack.push(e2.get(i));
        }

        int countE1 = eraCounts[numPlayers][0];
        for (int i = 0; i < Math.min(countE1, e1.size()); i++) {
            finalStack.push(e1.get(i));
        }

        return finalStack;
    }

    /**
     * Crea il mazzo Tribù unendo Personaggi ed Eventi.
     * Gestisce la logica dei Final Event in fondo al mazzo.
     */
    public static Stack<TribeCard> createTribeDeck(String charPath, String eventPath, int numPlayers) {

        List<TribeCard> finalEvents = new ArrayList<>();
        List<TribeCard> era1 = new ArrayList<>();
        List<TribeCard> era2 = new ArrayList<>();
        List<TribeCard> era3 = new ArrayList<>();

        // 1. Caricamento Personaggi (Filtrati con logica 2+, 3+, 4+, 5)
        try (BufferedReader br = new BufferedReader(new FileReader(charPath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] d = line.split(",");

                // Leggiamo il requisito minimo di giocatori (assumiamo sia il 7° parametro, indice 6)
                int minPlayers = Integer.parseInt(d[6]);

                // Aggiungiamo la carta SOLO se abbiamo abbastanza giocatori
                if (numPlayers >= minPlayers) {
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