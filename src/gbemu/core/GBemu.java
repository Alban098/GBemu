package gbemu.core;

import audio.AudioEngine;
import threading.GameBoyThread;
import threading.WindowThread;

/**
 * This class is the entry point of the emulator
 * to run an instance of the emulator, just instantiate it and call run
 */
public class GBemu {

    private final WindowThread window_thread;
    private final GameBoyThread gameboy_thread;
    private final AudioEngine audio_engine;

    /**
     * Create a new Instance of the Emulator with a specified config file
     * @param config_file the config file to load settings from
     */
    public GBemu(String config_file) {
        GameBoy gameboy = new GameBoy(config_file);
        audio_engine = new AudioEngine(gameboy);
        gameboy_thread = new GameBoyThread(gameboy);
        window_thread = new WindowThread(gameboy, gameboy_thread);
        gameboy.loadSettings();
    }

    /**
     * Run the emulator by launching every Thread alongside the Audio Engine
     */
    public void run() {
        audio_engine.start();
        gameboy_thread.start();
        window_thread.run();
        window_thread.destroy();
        gameboy_thread.kill();
        audio_engine.stop();
    }
}
