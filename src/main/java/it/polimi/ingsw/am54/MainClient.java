package it.polimi.ingsw.am54;

import it.polimi.ingsw.am54.network.client.*;
import it.polimi.ingsw.am54.network.rmi.RMIGameServer;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

/**
 * Avvio del client in modalità testuale (CLI). Chiede IP del server, protocollo
 * (Socket o RMI) e nickname, stabilisce la connessione, valida l'ingresso e infine
 * avvia l'interfaccia {@code CLIApp}.
 */
public class MainClient {
    /**
     * Punto d'ingresso della modalità CLI.
     *
     * @param args argomenti da riga di comando (non utilizzati oltre l'attivazione della CLI)
     */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== BENVENUTO IN MESOS (Modalità CLI) ===");

        // 1. CHIEDIAMO PRIMA IP E PROTOCOLLO
        System.out.print("\nInserisci IP Server [IP o localhost]: ");
        String ip = scanner.nextLine();
        if (ip.trim().isEmpty()) {
            ip = "localhost";
        }

        System.out.println("Scegli il protocollo di rete:");
        System.out.println("1) Socket (TCP)");
        System.out.println("2) RMI");
        System.out.print("Scelta [1/2]: ");
        String protocolChoice = scanner.nextLine();
        boolean isSocket = !"2".equals(protocolChoice.trim());

        try {
            // 2. USIAMO IL SINGLETON (Fondamentale affinché CLIApp e Connection usino gli stessi dati)
            ClientModel model = Client.getInstance().getModel();
            ServerConnection connection = null;

            // 3. STABILIAMO LA CONNESSIONE
            if (!isSocket) {
                System.out.println("\nConnessione via RMI in corso...");
                // Fix RMI: Espone correttamente l'IP del client al Server per i callback
                try (java.net.DatagramSocket s = new java.net.DatagramSocket()) {
                    s.connect(java.net.InetAddress.getByName(ip), 1099);
                    System.setProperty("java.rmi.server.hostname", s.getLocalAddress().getHostAddress());
                } catch (Exception ignored) {}

                Registry registry = LocateRegistry.getRegistry(ip, 1099);
                RMIGameServer serverStub = (RMIGameServer) registry.lookup("AM54-Server");
                connection = new RMIServerConnection(serverStub, model);
            } else {
                System.out.println("\nConnessione via Socket in corso...");
                connection = new SocketServerConnection(ip, 12345, model);
            }

            // Salviamo la connessione nel Client globale per eventuali riconnessioni magiche
            Client.getInstance().setConnection(connection);
            Client.getInstance().setConnectionInfo(ip, isSocket);

            // 4. CICLO DI VALIDAZIONE DEL NICKNAME
            boolean nicknameAccepted = false;
            String nickname = "";

            while (!nicknameAccepted) {
                System.out.print("\nInserisci il tuo Nickname: ");
                nickname = scanner.nextLine().trim();

                if (nickname.isEmpty()) {
                    System.out.println("❌ Il nickname non può essere vuoto.");
                    continue;
                }

                // Inviamo la richiesta
                Client.getInstance().setNickname(nickname);
                connection.joinGame(nickname);

                System.out.print("Verifica in corso sul Server...");

                // Aspettiamo la risposta (max 2 secondi)
                for (int i = 0; i < 20; i++) {
                    try { Thread.sleep(100); } catch (InterruptedException ignored) {}

                    // CASO A: Il server ci ha rimbalzato
                    if (model.getLastErrorMessage() != null) {
                        System.out.println("\n❌ Errore: " + model.getLastErrorMessage());
                        model.clearError(); // Puliamo l'errore
                        break; // Usciamo dal for per ricominciare il while!
                    }

                    // CASO B: Il server ci ha accettato (il nostro nome è nella lista)
                    if (model.getPlayers().contains(nickname)) {
                        System.out.println("\n✅ Entrato con successo!");
                        nicknameAccepted = true;
                        break; // Usciamo dal for e il while terminerà
                    }
                }
            }

            // 5. NOME ACCETTATO: AVVIAMO LA CLI E IL GIOCO
            VirtualServer virtualServer = new VirtualServer(connection, nickname);
            CLIApp cli = new CLIApp(virtualServer, model, nickname, scanner);

            // Per sicurezza, forziamo il primissimo render per mostrare la Lobby!
            cli.render();

        } catch (Exception e) {
            System.err.println("\n❌ Errore critico: Impossibile raggiungere il server a " + ip);
            System.err.println("Assicurati che il Server sia avviato e l'IP sia corretto.");
            // e.printStackTrace(); // Decommenta se ti serve debuggare
        }
    }
}