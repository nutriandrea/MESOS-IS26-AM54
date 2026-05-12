package it.polimi.ingsw.am54.controller;

import it.polimi.ingsw.am54.model.Game;
import it.polimi.ingsw.am54.model.Player;
import it.polimi.ingsw.am54.model.TotemColors;
import it.polimi.ingsw.am54.network.messages.*;
import it.polimi.ingsw.am54.network.server.ClientConnection;
import it.polimi.ingsw.am54.network.server.Server;
import it.polimi.ingsw.am54.network.server.VirtualView;

import java.util.*;
import java.util.logging.Logger;

/**
 * ============================================================================
 * Controller — Coordinatore lato Server (UNA istanza per partita)
 * ============================================================================
 *
 * RESPONSABILITA':
 * - Ricevere le Request dai client (tramite SocketClientHandler/RMIClientHandler)
 * - Inoltrarle al Model (Game) in modo thread-safe (synchronized)
 * - Gestire il ciclo di vita dei giocatori (join, disconnect, reconnect)
 * - Inviare SOLO messaggi di sistema (disconnessione/riconnessione), MAI eventi di gioco
 *
 * DISCONNESSIONE VS RIMOZIONE:
 * - markDisconnected(): il ping/pong rileva che un client non risponde.
 *   Il giocatore viene marcato come disconnesso ma NON rimosso dal gioco.
 *   Gli altri giocatori ricevono PlayerDisconnectedUpdate.
 * - removeClient(): pulizia definitiva. Succede quando il socket si chiude
 * ============================================================================
 */
public class Controller {
    private final Game game;
    private Server server;

    /** Mappa nickname -> VirtualView. Ogni VirtualView e' observer del Game */
    private final Map<String, VirtualView> virtualViews = new HashMap<>();

    /** Set di nickname dei giocatori temporaneamente disconnessi */
    private final Set<String> disconnectedPlayers = new HashSet<>();

    private static final Logger LOGGER = Logger.getLogger(Controller.class.getName());

    public Controller(Game game) {
        this.game = game;
    }

    /**
     * Imposta il riferimento al Server.
     * Necessario per permettere al Controller di chiamare saveState()
     * sul PersistenceManager dopo ogni azione di gioco.
     *
     * @param server l'istanza del Server
     */
    public void setServer(Server server) {
        this.server = server;
    }

    /**
     * Salva lo stato corrente del gioco su file JSON.
     * Chiamato dal Controller dopo ogni azione di gioco
     * (setup, chooseTotemColor, placeTotem, pickCard).
     * 
     * Se il server non è inizializzato (server null), non fa nulla.
     */
    private void saveState() {
        return;
    }

    @FunctionalInterface
    private interface GameAction {
        void execute() throws Exception;
    }

    /* =====================================================================
     * JOIN / RECONNECT
     * ===================================================================== */

    public void handleJoinGame(String nickname, ClientConnection connection) {
        synchronized (disconnectedPlayers) {
            if (disconnectedPlayers.contains(nickname)) {
                handleReconnect(nickname, connection);
                return;
            }
        }

        synchronized (virtualViews) {
            if (virtualViews.containsKey(nickname)) {
                handleReconnect(nickname, connection);
                return;
            }
        }

        VirtualView playerView = new VirtualView(connection, nickname);

        synchronized (virtualViews) {
            virtualViews.put(nickname, playerView);
        }
        game.addObserver(playerView);
        executeCommand(nickname, () -> {
            try {
                // --- AGGIUNTA FONDAMENTALE PER SINCRONIZZARE IL NUOVO CLIENT ---
                // Facciamo un "riassunto" al nuovo arrivato dicendogli chi c'è già
                for (Player p : game.getPlayers()) {
                    playerView.onPlayerJoined(game, p);
                    if (p.getTotemColors() != null) {
                        playerView.onTotemColorChosen(game, p, p.getTotemColors());
                    }
                }
                // ---------------------------------------------------------------

                game.joinGame(nickname);
                connection.startHeartbeat();
            } catch (Exception e) {
                synchronized (virtualViews) {
                    virtualViews.remove(nickname);
                }
                game.removeObserver(playerView);
                throw e;
            }
        });
    }

    public void handleReconnect(String nickname, ClientConnection connection) {
        synchronized (disconnectedPlayers) {
            disconnectedPlayers.remove(nickname);
        }

        VirtualView newView; // Salviamo un riferimento alla nuova vista
        synchronized (virtualViews) {
            VirtualView oldView = virtualViews.get(nickname);
            if (oldView != null) {
                game.removeObserver(oldView);
            }
            newView = new VirtualView(connection, nickname);
            virtualViews.put(nickname, newView);
        }
        game.addObserver(newView);
        connection.startHeartbeat();
        LOGGER.info("Giocatore " + nickname + " si e' riconnesso.");

        // 1. Invio lo snapshot SOLO al giocatore che è appena rientrato
        newView.sendFullStateOnReconnect(game);

        if (game.getCurrentPlayer() != null) {
            newView.onTurnChanged(game,  game.getCurrentPlayer());
        }

        // 2. Avviso TUTTI gli altri che lui è tornato
        broadcastPlayerReconnected(nickname);
    }

    /* =====================================================================
     * GESTIONE COMANDI DI GIOCO
     * ===================================================================== */

    public void handleSetupGame(String nickname) {
        executeCommand(nickname, () -> {
            game.setupGame();
            saveState();
        });
    }

