package it.polimi.ingsw.am54.network.server;

public interface ClientHandler {
    void sendError(String errorMessage);
    void disconnect();
}