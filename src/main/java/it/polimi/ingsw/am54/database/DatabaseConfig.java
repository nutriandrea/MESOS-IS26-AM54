package it.polimi.ingsw.am54.database;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Configuration for the PostgreSQL database.
 * Reads connection parameters from a {@code db.properties} file
 * located in the server's working directory.
 *
 * If the file is missing, sensible defaults are used (localhost:5432/am54).
 *
 * Example db.properties:
 * <pre>
 * db.host=localhost
 * db.port=5432
 * db.name=am54
 * db.user=postgres
 * db.password=
 * </pre>
 */
public class DatabaseConfig {
    private static final Logger LOGGER = Logger.getLogger(DatabaseConfig.class.getName());

    private final String host;
    private final int port;
    private final String dbName;
    private final String user;
    private final String password;

    public DatabaseConfig() {
        Properties props = loadProperties();
        this.host     = props.getProperty("db.host", "localhost");
        this.port     = Integer.parseInt(props.getProperty("db.port", "5432"));
        this.dbName   = props.getProperty("db.name", "am54");
        this.user     = props.getProperty("db.user", "postgres");
        this.password = props.getProperty("db.password", "");

        LOGGER.info("PostgreSQL config: " + host + ":" + port + "/" + dbName + " (user: " + user + ")");
    }

    public String getJdbcUrl() {
        return "jdbc:postgresql://" + host + ":" + port + "/" + dbName;
    }

    public String getUser()      { return user; }
    public String getPassword()  { return password; }
    public String getDbName()    { return dbName; }

    /**
     * Tries to load db.properties from the working directory first,
     * then falls back to classpath resource.
     */
    private static Properties loadProperties() {
        Properties props = new Properties();

        // 1) Working directory
        Path wdPath = Paths.get(System.getProperty("user.dir"), "db.properties");
        if (Files.exists(wdPath)) {
            try (InputStream is = new FileInputStream(wdPath.toFile())) {
                props.load(is);
                LOGGER.info("Loaded db.properties from working directory: " + wdPath.toAbsolutePath());
                return props;
            } catch (IOException e) {
                LOGGER.warning("Found db.properties but could not read it: " + e.getMessage());
            }
        }

        // 2) Classpath resource
        try (InputStream is = DatabaseConfig.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (is != null) {
                props.load(is);
                LOGGER.info("Loaded db.properties from classpath");
                return props;
            }
        } catch (IOException e) {
            LOGGER.warning("Could not read db.properties from classpath: " + e.getMessage());
        }

        // 3) Defaults
        LOGGER.info("No db.properties found – using default PostgreSQL config (localhost:5432/am54)");
        return props;
    }
}
