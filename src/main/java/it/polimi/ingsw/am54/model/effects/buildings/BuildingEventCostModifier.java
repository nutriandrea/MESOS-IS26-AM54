package it.polimi.ingsw.am54.model.effects.buildings;
import com.fasterxml.jackson.databind.JsonNode;
import it.polimi.ingsw.am54.model.*;

// Gestisce le 3 carte di Era 1 e 2 che danno sconti sul cibo durante l'Evento Sostentamento (per Artisti, Inventori e Raccoglitori)
// e le 2 carte che alterano gli eventi Caccia e Pitture Rupestri
// aggiungendo cibo o aura extra
public class BuildingEventCostModifier implements IEffect<BuildingCard> {
    private final JobType jobType;
    private final int foodDiscountMultiplier;
    private final boolean overrideFoodCost;
    private final boolean overrideAuraCost;

    public BuildingEventCostModifier(JsonNode params) {
        this.jobType = params != null && params.has("jobType") ? JobType.valueOf(params.get("jobType").asText()) : null;
        this.foodDiscountMultiplier = params != null && params.has("foodDiscountMultiplier") ? params.get("foodDiscountMultiplier").asInt() : 0;
        this.overrideFoodCost = params != null && params.has("overrideFoodCost") && params.get("overrideFoodCost").asBoolean();
        this.overrideAuraCost = params != null && params.has("overrideAuraCost") && params.get("overrideAuraCost").asBoolean();
    }

    // FIRMA AGGIORNATA PER LA FASE EVENTI
    @Override
    public void applyOnEvent(Player p, Game g, BuildingCard card, EventCard currentEvent) {
        if (jobType == null || currentEvent == null) return;

        // Recuperiamo il nome dell'evento in corso (es. "FARMER - ERA: 1")
        String eventName = currentEvent.getName() != null ? currentEvent.getName().toUpperCase() : "";
        long count = p.getMyTribe().stream().filter(c -> c.getJobType() == jobType).count();

        // 1. Sconti per l'Evento Sostentamento (FARMER)
        // Questi sconti usano sempre un moltiplicatore negativo nel JSON.
        // Caso speciale: Sostentamento (Legame arbitrario Edificio <-> Evento) quindi uso "FARMER"
        if (foodDiscountMultiplier < 0 && eventName.contains("FARMER")) {
            p.setEventCostFood(p.getEventCostFood() + (int) (count * foodDiscountMultiplier));
        }

        // 2. Bonus di Cibo per Eventi specifici (es. Caccia, Pitture Rupestri)
        // L'edificio reagisce SOLO se il nome dell'evento corrisponde al suo JobType (HUNTER o ARTIST)
        if ((overrideFoodCost || foodDiscountMultiplier > 0) && eventName.contains(jobType.name())) {
            int multi = foodDiscountMultiplier > 0 ? foodDiscountMultiplier : 1;
            p.setEventCostFood(p.getEventCostFood() + (int) (count * multi));
        }

        // 3. Bonus di Punti Aura (Es. Edificio del Cacciatore)
        if (overrideAuraCost && eventName.contains(jobType.name())) {
            p.setEventCostAura(p.getEventCostAura() + (int) count);
        }
    }
}