    public void handleChooseTotemColor(String nickname, String color) {
        executeCommand(nickname, () -> {
            game.chooseTotemColor(nickname, color);
            saveState();
        });
    }

    public void handlePlaceTotem(String nickname, int position) {
        executeCommand(nickname, () -> {
            game.placeTotem(nickname, position);
            saveState();
        });
    }

    public void handlePickCard(String nickname, String row, int index) {
        executeCommand(nickname, () -> {
            game.pickCard(nickname, row, index);
            checkAndResolveAutomaticPhases();
            saveState();
        });
    }

    public void handleEndTurn(String nickname) {
        executeCommand(nickname, () -> {
            game.endTurn(nickname);
            checkAndResolveAutomaticPhases();
            saveState();
        });
    }

    /* =====================================================================
     * DISCONNESSIONE
     * ===================================================================== */

    public void markDisconnected(String nickname) {
        boolean wasAlreadyDisconnected;
        synchronized (disconnectedPlayers) {
            // add() restituisce true se l'elemento NON era già presente.
            // Se c'era già, significa che abbiamo già avvisato tutti.
            wasAlreadyDisconnected = !disconnectedPlayers.add(nickname);
        }

        // Se è la prima volta che ci accorgiamo che è caduto, avvisiamo tutti!
        if (!wasAlreadyDisconnected) {
            LOGGER.warning("Giocatore " + nickname + " marcato come disconnesso.");
            broadcastPlayerDisconnected(nickname);
        }
    }

    public void removeClient(String nickname) {
        VirtualView view;
        synchronized (virtualViews) {
            view = virtualViews.remove(nickname);
        }

        if (view != null) {
            game.removeObserver(view);
        }
        LOGGER.info("Connessione di " + nickname + " rimossa dal server (Socket chiuso).");

        // FIX: Invece di rimuoverlo in silenzio, lo passiamo a markDisconnected
        // così siamo certi al 100% che la UI di tutti si aggiorni col pallino rosso!
        markDisconnected(nickname);
    }

   /* private void handlePlayerDrop(String disconnectedNickname) {
        // Quanti giocatori totali ci sono nel Game?
        int totalPlayers = game.getPlayers().size();
        // Quanti sono caduti?
        int offlinePlayers = disconnectedPlayers.size();
        // Quanti sono ancora online?
        int onlinePlayers = totalPlayers - offlinePlayers;

        if (onlinePlayers == 1) {
            System.out.println("[SERVER] È rimasto 1 solo giocatore! Sospensione partita...");

            // suspendGameAndStartTimer();
        }
        else if (onlinePlayers > 1) {
            System.out.println("[SERVER] Ci sono ancora " + onlinePlayers + " giocatori. Il gioco continua.");
            forceSkipTurn(disconnectedNickname);
        }
        else {
            System.out.println("[SERVER] Tutti i giocatori sono disconnessi. Stanza ibernata.");
            // (Opzionale: puoi fermare la partita o distruggere la stanza)
        }
    }

    private void forceSkipTurn(String nickname) {
        // Se tocca proprio al giocatore appena caduto, forziamo il passaggio
        if (game.getCurrentPlayer() != null && game.getCurrentPlayer().getNickname().equals(nickname)) {
            System.out.println("[SERVER] [ANTI-DEADLOCK] " + nickname + " era di turno. Passo al prossimo.");
            try {
                // Proviamo a finire il turno in modo pulito (il tuo metodo standard)
                handleEndTurn(nickname);
            } catch (Exception e) {
                // Se non può finire in modo pulito (es. non ha ancora pescato),
                // forziamo il passaggio dal Game! (Assicurati di avere un metodo nextTurn nel model)
                // game.nextTurn();
            }
        }
    } */

    public boolean isDisconnected(String nickname) {
        synchronized (disconnectedPlayers) {
            return disconnectedPlayers.contains(nickname);
        }
    }

    public int getPlayerCount() {
        return game.getPlayersNumber();
    }

    /* =====================================================================
     * METODI PRIVATI
     * ===================================================================== */

    private void broadcastPlayerReconnected(String reconnectedNickname) {
        synchronized (virtualViews) {
            for (VirtualView vv : virtualViews.values()) {
                vv.showPlayerReconnected(reconnectedNickname);
            }
        }
    }

    private void broadcastPlayerDisconnected(String disconnectedNickname) {
        synchronized (virtualViews) {
            for (VirtualView vv : virtualViews.values()) {
                vv.showPlayerDisconnected(disconnectedNickname);
            }
        }
    }

    private void executeCommand(String nickname, GameAction action) {
        try {
            action.execute();
        } catch (Exception e) {
            VirtualView view = virtualViews.get(nickname);
            if (view != null) {
                view.showError(e.getMessage());
            }
        }
    }

    private void checkAndResolveAutomaticPhases() throws Exception {
        // Se siamo finiti nella fase Eventi, risolvila automaticamente
        if (game.getCurrentState() instanceof it.polimi.ingsw.am54.model.states.EventState) {
            game.getCurrentState().resolvePhase(game);
        }

        // Se dopo gli eventi (o in altri modi) siamo finiti nella fase finale, calcola i vincitori
        if (game.getCurrentState() instanceof it.polimi.ingsw.am54.model.states.EndGameState) {
            game.getCurrentState().resolvePhase(game);
        }
    }
}