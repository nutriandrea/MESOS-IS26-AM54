package it.polimi.ingsw.am54;

import it.polimi.ingsw.am54.network.client.*;
import it.polimi.ingsw.am54.network.rmi.RMIGameServer;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

/**
 * Starts the client in textual mode (CLI). Asks for server IP, protocol
 * (Socket or RMI) and nickname, establishes the connection, validates the entry
 * and finally starts the {@code CLIApp} interface.
 */
public class MainClient {
    /**
     * Entry point for the CLI mode.
     *
     * @param args command line arguments (not used beyond CLI activation)
     */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== BENVENUTO IN MESOS (Modalità CLI) ===");

        // 1. WE ASK FOR IP AND PROTOCOL FIRST
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
            // 2. WE USE THE SINGLETON (Fundamental so that CLIApp and Connection use the same data)
            ClientModel model = Client.getInstance().getModel();
            ServerConnection connection = null;

            // 3. WE ESTABLISH THE CONNECTION
            if (!isSocket) {
                System.out.println("\nConnessione via RMI in corso...");
                // RMI Fix: Correctly exposes the client's IP to the Server for callbacks
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

            // We save the connection in the global Client for any magical reconnections
            Client.getInstance().setConnection(connection);
            Client.getInstance().setConnectionInfo(ip, isSocket);

            // 4. NICKNAME VALIDATION LOOP
            boolean nicknameAccepted = false;
            String nickname = "";

            while (!nicknameAccepted) {
                System.out.print("\nInserisci il tuo Nickname: ");
                nickname = scanner.nextLine().trim();

                if (nickname.isEmpty()) {
                    System.out.println("❌ Il nickname non può essere vuoto.");
                    continue;
                }

                // We send the request
                Client.getInstance().setNickname(nickname);
                connection.joinGame(nickname);

                System.out.print("Verifica in corso sul Server...");

                // We wait for the response (max 2 seconds)
                for (int i = 0; i < 20; i++) {
                    try { Thread.sleep(100); } catch (InterruptedException ignored) {}

                    // CASE A: The server rejected us
                    if (model.getLastErrorMessage() != null) {
                        System.out.println("\n❌ Errore: " + model.getLastErrorMessage());
                        model.clearError(); // We clear the error
                        break; // We exit the for to restart the while loop!
                    }

                    // CASE B: The server accepted us (our name is in the list)
                    if (model.getPlayers().contains(nickname)) {
                        System.out.println("\n✅ Entrato con successo!");
                        nicknameAccepted = true;
                        break; // We exit the for and the while loop will end
                    }
                }
            }

            // 5. NAME ACCEPTED: WE START THE CLI AND THE GAME
            VirtualServer virtualServer = new VirtualServer(connection, nickname);
            CLIApp cli = new CLIApp(virtualServer, model, nickname, scanner);

            // For safety, we force the very first render to show the Lobby!
            cli.render();

        } catch (Exception e) {
            System.err.println("\n❌ Errore critico: Impossibile raggiungere il server a " + ip);
            System.err.println("Assicurati che il Server sia avviato e l'IP sia corretto.");
            // e.printStackTrace(); // Uncomment if you need to debug
        }
    }
}