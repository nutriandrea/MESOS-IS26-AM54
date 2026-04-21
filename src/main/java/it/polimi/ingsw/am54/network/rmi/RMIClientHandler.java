package it.polimi.ingsw.am54.network.rmi;

import it.polimi.ingsw.am54.network.server.ClientHandler;
import it.polimi.ingsw.am54.network.messages.Update;

public class RMIClientHandler implements ClientHandler {
    private final String nickname;

    public RMIClientHandler(String nickname) {
        this.nickname = nickname;
    }

    @Override
    public void sendMessage(Update message) {
        System.out.println("[RMI] Sending to " + nickname + ": " + message);
    }

    @Override
    public void sendError(String errorMessage) {
        System.err.println("[RMI] Error to " + nickname + ": " + errorMessage);
    }

    @Override
    public void disconnect() {
        System.out.println("[RMI] Client disconnected: " + nickname);
    }
}