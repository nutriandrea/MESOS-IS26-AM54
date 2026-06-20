package it.polimi.ingsw.am54.network.client;

/**
 * Central hub of the Client side.
 * Brings together the network, local model, and user info.
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

    /**
     * Returns the singleton client instance, creating it if necessary.
     *
     * @return the shared {@code Client} instance
     */
    public static synchronized Client getInstance() {
        if (instance == null) instance = new Client();
        return instance;
    }

    /**
     * Discards the current client instance, disconnecting it first, and
     * replaces it with a fresh one.
     */
    public static synchronized void resetClient() {
        if (instance != null && instance.connection != null) {
            try {
                instance.connection.disconnect();
            } catch (Exception ignored) {}
        }
        instance = new Client();
    }

    /**
     * Sets the active server connection (Socket or RMI).
     *
     * @param connection the connection to use
     */
    public void setConnection(ServerConnection connection) {
        this.connection = connection;
    }

    /**
     * Stores the connection parameters needed for automatic reconnection.
     *
     * @param ip       the server IP address
     * @param isSocket {@code true} if using Socket, {@code false} for RMI
     */
    public void setConnectionInfo(String ip, boolean isSocket) {
        this.savedIp = ip;
        this.savedIsSocket = isSocket;
    }

    /**
     * Starts a background thread that periodically attempts to reconnect
     * to the server using the saved connection info. Stops once the
     * connection is re-established.
     */
    public void startAutoReconnect() {
        if (isReconnecting) return;
        isReconnecting = true;
        javafx.application.Platform.runLater(() -> model.setReconnectingStatus(true));

        new Thread(() -> {
            while (isReconnecting) {
                try {
                    System.out.println("[CLIENT] Tento riconnessione a " + savedIp + "...");
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
                    javafx.application.Platform.runLater(() -> model.setReconnectingStatus(false));
                    System.out.println("[CLIENT] Riconnesso con successo!");
                } catch (Exception e) {
                    // Retry
                }
            }
        }).start();
    }

    /** @return the current server connection, or {@code null} if none is set */
    public ServerConnection getConnection() { return connection; }
    /** @return the local client model */
    public ClientModel getModel() { return model; }
    /** @return the player's nickname */
    public String getNickname() { return nickname; }
    /**
     * Sets the player's nickname.
     *
     * @param nickname the nickname to use
     */
    public void setNickname(String nickname) { this.nickname = nickname; }
}