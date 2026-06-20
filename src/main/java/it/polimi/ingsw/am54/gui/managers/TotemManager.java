package it.polimi.ingsw.am54.gui.managers;

import it.polimi.ingsw.am54.network.client.Client;
import it.polimi.ingsw.am54.network.client.ClientModel;
import it.polimi.ingsw.am54.gui.states.ClientStateRegistry;
import it.polimi.ingsw.am54.model.TotemColors;
import javafx.scene.Group;
import javafx.scene.Cursor;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Manages the selection and placement of players' totems on the board.
 * Handles the update of the turn order track, the offering
 * track and the placement of 3D totems on offering tiles.
 *
 * @author AM54
 */
public class TotemManager {

    private final ImageView turnOrderImage;
    private final AnchorPane totemArea;
    private final HBox offeringTrack;
    private final double TOTEM_FACE_SIZE = 50.0;

    /**
     * Constructs the manager with the JavaFX nodes it will control.
     *
     * @param turnOrderImage the ImageView displaying the turn-order track image
     * @param totemArea      the AnchorPane where totem face icons are placed
     * @param offeringTrack  the HBox containing the offering tiles
     */
    public TotemManager(ImageView turnOrderImage, AnchorPane totemArea, HBox offeringTrack) {
        this.turnOrderImage = turnOrderImage;
        this.totemArea = totemArea;
        this.offeringTrack = offeringTrack;
    }

    /**
     * Updates the turn-order track image and places totem face icons for each
     * player in turn order. Disconnected or empty slots are rendered invisibly
     * to preserve layout.
     *
     * @param model the current client model
     */
    public void updateTurnOrderTrack(ClientModel model) {
        java.util.List<String> turnOrder = model.getTurnOrderTrack();
        java.util.List<String> players = model.getPlayers();
        java.util.Map<String, TotemColors> colors = model.getPlayerColors();

        if (turnOrder == null) return;

        int totalPlayers = (players != null && !players.isEmpty()) ? players.size() : 3;
        try {
            if(turnOrderImage != null) turnOrderImage.setImage(new javafx.scene.image.Image(getClass().getResourceAsStream("/images/turnorder/TO_" + totalPlayers + ".png")));
        } catch (Exception e) {}

        if(totemArea != null) totemArea.getChildren().clear();

        java.util.Set<String> disconnected = model.getDisconnectedPlayers();
        if (disconnected == null) disconnected = new java.util.HashSet<>();

        for (int i = 0; i < turnOrder.size(); i++) {
            String s = turnOrder.get(i);
            String pName = s.contains(" -> ") ? s.split(" -> ")[s.split(" -> ").length - 1].trim() : s;

            // Create a base image to ALWAYS occupy the physical space
            javafx.scene.image.ImageView totemFace = new javafx.scene.image.ImageView();
            totemFace.setFitWidth(TOTEM_FACE_SIZE);
            totemFace.setFitHeight(TOTEM_FACE_SIZE);
            totemFace.setPreserveRatio(true);

            // If the slot is EMPTY or of a DISCONNECTED player, make it invisible.
            // This keeps the layout intact, preventing shifting!
            if (pName.equalsIgnoreCase("vuoto") || pName.equalsIgnoreCase("null") || disconnected.contains(pName)) {
                totemFace.setOpacity(0.0);
            } else {
                TotemColors playerColor = colors != null ? colors.get(pName) : null;
                if (playerColor != null) {
                    String colorName = playerColor.name().toLowerCase();
                    java.io.InputStream is = getClass().getResourceAsStream("/images/totem_face_" + colorName + ".png");
                    if (is != null) {
                        totemFace.setImage(new javafx.scene.image.Image(is));
                    } else {
                        java.io.InputStream fbIs = getClass().getResourceAsStream("/images/totem_" + colorName + "_3d_160x270.png");
                        if (fbIs != null) totemFace.setImage(new javafx.scene.image.Image(fbIs));
                    }
                    totemFace.getStyleClass().add("totem-face");
                    javafx.scene.control.Tooltip.install(totemFace, new javafx.scene.control.Tooltip("Giocatore: " + pName + " (Pos: " + (i + 1) + ")"));
                } else {
                    totemFace.setOpacity(0.0);
                }
            }

            totemFace.setLayoutX(getTotemX(totalPlayers, i));
            totemFace.setLayoutY(getTotemY(totalPlayers, i));
            totemFace.setUserData(i);

            if(totemArea != null) totemArea.getChildren().add(totemFace);
        }
    }

