package it.polimi.ingsw.am54.network.rmi;

import it.polimi.ingsw.am54.controller.Controller;
import it.polimi.ingsw.am54.model.Game;
import it.polimi.ingsw.am54.network.server.ClientHandler;
import it.polimi.ingsw.am54.network.server.VirtualView;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

public class RMIServerImplementation extends UnicastRemoteObject implements RMIServerInterface {
    private final Game game;
    private final Controller controller;

    public RMIServerImplementation(Game game, Controller controller) throws RemoteException {
        super();
        this.game = game;
        this.controller = controller;
    }

    @Override
    public String connect(String nickname) throws RemoteException {
        return "CONNECTED:" + nickname;
    }

    @Override
    public void chooseTotemColor(String nickname, String color) throws RemoteException {
        controller.handleChooseTotemColor(nickname, color);
    }

    @Override
    public void setupGame(String nickname) throws RemoteException {
        controller.handleSetupGame(nickname);
    }

    @Override
    public void placeTotem(String nickname, int offerPosition) throws RemoteException {
        controller.handlePlaceTotem(nickname, offerPosition);
    }

    @Override
    public void pickCard(String nickname, String row, int cardIndex) throws RemoteException {
        controller.handlePickCard(nickname, row, cardIndex);
    }

    @Override
    public void reconnect(String nickname) throws RemoteException {
        controller.handleReconnect(nickname, new RMIClientHandler(nickname));
    }

    @Override
    public void disconnect(String nickname) throws RemoteException {
        controller.removeClient(nickname);
    }

    @Override
    public String[] getConnectedPlayers() throws RemoteException {
        return game.getPlayers().stream()
                .map(p -> p.getNickname())
                .toArray(String[]::new);
    }

    @Override
    public String getGameState() throws RemoteException {
        return game.getCurrentState().getClass().getSimpleName();
    }

    @Override
    public String getCurrentPlayer() throws RemoteException {
        return game.getCurrentPlayer() != null ? game.getCurrentPlayer().getNickname() : null;
    }
}