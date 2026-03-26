package model.states;

import model.Game;
import model.GameState;
import model.OfferingTile;
import model.Player;
import model.TurnOrderSlot;
import java.util.Comparator;

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

        // Imposta la offering tile corrente del giocatore
        OfferingTile tile = game.getBoard().getOfferingTrack().get(offerPosition);
        player.setCurrentTile(tile);

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

            // Reset turn order slots
            for (TurnOrderSlot slot : game.getBoard().getTurnOrderTile()) {
                slot.freeSlot();
            }

            // Ordina i giocatori per ID della offering tile (A, B, C...)
            game.getPlayers().sort(Comparator.comparing(p -> {
                OfferingTile playerTile = p.getCurrentTile();
                return playerTile != null ? playerTile.getId() : 'Z';
            }));

            // Imposta il primo giocatore (quello sulla tile A)
            game.setCurrentPlayer(game.getPlayers().get(0));

            // Passiamo la palla alla fase degli acquisti!
            game.setState(new PickingState());
        } else {
            // Se non hanno finito tutti, si passa semplicemente il turno al giocatore successivo
            game.nextPlayer(); // Scommenta se hai un metodo per avanzare il turno
        }
    }
}
