package gbemu.core;

import audio.AudioEngine;
import gbemu.settings.SettingsContainer;
import threading.GameBoyThread;
import threading.WindowThread;

public class GBemu {

    private final WindowThread windowThread;
    private final GameBoyThread gameboyThread;
    private final AudioEngine audioEngine;

    public GBemu(String configFile) {
        GameBoy gameboy = new GameBoy(configFile);
        audioEngine = new AudioEngine(gameboy);
        gameboy.loadSettings();
        gameboyThread = new GameBoyThread(gameboy);
        windowThread = new WindowThread(gameboy, gameboyThread);
        windowThread.init();
    }

    public void run() {
        audioEngine.start();
        gameboyThread.start();
        windowThread.run();
        windowThread.destroy();
        gameboyThread.kill();
        audioEngine.stop();
    }
}
