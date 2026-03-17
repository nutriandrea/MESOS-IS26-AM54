package model.Model;

import java.io.*;
import java.util.*;

public class DeckFactory {

    private static final Map<String, IEffect> buildingsEffectRegistry = new HashMap<>();
    private static final Map<String, IEffect> charactersEffectRegistry = new HashMap<>();

    // Inizializzazione del registro degli effetti tramite Lambda [cite: 146, 149]
    static {
        // Buildings

        //era1
        //WORK IN PROGRESS
        buildingsEffectRegistry.put("DOUBLE_INVENTOR", (p, g, cc) ->() );
        buildingsEffectRegistry.put("SIX_OF_TYPE", (p, g, cc) -> ());

        buildingsEffectRegistry.put("TURN_FOOD", (p, g, cc) -> g.getBoard().getTurnOrderTile().stream()
                .filter(t -> t.getOccupiedBy() != null && t.getOccupiedBy().equals(p))
                .forEach(t -> p.modifyFood(1)));

        buildingsEffectRegistry.put("DISCOUNT_FOOD_FARMER", (p, g, cc) -> p.setEventCostFood((int) (p.getMyTribe().stream().filter(c -> c.getJobType() == JobType.FARMER).count() * -1)) );
        buildingsEffectRegistry.put("DISCOUNT_FOOD_ARTIST", (p, g, cc) -> p.setEventCostFood((int) (p.getMyTribe().stream().filter(c -> c.getJobType() == JobType.ARTIST).count() * -1)) );
        buildingsEffectRegistry.put("NO_LOSS", (p,g,cc) -> p.setEventCostAura(- p.getEventCostAura()) );
        //era2
        buildingsEffectRegistry.put("PLUS_HUNTING", (p,g,cc) ->  {p.setEventCostFood((int) (p.getMyTribe().stream().filter(c -> c.getJobType() == JobType.HUNTER).count()));
                                                              p.setEventCostAura((int) (p.getMyTribe().stream().filter(c -> c.getJobType() == JobType.HUNTER).count()));});
        buildingsEffectRegistry.put("PLUS_FOOD_ARTIST", (p, g, cc) -> p.setEventCostFood((int) (p.getMyTribe().stream().filter(c -> c.getJobType() == JobType.ARTIST).count())) );
        buildingsEffectRegistry.put("DISCOUNT_FOOD_INVENTOR", (p, g, cc) -> p.setEventCostFood((int) (p.getMyTribe().stream()
                                                                                    .filter(c -> c.getJobType() == JobType.INVENTOR).count() * -1)) );
        buildingsEffectRegistry.put("DOUBLE_AURA_SHAMAN", (p, g, cc) -> p.setEventCostAura(p.getEventCostAura() * 2) );
        //double constructor aggiunge solo uno per construttori perche di base aggiunge uno il end_game di game, quello aggiuntivo lo fa qua il building
        buildingsEffectRegistry.put("DOUBLE_CONSTRUCTOR_AURA", (p, g, cc) -> p.getMyTribe().stream()
                                                                                .filter(c -> c.getJobType() == JobType.BUILDER).forEach(c -> p.modifyAuraPoints(c.getAura()) ));
        buildingsEffectRegistry.put("THREE_MAGIC", (p, g, cc) -> p.modifyMagicPoints(3) );
        
        buildingsEffectRegistry.put("SIX_DIFFERENT_AURA", (p, g, cc) -> p.modifyAuraPoints(6 * (int) Arrays.stream(JobType.values())
                                                                        .mapToLong(job -> p.getMyTribe().stream().filter(c -> c.getJobType() == job).count())
                                                                        .min().orElse(0)));
        buildingsEffectRegistry.put("AURA_SHAMAN", (p, g, cc) -> p.modifyAuraPoints((int) (4*(p.getMyTribe().stream().filter(c->c.getJobType()==JobType.SHAMAN).count()))) );
        buildingsEffectRegistry.put("AURA_HUNTER", (p, g, cc) -> p.modifyAuraPoints((int) (3*(p.getMyTribe().stream().filter(c->c.getJobType()==JobType.HUNTER).count()))) );
        buildingsEffectRegistry.put("AURA_BUILDER", (p, g, cc) -> p.modifyAuraPoints((int) (4*(p.getMyTribe().stream().filter(c->c.getJobType()==JobType.BUILDER).count()))) );
        buildingsEffectRegistry.put("AURA_ARTIST", (p, g, cc) -> p.modifyAuraPoints((int) (4*(p.getMyTribe().stream().filter(c->c.getJobType()==JobType.ARTIST).count()))) );
        buildingsEffectRegistry.put("AURA_FARMER", (p, g, cc) -> p.modifyAuraPoints((int) (4*(p.getMyTribe().stream().filter(c->c.getJobType()==JobType.FARMER).count()))) );
        buildingsEffectRegistry.put("AURA_INVENTOR", (p, g, cc) -> p.modifyAuraPoints((int) (2*(p.getMyTribe().stream().filter(c->c.getJobType()==JobType.INVENTOR).count()))) );
        buildingsEffectRegistry.put("25_AURA", (p, g, cc) -> p.modifyAuraPoints(25) );
        buildingsEffectRegistry.put("EXTRA_PICK", (p, g, cc) -> p.setExtraPick(true) );
        
        // Characters

        charactersEffectRegistry.put("HUNTER", (p, g, cc) -> p.modifyFood((int) (p.getMyTribe().stream().filter(c->c.getJobType()==JobType.HUNTER).count()+1)));
        charactersEffectRegistry.put("BUILDER", (p, g, cc) -> p.modifyMyBuildingDiscount(cc.getLevel()));
        charactersEffectRegistry.put("SHAMAN", (p, g, cc) -> p.modifyMagicPoints(cc.getLevel()));
        charactersEffectRegistry.put("FARMER", (p, g, cc) -> p.modifyMyFoodDiscount(cc.getLevel()));
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
                if (buildingsEffectRegistry.containsKey(d[4])) {
                    effects.add(buildingsEffectRegistry.get(d[4]));
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
                    if (charactersEffectRegistry.containsKey(d[4])) {
                        effects.add(charactersEffectRegistry.get(d[4]));
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