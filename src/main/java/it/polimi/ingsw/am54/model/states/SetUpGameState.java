package it.polimi.ingsw.am54.model.states;

import it.polimi.ingsw.am54.model.Game;
import it.polimi.ingsw.am54.model.GameException;
import it.polimi.ingsw.am54.model.GameState;
import it.polimi.ingsw.am54.model.Player;
import it.polimi.ingsw.am54.model.TotemColors;

/**
 * Manages the initial game setup phase.
 * Players join the room, choose their totem color
 * and, once the minimum number of participants is reached, start the game.
 *
 * @author AM54
 */
public class SetUpGameState extends GameState {
    /**
     * Allows a new player to join the game room, up to a maximum of 5 players.
     *
     * @param game     the current game instance
     * @param nickname the nickname of the player joining
     * @throws GameException if the room is full or the nickname is already in use
     */
    @Override
    public void joinGame(Game game, String nickname) throws GameException {
        if (game.getPlayers().size() >= 5) {
            throw new GameException("Unable to join: the room is full.");
        }

        for (Player p : game.getPlayers()) {
            if (p.getNickname().equalsIgnoreCase(nickname)) {
                throw new GameException("Unable to join: nickname '" + nickname + "' is already in use!");
            }
        }

        Player newPlayer = new Player();
        newPlayer.setNickname(nickname);
        game.getPlayers().add(newPlayer);

    }

    /**
     * Allows a player to choose their totem color, ensuring it has not
     * already been taken by another player.
     *
     * @param game     the current game instance
     * @param nickname the nickname of the player choosing the color
     * @param color    the desired totem color as a string
     * @throws GameException if the color is invalid or already taken by another player
     */
    @Override
    public void chooseTotemColor(Game game, String nickname, String color) throws GameException {
        Player p = game.getPlayerByNickname(nickname);

        TotemColors chosenColor;
        try {
            chosenColor = TotemColors.valueOf(color.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new GameException("Error: Color '" + color + "' is not valid.");
        }

        for (Player pl : game.getPlayers()) {
            if (pl.getTotemColors() == chosenColor) {
                throw new GameException("Error: The color " + color + " has already been chosen by another player!");
            }
        }

        p.setTotemColor(chosenColor);

    }

    /**
     * Starts the game after verifying that at least 2 players have joined
     * and all have chosen a totem color. Delegates the physical board setup
     * to the {@link Game} instance.
     *
     * @param game the current game instance
     * @throws GameException if fewer than 2 players or a player has not chosen a totem color
     */
    @Override
    public void setupGame(Game game) throws GameException {
        if (game.getPlayers().size() < 2) {
            throw new GameException("Unable to start: at least 2 players required.");
        }

        for (Player p : game.getPlayers()) {
            if (p.getTotemColors() == null) {
                throw new GameException("Unable to start: " + p.getNickname() + " has not chosen a Totem color yet!");
            }
        }


        // physical game construction delegated to Game (internally calls setState(new StartTurnState()))
        game.setupGame();
    }

    /**
     * Indicates whether permanent game elements should be removed in this state.
     *
     * @return {@code true} as the setup state clears all previous game data
     */
    @Override
    public boolean isRemovePermanent() {
        return true;
    }

    /**
     * Indicates whether player disconnection is blocked in this state.
     *
     * @return {@code true} as disconnections are not allowed during setup
     */
    @Override
    public boolean isDisconnectBlocked() {
        return true;
    }
}
