package audio;

import javax.sound.sampled.Mixer;

/**
 * Just a record that hold a Mixer, and it's id as stored by the Audio Engine
 */
public record AudioOutput(Mixer.Info mixer, int id) {}
