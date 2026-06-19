package it.polimi.ingsw.am54.network.client;

import java.util.ArrayList;
import java.util.List;

/**
 * Contenitore per il report di un evento risolto lato client.
 * Usato dalla GUI/CLI per mostrare le variazioni di risorse
 * (cibo, aura, magia) dei giocatori dopo un evento.
 */
public class EventReport {
    public String eventName;
    public List<PlayerDelta> playerDeltas = new ArrayList<>();
}