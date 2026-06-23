/**
 * Lato server della rete. {@link it.polimi.ingsw.am54.network.server.Server} avvia i due
 * trasporti (Socket TCP su 12345 e RMI su 1099); gli handler per-client
 * ({@link it.polimi.ingsw.am54.network.server.SocketClientHandler},
 * {@link it.polimi.ingsw.am54.network.server.RMIClientHandler}) implementano l'astrazione
 * {@link it.polimi.ingsw.am54.network.server.ClientConnection}.
 *
 * <p>{@link it.polimi.ingsw.am54.network.server.VirtualView} osserva il modello e traduce
 * ogni evento in un messaggio di rete verso il proprio client;
 * {@link it.polimi.ingsw.am54.network.server.PersistenceManager} salva/ripristina la
 * partita su file JSON per il recupero da crash.</p>
 */
package it.polimi.ingsw.am54.network.server;
