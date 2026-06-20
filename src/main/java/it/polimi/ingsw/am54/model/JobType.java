package it.polimi.ingsw.am54.model;

/**
 * Job types (roles) assignable to tribe characters.
 * <p>
 * Each {@link CharacterCard} has a JobType that defines its role
 * within the player's tribe. The job type determines:
 * <ul>
 *   <li>the special effect activated when the character is purchased or during game phases;</li>
 *   <li>the endgame bonuses calculated in {@link Game#calculatePlayerScore(Player)};</li>
 *   <li>compatibility with certain buildings and events that affect specific roles.</li>
 * </ul>
 * </p>
 * <p>
 * The value {@link #NULL} is a placeholder used internally to represent
 * the absence of a job type, for example before a character is assigned.
 * </p>
 *
 * @see CharacterCard
 * @see Game#getPlayerInventorsBonus(Player)
 * @see Game#getPlayerArtistsBonus(Player)
 * @see Game#getPlayerConstructorsBonus(Player)
 */
public enum JobType {
    /** Hunter: specialized in hunting, grants bonuses during hunting events. */
    HUNTER,
    /** Shaman: provides protection from events and passive aura bonuses. */
    SHAMAN,
    /** Builder: each constructor aura point contributes to the final score. */
    CONSTRUCTOR,
    /** Farmer: specialized in food production, bonuses during harvest events. */
    FARMER,
    /** Inventor: final score calculated as (number of inventors) × (unique levels owned). */
    INVENTOR,
    /** Artist: each pair of artists in the tribe is worth 10 aura points at endgame. */
    ARTIST,
    /** Null placeholder: used when no job has been assigned yet. */
    NULL
}