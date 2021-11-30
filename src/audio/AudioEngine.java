package audio;

import gbemu.core.GameBoy;
import gbemu.core.GameBoyState;
import gbemu.settings.SettingIdentifiers;
import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.UGen;
import net.beadsproject.beads.core.io.JavaSoundAudioIO;
import net.beadsproject.beads.ugens.Function;
import net.beadsproject.beads.ugens.WaveShaper;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is responsible for handling audio outside the core Game Boy context
 * by fetching the audio samples the Emulator compute one after the other at the right time
 */
public class AudioEngine {

    private final AudioContext ac;
    private final JavaSoundAudioIO jsaIO;
    private final GameBoy gameboy;
    private boolean started;
    private final List<AudioOutput> validOutputs;
    private float master_volume;

    /**
     * Create a new Audio Engine
     * linking ot to the currently selected Output if valid
     */
    public AudioEngine(GameBoy gameboy) {
        this.gameboy = gameboy;
        gameboy.setAudioEngine(this);
        jsaIO = new JavaSoundAudioIO();
        validOutputs = new ArrayList<>();
        //populating the valid audio output list
        verifyValidOutputs();

        //linking the current audio output
        if (validOutputs.size() > 0) {
            jsaIO.selectMixer(validOutputs.get(0).id());
            ac = new AudioContext(jsaIO);
            started = true;
            return;
        }
        ac = null;
        started = false;
    }

    /**
     * Populate the Audio output list with all existing OUtput that are valid
     */
    private void verifyValidOutputs() {
        int index = 0;
        //For each Mixer, we check if the defined function is ran, if so it's a valid output, and it's added to the list of valid outputs
        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            try {
                jsaIO.selectMixer(index);
                AudioContext checker = new AudioContext(jsaIO);
                final boolean[] valid = {false};
                //If this Function run, it will change the state of the valid array
                Function check = new Function(new WaveShaper(checker)) {
                    public float calculate() {
                        valid[0] = true;
                        return 0;
                    }
                };
                checker.out.addInput(check);
                checker.start();

                Thread.sleep(100);

                if (valid[0])
                    validOutputs.add(new AudioOutput(info, index));
                checker.stop();
            } catch (InterruptedException ignored) {}
            index++;
        }
    }

    /**
     * Start the Audio engine
     */
    public void start() {
        ac.out.addInput(new Function(new WaveShaper(ac)) {
            public float calculate() {
                //If the emulator is running then fetch an audio sample and applying the master volume
                //Otherwise return 0
                if (gameboy.getState() == GameBoyState.RUNNING)
                    return gameboy.getNextSample() * master_volume;
                return 0;
            }
        });
        ac.start();
        started = true;
    }

    /**
     * Set the current volume
     * @param volume the new volume to set
     */
    public void setVolume(float volume) {
        master_volume = volume;
    }

    /**
     * Return whether the Audio Engine is started or not
     * @return is the Audio Engine started
     */
    public boolean isStarted() {
        return started;
    }

    /**
     * Stop the Audio Engine
     */
    public void stop() {
        for (UGen input : ac.out.getConnectedInputs())
            input.kill();
        ac.stop();
    }
}
