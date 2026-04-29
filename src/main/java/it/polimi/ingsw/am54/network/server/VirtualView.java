package it.polimi.ingsw.am54.network.server;

import it.polimi.ingsw.am54.model.*;
import it.polimi.ingsw.am54.network.messages.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * La VirtualView agisce come un "Impostore" (Pattern Proxy).
 * Il Game (Model) pensa di chiamare i metodi di una vera interfaccia grafica,
 * ma in realtà sta chiamando questa classe, che traduce le chiamate in pacchetti di rete.
 */
public class VirtualView implements IGameObserver {

    // Il "tubo di comunicazione" astratto.
    // Non sappiamo se sia Socket o RMI, sappiamo solo che sa spedire pacchetti.
    private final ClientConnection connection;

    // L'identità di questa vista.
    // Serve per sapere a quale giocatore appartiene questo tubo.
    private final String nickname;

    /**
     * Costruttore: crea la vista quando un giocatore entra in partita.
     * PERCHÉ LO FA: Associa in modo indissolubile il nome del giocatore al suo tubo di rete.
     */
    public VirtualView(ClientConnection connection, String nickname) {
        this.connection = connection;
        this.nickname = nickname;
    }

    public String getNickname() {
        return nickname;
    }

    /**
     * --- FLUSSO UNICAST (MESSAGGI PRIVATI) ---
     * Invia un messaggio di errore SOLO a questo specifico giocatore.
     * PERCHÉ LO FA: Se Bob sbaglia mossa, il Controller chiama questo metodo sulla VirtualView di Bob.
     * Così l'errore finisce solo nel tubo di Bob, e gli altri giocatori non vedono nulla.
     */
    public void showError(String errorMessage) {
        connection.sendUpdate(new ErrorMessageUpdate(errorMessage));
    }


    /* =====================================================================
     * --- FLUSSO BROADCAST (METODI DELL'OBSERVER) ---
     * Tutti i metodi qui sotto vengono chiamati IN AUTOMATICO dal Model (Game)
     * quando succede qualcosa sul tabellone.
     * ===================================================================== */

    /**
     * COSA FA: Prende l'evento "un giocatore si è unito", crea il pacchetto PlayerJoinedUpdate e lo spedisce.
     * PERCHÉ LO FA: Per dire al Client di aggiungere questo nome alla lista dei giocatori a schermo.
     */
    @Override
    public void onPlayerJoined(Game game, Player player) {
        connection.sendUpdate(new PlayerJoinedUpdate(player.getNickname()));
    }

    /**
     * COSA FA: Traduce l'evento della scelta del colore in un pacchetto di rete.
     * PERCHÉ LO FA: Per aggiornare l'interfaccia grafica del Client mostrando il colore del totem scelto.
     */
    @Override
    public void onTotemColorChosen(Game game, Player player, TotemColors color) {
        connection.sendUpdate(new TotemColorUpdate(player.getNickname(), color));
    }

    /**
     * COSA FA: Segnala che un giocatore ha piazzato il suo Totem su una tessera.
     * PERCHÉ LO FA: Il Client riceverà la posizione e sposterà graficamente il pedone del giocatore su quella tessera.
     */
    @Override
    public void onTotemPlaced(Game game, Player player, int offerPosition) {
        connection.sendUpdate(new TotemPlacedUpdate(player.getNickname(), offerPosition));
    }

    /**
     * COSA FA: Impacchetta le informazioni di una carta appena pescata (Chi l'ha presa, da quale fila, e l'indice).
     * PERCHÉ LO FA: Quando il pacchetto arriva, il Client rimuove quella carta dal tabellone grafico e la mette nella tribù del giocatore.
     */
    @Override
    public void onCardPicked(Game game, Player player, String row, int cardIndex, int newFoodBalance) {
        // Crei il nuovo pacchetto e lo invii a tutti!
        connection.sendUpdate(new CardPickedUpdate(player.getNickname(), row, cardIndex, newFoodBalance));
    }

    @Override
    public void onTotemReturned(Game game, Player player) {
        // Crea il nuovo messaggio dedicato che abbiamo definito prima
        Update update = new TotemReturnedUpdate(player.getNickname());
        connection.sendUpdate(update);
    }

    /**
     * COSA FA: Notifica a tutti chi è il nuovo giocatore di turno.
     * PERCHÉ LO FA: Permette all'interfaccia grafica di attivare i pulsanti solo per quel giocatore, e mostrare agli altri la scritta "In attesa di...".
     */
    @Override
    public void onTurnChanged(Game game, Player newCurrentPlayer) {
        // Se newCurrentPlayer è null (es. tra una fase e l'altra), non mandiamo il nome
        String turnOwner = (newCurrentPlayer != null) ? newCurrentPlayer.getNickname() : null;
        connection.sendUpdate(new TurnChangedUpdate(turnOwner));
    }

    /**
     * COSA FA: Avvisa che la Macchina a Stati del gioco ha cambiato fase (es. da PickingState a EventState).
     * PERCHÉ LO FA: Il Client userà questa info per cambiare schermata o aggiornare un testo a video (es. "Fase Eventi in corso...").
     */
    @Override
    public void onStateChanged(Game game, GameState newState) {
        connection.sendUpdate(new StateChangedUpdate(newState.getClass().getSimpleName()));
    }

    /**
     * COSA FA: Quando il gioco finisce, estrae i nomi dei vincitori e crea l'update finale.
     * PERCHÉ LO FA: Per permettere al Client di mostrare la schermata finale dei punteggi e il podio.
     */
    @Override
    public void onEndGame(Game game, List<Player> winners) {
        // Il Model passa oggetti 'Player' (pesanti e pieni di logica).
        // Noi li convertiamo in semplici 'String' (leggere) prima di spedirli su Internet!
        List<String> winnerNames = winners.stream()
                .map(Player::getNickname)
                .collect(Collectors.toList());

        connection.sendUpdate(new EndGameUpdate(winnerNames));
    }
}