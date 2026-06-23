/**
 * Client side of the network and local state. {@link it.polimi.ingsw.am54.network.client.Client}
 * is the singleton hub that guards the active connection and the
 * {@link it.polimi.ingsw.am54.network.client.ClientModel} (lightweight copy of the game
 * state updated by server Updates).
 *
 * <p>The {@link it.polimi.ingsw.am54.network.client.ServerConnection} abstraction has two
 * implementations ({@link it.polimi.ingsw.am54.network.client.SocketServerConnection} and
 * {@link it.polimi.ingsw.am54.network.client.RMIServerConnection});
 * {@link it.polimi.ingsw.am54.network.client.VirtualServer} is the facade that the View uses
 * to send actions, always retrieving the current connection (so moves
 * survive an automatic reconnection). {@code CLIApp} is the textual interface.</p>
 */
package it.polimi.ingsw.am54.network.client;