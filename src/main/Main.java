package main;

import gbemu.core.GBemu;
import javafx.application.Platform;

public class Main {

    public static void main(String[] args) {
        new GBemu("GBemu.ini").run();
    }
}