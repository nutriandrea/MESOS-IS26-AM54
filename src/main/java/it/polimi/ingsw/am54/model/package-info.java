/**
 * Game domain model (the "M" in MVC): rules, entities and game state.
 *
 * <p>The core is {@link it.polimi.ingsw.am54.model.Game}, orchestrator of the state
 * machine ({@link it.polimi.ingsw.am54.model.GameState} and the
 * {@link it.polimi.ingsw.am54.model.states} package), of the turn system, of the decks
 * ({@link it.polimi.ingsw.am54.model.Deck}, {@link it.polimi.ingsw.am54.model.DeckFactory})
 * and of the endgame scoring.</p>
 *
 * <p>The main entities are {@link it.polimi.ingsw.am54.model.Player},
 * {@link it.polimi.ingsw.am54.model.Board} and the card hierarchy
 * ({@link it.polimi.ingsw.am54.model.Card}, {@link it.polimi.ingsw.am54.model.CharacterCard},
 * {@link it.polimi.ingsw.am54.model.BuildingCard}, {@link it.polimi.ingsw.am54.model.EventCard}).
 * The card effects are encapsulated in {@link it.polimi.ingsw.am54.model.IEffect}.</p>
 */
package it.polimi.ingsw.am54.model;