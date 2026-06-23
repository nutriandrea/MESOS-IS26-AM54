/**
 * Radice del progetto <b>Mesos</b> (gruppo AM54), gioco da tavolo multiplayer
 * realizzato per la Prova Finale di Ingegneria del Software.
 *
 * <p>Contiene i punti d'ingresso eseguibili:</p>
 * <ul>
 *   <li>{@link it.polimi.ingsw.am54.MainLauncher} / {@link it.polimi.ingsw.am54.Launcher}
 *       — avvio del client (scelta GUI o CLI);</li>
 *   <li>{@link it.polimi.ingsw.am54.network.server.Server} — avvio del server;</li>
 *   <li>{@link it.polimi.ingsw.am54.MainClient} — avvio diretto del client in modalità testuale.</li>
 * </ul>
 *
 * <p>L'architettura segue il pattern <b>MVC</b> con <b>State Pattern</b> per le fasi di
 * gioco, <b>Observer</b> per la propagazione degli aggiornamenti e un doppio strato di
 * trasporto (Socket TCP e RMI).</p>
 */
package it.polimi.ingsw.am54;
