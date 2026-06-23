package it.polimi.ingsw.am54.model.states;

import it.polimi.ingsw.am54.model.Game;
import it.polimi.ingsw.am54.model.GameState;
import it.polimi.ingsw.am54.model.Player;
import it.polimi.ingsw.am54.model.TotemColors;

// Setup state: players join, choose color and start the game
public class SetUpGameState extends GameState {
    /**
     * Adds a new player to the lobby, verifying that the room is not full
     * (max 5) and that the nickname is not already in use.
     *
     * @param game     the current game
     * @param nickname the nickname requested by the new player
     * @throws Exception if the room is full or the nickname is duplicated
     */
    @Override
    public void joinGame(Game game, String nickname) throws Exception {
        if (game.getPlayers().size() >= 5) {
            throw new Exception("Impossibile unirsi: la stanza è piena.");
        }

        for (Player p : game.getPlayers()) {
            if (p.getNickname().equalsIgnoreCase(nickname)) {
                throw new Exception("Impossibile unirsi: il nickname '" + nickname + "' è già in uso!");
            }
        }

        Player newPlayer = new Player();
        newPlayer.setNickname(nickname);
        game.getPlayers().add(newPlayer);

    }

    /**
     * Assigns the totem color to a player, validating that the color exists and
     * has not already been chosen by another player.
     *
     * @param game     the current game
     * @param nickname the choosing player
     * @param color    the desired color name
     * @throws Exception if the color is invalid or already occupied
     */
    @Override
    public void chooseTotemColor(Game game, String nickname, String color) throws Exception {
        Player p = game.getPlayerByNickname(nickname);

        TotemColors chosenColor;
        try {
            chosenColor = TotemColors.valueOf(color.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new Exception("Errore: Il colore '" + color + "' non è valido.");
        }

        for (Player pl : game.getPlayers()) {
            if (pl.getTotemColors() == chosenColor) {
                throw new Exception("Errore: Il colore " + color + " è già stato scelto da un altro giocatore!");
            }
        }

        p.setTotemColor(chosenColor);

    }

    /**
     * Starts the game: requires at least 2 players and that everyone has chosen a
     * color, then delegates to {@link Game#setupGame()} the construction of decks and board
     * and finally transitions to the {@link StartTurnState}.
     *
     * @param game the game to start
     * @throws Exception if players are less than 2 or someone hasn't chosen a color
     */
    @Override
    public void setupGame(Game game) throws Exception {
        if (game.getPlayers().size() < 2) {
            throw new Exception("Impossibile avviare: servono almeno 2 giocatori.");
        }

        for (Player p : game.getPlayers()) {
            if (p.getTotemColors() == null) {
                throw new Exception("Impossibile avviare: " + p.getNickname() + " non ha ancora scelto il colore del Totem!");
            }
        }


        // physical construction of the game delegated to Game
        game.setupGame();

        // State change
        game.setState(new StartTurnState());
    }

    @Override
    public boolean isRemovePermanent() {
        return true;
    }

    @Override
    public boolean isDisconnectBlocked() {
        return true;
    }
}