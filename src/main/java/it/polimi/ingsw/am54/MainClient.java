package it.polimi.ingsw.am54;

import it.polimi.ingsw.am54.network.client.*;
import it.polimi.ingsw.am54.network.rmi.RMIGameServer;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

/**
 * Launches the client (CLI or GUI). Manages the connection to the server,
 * the choice of network protocol (Socket or RMI) and the initialization
 * of the command line interface for interaction with the player.
 *
 * @author AM54
 */
public class MainClient {
    /**
     * Entry point that prompts for nickname, server IP and protocol,
     * then starts the client (CLI or GUI).
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== WELCOME TO MESOS ===");
        System.out.print("Enter your Nickname: ");
        String nickname = scanner.nextLine();

        System.out.print("Enter Server IP [IP or localhost]: ");
        String ip = scanner.nextLine();
        if (ip.trim().isEmpty()) {
            ip = "localhost";
        }

        // --- ADDED: PROTOCOL CHOICE ---
        System.out.println("\nChoose the network protocol:");
        System.out.println("1) Socket (TCP)");
        System.out.println("2) RMI");
        System.out.print("Choice [1/2]: ");
        String protocolChoice = scanner.nextLine();

        try {
            // 1. Create the local Model (empty constructor, correct as per your file)
            ClientModel model = new ClientModel();

            // Generic interface for connection (Polymorphism)
            ServerConnection connection = null;

            // 2. Connect to server based on choice
            if ("2".equals(protocolChoice.trim())) {
                System.out.println("Connecting via RMI...");

                // Look for the RMI Registry at the specified IP (default port 1099)
                Registry registry = LocateRegistry.getRegistry(ip, 1099);

                // Get the Server Stub (Exact name taken from your Server.java)
                RMIGameServer serverStub = (RMIGameServer) registry.lookup("AM54-Server");

                // Creates the RMI connection
                connection = new RMIServerConnection(serverStub, model);
            } else {
                System.out.println("Connecting via Socket...");

                // Creates the default Socket connection
                connection = new SocketServerConnection(ip, 12345, model);
            }

            // 3. Create the VirtualServer to conveniently send commands
            // The CLI won't care if underneath it's RMI or Socket!
            VirtualServer virtualServer = new VirtualServer(connection, nickname);

            // 4. Initialize the command line interface (CLI)
            CLIApp cli = new CLIApp(virtualServer, model, nickname, scanner);

            // 5. Send the very first request: "I want to join!"
            virtualServer.joinGame();

        } catch (Exception e) {
            System.err.println("Connection error: Unable to reach server at " + ip);
            e.printStackTrace();
        }
    }
}