/**
 * Messaggi di rete scambiati tra client e server, con dispatch polimorfico.
 *
 * <p>Tutti implementano l'interfaccia marcatore
 * {@link it.polimi.ingsw.am54.network.messages.ClientMessage}. Le due sottogerarchie
 * {@code sealed} sono:</p>
 * <ul>
 *   <li>{@link it.polimi.ingsw.am54.network.messages.Request} (client → server): porta
 *       {@code execute(Controller, String, ClientConnection)};</li>
 *   <li>{@link it.polimi.ingsw.am54.network.messages.Update} (server → client): porta
 *       {@code applyTo(ClientModel)}.</li>
 * </ul>
 * In questo modo non servono {@code switch}/{@code instanceof} nei loop di rete.
 */
package it.polimi.ingsw.am54.network.messages;
