package main;

import core.GameBoy;
import core.GameBoyState;
import gui.Window;
import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.io.JavaSoundAudioIO;
import net.beadsproject.beads.ugens.Function;
import net.beadsproject.beads.ugens.WaveShaper;

public class Main {

    private static AudioContext ac;

    public static void main(String[] args) {
        GameBoy gb = new GameBoy();
        launchSoundEngine(gb);
        Window window = new Window(gb);
        window.init();
        window.run();
        window.destroy();
        if (ac != null)
            ac.stop();
    }

    public static void launchSoundEngine(GameBoy gameBoy) {
        JavaSoundAudioIO jsaIO = new JavaSoundAudioIO();
        JavaSoundAudioIO.printMixerInfo();
        jsaIO.selectMixer(12);
        ac = new AudioContext(jsaIO);
        Function audioProcessor = new Function(new WaveShaper(ac)) {
            public float calculate() {
                if (gameBoy.getState() == GameBoyState.RUNNING)
                    return gameBoy.getNextSample();
                return 0;
            }
        };
        ac.out.addInput(audioProcessor);
        ac.start();
    }
}