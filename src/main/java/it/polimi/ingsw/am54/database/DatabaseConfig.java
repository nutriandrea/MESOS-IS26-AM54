package it.polimi.ingsw.am54.database;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Configuration for the MySQL database, read from a .env file
 * in the working directory (gitignored — secrets stay local).
 *
 * If .env is missing, falls back to localhost defaults (for development).
 */
public class DatabaseConfig {
    private static final Logger LOGGER = Logger.getLogger(DatabaseConfig.class.getName());

    private final String host;
    private final int port;
    private final String dbName;
    private final String user;
    private final String password;
    private final String sslMode;
    private final String params;

    /**
     * Constructs a DatabaseConfig, loading values from a {@code .env} file
     * or falling back to localhost defaults when the file is absent.
     */
    public DatabaseConfig() {
        Map<String, String> env = loadEnv();

        this.host     = env.getOrDefault("DB_HOST", "localhost");
        this.port     = Integer.parseInt(env.getOrDefault("DB_PORT", "3306"));
        this.dbName   = env.getOrDefault("DB_NAME", "am54");
        this.user     = env.getOrDefault("DB_USER", "root");
        this.password = env.getOrDefault("DB_PASSWORD", "");
        this.sslMode  = env.getOrDefault("DB_SSL_MODE", "PREFERRED");
        this.params   = env.getOrDefault("DB_PARAMS", "");
    }

    /** @return the JDBC URL assembled from host, port, database name, SSL mode and extra params */
    public String getJdbcUrl() {
        StringBuilder url = new StringBuilder();
        url.append("jdbc:mysql://").append(host).append(":").append(port).append("/").append(dbName);
        url.append("?sslMode=").append(sslMode);
        url.append("&allowPublicKeyRetrieval=true");
        url.append("&enabledTLSProtocols=TLSv1.2,TLSv1.3");
        if (!params.isEmpty()) {
            url.append("&").append(params);
        }
        return url.toString();
    }

    /** @return the database user */
    public String getUser()       { return user; }
    /** @return the database password */
    public String getPassword()   { return password; }
    /** @return the database host */
    public String getHost()       { return host; }
    /** @return the database port */
    public int    getPort()       { return port; }
    /** @return the database name */
    public String getDbName()     { return dbName; }

    /**
     * Reads key=value pairs from .env file in the working directory.
     * Returns empty map if the file does not exist.
     */
    private static Map<String, String> loadEnv() {
        Map<String, String> map = new HashMap<>();
        Path path = Paths.get(".env");
        if (!Files.exists(path)) {
            LOGGER.info(".env not found — uso impostazioni predefinite (localhost)");
            return map;
        }
        try {
            for (String line : Files.readAllLines(path)) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int eq = line.indexOf('=');
                if (eq > 0) {
                    String key = line.substring(0, eq).trim();
                    String val = line.substring(eq + 1).trim();
                    map.put(key, val);
                }
            }
        } catch (IOException e) {
            LOGGER.warning("Errore leggendo .env: " + e.getMessage());
        }
        return map;
    }
}
