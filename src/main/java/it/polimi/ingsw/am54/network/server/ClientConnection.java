package it.polimi.ingsw.am54.network.server;

import it.polimi.ingsw.am54.network.messages.Update;

public interface ClientConnection {
    void sendUpdate(Update update);
    void disconnect();
}