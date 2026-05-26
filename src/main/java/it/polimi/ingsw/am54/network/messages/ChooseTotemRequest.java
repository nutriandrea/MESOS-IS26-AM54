package it.polimi.ingsw.am54.network.messages;

import it.polimi.ingsw.am54.model.TotemColors;

/**
 * Richiesta inviata dal Client per scegliere il colore del proprio Totem.
 */
public class ChooseTotemRequest implements Request {

    // Niente nickname! Solo la scelta nuda e cruda.
    private final TotemColors color;

    public ChooseTotemRequest(TotemColors color) {
        this.color = color;
    }

    public TotemColors getColor() {
        return color;
    }

    public static final String TYPE = "choose_totem";
    @Override public String getType() { return TYPE; }
}