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

        // Cicla su tutti gli eventi accumulati dall'alto in basso
        for (EventReport report : pendingReports) {

            // Box contenitore per il singolo evento (Usa lo stile opponent-box dall'FXML/CSS)
            VBox eventBox = new VBox(15);
            eventBox.getStyleClass().add("opponent-box");

            // Intestazione col nome dell'evento (Stile delegato al CSS esterno)
            Label eventTitle = new Label("📜 EVENTO: " + report.eventName.toUpperCase());
            eventTitle.getStyleClass().addAll("opponent-name-label", "event-summary-title");
            eventBox.getChildren().add(eventTitle);

            // Genera le righe per ciascun giocatore partecipante all'evento
            for (PlayerDelta data : report.playerDeltas) {
                HBox row = new HBox(20);
                row.setAlignment(Pos.CENTER_LEFT);
                row.getStyleClass().add("event-player-row"); // Fissato nel CSS esterno

                // 1. Iconcina totem_front del colore del player
                ImageView totemIcon = new ImageView();
                totemIcon.setFitWidth(40);
                totemIcon.setFitHeight(40);
                totemIcon.setPreserveRatio(true);
                if (playerColors != null && playerColors.get(data.nickname) != null) {
                    String colorName = playerColors.get(data.nickname).name().toLowerCase();
                    try {
                        totemIcon.setImage(new Image(getClass().getResourceAsStream("/images/totem_front_" + colorName + ".png")));
                    } catch (Exception e) {
                        System.err.println("[GUI] Impossibile caricare /images/totem_front_" + colorName + ".png");
                    }
                }

                // 2. Nickname player
                Label lblName = new Label(data.nickname.toUpperCase());
                lblName.setPrefWidth(130);
                lblName.getStyleClass().add("event-player-name");

                // 3. Stringhe dei valori memorizzati "all'attivazione" dell'evento
                Label lblActivation = new Label(String.format("Cibo all’attivazione=%d, Aura all’attivazione=%d, Magia all’attivazione dell’evento=%d",
                        data.oldFood, data.oldAura, data.oldMagic));
                lblActivation.setPrefWidth(540);
                lblActivation.getStyleClass().add("event-player-activation");

                // 4. Calcolo variazioni con segno esplicito (+/-)
                String foodSign = data.diffFood >= 0 ? "+" : "";
                String auraSign = data.diffAura >= 0 ? "+" : "";
                String magicSign = data.diffMagic >= 0 ? "+" : "";

                Label lblDiff = new Label(String.format("Aura guadagnata/persa = %s%d ; Cibo guadagnato/perso = %s%d ; Magia = %s%d",
                        auraSign, data.diffAura, foodSign, data.diffFood, magicSign, data.diffMagic));

                // Colorazione e stile dinamico gestiti in base alle soglie logiche nel CSS esterno
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

            // Aggiungiamo il riquadro dell'evento completato nel VBox principale scorribile
            eventSummaryContainer.getChildren().add(eventBox);
        }
    }
}