package it.polimi.ingsw.am54.model.effects.buildings;

import com.fasterxml.jackson.databind.JsonNode;
import it.polimi.ingsw.am54.model.*;

//abbiamo unito event e start turn dato che sono sempre modificatori passivi o bonus diretti calcolati in base ai membri della tribù
public class EventPhaseEffect implements IEffect<BuildingCard> {

    private final String effectName;
    private final JobType jobType;
    private final int multiplier;
    private final boolean food;
    private final boolean aura;

    public EventPhaseEffect(String effectName, JsonNode params) {
        this.effectName = effectName;

        if (params == null) {
            this.jobType = null;
            this.multiplier = 0;
            this.food = false;
            this.aura = false;
            return;
        }

        this.jobType = params.has("jobType")
                ? JobType.valueOf(params.get("jobType").asText()) : null;

        this.multiplier = params.has("multiplier")
                ? params.get("multiplier").asInt() : 0;

        this.food = params.has("food")
                ? params.get("food").asBoolean() : false;

        this.aura = params.has("aura")
                ? params.get("aura").asBoolean() : false;
    }

    public String getEffectName() {
        return effectName;
    }

    @Override
    public boolean isNoLossEffect() {
        return "NO_LOSS".equals(effectName);
    }

    @Override
    public boolean isDoubleAuraShamanEffect() {
        return "DOUBLE_AURA_SHAMAN".equals(effectName);
    }

    @Override
    public void apply(Player p, Game g, BuildingCard card) {
        switch (effectName) {
            case "EVENT_FOOD_DISCOUNT" -> {
                long count = p.getMyTribe().stream()
                        .filter(c -> c.getJobType() == jobType).count();
                p.setEventCostFood((int) (count * multiplier));
            }
            case "EVENT_BONUS" -> {
                long count = p.getMyTribe().stream()
                        .filter(c -> c.getJobType() == jobType).count();
                if (food) p.setEventCostFood((int) count);
                if (aura) p.setEventCostAura((int) count);
            }
            case "BONUS_FOOD" -> {
                //con il riferimento a game cerchiamo su quale slot è messo il giocatore
                TurnOrderSlot playerSlot = g.getBoard().getTurnOrderTile().stream()
                        .filter(slot -> slot.getOccupiedBy() != null && slot.getOccupiedBy().equals(p))
                        .findFirst()
                        .orElse(null);

                // Se lo troviamo e quello slot fornisce effettivamente del cibo (> 0), glielo diamo
                if (playerSlot != null && playerSlot.getBonusFood() > 0) {
                    p.modifyFood(1);
                }
            }
            case "NO_LOSS" -> { /* marker: controllato dall'evento SHAMAN */ }
            case "DOUBLE_AURA_SHAMAN" -> { /* marker: controllato dall'evento SHAMAN */ }
        }
    }
}
