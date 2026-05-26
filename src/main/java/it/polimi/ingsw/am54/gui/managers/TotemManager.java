package it.polimi.ingsw.am54.gui.managers;

import it.polimi.ingsw.am54.network.client.Client;
import it.polimi.ingsw.am54.network.client.ClientModel;
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

public class TotemManager {

    private final ImageView turnOrderImage;
    private final AnchorPane totemArea;
    private final HBox offeringTrack;
    private final double TOTEM_FACE_SIZE = 50.0;

    public TotemManager(ImageView turnOrderImage, AnchorPane totemArea, HBox offeringTrack) {
        this.turnOrderImage = turnOrderImage;
        this.totemArea = totemArea;
        this.offeringTrack = offeringTrack;
    }

    public void updateTurnOrderTrack(ClientModel model) {
        List<String> turnOrder = model.getTurnOrderTrack();
        List<String> players = model.getPlayers();
        Map<String, TotemColors> colors = model.getPlayerColors();

        if (turnOrder == null || turnOrder.isEmpty()) {
            turnOrder = players != null && !players.isEmpty() ? new ArrayList<>(players) : (colors != null ? new ArrayList<>(colors.keySet()) : null);
        }
        if (turnOrder == null) return;

        int totalPlayers = (players != null && !players.isEmpty()) ? players.size() : 3;
        try {
            if(turnOrderImage != null) turnOrderImage.setImage(new Image(getClass().getResourceAsStream("/images/turnorder/TO_" + totalPlayers + ".png")));
        } catch (Exception e) {}

        if(totemArea != null) totemArea.getChildren().clear();

        List<String> activeOrEmpty = new ArrayList<>();
        List<String> disconnectedInTrack = new ArrayList<>();

        // ==========================================================
        // FIX "INDEXOF": Riversiamo i disconnessi in un ArrayList locale.
        // Questo sblocca la funzione indexOf, evitando errori di compilazione
        // e mantenendo fedele l'ordine cronologico di disconnessione!
        // ==========================================================
        List<String> chronDisconnected = new ArrayList<>();
        if (model.getDisconnectedPlayers() != null) {
            chronDisconnected.addAll(model.getDisconnectedPlayers());
        }

        for (String s : turnOrder) {
            String pName = s.contains(" -> ") ? s.split(" -> ")[s.split(" -> ").length - 1].trim() : s;

            // Usiamo la lista locale chronDisconnected per controllare lo stato
            if (!pName.equalsIgnoreCase("vuoto") && !pName.equalsIgnoreCase("null") && chronDisconnected.contains(pName)) {
                disconnectedInTrack.add(pName);
            } else {
                // Conserviamo i VUOTO (fanno da spaziatori) e i giocatori attivi
                activeOrEmpty.add(pName);
            }
        }

        // Ordiniamo cronologicamente in base alla posizione nella lista dei disconnessi
        disconnectedInTrack.sort((n1, n2) -> {
            int i1 = chronDisconnected.indexOf(n1);
            int i2 = chronDisconnected.indexOf(n2);
            return Integer.compare(i1, i2);
        });

        // Ribaltiamo: il primo che si è disconnesso finirà nell'ultimo slot grafico assoluto
        java.util.Collections.reverse(disconnectedInTrack);

        // Combiniamo: prima i vivi e gli spazi vuoti, poi in coda i disconnessi
        List<String> combinedList = new ArrayList<>(activeOrEmpty);
        combinedList.addAll(disconnectedInTrack);

        int visualPos = (totalPlayers - combinedList.size());
        if (visualPos < 0) visualPos = 0;

        for (String pName : combinedList) {
            int actualPos = visualPos;

            // Incrementiamo la posizione PRIMA di scartare i "VUOTO".
            // Questo permette di lasciare i buchi necessari per spingere i disconnessi a destra.
            visualPos++;

            if (pName.equalsIgnoreCase("vuoto") || pName.equalsIgnoreCase("null")) continue;

            TotemColors playerColor = colors != null ? colors.get(pName) : null;
            if (playerColor == null) continue;

            String colorName = playerColor.name().toLowerCase();
            ImageView totemFace = new ImageView();
            InputStream is = getClass().getResourceAsStream("/images/totem_face_" + colorName + ".png");
            if (is != null) {
                totemFace.setImage(new Image(is));
            } else {
                InputStream fbIs = getClass().getResourceAsStream("/images/totem_" + colorName + "_3d_160x270.png");
                if (fbIs != null) totemFace.setImage(new Image(fbIs));
            }

            totemFace.setFitWidth(TOTEM_FACE_SIZE);
            totemFace.setFitHeight(TOTEM_FACE_SIZE);
            totemFace.setPreserveRatio(true);
            totemFace.getStyleClass().add("totem-face");

            totemFace.setLayoutX(getTotemX(totalPlayers, actualPos));
            totemFace.setLayoutY(getTotemY(totalPlayers, actualPos));
            totemFace.setUserData(actualPos);

            // Applica semi-trasparenza ai disconnessi
            if (disconnectedInTrack.contains(pName)) {
                totemFace.setOpacity(0.5);
            }

            Tooltip.install(totemFace, new Tooltip("Giocatore: " + pName + " (Pos: " + (actualPos + 1) + ")"));
            if(totemArea != null) totemArea.getChildren().add(totemFace);
        }
    }

    public void updateOfferingTrack(ClientModel model, double currentTileHeight) {
        if(offeringTrack == null) return;
        offeringTrack.getChildren().clear();

        // Rimuoviamo il blocco che fermava il disegno se la mappa era vuota.
        Map<Integer, String> offeringData = model.getOfferingTrack();

        int numPlayers = (model.getPlayers() != null) ? model.getPlayers().size() : 3;
        List<String> expectedTiles = getExpectedOfferingTiles(numPlayers);

        // Iteriamo in modo fisso sul numero di tessere ATTESE in base al numero di giocatori
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

            // Controlliamo in modo sicuro se c'è un totem in questa specifica posizione
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
                        (state.equalsIgnoreCase("STARTTURNSTATE") || state.equalsIgnoreCase("STARTTURN") || state.equalsIgnoreCase("BIDDINGSTATE"))) {
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
                        (state.equalsIgnoreCase("STARTTURNSTATE") || state.equalsIgnoreCase("STARTTURN") || state.equalsIgnoreCase("BIDDINGSTATE"))) {
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

        switch (totalPlayers) {
            case 2: startYOriginal = 222.0; stepOriginal = 170.0; break;
            case 3: startYOriginal = 175.0; stepOriginal = 173.0; break;
            case 4: startYOriginal = 133.0; stepOriginal = 173.0; break;
            case 5: startYOriginal = 64.0;  stepOriginal = 174.0; break;
            default: startYOriginal = 222.0; stepOriginal = 170.0; break;
        }

        double centerYOriginal = startYOriginal + (positionIndex * stepOriginal) + (boxHeight / 2.0);
        return (centerYOriginal * oldScaleFactor) - (TOTEM_FACE_SIZE / 2.0) + (8.0 * scaleFactor);
    }

    private List<String> getExpectedOfferingTiles(int numPlayers) {
        switch (numPlayers) {
            case 2: return List.of("B", "C", "E", "F");
            case 3: return List.of("B", "C", "D", "E", "F");
            case 4: return List.of("B", "C", "D", "E", "F", "G");
            case 5: return List.of("A", "B", "C", "D", "E", "F", "G");
            default: return List.of("B", "C", "D", "E", "F");
        }
    }
}