package it.polimi.ingsw.am54.model.states;

import it.polimi.ingsw.am54.model.Game;
import it.polimi.ingsw.am54.model.GameState;
import it.polimi.ingsw.am54.model.Player;
import it.polimi.ingsw.am54.model.TotemColors;

/**
 * Stato di setup: i giocatori si uniscono, scelgono il colore e avviano la partita.
 */
public class SetUpGameState extends GameState {

    @Override
    public void joinGame(Game game, String nickname) throws Exception {
        if (game.getPlayers().size() >= 5) {
            throw new Exception("Impossibile unirsi: la stanza è piena (max 5 giocatori).");
        }

        // 2. Controllo nickname duplicati
        for (Player p : game.getPlayers()) {
            if (p.getNickname().equalsIgnoreCase(nickname)) {
                throw new Exception("Impossibile unirsi: il nickname '" + nickname + "' è già in uso!");
            }
        }

        // 3. Se i controlli passano, creiamo il giocatore e lo aggiungiamo
        Player newPlayer = new Player();
        newPlayer.setNickname(nickname);
        game.getPlayers().add(newPlayer);

        System.out.println(nickname + " è entrato nella lobby. Giocatori attuali: " + game.getPlayers().size());
    }

    @Override
    public void chooseTotemColor(Game game, String nickname, String color) throws Exception {
        // 1. Cerchiamo il giocatore nella lista
        Player p = game.getPlayerByNickname(nickname);
        // 2. Convertiamo la stringa in Enum
        TotemColors chosenColor;
        try {
            chosenColor = TotemColors.valueOf(color.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new Exception("Errore: Il colore '" + color + "' non è valido.");
        }

        // 3. Controlliamo che il colore non sia già stato preso
        for (Player pl : game.getPlayers()) {
            if (pl.getTotemColors() == chosenColor) {
                throw new Exception("Errore: Il colore " + color + " è già stato scelto da un altro giocatore!");
            }
        }

        // 4. Assegniamo il colore
        p.setTotemColor(chosenColor);
        System.out.println(nickname + " ha scelto il totem " + chosenColor.name());
    }

    @Override
    public void setupGame(Game game) throws Exception {
        // 1. Controllo numero minimo di giocatori
        if (game.getPlayers().size() < 2) {
            throw new Exception("Impossibile avviare: servono almeno 2 giocatori.");
        }

        // 2. Sicurezza: controlliamo che tutti abbiano scelto un colore!
        for (Player p : game.getPlayers()) {
            if (p.getTotemColors() == null) {
                throw new Exception("Impossibile avviare: " + p.getNickname() + " non ha ancora scelto il colore del Totem!");
            }
        }

        System.out.println("Tutti i giocatori sono pronti! Creazione della Board e dei mazzi...");

        // 3. Costruzione fisica del gioco delegata a Game
        game.setupGame();

        // 4. CAMBIO DI STATO
        // Passiamo alla fase successiva: inizio del round/piazzamento totem
        System.out.println("Setup completato! Transizione a StartTurnState...");
        game.setState(new StartTurnState());
    }
}
