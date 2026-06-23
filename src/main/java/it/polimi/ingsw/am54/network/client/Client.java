package it.polimi.ingsw.am54.network.client;

/**
 * Central hub of the Client side.
 * Ties together the network, local model, and user info.
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

    /**
     * Reinitializes the client (e.g. returning to the menu): closes any active
     * connection and recreates the singleton instance from scratch.
     */
    public static void resetClient() {
        if (instance != null && instance.connection != null) {
            try {
                instance.connection.disconnect();
            } catch (Exception ignored) {}
        }
        instance = new Client();
    }

    // Method to "turn on" the client after the RMI/Socket choice
    public void setConnection(ServerConnection connection) {
        this.connection = connection;
    }

    public void setConnectionInfo(String ip, boolean isSocket) {
        this.savedIp = ip;
        this.savedIsSocket = isSocket;
    }

    /**
     * Starts auto-reconnection in a dedicated thread: every 3 seconds attempts to
     * establish a new connection (Socket or RMI, depending on the last used) and to
     * rejoin the game with the same nickname, until successful. By updating the
     * connection in the singleton, subsequent moves travel immediately on the new one.
     */
    public void startAutoReconnect() {
        if (isReconnecting) return;
        isReconnecting = true;

        // FIX: Removed Platform.runLater which caused the thread to crash in CLI mode!
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

                    // We announce the recovery loudly!
                    model.triggerSystemMessage("✅ RICONNESSO AL SERVER CON SUCCESSO!");
                    model.setReconnectingStatus(false);

                } catch (Exception e) {
                    // The connection fails, retries at the next cycle silently
                }
            }
        }).start();
    }

    public ServerConnection getConnection() { return connection; }
    public ClientModel getModel() { return model; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
}