module it.polimi.ingsw.am54 {
    requires javafx.controls;
    requires javafx.fxml;


    opens it.polimi.ingsw.am54 to javafx.fxml;
    exports it.polimi.ingsw.am54;
}