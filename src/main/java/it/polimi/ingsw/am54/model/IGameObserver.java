package it.polimi.ingsw.am54.model;

// VERRA' IMPLEMENTATA DALLE VIEW DI GUI ETC...

import java.util.List;

public interface IGameObserver {
    // Eventi di Setup
    void onPlayerJoined(Game game, Player player);
    void onTotemColorChosen(Game game, Player player, TotemColors color);

    // Eventi di Gioco
    void onTotemPlaced(Game game, Player player, int offerPosition);
    void onCardPicked(Game game, Player player, String row, int cardIndex, int newFoodBalance);
    void onTotemReturned(Game game, Player player, int position, int newFoodBalance, int newAuraBalance);
    // Eventi di Flusso (Turni e Stati)
    void onTurnChanged(Game game, Player newCurrentPlayer);
    void onStateChanged(Game game, GameState newState);

    void onEventResolved(Game game);
    // Evento Finale
    void onEndGame(Game game, List<Player> winners);
}