package it.polimi.ingsw.am54.network.client;

/**
 * Change in a player's resources after an event.
 * Contains pre-event values and the applied differences (diff).
 */
public class PlayerDelta {
    /** Nickname of the affected player */
    public String nickname;
    /** Resource values before the event */
    public int oldFood, oldAura, oldMagic;
    /** Differences applied by the event (new - old) */
    public int diffFood, diffAura, diffMagic;
}