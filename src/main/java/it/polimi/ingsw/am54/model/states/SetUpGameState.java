package it.polimi.ingsw.am54.model.states;

import it.polimi.ingsw.am54.model.Game;
import it.polimi.ingsw.am54.model.GameState;
import it.polimi.ingsw.am54.model.Player;
import it.polimi.ingsw.am54.model.TotemColors;

//Stato di setup: i giocatori si uniscono, scelgono il colore e avviano la partita
public class SetUpGameState extends GameState {
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


        // costruzione fisica del gioco delegata a Game
        game.setupGame();

        // Cambio di stato
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
