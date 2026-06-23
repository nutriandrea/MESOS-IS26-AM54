/**
 * RMI remote interfaces. {@link it.polimi.ingsw.am54.network.rmi.RMIGameServer} is the entry
 * point in the registry; {@link it.polimi.ingsw.am54.network.rmi.RMIClientHandlerInterface}
 * exposes the client's game actions to the server, while
 * {@link it.polimi.ingsw.am54.network.rmi.RMIClientInterface} is the callback stub with which
 * the server delivers updates to the client.
 */
package it.polimi.ingsw.am54.network.rmi;