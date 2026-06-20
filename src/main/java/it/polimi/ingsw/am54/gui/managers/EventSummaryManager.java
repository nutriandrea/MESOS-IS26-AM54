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

/**
 * Manages the creation and display of the event summary at the end of a round.
 * Shows for each event the resource trends (food, aura, magic) of each
 * player, comparing the initial situation with the post-event final one.
 *
 * @author AM54
 */
public class EventSummaryManager {

    private final VBox eventSummaryContainer;

    /**
     * Constructs the manager with the container used to display event summaries.
     *
     * @param eventSummaryContainer the VBox that will hold event summary nodes
     */
    public EventSummaryManager(VBox eventSummaryContainer) {
        this.eventSummaryContainer = eventSummaryContainer;
    }

    /**
     * Builds the event summary UI from the given round reports, showing each
     * event's resource changes per player.
     *
     * @param pendingReports the list of event reports from the resolved round
     * @param playerColors   map of player nickname to their totem color
     */
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
                lblName.setPrefWidth(140);
                lblName.getStyleClass().add("event-player-name");

                // First Column: Initial Situation
                Label lblActivation = new Label(String.format("Inizio: 🍖%d  ✨%d  🔮%d",
                        data.oldFood, data.oldAura, data.oldMagic));
                lblActivation.setPrefWidth(220);
                lblActivation.getStyleClass().add("event-player-activation");

                String foodSign = data.diffFood >= 0 ? "+" : "";
                String auraSign = data.diffAura >= 0 ? "+" : "";
                String magicSign = data.diffMagic >= 0 ? "+" : "";

                // Second Column: Resource Variation
                Label lblDiff = new Label(String.format("Variazione: 🍖%s%d  ✨%s%d  🔮%s%d",
                        foodSign, data.diffFood, auraSign, data.diffAura, magicSign, data.diffMagic));
                lblDiff.setPrefWidth(240);

                if (data.diffAura < 0 || data.diffFood < 0 || data.diffMagic < 0) {
                    lblDiff.getStyleClass().add("event-delta-negative");
                } else if (data.diffAura > 0 || data.diffFood > 0 || data.diffMagic > 0) {
                    lblDiff.getStyleClass().add("event-delta-positive");
                } else {
                    lblDiff.getStyleClass().add("event-delta-neutral");
                }

                // Third Column: Post-Event Final Situation
                Label lblFinal = new Label(String.format("Fine: 🍖%d  ✨%d  🔮%d",
                        data.oldFood + data.diffFood, data.oldAura + data.diffAura, data.oldMagic + data.diffMagic));
                lblFinal.setPrefWidth(220);
                lblFinal.getStyleClass().add("event-player-activation");

                row.getChildren().addAll(totemIcon, lblName, lblActivation, lblDiff, lblFinal);
                eventBox.getChildren().add(row);
            }
            eventSummaryContainer.getChildren().add(eventBox);
        }
    }
}