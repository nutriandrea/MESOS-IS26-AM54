/**
 * Modello di dominio del gioco (la "M" di MVC): regole, entità e stato della partita.
 *
 * <p>Il cuore è {@link it.polimi.ingsw.am54.model.Game}, orchestratore della macchina a
 * stati ({@link it.polimi.ingsw.am54.model.GameState} e il package
 * {@link it.polimi.ingsw.am54.model.states}), della turnazione, dei mazzi
 * ({@link it.polimi.ingsw.am54.model.Deck}, {@link it.polimi.ingsw.am54.model.DeckFactory})
 * e dello scoring di fine partita.</p>
 *
 * <p>Le entità principali sono {@link it.polimi.ingsw.am54.model.Player},
 * {@link it.polimi.ingsw.am54.model.Board} e la gerarchia delle carte
 * ({@link it.polimi.ingsw.am54.model.Card}, {@link it.polimi.ingsw.am54.model.CharacterCard},
 * {@link it.polimi.ingsw.am54.model.BuildingCard}, {@link it.polimi.ingsw.am54.model.EventCard}).
 * Gli effetti delle carte sono incapsulati in {@link it.polimi.ingsw.am54.model.IEffect}.</p>
 */
package it.polimi.ingsw.am54.model;
