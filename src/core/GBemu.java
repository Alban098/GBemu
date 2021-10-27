package core;

import audio.AudioEngine;
import core.settings.SettingsContainer;
import threading.ConsoleThread;
import threading.DebuggerThread;
import threading.GameBoyThread;
import threading.WindowThread;

public class GBemu {

    private final WindowThread windowThread;
    private final GameBoyThread gameboyThread;

    public GBemu(String configFile) {
        SettingsContainer.getInstance();
        GameBoy gameboy = new GameBoy();
        SettingsContainer.hook(gameboy);
        SettingsContainer.loadFile(configFile);
        AudioEngine.getInstance().linkGameboy(gameboy);
        gameboyThread = new GameBoyThread(gameboy);
        windowThread = new WindowThread(gameboy, gameboyThread);
        windowThread.init();
    }

    public void run() {
        AudioEngine.getInstance().start();
        gameboyThread.start();
        windowThread.run();
        windowThread.destroy();
        gameboyThread.kill();
        AudioEngine.getInstance().stop();
    }
}
