/**
 * Server-side controller (the "C" in MVC). {@link it.polimi.ingsw.am54.controller.Controller}
 * receives client requests, serializes them using a single monitor, and forwards them to the
 * {@link it.polimi.ingsw.am54.model.Game} model.
 *
 * <p>It also manages the connection lifecycle: joins/reconnections,
 * temporary disconnections, game suspension when only one active player remains,
 * victory by abandonment, persistence for crash recovery, and saving
 * match results to the database.</p>
 */
package it.polimi.ingsw.am54.controller;