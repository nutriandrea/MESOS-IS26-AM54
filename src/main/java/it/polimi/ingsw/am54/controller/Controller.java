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
        if (server != null && server.getPersistenceManager() != null) {
            server.getPersistenceManager().saveState();
        }
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
                    playerView.sendUpdate(new PlayerJoinedUpdate(p.getNickname()));
                    if (p.getTotemColors() != null) {
                        playerView.sendUpdate(new TotemColorUpdate(p.getNickname(), p.getTotemColors()));
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

        // 2. Avviso TUTTI gli altri che lui è tornato
        broadcastSystemUpdate(new PlayerReconnectedUpdate(nickname));
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
            saveState();
        });
    }

    public void handleEndTurn(String nickname) {
        executeCommand(nickname, () -> {
            game.endTurn(nickname);
            saveState();
        });
    }

    /* =====================================================================
     * DISCONNESSIONE
     * ===================================================================== */

    public void markDisconnected(String nickname) {
        synchronized (disconnectedPlayers) {
            disconnectedPlayers.add(nickname);
        }
        LOGGER.warning("Giocatore " + nickname + " marcato come disconnesso.");
        broadcastSystemUpdate(new PlayerDisconnectedUpdate(nickname));
    }

    public void removeClient(String nickname) {
        VirtualView view;
        synchronized (virtualViews) {
            view = virtualViews.remove(nickname);
        }
        synchronized (disconnectedPlayers) {
            disconnectedPlayers.remove(nickname);
        }
        if (view != null) {
            game.removeObserver(view);
        }
        LOGGER.info("Giocatore " + nickname + " rimosso dal server.");
    }

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

    private void executeCommand(String nickname, GameAction action) {
        try {
            action.execute();
        } catch (Exception e) {
            VirtualView view = virtualViews.get(nickname);
            if (view != null) {
                view.sendUpdate(new ErrorMessageUpdate(e.getMessage()));
            }
        }
    }

    private void broadcastSystemUpdate(Update update) {
        synchronized (virtualViews) {
            for (VirtualView vv : virtualViews.values()) {
                vv.sendUpdate(update);
            }
        }
    }
}