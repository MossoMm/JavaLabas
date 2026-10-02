package PartyModel;

import javafx.application.Application;

/**
 * Точка входа для запуска из «толстого» JAR.
 *

 */
public final class Launcher {

    private Launcher() {}

    public static void main(String[] args) {
        Application.launch(Main.class, args);
    }
}