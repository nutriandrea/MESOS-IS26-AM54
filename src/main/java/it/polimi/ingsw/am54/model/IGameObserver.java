package it.polimi.ingsw.am54.model;

import java.util.List;

public interface IGameObserver {
    // Setup Events
    void onPlayerJoined(Game game, Player player);
    void onTotemColorChosen(Game game, Player player, TotemColors color);

    // Game Events
    void onTotemPlaced(Game game, Player player, int offerPosition);
    void onCardPicked(Game game, Player player, String row, int cardIndex, int newFoodBalance,  int newMagicBalance);
    void onTotemReturned(Game game, Player player, int position, int newFoodBalance, int newAuraBalance);
    // Flow Events (Turns and States)
    void onTurnChanged(Game game, Player newCurrentPlayer);
    void onStateChanged(Game game, GameState newState);

    void onEventResolved(Game game, EventCard eventCard);
    // Final Event
    void onEndGame(Game game, List<Player> winners);
}