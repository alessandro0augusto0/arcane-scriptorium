module com.arcane.scriptorium {
    requires javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.graphics;

    opens com.arcane.scriptorium to javafx.fxml;

    exports com.arcane.scriptorium;
}