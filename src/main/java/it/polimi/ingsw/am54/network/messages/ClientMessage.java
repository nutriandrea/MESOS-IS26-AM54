package it.polimi.ingsw.am54.network.messages;

import java.io.Serializable;

/**
 * Root interface (Marker Interface) for all messages traveling
 * over the network between Client and Server.
 * <p>
 * Extends {@link Serializable} to ensure every message can be
 * serialized and sent through Java object streams (ObjectOutputStream).
 * Acting as a type "label", allows the typing system to distinguish
 * legitimate messages from other objects that might travel on the network.
 * <p>
 * <b>Gerarchia:</b><br>
 * {@code ClientMessage} (marcatore serializzabile)<br>
 * ├── {@link Request} — messaggi Client → Server (azioni di gioco)<br>
 * └── {@link Update} — messaggi Server → Client (notifiche di stato)<br>
 * <p>
 * Contains no methods of its own: behavior is delegated to sub-interfaces
 * {@link Request#execute} and {@link Update#applyTo}.
 */
public interface ClientMessage extends Serializable {
    // Marker interface: no methods required.
    // All subclasses are automatically Serializable to travel via socket.
}