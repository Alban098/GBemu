import core.GameBoy;
import core.GameBoyState;
import gui.Window;
import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.io.JavaSoundAudioIO;
import net.beadsproject.beads.ugens.Function;
import net.beadsproject.beads.ugens.WaveShaper;

import javax.sound.sampled.LineUnavailableException;
import java.util.Random;

public class Main {

    private static AudioContext ac;
    private static JavaSoundAudioIO jsaIO;

    public static void main(String[] args) throws LineUnavailableException {
        String rom = "OK-01-special.gb";
        GameBoy gb = new GameBoy("roms\\DMG_ROM.bin");
        gb.insertCartridge("roms\\" + rom);
        launchSoundEngine(gb);
        Window window = new Window(gb);
        window.init();
        window.run();
        window.destroy();
        ac.stop();
    }

    public static void launchSoundEngine(GameBoy gameBoy) {
        jsaIO = new JavaSoundAudioIO();
        jsaIO.selectMixer(2);
        ac = new AudioContext(jsaIO);
        Function audioProcessor = new Function(new WaveShaper(ac)) {
            public float calculate() {
                if (gameBoy.getState() == GameBoyState.RUNNING)
                    return gameBoy.getLastSample();
                return 0;
            }
        };
        ac.out.addInput(audioProcessor);
        ac.start();
    }
}