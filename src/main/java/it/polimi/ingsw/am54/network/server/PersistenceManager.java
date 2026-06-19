package it.polimi.ingsw.am54.network.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import it.polimi.ingsw.am54.model.Game;
import it.polimi.ingsw.am54.model.GameState;
import it.polimi.ingsw.am54.model.IEffect;
import it.polimi.ingsw.am54.model.effects.buildings.*;
import it.polimi.ingsw.am54.model.effects.characters.*;
import it.polimi.ingsw.am54.model.effects.events.*;
import it.polimi.ingsw.am54.model.states.*;
import java.io.*;

/**
 * Gestore della persistenza del Game su file JSON (crash recovery).
 * Usa Gson con adapter polimorfici per serializzare/deserializzare
 * GameState, IEffect e TribeCard nelle loro sottoclassi concrete.
 * Il file di backup viene creato dal Server all'avvio e cancellato
 * al termine della partita (clearBackup).
 */
public class PersistenceManager {
    private final File backupFile;
    private final Gson gson;

    public PersistenceManager(String filename) {
        this.backupFile = new File(filename);

        // Factory per GameState (già presente)
        RuntimeTypeAdapterFactory<GameState> stateAdapter = RuntimeTypeAdapterFactory
                .of(GameState.class, "type")
                .registerSubtype(SetUpGameState.class, "SetUpGameState")
                .registerSubtype(StartTurnState.class, "StartTurnState")
                .registerSubtype(PickingState.class, "PickingState")
                .registerSubtype(EventState.class, "EventState")
                .registerSubtype(EndGameState.class, "EndGameState");

        // Factory per IEffect
        RuntimeTypeAdapterFactory<IEffect> effectAdapter = RuntimeTypeAdapterFactory
                .of(IEffect.class, "effectType")
                // Edifici
                .registerSubtype(BuildingEventCostModifier.class, "BuildingEventCostModifier")
                .registerSubtype(BuildingShamanPassive.class, "BuildingShamanPassive")
                .registerSubtype(BuildingSixDifferentAura.class, "BuildingSixDifferentAura")
                .registerSubtype(BuildingImmediateResource.class, "BuildingImmediateResource")
                .registerSubtype(BuildingJobAuraBonus.class, "BuildingJobAuraBonus")
                .registerSubtype(BuildingExtraPick.class, "BuildingExtraPick")
                .registerSubtype(BuildingTurnOrderBonus.class, "BuildingTurnOrderBonus")
                .registerSubtype(BuildingDoubleInventor.class, "BuildingDoubleInventor")
                .registerSubtype(BuildingSixOfType.class, "BuildingSixOfType")
                .registerSubtype(BuildingEndgameAura.class, "BuildingEndgameAura")
                // Personaggi
                .registerSubtype(CharacterPassive.class, "CharacterPassive")
                .registerSubtype(CharacterHunter.class, "CharacterHunter")
                .registerSubtype(CharacterShaman.class, "CharacterShaman")
                .registerSubtype(CharacterConstructor.class, "CharacterConstructor")
                // Eventi
                .registerSubtype(EventShaman.class, "EventShaman")
                .registerSubtype(EventFarmer.class, "EventFarmer")
                .registerSubtype(EventArtist.class, "EventArtist")
                .registerSubtype(EventHunter.class, "EventHunter");

        // Factory per TribeCard
        RuntimeTypeAdapterFactory<it.polimi.ingsw.am54.model.TribeCard> tribeCardAdapter = RuntimeTypeAdapterFactory
                .of(it.polimi.ingsw.am54.model.TribeCard.class, "tribeCardType")
                .registerSubtype(it.polimi.ingsw.am54.model.CharacterCard.class, "CharacterCard")
                .registerSubtype(it.polimi.ingsw.am54.model.EventCard.class, "EventCard");

        this.gson = new GsonBuilder()
                .registerTypeAdapterFactory(stateAdapter)
                .registerTypeAdapterFactory(effectAdapter)
                .registerTypeAdapterFactory(tribeCardAdapter)
                .create();
    }

    public synchronized void saveGame(Game game) {
        File tempFile = new File(backupFile.getAbsolutePath() + ".tmp");
        try (FileWriter writer = new FileWriter(tempFile)) {
            gson.toJson(game, writer);
            if (backupFile.exists() && !backupFile.delete()) {
                System.err.println("[PERSISTENZA] Impossibile rimuovere il vecchio backup.");
            }
            tempFile.renameTo(backupFile);
        } catch (IOException e) {
            System.err.println("[PERSISTENZA] Errore salvataggio: " + e.getMessage());
        }
    }

    public synchronized Game loadGame() {
        if (!backupFile.exists()) return null;
        try (FileReader reader = new FileReader(backupFile)) {
            Game loaded = gson.fromJson(reader, Game.class);
            if (loaded != null) {
                loaded.restoreReferences();
            }

            return loaded;
        } catch (Exception e) {
            System.err.println("[PERSISTENZA] Errore caricamento: " + e.getMessage());
            return null;
        }
    }

    public synchronized void clearBackup() {
        if (backupFile.exists()) {
            backupFile.delete();
            System.out.println("[PERSISTENZA] Partita terminata. Backup eliminato.");
        }
    }
}