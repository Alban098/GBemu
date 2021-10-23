package audio;

import core.GameBoy;
import core.GameBoyState;
import core.settings.SettingsContainer;
import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.io.JavaSoundAudioIO;
import net.beadsproject.beads.ugens.Function;
import net.beadsproject.beads.ugens.WaveShaper;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import java.util.ArrayList;
import java.util.List;

public class AudioEngine {

    private static AudioEngine instance;

    private AudioContext ac;
    private final JavaSoundAudioIO jsaIO;
    private GameBoy gameboy;
    private boolean started;
    private final List<AudioOutput> validOutputs;
    private float master_volume;

    public static AudioEngine getInstance() {
        if (instance == null)
            instance = new AudioEngine();
        return instance;
    }

    public AudioEngine() {
        jsaIO = new JavaSoundAudioIO();
        validOutputs = new ArrayList<>();
        verifyValidOutputs();
        int out = (int) SettingsContainer.getInstance().getSetting(SettingsContainer.SettingIdentifiers.AUDIO_MIXER).getValue();
        if (validOutputs.size() > out) {
            jsaIO.selectMixer(validOutputs.get(out).id());
            ac = new AudioContext(jsaIO);
            started = true;
            return;
        }
        started = false;
    }

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

                Thread.sleep(200);

                if (valid[0])
                    validOutputs.add(new AudioOutput(info, index));
                checker.stop();
            } catch (InterruptedException ignored) {}
            index++;
        }
    }

    public void linkGameboy(GameBoy gameboy) {
        this.gameboy = gameboy;
    }

    public void start() {
        Function audioProcessor = new Function(new WaveShaper(ac)) {
            public float calculate() {
                if (gameboy.getState() == GameBoyState.RUNNING)
                    return gameboy.getNextSample() * master_volume;
                return 0;
            }
        };
        ac.out.addInput(audioProcessor);
        ac.start();
    }

    public void setOutput(int id) {
        if (validOutputs.size() > id) {
            jsaIO.selectMixer(validOutputs.get(id).id());
            started = true;
            return;
        }
        started = false;
    }

    public void setVolume(float volume) {
        master_volume = volume;
    }

    public List<AudioOutput> getValidOutputs() {
        return validOutputs;
    }

    public boolean isStarted() {
        return started;
    }

    public void stop() {
        ac.stop();
    }
}
