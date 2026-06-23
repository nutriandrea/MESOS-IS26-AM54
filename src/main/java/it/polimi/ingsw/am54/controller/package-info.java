/**
 * Controller del server (la "C" di MVC). {@link it.polimi.ingsw.am54.controller.Controller}
 * riceve le richieste dei client, le serializza su un unico monitor e le inoltra al
 * modello {@link it.polimi.ingsw.am54.model.Game}.
 *
 * <p>Gestisce inoltre il ciclo di vita delle connessioni: join/riconnessione,
 * disconnessioni temporanee, sospensione della partita con un solo giocatore attivo,
 * vittoria per abbandono, persistenza per il recupero da crash e salvataggio dei
 * risultati su database.</p>
 */
package it.polimi.ingsw.am54.controller;
