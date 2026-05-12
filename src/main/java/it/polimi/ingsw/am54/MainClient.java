package it.polimi.ingsw.am54;

import it.polimi.ingsw.am54.network.client.*;
import it.polimi.ingsw.am54.network.rmi.RMIGameServer;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

public class MainClient {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== BENVENUTO IN MESOS ===");
        System.out.print("Inserisci il tuo Nickname: ");
        String nickname = scanner.nextLine();

        System.out.print("Inserisci IP Server [IP o localhost]: ");
        String ip = scanner.nextLine();
        if (ip.trim().isEmpty()) {
            ip = "localhost";
        }

        // --- AGGIUNTA: SCELTA DEL PROTOCOLLO ---
        System.out.println("\nScegli il protocollo di rete:");
        System.out.println("1) Socket (TCP)");
        System.out.println("2) RMI");
        System.out.print("Scelta [1/2]: ");
        String protocolChoice = scanner.nextLine();

        try {
            // 1. Creo il Modello locale (costruttore vuoto, corretto come da tuo file)
            ClientModel model = new ClientModel();

            // Interfaccia generica per la connessione (Polimorfismo)
            ServerConnection connection = null;

            // 2. Connetto al server in base alla scelta
            if ("2".equals(protocolChoice.trim())) {
                System.out.println("Connessione via RMI in corso...");

                // Cerca il Registry RMI all'IP specificato (porta default 1099)
                Registry registry = LocateRegistry.getRegistry(ip, 1099);

                // Ottieni lo Stub del Server (Nome esatto preso dal tuo Server.java)
                RMIGameServer serverStub = (RMIGameServer) registry.lookup("AM54-Server");

                // Crea la connessione RMI
                connection = new RMIServerConnection(serverStub, model);
            } else {
                System.out.println("Connessione via Socket in corso...");

                // Crea la connessione Socket di default
                connection = new SocketServerConnection(ip, 12345, model);
            }

            // 3. Creo il VirtualServer per mandare comodamente i comandi
            // Alla CLI non importerà nulla se sotto c'è RMI o Socket!
            VirtualServer virtualServer = new VirtualServer(connection, nickname);

            // 4. Inizializzo l'interfaccia a riga di comando (CLI)
            CLIApp cli = new CLIApp(virtualServer, model, nickname, scanner);

            // 5. Mando la primissima richiesta: "Voglio entrare!"
            virtualServer.joinGame();

        } catch (Exception e) {
            System.err.println("Errore di connessione: Impossibile raggiungere il server a " + ip);
            e.printStackTrace();
        }
    }
}