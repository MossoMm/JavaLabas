package PartyModel;

import GUI.GUI;
import javafx.application.Application;

public final class Launcher {

    private Launcher() {}

    public static void main(String[] args) {
        Application.launch(GUI.class, args);
    }
}