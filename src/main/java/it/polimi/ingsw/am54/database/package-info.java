/**
 * Persistenza dei risultati e classifica su database PostgreSQL (Aiven cloud).
 *
 * <p>{@link it.polimi.ingsw.am54.database.DatabaseConfig} legge le credenziali dal file
 * {@code .env}; {@link it.polimi.ingsw.am54.database.DatabaseManager} crea la tabella,
 * salva i risultati delle partite e interroga la classifica (filtrata per numero di
 * giocatori, con posizione del richiedente). Se il database non è raggiungibile, il
 * layer degrada con grazia disabilitando la classifica.</p>
 */
package it.polimi.ingsw.am54.database;
