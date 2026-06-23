/**
 * Network messages exchanged between client and server, with polymorphic dispatch.
 *
 * <p>All implement the marker interface
 * {@link it.polimi.ingsw.am54.network.messages.ClientMessage}. The two {@code sealed} sub-hierarchies
 * are:</p>
 * <ul>
 *   <li>{@link it.polimi.ingsw.am54.network.messages.Request} (client → server): provides
 *       {@code execute(Controller, String, ClientConnection)};</li>
 *   <li>{@link it.polimi.ingsw.am54.network.messages.Update} (server → client): provides
 *       {@code applyTo(ClientModel)}.</li>
 * </ul>
 * This way, no {@code switch}/{@code instanceof} are needed in the network loops.
 */
package it.polimi.ingsw.am54.network.messages;