/**
 * Concrete states of the game's state machine (State Pattern).
 *
 * <p>Each state extends {@link it.polimi.ingsw.am54.model.GameState} and implements only
 * the actions allowed in its own phase, throwing an exception for the others:</p>
 * <ul>
 * <li>{@link it.polimi.ingsw.am54.model.states.SetUpGameState} — lobby: player joining,
 * color selection and game start;</li>
 * <li>{@link it.polimi.ingsw.am54.model.states.StartTurnState} — totem placement on the
 * offering tiles;</li>
 * <li>{@link it.polimi.ingsw.am54.model.states.PickingState} — card picking;</li>
 * <li>{@link it.polimi.ingsw.am54.model.states.EventState} — automatic resolution of the
 * end-of-round events;</li>
 * <li>{@link it.polimi.ingsw.am54.model.states.EndGameState} — final resolution and winner
 * proclamation.</li>
 * </ul>
 */
package it.polimi.ingsw.am54.model.states;