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
 * Manages the JSON file backup of the game for server crash recovery.
 * Uses Gson with {@link RuntimeTypeAdapterFactory} to polymorphically serialize
 * game states, card effects, and Tribe cards.
 */
public class PersistenceManager {
    private final File backupFile;
    private final Gson gson;

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
                // Buildings
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
                // Events
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
     * Atomically saves the entire game to a file: first writes to a temporary file
     * (closing it) and then moves it to the final backup. This prevents a crash
     * mid-write from corrupting the previous backup.
     *
     * @param game the game to save
     */
    public synchronized void saveGame(Game game) {
        File tempFile = new File(backupFile.getAbsolutePath() + ".tmp");

        // 1. Write the whole game to the temp file and CLOSE the writer first.
        //    Renaming/deleting while the FileWriter is still open fails silently on
        //    Windows (the file is locked and the buffer may be unflushed), which would
        //    leave a stranded ".tmp" file and a missing backup -> recovery loads null.
        try (FileWriter writer = new FileWriter(tempFile)) {
            gson.toJson(game, writer);
        } catch (IOException e) {
            System.err.println("[PERSISTENZA] Errore salvataggio: " + e.getMessage());
            return;
        }

        // 2. Atomically replace the old backup now that the temp file is fully flushed.
        try {
            java.nio.file.Files.move(tempFile.toPath(), backupFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException atomicEx) {
            // Fallback for filesystems that don't support atomic moves.
            try {
                java.nio.file.Files.move(tempFile.toPath(), backupFile.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                System.err.println("[PERSISTENZA] Errore salvataggio (rename): " + e.getMessage());
            }
        } catch (IOException e) {
            System.err.println("[PERSISTENZA] Errore salvataggio (rename): " + e.getMessage());
        }
    }

    /**
     * Loads the game from the backup file, if present, and restores its
     * transient references between objects ({@link Game#restoreReferences()}).
     *
     * @return the restored game, or {@code null} if there is no backup or it is unreadable
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

    /**
     * Deletes the backup file (when the game is over) so that a server restart does not
     * "recover" an already finished game.
     */
    public synchronized void clearBackup() {
        if (backupFile.exists()) {
            backupFile.delete();
            System.out.println("[PERSISTENZA] Partita terminata. Backup eliminato.");
        }
    }
}