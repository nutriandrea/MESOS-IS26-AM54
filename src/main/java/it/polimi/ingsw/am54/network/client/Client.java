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

    private String savedIp;
    private boolean savedIsSocket;
    private boolean isReconnecting = false;

    private Client() {
        this.model = new ClientModel();
        this.model.setOnConnectionLostCallback(() -> this.startAutoReconnect());
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

    public void setConnectionInfo(String ip, boolean isSocket) {
        this.savedIp = ip;
        this.savedIsSocket = isSocket;
    }

    public void startAutoReconnect() {
        if (isReconnecting) return;
        isReconnecting = true;

        // FIX: Rimosso Platform.runLater che faceva crashare il thread in modalità CLI!
        model.setReconnectingStatus(true);

        new Thread(() -> {
            while (isReconnecting) {
                try {
                    Thread.sleep(3000);

                    ServerConnection newConn;
                    if (savedIsSocket) {
                        newConn = new SocketServerConnection(savedIp, 12345, model);
                    } else {
                        java.rmi.registry.Registry registry = java.rmi.registry.LocateRegistry.getRegistry(savedIp, 1099);
                        it.polimi.ingsw.am54.network.rmi.RMIGameServer serverStub = (it.polimi.ingsw.am54.network.rmi.RMIGameServer) registry.lookup("AM54-Server");
                        newConn = new RMIServerConnection(serverStub, model);
                    }

                    this.connection = newConn;
                    newConn.joinGame(nickname);

                    isReconnecting = false;

                    // Annunciamo la ripresa a gran voce!
                    model.triggerSystemMessage("✅ RICONNESSO AL SERVER CON SUCCESSO!");
                    model.setReconnectingStatus(false);

                } catch (Exception e) {
                    // La connessione fallisce, riprova al prossimo ciclo silenziosamente
                }
            }
        }).start();
    }

    public ServerConnection getConnection() { return connection; }
    public ClientModel getModel() { return model; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
}