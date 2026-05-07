module com.arcane.scriptorium {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.arcane.scriptorium to javafx.fxml;
    exports com.arcane.scriptorium;
}