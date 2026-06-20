package it.polimi.ingsw.am54.model;

/**
 * Functional interface for defining game card effects.
 * <p>
 * Each concrete effect implements one or more default methods of this interface
 * to react to specific moments in the game flow. The interface is generic
 * on the card type to which the effect is associated ({@code T extends Card}), allowing
 * creating specialized effects for {@link BuildingCard}, {@link CharacterCard}
 * and {@link EventCard}.
 * </p>
 * <p>
 * All methods are {@code default} and do nothing by default, so
 * a concrete implementation only needs to override the methods corresponding
 * to the phases in which it must activate. This avoids having to implement empty methods
 * when an effect acts in only one phase.
 * </p>
 * <p>
 * The activation phases, in chronological order within a turn, are:
 * <ol>
 *   <li>{@link #applyOnPurchase(Player, Game, Card)} — card purchase/draw;</li>
 *   <li>{@link #applyOnCharacterPicked(Player, Game, Card, CharacterCard)} — aggiunta
 *       of a character to the tribe (passive activation);</li>
 *   <li>{@link #applyOnTurnEnd(Player, Game, Card)} — end of the player's turn;</li>
 *   <li>{@link #applyOnEvent(Player, Game, Card, EventCard)} — global event resolution;</li>
 *   <li>{@link #applyOnEndGame(Player, Game, Card)} — final score calculation.</li>
 * </ol>
 * </p>
 *
 * @param <T> the card type to which the effect is associated (extension of {@link Card})
 *
 * @see BuildingCard
 * @see CharacterCard
 * @see EventCard
 * @see Phases
 */
public interface IEffect<T extends Card> {

    /**
     * Purchase phase: activated instantly when the card is bought or drawn.
     * <p>
     * Used for immediate effects such as obtaining resources ({@code food}, {@code aura},
     * {@code magicPoints}), discounts on future purchases or extra draws. Called
     * within the method {@link Card#pick(Player, Game)} before the card is
     * actually assigned to the player.
     * </p>
     *
     * @param p    the player who bought/drew the card
     * @param g    the current game instance, to access board, decks and other players
     * @param card the card that was purchased (source of the effect)
     */
    default void applyOnPurchase(Player p, Game g, T card) {
        // By default does nothing
    }

    /**
     * End of turn phase: activated when the player returns the totem
     * and receives the bonus from the turn order track ({@link TurnOrderSlot}).
     * <p>
     * Used for effects that resolve at the end of the player's turn,
     * like most building effects that produce resources
     * or modify the player's state after the main action.
     * </p>
     *
     * @param p    the player ending their turn
     * @param g    the current game instance
     * @param card the card associated with the effect
     */
    default void applyOnTurnEnd(Player p, Game g, T card) {
        // By default does nothing
    }

    /**
     * Event phase: activated immediately before resolving a global Event card.
     * <p>
     * Used for effects that modify the cost of events for the player
     * (e.g. {@code Shaman} protects from aura loss) or alter the
     * multiplier of the current event's bonus/penalty.
     * </p>
     *
     * @param p             the player on whom the event will be resolved
     * @param g             the current game instance
     * @param card          the card associated with the effect
     * @param currentEvent  the global Event card about to be resolved
     */
    default void applyOnEvent(Player p, Game g, T card, EventCard currentEvent) {
        // By default does nothing
    }

    /**
     * Endgame phase: activated during final victory point calculation.
     * <p>
     * Used for effects that grant additional Aura points based on conditions
     * verified only at endgame, such as owning six buildings of different types
     * ({@code BuildingSixDifferentAura}) or six buildings of the same type
     * ({@code BuildingSixOfType}).
     * </p>
     *
     * @param p    the player whose final points are being calculated
     * @param g    the current game instance
     * @param card the card associated with the effect
     */
    default void applyOnEndGame(Player p, Game g, T card) {
        // By default does nothing
    }

    /**
     * Passive phase: activated every time a Character is added to the player's tribe.
     * <p>
     * Used for effects that react to the tribe composition, such as
     * the passive Shaman bonus on buildings or the building bonuses that
     * count the number of characters of a certain type in the tribe.
     * </p>
     *
     * @param p             the player who owns the tribe
     * @param g             the current game instance
     * @param card          the card associated with the effect (typically a building with passive activation)
     * @param newCharacter  the character that was just added to the tribe
     */
    default void applyOnCharacterPicked(Player p, Game g, T card, CharacterCard newCharacter) {
        // By default does nothing
    }
}