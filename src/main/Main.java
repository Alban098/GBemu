package main;

import core.GBemu;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        new GBemu("GBemu.ini").run();
    }
}