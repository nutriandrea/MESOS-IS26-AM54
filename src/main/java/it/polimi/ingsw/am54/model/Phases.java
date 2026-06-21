package it.polimi.ingsw.am54.model;

/**
 * Phases of the game turn in which card effects are activated.
 * <p>
 * Each constant represents a precise moment in the game flow and is used
 * by the {@link IEffect} interface to determine when a given effect should
 * be applied. Concrete effect implementations check this enum to
 * execute the appropriate logic at the right time.
 * </p>
 * <p>
 * The logical execution order of phases within a turn is:
 * <ol>
 *   <li>{@link #STARTTURN} — at the start of the player's turn;</li>
 *   <li>{@link #POSTPICK} — immediately after drawing a card;</li>
 *   <li>{@link #POSTACTION} — after the totem return and bonus assignment;</li>
 *   <li>{@link #EVENT} — during global Event card resolution;</li>
 *   <li>{@link #ENDGAME} — at the end of the game, for final score calculation.</li>
 * </ol>
 * </p>
 *
 * @see IEffect
 * @see Card#pick(Player, Game)
 */
public enum Phases {
    /** Start of turn phase: activated when a player becomes the current one. */
    STARTTURN,
    /** Post-pick phase: activated right after a card has been bought or drawn. */
    POSTPICK,
    /** Phase following the main action: activated after returning the totem and obtaining the bonus. */
    POSTACTION,
    /** Event resolution phase: activated during global Event card handling. */
    EVENT,
    /** Endgame phase: activated during final score calculation. */
    ENDGAME
}