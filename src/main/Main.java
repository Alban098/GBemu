package main;

import gbemu.core.GBemu;
import javafx.application.Platform;

import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        String str = "{{42;54}{125;135}{176;35}{49;124}{110;20}}{{42;54}{125;135}{176;35}{49;124}{110;20}}";
        int level = 0;
        StringBuilder currentItem = new StringBuilder();
        List<String> items = new ArrayList<>();
        for (char c : str.toCharArray()) {
            if (c == '{') {
                if (level > 0)
                    currentItem.append(c);
                level++;
                continue;
            } else if (c == '}') {
                level--;
            }
            if (level >= 1) {
                currentItem.append(c);
            } else {
                items.add(currentItem.toString());
                currentItem = new StringBuilder();
            }
        }
        new GBemu("GBemu.ini").run();
    }
}