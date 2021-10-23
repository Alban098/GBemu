package core;

import audio.AudioEngine;
import core.settings.SettingsContainer;
import gui.Window;

public class GBemu {

    private final Window window;

    public GBemu(String configFile) {
        SettingsContainer.getInstance();
        GameBoy gameboy = new GameBoy();
        SettingsContainer.hook(gameboy);
        SettingsContainer.loadFile(configFile);
        AudioEngine.getInstance().linkGameboy(gameboy);
        window = new Window(gameboy);
        window.init();
    }

    public void run() {
        AudioEngine.getInstance().start();
        window.run();
        window.destroy();
        AudioEngine.getInstance().stop();
    }
}
