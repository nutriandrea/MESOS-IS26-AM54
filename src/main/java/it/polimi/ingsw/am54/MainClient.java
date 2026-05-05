package it.polimi.ingsw.am54; // <--- Package corretto!

import it.polimi.ingsw.am54.network.client.*;
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


        try {
            // 1. Creo il Modello locale (che terrà traccia di tutto)
            ClientModel model = new ClientModel();

            // 2. Connetto al server tramite Socket (in futuro potrai scegliere RMI)
            ServerConnection connection = new SocketServerConnection(ip, 12345, model);

            // 3. Creo il VirtualServer per mandare comodamente i comandi
            VirtualServer virtualServer = new VirtualServer(connection, nickname);

            // 4. Inizializzo l'interfaccia a riga di comando (CLI)
            CLIApp cli = new CLIApp(virtualServer, model, nickname, scanner);

            // 5. Mando la primissima richiesta: "Voglio entrare!"
            // Il Controller lato server capirà da solo se sei nuovo o se ti stai riconnettendo.
            virtualServer.joinGame();

            // Il main ha finito il suo lavoro!
            // Da questo momento in poi, la CLI prenderà vita da sola ogni volta
            // che il ServerHandler riceverà un aggiornamento di rete.

        } catch (Exception e) {
            System.err.println("Errore di connessione: Impossibile raggiungere il server a " + ip);
            e.printStackTrace();
        }
    }
}