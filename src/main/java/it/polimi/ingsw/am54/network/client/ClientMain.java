package it.polimi.ingsw.am54.network.client;

import it.polimi.ingsw.am54.network.messages.JoinGameRequest;
import it.polimi.ingsw.am54.network.messages.ChooseTotemRequest;
import it.polimi.ingsw.am54.network.messages.SetupGameRequest;
import it.polimi.ingsw.am54.network.messages.PlaceTotemRequest;
import it.polimi.ingsw.am54.network.messages.PickCardRequest;
import it.polimi.ingsw.am54.network.messages.ReconnectRequest;
import it.polimi.ingsw.am54.model.TotemColors;
import it.polimi.ingsw.am54.view.CLIView;

import java.util.Scanner;

public class ClientMain {
    private static CLIView cliView;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Client client = new Client();

        System.out.print("Inserisci IP del server [localhost]: ");
        String ip = scanner.nextLine().trim();
        if (ip.isEmpty()) ip = "localhost";

        System.out.print("Inserisci porta [12345]: ");
        String portStr = scanner.nextLine().trim();
        int port = portStr.isEmpty() ? 12345 : Integer.parseInt(portStr);

        System.out.print("Inserisci il tuo nickname: ");
        String nickname = scanner.nextLine().trim();

        System.out.println("Connessione in corso...");

        if (!client.connect(ip, port)) {
            System.err.println("Impossibile connettersi al server!");
            return;
        }

        cliView = new CLIView(nickname);
        client.getServerHandler().setCLIView(cliView);

        System.out.println("Connesso! Invio richiesta di join...");
        client.sendMessage(new JoinGameRequest(nickname));

        displayHelp();

        boolean running = true;
        while (running) {
            System.out.print("\n> ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) continue;
            String[] parts = input.split("\\s+");

            try {
                switch (parts[0]) {
                    case "1" -> {
                        if (parts.length < 2) {
                            System.out.println("Usage: 1 <RED|YELLOW|WHITE|BLACK>");
                        } else {
                            client.sendMessage(new ChooseTotemRequest(TotemColors.valueOf(parts[1].toUpperCase())));
                        }
                    }
                    case "2" -> client.sendMessage(new SetupGameRequest());
                    case "3" -> {
                        if (parts.length < 2) {
                            System.out.println("Usage: 3 <position 1-N>");
                        } else {
                            client.sendMessage(new PlaceTotemRequest(Integer.parseInt(parts[1])));
                        }
                    }
                    case "4" -> {
                        if (parts.length < 3) {
                            System.out.println("Usage: 4 <UPPER|LOWER> <index>");
                        } else {
                            client.sendMessage(new PickCardRequest(parts[1].toUpperCase(), Integer.parseInt(parts[2])));
                        }
                    }
                    case "5" -> client.sendMessage(new ReconnectRequest(nickname));
                    case "0" -> running = false;
                    case "help" -> displayHelp();
                    case "status" -> cliView.displayGameState();
                    default -> System.out.println("Comando sconosciuto. Digita 'help' per info.");
                }
            } catch (Exception e) {
                System.err.println("Errore: " + e.getMessage());
            }
        }

        client.disconnect();
        System.out.println("Disconnesso. Arrivederci!");
        scanner.close();
    }

    private static void displayHelp() {
        System.out.println();
        System.out.println("+============================================================+");
        System.out.println("|           COMANDI DISPONIBILI - MESOS                      |");
        System.out.println("+============================================================+");
        System.out.println("|  1. Scegli colore totem: 1 <RED|YELLOW|WHITE|BLACK>        |");
        System.out.println("|  2. Avvia partita (setup) quando tutti pronti              |");
        System.out.println("|  3. Piazza totem: 3 <posizione 1-N>                        |");
        System.out.println("|  4. Pesca carta: 4 <UPPER|LOWER> <index>                   |");
        System.out.println("|  5. Riconnettiti se disconnesso                            |");
        System.out.println("|                                                            |");
        System.out.println("|  help - Mostra questo messaggio                            |");
        System.out.println("|  status - Mostra stato attuale del gioco                   |");
        System.out.println("|  0. Esci                                                   |");
        System.out.println("+============================================================+");
        System.out.println();
    }
}