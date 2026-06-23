/**
 * Interfacce remote RMI. {@link it.polimi.ingsw.am54.network.rmi.RMIGameServer} è il punto
 * d'ingresso nel registry; {@link it.polimi.ingsw.am54.network.rmi.RMIClientHandlerInterface}
 * espone le azioni di gioco del client verso il server, mentre
 * {@link it.polimi.ingsw.am54.network.rmi.RMIClientInterface} è lo stub di callback con cui
 * il server consegna gli aggiornamenti al client.
 */
package it.polimi.ingsw.am54.network.rmi;
