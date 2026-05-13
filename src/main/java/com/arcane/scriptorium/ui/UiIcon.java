package com.arcane.scriptorium.ui;

import javafx.scene.image.Image;
import javafx.stage.Stage;

public final class UiIcon {
    private static final String ICON_RESOURCE = "/icons/icon.png";
    private static final Image ICON = new Image(
        UiIcon.class.getResourceAsStream(ICON_RESOURCE)
    );

    private UiIcon() {
    }

    public static void apply(Stage stage) {
        if (stage != null) {
            stage.getIcons().add(ICON);
        }
    }
}
