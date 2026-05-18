package it.polimi.ingsw.am54.gui.managers;

import it.polimi.ingsw.am54.network.client.EventReport;
import it.polimi.ingsw.am54.network.client.PlayerDelta;
import it.polimi.ingsw.am54.model.TotemColors;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Map;

public class EventSummaryManager {

    private final VBox eventSummaryContainer;

    public EventSummaryManager(VBox eventSummaryContainer) {
        this.eventSummaryContainer = eventSummaryContainer;
    }

    public void buildEventSummary(List<EventReport> pendingReports, Map<String, TotemColors> playerColors) {
        if (eventSummaryContainer == null) return;
        eventSummaryContainer.getChildren().clear();

        for (EventReport report : pendingReports) {
            VBox eventBox = new VBox(15);
            eventBox.getStyleClass().add("opponent-box");

            Label eventTitle = new Label("📜 EVENTO: " + report.eventName.toUpperCase());
            eventTitle.getStyleClass().addAll("opponent-name-label", "event-summary-title");
            eventBox.getChildren().add(eventTitle);

            for (PlayerDelta data : report.playerDeltas) {
                HBox row = new HBox(20);
                row.setAlignment(Pos.CENTER_LEFT);
                row.getStyleClass().add("event-player-row");

                ImageView totemIcon = new ImageView();
                totemIcon.setFitWidth(40);
                totemIcon.setFitHeight(40);
                totemIcon.setPreserveRatio(true);
                if (playerColors != null && playerColors.get(data.nickname) != null) {
                    String colorName = playerColors.get(data.nickname).name().toLowerCase();
                    try {
                        totemIcon.setImage(new Image(getClass().getResourceAsStream("/images/totem_front_" + colorName + ".png")));
                    } catch (Exception e) {}
                }

                Label lblName = new Label(data.nickname.toUpperCase());
                lblName.setPrefWidth(130);
                lblName.getStyleClass().add("event-player-name");

                // FIX SPAZIO: Stringa ridotta al minimo, ravvicinata ed espressa con icone chiare
                Label lblActivation = new Label(String.format("Inizio: 🍖%d  ✨%d  🔮%d",
                        data.oldFood, data.oldAura, data.oldMagic));
                lblActivation.setPrefWidth(200);
                lblActivation.getStyleClass().add("event-player-activation");

                String foodSign = data.diffFood >= 0 ? "+" : "";
                String auraSign = data.diffAura >= 0 ? "+" : "";
                String magicSign = data.diffMagic >= 0 ? "+" : "";

                // FIX SPAZIO: Formato compatto per i delta numerici relativi alle variazioni
                Label lblDiff = new Label(String.format("Variazione: 🍖%s%d  ✨%s%d  🔮%s%d",
                        foodSign, data.diffFood, auraSign, data.diffAura, magicSign, data.diffMagic));

                if (data.diffAura < 0 || data.diffFood < 0 || data.diffMagic < 0) {
                    lblDiff.getStyleClass().add("event-delta-negative");
                } else if (data.diffAura > 0 || data.diffFood > 0 || data.diffMagic > 0) {
                    lblDiff.getStyleClass().add("event-delta-positive");
                } else {
                    lblDiff.getStyleClass().add("event-delta-neutral");
                }

                row.getChildren().addAll(totemIcon, lblName, lblActivation, lblDiff);
                eventBox.getChildren().add(row);
            }
            eventSummaryContainer.getChildren().add(eventBox);
        }
    }
}