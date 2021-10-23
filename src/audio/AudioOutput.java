package audio;

import javax.sound.sampled.Mixer;

public record AudioOutput(Mixer.Info mixer, int id) {
}
