package main;

import gbemu.core.GBemu;
import javafx.application.Platform;

import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) throws Exception {
        new GBemu("GBemu.xml").run();
    }
}