/**
 * Stati concreti della macchina a stati del gioco (State Pattern).
 *
 * <p>Ogni stato estende {@link it.polimi.ingsw.am54.model.GameState} e implementa solo
 * le azioni lecite nella propria fase, lanciando eccezione per le altre:</p>
 * <ul>
 *   <li>{@link it.polimi.ingsw.am54.model.states.SetUpGameState} — lobby: ingresso giocatori,
 *       scelta colore e avvio partita;</li>
 *   <li>{@link it.polimi.ingsw.am54.model.states.StartTurnState} — piazzamento dei totem sulle
 *       offering tile;</li>
 *   <li>{@link it.polimi.ingsw.am54.model.states.PickingState} — pesca delle carte;</li>
 *   <li>{@link it.polimi.ingsw.am54.model.states.EventState} — risoluzione automatica degli eventi
 *       di fine round;</li>
 *   <li>{@link it.polimi.ingsw.am54.model.states.EndGameState} — risoluzione finale e proclamazione
 *       dei vincitori.</li>
 * </ul>
 */
package it.polimi.ingsw.am54.model.states;
