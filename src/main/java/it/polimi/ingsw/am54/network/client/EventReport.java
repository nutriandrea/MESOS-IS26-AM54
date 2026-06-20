package it.polimi.ingsw.am54.network.client;

import java.util.ArrayList;
import java.util.List;

/**
 * Container for the report of an event resolved on the client side.
 * Used by GUI/CLI to show resource variations
 * (food, aura, magic) of players after an event.
 */
public class EventReport {
    /** Human-readable name of the resolved event */
    public String eventName;
    /** List of per-player resource deltas produced by the event */
    public List<PlayerDelta> playerDeltas = new ArrayList<>();
}