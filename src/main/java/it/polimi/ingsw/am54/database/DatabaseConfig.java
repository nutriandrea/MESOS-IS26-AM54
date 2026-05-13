package it.polimi.ingsw.am54.database;

/**
 * Configuration for the embedded H2 database.
 * The database file (am54.mv.db) is created automatically in the working directory.
 * No external setup required.
 *
 * Optionally override via system properties (only needed for custom paths):
 *   - db.file     (default: am54)
 */
public class DatabaseConfig {
    private final String file;

    public DatabaseConfig() {
        this.file = System.getProperty("db.file", "am54");
    }

    public DatabaseConfig(String file) {
        this.file = file;
    }

    public String getJdbcUrl() {
        return "jdbc:h2:./" + file + ";AUTO_SERVER=TRUE";
    }

    public String getUser()   { return "sa"; }
    public String getPassword() { return ""; }
    public String getFile()   { return file; }
}
