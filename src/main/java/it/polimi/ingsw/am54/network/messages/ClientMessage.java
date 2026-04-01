package it.polimi.ingsw.am54.network.messages;

import java.io.Serializable;

public interface ClientMessage extends Serializable {
    String getNickname();
}
