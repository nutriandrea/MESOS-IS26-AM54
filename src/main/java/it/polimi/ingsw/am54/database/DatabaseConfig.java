package it.polimi.ingsw.am54.database;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Configuration for the MySQL database connection.
 * Default: localhost:3306/am54, user: root, password: (empty)
 *
 * Override via system properties or via .env file (project root):
 *   - db.host     (default: localhost)
 *   - db.port     (default: 3306)
 *   - db.name     (default: am54)
 *   - db.user     (default: root)
 *   - db.password (default: )
 *   - db.sslMode  (default: PREFERRED; REQUIRED per SSL obbligatorio)
 *   - db.params   (default: , parametri JDBC extra, es. allowPublicKeyRetrieval=true)
 */
public class DatabaseConfig {
    private final String host;
    private final int port;
    private final String dbName;
    private final String user;
    private final String password;
    private final String sslMode;
    private final String extraParams;

    public DatabaseConfig() {
        loadEnvFile();
        this.host = System.getProperty("db.host", "localhost");
        this.port = Integer.parseInt(System.getProperty("db.port", "3306"));
        this.dbName = System.getProperty("db.name", "am54");
        this.user = System.getProperty("db.user", "root");
        this.password = System.getProperty("db.password", "");
        this.sslMode = System.getProperty("db.sslMode", "PREFERRED");
        this.extraParams = System.getProperty("db.params", "");
    }

    /**
     * Carica le variabili dal file .env nella directory di lavoro.
     * I valori vengono impostati come system properties solo se non
     * sono già state definite (es. tramite -D sulla riga di comando).
     * Mappa DB_HOST → db.host, DB_PORT → db.port, ecc.
     */
    private static void loadEnvFile() {
        File envFile = new File(".env");
        if (!envFile.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(envFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                int eq = line.indexOf('=');
                if (eq < 0) continue;

                String key = line.substring(0, eq).trim();
                String value = line.substring(eq + 1).trim();

                String sysProp;
                switch (key) {
                    case "DB_HOST":     sysProp = "db.host"; break;
                    case "DB_PORT":     sysProp = "db.port"; break;
                    case "DB_NAME":     sysProp = "db.name"; break;
                    case "DB_USER":     sysProp = "db.user"; break;
                    case "DB_PASSWORD": sysProp = "db.password"; break;
                    case "DB_SSL_MODE": sysProp = "db.sslMode"; break;
                    case "DB_PARAMS":   sysProp = "db.params"; break;
                    default: continue;
                }

                if (System.getProperty(sysProp) == null) {
                    System.setProperty(sysProp, value);
                }
            }
        } catch (IOException e) {
            System.err.println("[DatabaseConfig] Errore lettura .env: " + e.getMessage());
        }
    }

    public String getJdbcUrl() {
        StringBuilder url = new StringBuilder();
        url.append("jdbc:mysql://").append(host).append(":").append(port).append("/").append(dbName);

        // Query parameters
        url.append("?sslMode=").append(sslMode);

        if (!extraParams.isEmpty()) {
            url.append("&").append(extraParams);
        }

        return url.toString();
    }

    public String getUser()      { return user; }
    public String getPassword()  { return password; }
}
