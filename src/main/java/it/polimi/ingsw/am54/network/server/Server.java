package it.polimi.ingsw.am54.network.server;

import it.polimi.ingsw.am54.controller.Controller;
import it.polimi.ingsw.am54.model.Game;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * ============================================================================
 * Server — Entry point del server AM54 (single game)
 * ============================================================================
 */
public class Server {

    private final Game game;
    private final Controller controller;
    private boolean isRunning;

    /**
     * Costruisce il server. Tenta di ripristinare una partita da {@code backup.json}:
     * se esiste un backup valido riprende quella partita (recupero da crash), altrimenti
     * ne crea una nuova e pulita.
     */
    public Server() {
        PersistenceManager pm = new PersistenceManager("backup.json");
        Game loadedGame = pm.loadGame();

        // Si attiva SOLO se il file di backup esiste ed è valido
        if (loadedGame != null) {
            this.game = loadedGame;
            this.controller = new Controller(this.game);

            // CHIAMATA QUI: Solo durante il vero ripristino
            this.controller.validateGameStateAfterRecovery();

            System.out.println("[Server] Crash rilevato: partita ripristinata.");
        } else {
            // Avvio normale pulito
            this.game = new Game();
            this.controller = new Controller(game);
            System.out.println("Server creato con una nuova partita.");
        }

        this.controller.setServer(this);
        this.isRunning = true;
    }

    public Controller getController() {
        return controller;
    }

    /**
     * Avvia entrambi i trasporti del server: prima il registry RMI, poi il loop di
     * accettazione TCP (che è bloccante e tiene vivo il server).
     *
     * @param tcpPort la porta per le connessioni Socket TCP
     * @param rmiPort la porta del registry RMI
     */
    public void start(int tcpPort, int rmiPort) {
        startRMI(rmiPort);
        startTCP(tcpPort);
    }

    /**
     * Crea il registry RMI sulla porta indicata e vi pubblica il server di gioco
     * ("AM54-Server"). Eventuali errori vengono loggati senza interrompere il server.
     *
     * @param port la porta del registry RMI
     */
    private void startRMI(int port) {
        try {
            // L'hostname RMI è già stato settato globalmente nel main!
            Registry registry = LocateRegistry.createRegistry(port);
            RMIServerImpl rmiServer = new RMIServerImpl(this);
            registry.rebind("AM54-Server", rmiServer);
            System.out.println("[Server] Motore RMI avviato su porta " + port);
        } catch (Exception e) {
            System.err.println("[Server] Impossibile avviare RMI: " + e.getMessage());
        }
    }

    /**
     * Avvia il loop di accettazione TCP: per ogni client che si connette crea un
     * {@link SocketClientHandler} su un thread dedicato. Bloccante finché il server
     * resta attivo.
     *
     * @param port la porta su cui ascoltare le connessioni TCP
     */
    private void startTCP(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[Server] Motore TCP avviato su porta " + port);

            while (isRunning) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[Server TCP] Nuova connessione da: " + clientSocket.getInetAddress());

                SocketClientHandler handler = new SocketClientHandler(clientSocket, this);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.err.println("[Server] Errore critico TCP: " + e.getMessage());
        }
    }

    /**
     * Punto d'ingresso del server: rileva automaticamente l'IP della scheda di rete
     * attiva (da comunicare ai giocatori), lo imposta come hostname RMI e avvia il
     * server sulle porte TCP 12345 e RMI 1099.
     *
     * @param args argomenti da riga di comando (non utilizzati)
     */
    public static void main(String[] args) {
        System.out.println("=== Avvio Server AM54 ===");

        String serverIp = "127.0.0.1"; // Fallback di sicurezza

        try (java.net.DatagramSocket s = new java.net.DatagramSocket()) {
            // Finge di connettersi all'esterno per scoprire l'IP della scheda Wi-Fi attiva
            s.connect(java.net.InetAddress.getByName("8.8.8.8"), 10002);
            serverIp = s.getLocalAddress().getHostAddress();
        } catch (Exception e) {
            System.err.println("Autodetect IP fallito. Uso localhost.");
        }

        // Imposta l'hostname RMI in modo dinamico
        System.setProperty("java.rmi.server.hostname", serverIp);
        System.out.println("[Server] RMI Hostname globale impostato a: " + serverIp);
        System.out.println("\n---> COMUNICA QUESTO IP AGLI ALTRI GIOCATORI: " + serverIp + " <--- \n");

        Server server = new Server();
        server.start(12345, 1099);
    }
}