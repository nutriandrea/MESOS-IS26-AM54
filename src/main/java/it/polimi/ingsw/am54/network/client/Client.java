package it.polimi.ingsw.am54.network.client;

/**
 * Hub centrale del lato Client.
 * Tiene insieme la rete, il modello locale e le info dell'utente.
 */
public class Client {
    private static Client instance;

    private ServerConnection connection;
    private ClientModel model;
    private String nickname;

    private Client() {
        this.model = new ClientModel();
    }

    public static Client getInstance() {
        if (instance == null) instance = new Client();
        return instance;
    }

    public static void resetClient() {
        if (instance != null && instance.connection != null) {
            try {
                instance.connection.disconnect();
            } catch (Exception ignored) {}
        }
        instance = new Client();
    }

    // Metodo per "accendere" il client dopo la scelta RMI/Socket
    public void setConnection(ServerConnection connection) {
        this.connection = connection;
    }

    public ServerConnection getConnection() { return connection; }
    public ClientModel getModel() { return model; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
}