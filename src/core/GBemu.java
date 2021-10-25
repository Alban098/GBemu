package core;

import audio.AudioEngine;
import core.settings.SettingsContainer;
import threading.DebuggerThread;
import threading.GameBoyThread;
import threading.WindowThread;

public class GBemu {

    private final WindowThread windowThread;
    private final GameBoyThread gameboyThread;
    private final DebuggerThread debuggerThread;

    public GBemu(String configFile) {
        SettingsContainer.getInstance();
        GameBoy gameboy = new GameBoy();
        SettingsContainer.hook(gameboy);
        SettingsContainer.loadFile(configFile);
        AudioEngine.getInstance().linkGameboy(gameboy);
        gameboyThread = new GameBoyThread(gameboy);
        debuggerThread = new DebuggerThread(gameboy.getDebugger());
        windowThread = new WindowThread(gameboy, gameboyThread, debuggerThread);
        windowThread.init();
    }

    public void run() throws InterruptedException {
        AudioEngine.getInstance().start();
        gameboyThread.start();
        debuggerThread.start();
        windowThread.run();
        windowThread.destroy();
        gameboyThread.shouldExit();
        debuggerThread.shouldExit();
        gameboyThread.join();
        debuggerThread.join();
        AudioEngine.getInstance().stop();
    }
}
