package it.polimi.ingsw.am54.network.client;

import java.util.ArrayList;
import java.util.List;

public class EventReport {
    public String eventName;
    public List<PlayerDelta> playerDeltas = new ArrayList<>();
}