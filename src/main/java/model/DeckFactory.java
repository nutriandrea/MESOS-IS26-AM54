package model;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

import java.io.*;
import java.util.*;

public class DeckFactory {

    private static final Map<String, IEffect<BuildingCard>> buildingsEffectRegistry = new HashMap<>();
    private static final Map<String, IEffect<CharacterCard>> charactersEffectRegistry = new HashMap<>();
    private static final Map<String, IEffect<EventCard>> eventEffectRegistry = new HashMap<>();

    // Inizializzazione del registro degli effetti tramite Lambda
    static {
        // Buildings

        //era1
        //WORK IN PROGRESS
        buildingsEffectRegistry.put("DOUBLE_INVENTOR", (p, g, edificio) -> {
            // 1. Prendi la lista delle carte del giocatore
            List<CharacterCard> tribe = p.getMyTribe();
            if (tribe.isEmpty()) return;

            // 2. Prendi l'ultima carta appena pescata (che ha innescato la fase POSTPICK)
            CharacterCard ultimaCarta = tribe.get(tribe.size() - 1);

            // 3. È un inventore?
            if (ultimaCarta.getJobType() == JobType.INVENTOR) {

                int iconaInvenzione = ultimaCarta.getLevel();

                // 4. Contiamo TUTTI gli inventori con QUESTA specifica icona nella tribù
                long conteggioTotale = tribe.stream()
                        .filter(c -> c.getJobType() == JobType.INVENTOR)
                        .filter(c -> c.getLevel() == iconaInvenzione)
                        .count();

                // 5. Se il totale attuale è pari, significa che QUESTA carta ha appena chiuso una coppia!
                if (conteggioTotale > 0 && conteggioTotale % 2 == 0) {

                    p.modifyFood(3); // Assegna il cibo (modifica la quantità se serve)
                }
            }
        });

        buildingsEffectRegistry.put("SIX_OF_TYPE", (p, g, edificio) -> {
            List<CharacterCard> tribe = p.getMyTribe();
            if (tribe.isEmpty()) return;

            // 1. Prendi la carta che ha appena innescato l'effetto
            CharacterCard ultimaCarta = tribe.get(tribe.size() - 1);
            JobType lavoroAppenaPescato = ultimaCarta.getJobType();

            // Se per qualche motivo è NULL, usciamo
            if (lavoroAppenaPescato == null) return;

            // 2. Quanti personaggi di QUESTO specifico lavoro abbiamo in totale ora? (Es. 2)
            long conteggioQuestoLavoro = tribe.stream()
                    .filter(c -> c.getJobType() == lavoroAppenaPescato)
                    .count();

            // 3. Elenco dei 6 lavori che compongono un set completo
            JobType[] tuttiILavori = {
                    JobType.HUNTER, JobType.BUILDER, JobType.SHAMAN,
                    JobType.FARMER, JobType.ARTIST, JobType.INVENTOR
            };

            boolean setCompletato = true;

            // 4. Verifichiamo se TUTTI i lavori hanno raggiunto almeno la quota di quello appena pescato
            for (JobType job : tuttiILavori) {
                long conteggio = tribe.stream()
                        .filter(c -> c.getJobType() == job)
                        .count();

                // Se troviamo anche solo un lavoro che sta "indietro", il set non è chiuso
                if (conteggio < conteggioQuestoLavoro) {
                    setCompletato = false;
                    break; // Inutile continuare a controllare gli altri
                }
            }

            // 5. Se nessuno era rimasto indietro, abbiamo appena chiuso il set!
            if (setCompletato) {
                p.modifyFood(1); // Sostituisci con il bonus esatto previsto dalla carta
                System.out.println("Set di 6 personaggi diversi completato! Cibo assegnato.");
            }
        });

        buildingsEffectRegistry.put("TURN_FOOD", (p, g, cc) -> g.getBoard().getTurnOrderTile().stream()
                .filter(t -> t.getOccupiedBy() != null && t.getOccupiedBy().equals(p))
                .forEach(t -> p.modifyFood(1)));

        buildingsEffectRegistry.put("DISCOUNT_FOOD_FARMER", (p, g, cc) -> p.setEventCostFood((int) (p.getMyTribe().stream().filter(c -> c.getJobType() == JobType.FARMER).count() * -1)));
        buildingsEffectRegistry.put("DISCOUNT_FOOD_ARTIST", (p, g, cc) -> p.setEventCostFood((int) (p.getMyTribe().stream().filter(c -> c.getJobType() == JobType.ARTIST).count() * -1)));
        buildingsEffectRegistry.put("NO_LOSS", (p, g, cc) -> p.setEventCostAura(-p.getEventCostAura()));
        //era2
        buildingsEffectRegistry.put("PLUS_HUNTING", (p, g, cc) -> {
            p.setEventCostFood((int) (p.getMyTribe().stream().filter(c -> c.getJobType() == JobType.HUNTER).count()));
            p.setEventCostAura((int) (p.getMyTribe().stream().filter(c -> c.getJobType() == JobType.HUNTER).count()));
        });
        buildingsEffectRegistry.put("PLUS_FOOD_ARTIST", (p, g, cc) -> p.setEventCostFood((int) (p.getMyTribe().stream().filter(c -> c.getJobType() == JobType.ARTIST).count())));
        buildingsEffectRegistry.put("DISCOUNT_FOOD_INVENTOR", (p, g, cc) -> p.setEventCostFood((int) (p.getMyTribe().stream()
                .filter(c -> c.getJobType() == JobType.INVENTOR).count() * -1)));
        buildingsEffectRegistry.put("DOUBLE_AURA_SHAMAN", (p, g, cc) -> p.setEventCostAura(p.getEventCostAura() * 2));

        buildingsEffectRegistry.put("DOUBLE_CONSTRUCTOR_AURA", (p, g, cc) -> p.getMyTribe().stream()
                .filter(c -> c.getJobType() == JobType.BUILDER).forEach(c -> p.modifyAuraPoints(c.getAura())));
        buildingsEffectRegistry.put("THREE_MAGIC", (p, g, cc) -> p.modifyMagicPoints(3));

        buildingsEffectRegistry.put("SIX_DIFFERENT_AURA", (p, g, cc) -> p.modifyAuraPoints(6 * (int) Arrays.stream(JobType.values())
                .mapToLong(job -> p.getMyTribe().stream().filter(c -> c.getJobType() == job).count())
                .min().orElse(0)));
        buildingsEffectRegistry.put("AURA_SHAMAN", (p, g, cc) -> p.modifyAuraPoints((int) (4 * (p.getMyTribe().stream().filter(c -> c.getJobType() == JobType.SHAMAN).count()))));
        buildingsEffectRegistry.put("AURA_HUNTER", (p, g, cc) -> p.modifyAuraPoints((int) (3 * (p.getMyTribe().stream().filter(c -> c.getJobType() == JobType.HUNTER).count()))));
        buildingsEffectRegistry.put("AURA_BUILDER", (p, g, cc) -> p.modifyAuraPoints((int) (4 * (p.getMyTribe().stream().filter(c -> c.getJobType() == JobType.BUILDER).count()))));
        buildingsEffectRegistry.put("AURA_ARTIST", (p, g, cc) -> p.modifyAuraPoints((int) (4 * (p.getMyTribe().stream().filter(c -> c.getJobType() == JobType.ARTIST).count()))));
        buildingsEffectRegistry.put("AURA_FARMER", (p, g, cc) -> p.modifyAuraPoints((int) (4 * (p.getMyTribe().stream().filter(c -> c.getJobType() == JobType.FARMER).count()))));
        buildingsEffectRegistry.put("AURA_INVENTOR", (p, g, cc) -> p.modifyAuraPoints((int) (2 * (p.getMyTribe().stream().filter(c -> c.getJobType() == JobType.INVENTOR).count()))));
        buildingsEffectRegistry.put("25_AURA", (p, g, cc) -> p.modifyAuraPoints(25));
        buildingsEffectRegistry.put("EXTRA_PICK", (p, g, cc) -> p.setExtraPick(true));

        // Characters
        charactersEffectRegistry.put("HUNTER", (p, g, cc) -> p.modifyFood((int) (p.getMyTribe().stream().filter(c -> c.getJobType() == JobType.HUNTER).count() + 1)));
        charactersEffectRegistry.put("BUILDER", (p, g, cc) -> p.modifyMyBuildingDiscount(cc.getLevel()));
        charactersEffectRegistry.put("SHAMAN", (p, g, cc) -> p.modifyMagicPoints(cc.getLevel()));
        charactersEffectRegistry.put("FARMER", (p, g, cc) -> p.modifyMyFoodDiscount(cc.getLevel()));

        // Event
        // HUNTER: baseFood + aura per hunter * era
        eventEffectRegistry.put("HUNTER", (p, g, ec) ->
                g.getPlayers().forEach(pl -> {
                    pl.modifyFood(ec.baseFood);
                    pl.modifyAuraPoints((int) pl.getMyTribe().stream().count() * ec.auraPerCharacter);
                })
        );

        // FARMER: ogni character costa 1 food, se non c'è food paga in aura
        eventEffectRegistry.put("FARMER", (p, g, ec) ->
                g.getPlayers().forEach(pl -> {
                    pl.getMyTribe().forEach(c -> {
                        if (pl.getFood() > 0) pl.modifyFood(-1);
                        else pl.modifyAuraPoints(-ec.auraPerCharacter);
                    });
                })
        );

        // ARTIST: penalità o bonus in aura secondo il numero di painter
        eventEffectRegistry.put("ARTIST", (p, g, ec) ->
                g.getPlayers().forEach(pl -> {
                    long painters = pl.getMyTribe().stream()
                            .filter(c -> c.getJobType() == JobType.ARTIST).count();
                    pl.modifyAuraPoints(painters <= ec.threshold ? -ec.penalty : (int) painters * ec.bonusMultiplier);
                })
        );

        // SHAMAN: bonus/malus in aura per player con più o meno aura
        eventEffectRegistry.put("SHAMAN", (p, g, ec) -> {
            int max = g.getPlayers().stream().mapToInt(Player::getAuraPoints).max().orElse(0);
            int min = g.getPlayers().stream().mapToInt(Player::getAuraPoints).min().orElse(0);
            g.getPlayers().forEach(pl -> {
                if (pl.getAuraPoints() == max) pl.modifyAuraPoints(ec.bonus);
                if (pl.getAuraPoints() == min) pl.modifyAuraPoints(-ec.malus);
            });
        });
}
        /**
         * Crea il mazzo edifici leggendo dal file specifico e filtrando per numero di giocatori.
         * @param filePath il percorso del file
         * @param numPlayers il numero di giocatori nella partita
         */
        public static Deck<BuildingCard> createBuildingDeck (String filePath,int numPlayers){

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

            Gson gson = new Gson();

            try (Reader reader = new FileReader(filePath)) {

                Type listType = new TypeToken<List<BuildingDTO>>() {
                }.getType();
                List<BuildingDTO> carteLette = gson.fromJson(reader, listType);

                for (BuildingDTO dto : carteLette) {
                    JobType type = JobType.valueOf(dto.jobType);
                    Phases phase = Phases.valueOf(dto.phase);

                    List<IEffect> effects = new ArrayList<>();
                    if (buildingsEffectRegistry.containsKey(dto.nomeLambda)) {
                        effects.add(buildingsEffectRegistry.get(dto.nomeLambda));
                    }

                    BuildingCard card = new BuildingCard(type, dto.cost, dto.finalAura, phase, effects, dto.era);
                    // Smistamento per Era (qui carichiamo TUTTE le carte del file)
                    if (dto.era == 1) e1.add(card);
                    else if (dto.era == 2) e2.add(card);
                    else if (dto.era == 3) e3.add(card);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Mischiamo tutte le carte di ogni era
            Collections.shuffle(e1);
            Collections.shuffle(e2);
            Collections.shuffle(e3);

            Stack<BuildingCard> finalDeck = new Stack<>();

            // Push in ordine inverso (Era 3, poi 2, poi 1) limitato dal numero di giocatori
            // Usiamo Math.min per evitare errori se nel file ci sono meno carte del previsto

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

            Deck<BuildingCard> deck = new Deck<>(finalDeck);

            return deck;
        }


        /**
         * Crea il mazzo Tribù unendo Personaggi ed Eventi.
         * Gestisce la logica dei Final Event in fondo al mazzo.
         */
        public static Deck<TribeCard> createTribeDeck (String charPath, String eventPath,int numPlayers){

            List<TribeCard> finalEvents = new ArrayList<>();
            List<TribeCard> era1 = new ArrayList<>();
            List<TribeCard> era2 = new ArrayList<>();
            List<TribeCard> era3 = new ArrayList<>();

            Gson gson = new Gson();

            // 1. Caricamento Personaggi (Filtrati con logica 2+, 3+, 4+, 5)
            try (Reader reader = new FileReader(charPath)) {

                Type charListType = new TypeToken<List<CharacterDTO>>() {
                }.getType();
                List<CharacterDTO> charLetti = gson.fromJson(reader, charListType);

                for (CharacterDTO dto : charLetti) {
                    // Filtro sui giocatori
                    if (numPlayers >= dto.minPlayers) {

                        JobType type = JobType.valueOf(dto.characterType);
                        Phases phase = Phases.valueOf(dto.phase);

                        List<IEffect> effects = new ArrayList<>();
                        if (charactersEffectRegistry.containsKey(dto.nomeLambda)) {
                            effects.add(charactersEffectRegistry.get(dto.nomeLambda));
                        }

                        CharacterCard card = new CharacterCard(dto.level, type, dto.aura, phase, effects, dto.era);

                        if (dto.era == 1) era1.add(card);
                        else if (dto.era == 2) era2.add(card);
                        else era3.add(card);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // 2. Caricamento Eventi (NON hanno IEffects, usano resolveEvent)
            try (Reader reader = new FileReader(eventPath)) {

                Type eventListType = new TypeToken<List<EventDTO>>() {
                }.getType();
                List<EventDTO> eventiLetti = gson.fromJson(reader, eventListType);

                for (EventDTO dto : eventiLetti) {
                    IEffect<EventCard> eff = eventEffectRegistry.get(dto.etichettaSottoclasse);
                    if (eff == null) continue;

                    EventCard event = new EventCard(dto.era, dto.finalEvent);
                    event.baseFood = dto.baseFood;
                    event.auraPerCharacter = dto.auraPerCharacter;
                    event.threshold = dto.threshold;
                    event.bonusMultiplier = dto.bonusMultiplier;
                    event.penalty = dto.penalty;
                    event.bonus = dto.bonus;
                    event.malus = dto.malus;

                    event.setEffect(g -> eff.apply(null, g, event));

                    if (dto.finalEvent) {
                        finalEvents.add(event);
                    } else {
                        if (dto.era == 1) era1.add(event);
                        else if (dto.era == 2) era2.add(event);
                        else era3.add(event);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Mischiamo tutto per blocchi
            Collections.shuffle(era1);
            Collections.shuffle(era2);
            Collections.shuffle(era3);

            Stack<TribeCard> finalDeck = new Stack<>();

            // ORDINE DI PUSH (Bottom to Top):
            // 1. Final Events (Abs bottom)
            for (TribeCard c : finalEvents) finalDeck.push(c);
            // 2. Era 3
            for (TribeCard c : era3) finalDeck.push(c);
            // 3. Era 2
            for (TribeCard c : era2) finalDeck.push(c);
            // 4. Era 1 (Top)
            for (TribeCard c : era1) finalDeck.push(c);

            Deck<TribeCard> deck = new Deck<>(finalDeck);

            return deck;
        }
    }
}