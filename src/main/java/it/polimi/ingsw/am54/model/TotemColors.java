package it.polimi.ingsw.am54.model;

/**
 * Available colors for player totems.
 * <p>
 * Each player chooses a unique color at the start of the game during the setup phase.
 * The totem is used to mark the player's position on the offering track
 * and to visually identify the owner of each turn order slot.
 * The five colors support games from 2 to 5 players; in games with fewer than 5 participants
 * some colors simply remain unused.
 * </p>
 *
 * @see Player#setTotemColor(TotemColors)
 * @see OfferingTile#occupy(Player)
 */
public enum TotemColors {
    /** Rosso. */
    RED,
    /** Giallo. */
    YELLOW,
    /** Nero. */
    BLACK,
    /** Bianco. */
    WHITE,
    /** Blu. */
    BLUE
}