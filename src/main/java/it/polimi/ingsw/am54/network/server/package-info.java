/**
 * Server side of the network. {@link it.polimi.ingsw.am54.network.server.Server} starts the two
 * transports (TCP Socket on 12345 and RMI on 1099); the per-client handlers
 * ({@link it.polimi.ingsw.am54.network.server.SocketClientHandler},
 * {@link it.polimi.ingsw.am54.network.server.RMIClientHandler}) implement the abstraction
 * {@link it.polimi.ingsw.am54.network.server.ClientConnection}.
 *
 * <p>{@link it.polimi.ingsw.am54.network.server.VirtualView} observes the model and translates
 * each event into a network message to its own client;
 * {@link it.polimi.ingsw.am54.network.server.PersistenceManager} saves/restores the
 * game on JSON files for crash recovery.</p>
 */
package it.polimi.ingsw.am54.network.server;