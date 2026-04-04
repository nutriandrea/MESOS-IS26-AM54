package it.polimi.ingsw.am54.model.states;

import it.polimi.ingsw.am54.model.Game;
import it.polimi.ingsw.am54.model.GameState;
import it.polimi.ingsw.am54.model.OfferingTile;
import it.polimi.ingsw.am54.model.Player;

import java.util.List;

//Stato di inizio turno: i giocatori piazzano il totem sulle offering tiles
public class StartTurnState extends GameState {

    @Override
    public void placeTotem(Game game, String nickname, int offerPosition) throws Exception {
        // troviamo il giocatore
        Player player = game.getPlayerByNickname(nickname);

        //check sul turno
        var casellaDiPartenza = game.getBoard().getTurnOrderTile().stream()
                .filter(tile -> tile.getOccupiedBy() != null && tile.getOccupiedBy().getNickname().equalsIgnoreCase(nickname))
                .findFirst()
                .orElseThrow(() -> new Exception("Errore: Nessun totem trovato sulla pista dei turni per " + nickname));

        if (!game.getCurrentPlayer().equals(player)) {
            throw new Exception("Non è il tuo turno per piazzare il totem!");
        }

        // delega alla Board la costruzione fisica
        OfferingTile tileScelta = game.getBoard().getOfferingTrack().get(offerPosition);

        game.getBoard().addTotemToPosition(player, game.getBoard().getOfferingTrack().get(offerPosition));


        casellaDiPartenza.occupy(null);
        // Segniamo che il giocatore ha fatto la sua mossa (aggiungi un booleano in Player se non ce l'hai)
        player.setHasPlacedTotem(true);
        player.setCurrentTile(tileScelta);

        //  hanno piazzato tutti il totem?
        boolean tuttiHannoPiazzato = true;
        int index = 0;
        List<Player> listaGiocatori = game.getPlayers();

        while (index < listaGiocatori.size() && tuttiHannoPiazzato) {
            if (!listaGiocatori.get(index).hasPlacedTotem()) {
                tuttiHannoPiazzato = false;
            }
            index++;
        }

        //TRANSIZIONE DI STATO O CAMBIO TURNO
        if (tuttiHannoPiazzato) {


            //prima di passare allo stato successivo, resettiamo le variabili dei giocatori
            for (Player p : game.getPlayers()) {
                p.setHasPlacedTotem(false);
            }

            // Passiamo la palla alla fase degli acquisti!
            game.setState(new PickingState());
        } else {
            // Se non hanno finito tutti, si passa il turno al giocatore successivo
            game.nextPlayer();
        }
    }
}