package model.states;

import model.Game;
import model.GameState;
import model.Player;

/**
 * Stato di inizio turno: i giocatori piazzano il totem sulle offering tiles.
 */
public class StartTurnState extends GameState {

    @Override
    public void placeTotem(Game game, String nickname, int offerPosition) throws Exception {
        // 1. Troviamo il giocatore
        Player player = game.getPlayers().stream()
                .filter(p -> p.getNickname().equalsIgnoreCase(nickname))
                .findFirst()
                .orElseThrow(() -> new Exception("Errore: Giocatore '" + nickname + "' non trovato."));

        // 2. Controllo del turno (opzionale ma consigliato se il piazzamento è a turni)
        // Se nel tuo gioco i totem si piazzano uno alla volta seguendo un ordine:

        if (!game.getCurrentPlayer().equals(player)) {
            throw new Exception("Non è il tuo turno per piazzare il totem!");
        }

        // 3. Esecuzione fisica del piazzamento (delega alla Board)
        // Immagino tu abbia un metodo nella Board o nel Game per registrare dove va il totem.
        // Ad esempio: game.getBoard().addTotemToPosition(player.getTotemColors(), offerPosition);
        System.out.println(nickname + " ha piazzato il totem sulla posizione " + offerPosition);

        // Segniamo che il giocatore ha fatto la sua mossa (aggiungi un booleano in Player se non ce l'hai)
        player.setHasPlacedTotem(true);

        // 4. Controllo di fine fase: Tutti hanno piazzato il totem?
        boolean tuttiHannoPiazzato = true;
        for (Player p : game.getPlayers()) {
            if (!p.hasPlacedTotem()) {
                tuttiHannoPiazzato = false;
                break;
            }
        }

        // 5. TRANSIZIONE DI STATO O CAMBIO TURNO
        if (tuttiHannoPiazzato) {
            System.out.println("Tutti i totem sono stati piazzati! Transizione a PickingState...");

            // Prima di passare allo stato successivo, resettiamo le variabili dei giocatori
            for (Player p : game.getPlayers()) {
                p.setHasPlacedTotem(false);
            }

            // Passiamo la palla alla fase degli acquisti!
            game.setState(new PickingState());
        } else {
            // Se non hanno finito tutti, si passa semplicemente il turno al giocatore successivo
            game.nextPlayer(); // Scommenta se hai un metodo per avanzare il turno
        }
    }
}