    /**
     * Rebuilds the offering track with tiles and 3D totem markers for each
     * occupant, wiring up hover and click interactions for the active player.
     *
     * @param model              the current client model
     * @param currentTileHeight  the rendered height of each offering tile
     */
    public void updateOfferingTrack(ClientModel model, double currentTileHeight) {
        if(offeringTrack == null) return;
        offeringTrack.getChildren().clear();

        // Remove the block that stopped drawing if the map was empty.
        Map<Integer, String> offeringData = model.getOfferingTrack();

        int numPlayers = (model.getPlayers() != null) ? model.getPlayers().size() : 3;
        List<String> expectedTiles = getExpectedOfferingTiles(numPlayers);

        // Iterate fixed on the number of EXPECTED tiles based on the number of players
        for (int position = 0; position < expectedTiles.size(); position++) {
            String tileLetter = expectedTiles.get(position);
            Group tileGroup = new Group();

            ImageView tileView = new ImageView();
            try { tileView.setImage(new Image(getClass().getResourceAsStream("/images/offeringtiles/OT_" + tileLetter + ".png"))); } catch (Exception e) { continue; }
            tileView.setFitHeight(currentTileHeight > 0 ? currentTileHeight : 290.0);
            tileView.setPreserveRatio(true);
            tileView.getStyleClass().add("offering-tile-img");

            AnchorPane totemPane = new AnchorPane();
            totemPane.setPickOnBounds(false);

            // Safely check if there is a totem at this specific position
            String occupier = (offeringData != null) ? offeringData.get(position) : null;

            boolean hasReturned = false;
            String playerName = "";

            if (occupier != null && !occupier.equalsIgnoreCase("VUOTO") && !occupier.equalsIgnoreCase("NULL")) {
                playerName = occupier.contains(" -> ") ? occupier.split(" -> ")[occupier.split(" -> ").length - 1].trim() : occupier;
                if (model.getTurnOrderTrack() != null) {
                    for (String returnedPlayer : model.getTurnOrderTrack()) {
                        String returnedName = returnedPlayer.contains(" -> ") ? returnedPlayer.split(" -> ")[returnedPlayer.split(" -> ").length - 1].trim() : returnedPlayer;
                        if (returnedName.equalsIgnoreCase(playerName)) {
                            hasReturned = true;
                            break;
                        }
                    }
                }
            }

            if (!hasReturned && !playerName.isEmpty()) {
                TotemColors color = model.getPlayerColors().get(playerName);
                if (color == null) {
                    for(Map.Entry<String, TotemColors> entry : model.getPlayerColors().entrySet()) {
                        if(entry.getKey().equalsIgnoreCase(playerName)) { color = entry.getValue(); break; }
                    }
                }

                if (color != null) {
                    String colorName = color.name().toLowerCase();
                    InputStream is = getClass().getResourceAsStream("/images/totem_" + colorName + "_3d_160x270.png");

                    if (is != null) {
                        try {
                            ImageView totem3dView = new ImageView(new Image(is));
                            totem3dView.setPreserveRatio(true);
                            totem3dView.getStyleClass().add("offering-totem");

                            double targetTotemH = 220.0;
                            double targetTotemW = targetTotemH * (160.0 / 270.0);
                            double centerX = 183.0 + ((392.0 - 183.0) / 2.0);
                            double bottomY = 306.0;

                            double h = currentTileHeight > 0 ? currentTileHeight : 290.0;
                            double totemW = h * (targetTotemW / 940.0);
                            double totemH = h * (targetTotemH / 940.0);
                            double totemX = h * ((centerX - targetTotemW / 2.0) / 940.0);
                            double totemY = h * ((bottomY - targetTotemH) / 940.0);

                            totem3dView.setFitWidth(totemW);
                            totem3dView.setFitHeight(totemH);
                            totem3dView.setLayoutX(totemX);
                            totem3dView.setLayoutY(totemY);

                            totemPane.getChildren().add(totem3dView);
                        } catch (Exception e) {}
                    }
                }
                Tooltip.install(tileGroup, new Tooltip("Occupata da: " + playerName));
            } else {
                Tooltip.install(tileGroup, new Tooltip("Tessera " + tileLetter + " (Libera)"));
            }

            tileGroup.getChildren().addAll(tileView, totemPane);

            tileGroup.setOnMouseEntered(e -> {
                String myName = Client.getInstance().getNickname();
                String state = model.getCurrentState();
                if (myName != null && myName.equals(model.getCurrentPlayer()) && state != null &&
                        ClientStateRegistry.getHandler(state).canInteractWithTiles()) {
                    tileGroup.setTranslateY(-10);
                    tileGroup.setCursor(Cursor.HAND);
                }
            });

            tileGroup.setOnMouseExited(e -> {
                tileGroup.setTranslateY(0);
                tileGroup.setCursor(Cursor.DEFAULT);
            });

            final int tileIndex = position;
            tileGroup.setOnMouseClicked(e -> {
                String myName = Client.getInstance().getNickname();
                String state = model.getCurrentState();

                if (myName != null && myName.equals(model.getCurrentPlayer()) && state != null &&
                        ClientStateRegistry.getHandler(state).canInteractWithTiles()) {
                    if (Client.getInstance().getConnection() != null) {
                        try {
                            Client.getInstance().getConnection().placeTotem(tileIndex);
                        } catch (Exception ex) {}
                    }
                }
            });

            offeringTrack.getChildren().add(tileGroup);
        }
    }
    /**
     * Recomputes and repositions all totem face icons on the turn-order track
     * based on the current layout dimensions.
     */
    public void refreshTotemsPosition() {
        ClientModel model = Client.getInstance().getModel();
        if (model == null || totemArea == null || totemArea.getChildren().isEmpty()) return;

        int totalPlayers = (model.getPlayers() != null && !model.getPlayers().isEmpty()) ? model.getPlayers().size() : 3;

        for (javafx.scene.Node node : totemArea.getChildren()) {
            Object userData = node.getUserData();
            if (userData instanceof Integer) {
                int actualPos = (Integer) userData;
                node.setLayoutX(getTotemX(totalPlayers, actualPos));
                node.setLayoutY(getTotemY(totalPlayers, actualPos));
            }
        }
    }

