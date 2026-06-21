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
 * Handles Game persistence to JSON file (crash recovery).
 * Usa Gson con adapter polimorfici per serializzare/deserializzare
 * GameState, IEffect and TribeCard in their concrete subclasses.
 * The backup file is created by the Server at startup and deleted
 * at the end of the game (clearBackup).
 */
public class PersistenceManager {
    private final File backupFile;
    private final Gson gson;

    /**
     * Constructs a PersistenceManager that reads from and writes to the
     * given file, configuring Gson with polymorphic type adapters.
     *
     * @param filename the backup file name
     */
    public PersistenceManager(String filename) {
        this.backupFile = new File(filename);

        // Factory for GameState (already present)
        RuntimeTypeAdapterFactory<GameState> stateAdapter = RuntimeTypeAdapterFactory
                .of(GameState.class, "type")
                .registerSubtype(SetUpGameState.class, "SetUpGameState")
                .registerSubtype(StartTurnState.class, "StartTurnState")
                .registerSubtype(PickingState.class, "PickingState")
                .registerSubtype(EventState.class, "EventState")
                .registerSubtype(EndGameState.class, "EndGameState");

        // Factory for IEffect
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
                // Characters
                .registerSubtype(CharacterPassive.class, "CharacterPassive")
                .registerSubtype(CharacterHunter.class, "CharacterHunter")
                .registerSubtype(CharacterShaman.class, "CharacterShaman")
                .registerSubtype(CharacterConstructor.class, "CharacterConstructor")
                // Eventi
                .registerSubtype(EventShaman.class, "EventShaman")
                .registerSubtype(EventFarmer.class, "EventFarmer")
                .registerSubtype(EventArtist.class, "EventArtist")
                .registerSubtype(EventHunter.class, "EventHunter");

        // Factory for TribeCard
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

    /**
     * Serializes the given game to the backup file atomically (write to
     * a temp file then rename).
     *
     * @param game the game to save
     */
    public synchronized void saveGame(Game game) {
        File tempFile = new File(backupFile.getAbsolutePath() + ".tmp");
        try (FileWriter writer = new FileWriter(tempFile)) {
            gson.toJson(game, writer);
            if (backupFile.exists() && !backupFile.delete()) {
                System.err.println("[PERSISTENZA] Unable rimuovere il vecchio backup.");
            }
            tempFile.renameTo(backupFile);
        } catch (IOException e) {
            System.err.println("[PERSISTENZA] Errore salvataggio: " + e.getMessage());
        }
    }

    /**
     * Loads and deserializes the game from the backup file.
     *
     * @return the restored game, or {@code null} if no backup exists or loading fails
     */
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

    /** Deletes the backup file if it exists, signalling the game is over. */
    public synchronized void clearBackup() {
        if (backupFile.exists()) {
            backupFile.delete();
            System.out.println("[PERSISTENZA] Partita terminata. Backup eliminato.");
        }
    }
}