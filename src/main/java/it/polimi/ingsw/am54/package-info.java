/**
 * Root of the <b>Mesos</b> project (group AM54), a multiplayer board game
 * developed for the Software Engineering Final Project.
 *
 * <p>Contains the executable entry points:</p>
 * <ul>
 * <li>{@link it.polimi.ingsw.am54.MainLauncher} / {@link it.polimi.ingsw.am54.Launcher}
 * — client startup (GUI or CLI choice);</li>
 * <li>{@link it.polimi.ingsw.am54.network.server.Server} — server startup;</li>
 * <li>{@link it.polimi.ingsw.am54.MainClient} — direct startup of the client in textual mode.</li>
 * </ul>
 *
 * <p>The architecture follows the <b>MVC</b> pattern with <b>State Pattern</b> for the game
 * phases, <b>Observer</b> for update propagation and a dual transport
 * layer (TCP Socket and RMI).</p>
 */
package it.polimi.ingsw.am54;