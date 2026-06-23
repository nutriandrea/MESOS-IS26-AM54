/**
 * Lato client della rete e stato locale. {@link it.polimi.ingsw.am54.network.client.Client}
 * è l'hub singleton che custodisce la connessione attiva e il
 * {@link it.polimi.ingsw.am54.network.client.ClientModel} (copia leggera dello stato di
 * gioco aggiornata dagli Update del server).
 *
 * <p>L'astrazione {@link it.polimi.ingsw.am54.network.client.ServerConnection} ha due
 * implementazioni ({@link it.polimi.ingsw.am54.network.client.SocketServerConnection} e
 * {@link it.polimi.ingsw.am54.network.client.RMIServerConnection});
 * {@link it.polimi.ingsw.am54.network.client.VirtualServer} è la facciata che la View usa
 * per inviare azioni, recuperando sempre la connessione corrente (così le mosse
 * sopravvivono a una riconnessione automatica). {@code CLIApp} è l'interfaccia testuale.</p>
 */
package it.polimi.ingsw.am54.network.client;