    private double getTotemX(int totalPlayers, int positionIndex) {
        double currentHeight = turnOrderImage != null ? turnOrderImage.getFitHeight() : 290.0;
        if (currentHeight <= 0) currentHeight = 290.0;
        double scaleFactor = currentHeight / 290.0;
        double turnOrderWidth = 180.5 * scaleFactor;
        return (turnOrderWidth / 2.0) - (TOTEM_FACE_SIZE / 2.0);
    }

    private double getTotemY(int totalPlayers, int positionIndex) {
        double currentHeight = turnOrderImage != null ? turnOrderImage.getFitHeight() : 290.0;
        if (currentHeight <= 0) currentHeight = 290.0;
        double scaleFactor = currentHeight / 290.0;

        double oldScaleFactor = currentHeight / 940.0;
        double boxHeight = 332.0 - 222.0;
        double startYOriginal = 0;
        double stepOriginal = 0;

        Map<Integer, double[]> config = Map.of(
            2, new double[]{222.0, 170.0},
            3, new double[]{175.0, 173.0},
            4, new double[]{133.0, 173.0},
            5, new double[]{64.0, 174.0}
        );
        double[] coords = config.getOrDefault(totalPlayers, new double[]{222.0, 170.0});
        startYOriginal = coords[0];
        stepOriginal = coords[1];

        double centerYOriginal = startYOriginal + (positionIndex * stepOriginal) + (boxHeight / 2.0);
        return (centerYOriginal * oldScaleFactor) - (TOTEM_FACE_SIZE / 2.0) + (8.0 * scaleFactor);
    }

    private List<String> getExpectedOfferingTiles(int numPlayers) {
        Map<Integer, List<String>> tiles = Map.of(
            2, List.of("B", "C", "E", "F"),
            3, List.of("B", "C", "D", "E", "F"),
            4, List.of("B", "C", "D", "E", "F", "G"),
            5, List.of("A", "B", "C", "D", "E", "F", "G")
        );
        return tiles.getOrDefault(numPlayers, List.of("B", "C", "D", "E", "F"));
    }
}