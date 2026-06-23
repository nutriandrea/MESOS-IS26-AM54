/**
 * Persistence of results and leaderboard on a PostgreSQL database (Aiven cloud).
 *
 * <p>{@link it.polimi.ingsw.am54.database.DatabaseConfig} reads credentials from the
 * {@code .env} file; {@link it.polimi.ingsw.am54.database.DatabaseManager} creates the table,
 * saves match results, and queries the leaderboard (filtered by number of
 * players, including the requester's position). If the database is unreachable, the
 * layer degrades gracefully by disabling the leaderboard.</p>
 */
package it.polimi.ingsw.am54.database;