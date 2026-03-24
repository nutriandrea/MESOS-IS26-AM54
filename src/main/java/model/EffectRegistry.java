package model;

import java.util.Arrays;
import java.util.List;

public class EffectRegistry {
    /* buildingsEffectRegistry.put("DOUBLE_INVENTOR", (p, g, edificio) -> {
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
        buildingsEffectRegistry.put("NO_LOSS", (p, g, cc) -> {});
    //era2
        buildingsEffectRegistry.put("PLUS_HUNTING", (p, g, cc) -> {
        p.setEventCostFood((int) (p.getMyTribe().stream().filter(c -> c.getJobType() == JobType.HUNTER).count()));
        p.setEventCostAura((int) (p.getMyTribe().stream().filter(c -> c.getJobType() == JobType.HUNTER).count()));
    });
        buildingsEffectRegistry.put("PLUS_FOOD_ARTIST", (p, g, cc) -> p.setEventCostFood((int) (p.getMyTribe().stream().filter(c -> c.getJobType() == JobType.ARTIST).count())));
        buildingsEffectRegistry.put("DISCOUNT_FOOD_INVENTOR", (p, g, cc) -> p.setEventCostFood((int) (p.getMyTribe().stream()
                .filter(c -> c.getJobType() == JobType.INVENTOR).count() * -1)));
        buildingsEffectRegistry.put("DOUBLE_AURA_SHAMAN", (p, g, cc) -> {});

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
        long cacciatori = pl.getMyTribe().stream()
                .filter(c -> c.getJobType() == JobType.HUNTER).count();

        //prendo gli attributi dati dai building per i plus degli eventi
        int buildingFood = pl.getEventCostFood();
        int buildingAura = pl.getEventCostAura();

        // Usa ec.baseFood moltiplicato per i cacciatori (così il JSON comanda tutto!)
        pl.modifyFood((int) cacciatori * ec.getBaseFood() + buildingFood);

        // I Punti Prestigio (Aura)
        pl.modifyAuraPoints((int) cacciatori * ec.getAuraPerCharacter() + buildingAura);

        //resetto gli attributi, per il prossimo evento
        pl.resetEventAttributes();
    })
            );

    // FARMER: ogni character costa 1 food, se non c'è food paga in aura
        eventEffectRegistry.put("FARMER", (p, g, ec) ->
            g.getPlayers().forEach(pl -> {
        //vedo quanto cibo ha in totale
        int playerMaxFood = pl.getFood();
        //conto quant'è lo sconto dei building (n.b. sconto sarà negativo)
        int sconto = pl.getEventCostFood();

        //conto quanto ne deve pagare
        int costo = pl.getMyTribe().size() + sconto;

        if(costo>0){
            if(costo>playerMaxFood){
                pl.modifyFood(-playerMaxFood);
                pl.modifyAuraPoints((playerMaxFood-costo)*ec.getAuraPerCharacter());
            }
            else{
                pl.modifyFood(-costo);
            }
        }
        pl.resetEventAttributes();

    })
            );

    // ARTIST: penalità o bonus in aura secondo il numero di painter
        eventEffectRegistry.put("ARTIST", (p, g, ec) ->
            g.getPlayers().forEach(pl -> {
        long painters = pl.getMyTribe().stream()
                .filter(c -> c.getJobType() == JobType.ARTIST).count();
        pl.modifyAuraPoints(painters <= ec.getThreshold()
                ? -ec.getPenalty()
                : (int) painters * ec.getBonusMultiplier());

        //controllo se c'è il building che da anche cibo durante questo evento:
        int extraFood = pl.getEventCostFood();
        if(extraFood>0) pl.modifyFood(extraFood);

        pl.resetEventAttributes();
    })
            );

    // SHAMAN: bonus/malus in aura per player con più o meno aura
        eventEffectRegistry.put("SHAMAN", (p, g, ec) -> {
        int max = g.getPlayers().stream().mapToInt(Player::getAuraPoints).max().orElse(0);
        int min = g.getPlayers().stream().mapToInt(Player::getAuraPoints).min().orElse(0);

        // dato che non salvo il nome della lambda, prendo dal registro i riferimenti originali degli effetti
        IEffect<BuildingCard> noLossEffect = buildingsEffectRegistry.get("NO_LOSS");
        IEffect<BuildingCard> doubleAuraEffect = buildingsEffectRegistry.get("DOUBLE_AURA_SHAMAN");

        g.getPlayers().forEach(pl -> {

            boolean isZero = pl.getBuildings().stream()
                    .anyMatch(b -> b.getEffects().contains(noLossEffect));
            boolean isDouble = pl.getBuildings().stream()
                    .anyMatch(b -> b.getEffects().contains(doubleAuraEffect));

            if (pl.getAuraPoints() == max) {
                if (isDouble)
                    pl.modifyAuraPoints(2 * ec.getBonus());
                else pl.modifyAuraPoints(ec.getBonus());
            }
            if (pl.getAuraPoints() == min) {
                if (!isZero) pl.modifyAuraPoints(-ec.getMalus());
            }
            pl.resetEventAttributes();
        });
    });
     */
}
