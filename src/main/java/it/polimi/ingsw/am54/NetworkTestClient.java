package it.polimi.ingsw.am54;

import it.polimi.ingsw.am54.network.client.SocketServerConnection;
import it.polimi.ingsw.am54.network.messages.JoinGameRequest;
import java.io.IOException;

public class NetworkTestClient {
    public static void main(String[] args) {
        try {
            System.out.println("[CLIENT] Tentativo di connessione al server...");

            // 1. Inizializza la connessione Socket sulla porta 12345 (quella del Server)
            // Passiamo null come View per questo test rapido di invio
            SocketServerConnection connection = new SocketServerConnection("127.0.0.1", 12345, null);

            System.out.println("[CLIENT] Connesso! Invio richiesta di ingresso...");

            // 2. Crea e invia la richiesta JoinGameRequest
            String nickname =  "Mario Rossi";
            connection.joinGame(nickname);

            System.out.println("[CLIENT] Richiesta inviata per l'utente: " + nickname);

            // Teniamo il client acceso un attimo per vedere la risposta del server nei log
            Thread.sleep(2000);

            connection.disconnect();
            System.out.println("[CLIENT] Test terminato.");

        } catch (IOException | InterruptedException e) {
            System.err.println("[CLIENT] Errore: Il server è spento o la porta è errata.");
            e.printStackTrace();
        }
    }
}