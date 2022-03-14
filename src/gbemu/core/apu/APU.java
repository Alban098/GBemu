package gbemu.core.apu;

import gbemu.core.Flags;
import gbemu.core.GameBoy;
import gbemu.core.apu.channels.*;
import gbemu.core.memory.MMU;
import gbemu.core.memory.IMMUListener;
import gbemu.extension.debug.Debugger;
import gbemu.extension.debug.DebuggerMode;
import gui.debug.APULayer;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class APU implements IMMUListener {

    public static final int SAMPLE_RATE = 44100;
    public static final int[][] WAVE_PATTERN = {
            { 0, 0, 0, 0, 0, 0, 0, 1 },
            { 1, 0, 0, 0, 0, 0, 0, 1 },
            { 1, 0, 0, 0, 0, 1, 1, 1 },
            { 0, 1, 1, 1, 1, 1, 1, 0 }
    };
    public static final int[] AUDIO_DIVISOR = {8, 16, 32, 48, 64, 80, 96, 112};

    private final GameBoy gameboy;
    private final Debugger debugger;

    private final Queue<Sample> sample_queue;
    private final Queue<Sample> debug_sample_queue;

    private final SweepingSquareChannel square_1;
    private final SquareChannel square_2;
    private final WaveChannel wave;
    private final NoiseChannel noise;

    private double cycle = 0;
    private int cycle_length = 0;
    private int cycle_envelope = 0;
    private int cycle_sweep = 0;

    private long sample_index = 0;
    private boolean adaptive_sample_rate_started = false;

    private float last_sample = 0;
    private boolean square_1_rendered = true;
    private boolean square_2_rendered = true;
    private boolean wave_rendered = true;
    private boolean noise_rendered = true;

    public APU(GameBoy gameboy) {
        this.gameboy = gameboy;
        gameboy.getMemory().addListener(this);
        sample_queue = new ConcurrentLinkedQueue<>();
        debug_sample_queue = new ConcurrentLinkedQueue<>();
        square_1 = new SweepingSquareChannel(gameboy.getMemory(), MMU.NR11, MMU.NR12, MMU.NR13, MMU.NR14, Flags.NR52_CHANNEL_1_ON, Flags.NR11_PATTERN_DUTY, Flags.NR11_SOUND_LENGTH, Flags.NR12_ENVELOPE_SWEEP_NB, Flags.NR12_ENVELOPE_VOLUME, Flags.NR12_ENVELOPE_DIR, Flags.NR14_LOOP_CHANNEL, Flags.NR14_FREQ_HIGH);
        square_2 = new SquareChannel(gameboy.getMemory(), MMU.NR21, MMU.NR22, MMU.NR23, MMU.NR24, Flags.NR52_CHANNEL_2_ON, Flags.NR21_PATTERN_DUTY, Flags.NR21_SOUND_LENGTH, Flags.NR22_ENVELOPE_SWEEP_NB, Flags.NR22_ENVELOPE_VOLUME, Flags.NR22_ENVELOPE_DIR, Flags.NR24_LOOP_CHANNEL, Flags.NR24_FREQ_HIGH);
        wave = new WaveChannel(gameboy.getMemory());
        noise = new NoiseChannel(gameboy.getMemory());
        debugger = gameboy.getDebugger();
        debugger.link(debug_sample_queue);
    }

    public void clock(double time) {
        clockLength();
        clockEnvelope();
        clockSweep();
        clockChannels(time);
        clockSamples();
    }

    public void onWriteToMMU(int addr, int data) {
        switch(addr) {
            case MMU.NR14 -> {
                if ((data & Flags.NR14_RESTART) != 0)
                    square_1.restart();
            }
            case MMU.NR24 -> {
                if ((data & Flags.NR24_RESTART) != 0)
                    square_2.restart();
            }
            case MMU.NR34 -> {
                if ((data & Flags.NR34_RESTART) != 0)
                    wave.restart();
            }
            case MMU.NR44 -> {
                if ((data & Flags.NR44_RESTART) != 0)
                    noise.restart();
            }
        }
    }

    private void clockLength() {
        cycle_length++;
        while (cycle_length >= gameboy.mode.CYCLES_256HZ) {
            square_1.tickLength();
            square_2.tickLength();
            wave.tickLength();
            noise.tickLength();
            cycle_length -= gameboy.mode.CYCLES_256HZ;
        }
    }

    private void clockEnvelope() {
        cycle_envelope++;
        while (cycle_envelope >= gameboy.mode.CYCLES_64HZ) {
            square_1.tickEnvelope();
            square_2.tickEnvelope();
            noise.tickEnvelope();
            cycle_envelope -= gameboy.mode.CYCLES_64HZ;
        }
    }

    private void clockSweep() {
        cycle_sweep++;
        while (cycle_sweep >= gameboy.mode.CYCLES_128HZ) {
            square_1.tickSweep();
            cycle_sweep -= gameboy.mode.CYCLES_128HZ;
        }
    }

    private void clockChannels(double time) {
        square_1.clock(time);
        square_2.clock(time);
        wave.clock();
        noise.clock();
    }

    private void clockSamples() {
        cycle++;
        while (cycle >= gameboy.mode.CYCLES_PER_SAMPLE) {
            if (gameboy.getAudioEngine().isStarted()) {
                Sample sample;
                if (gameboy.getMemory().readIORegisterBit(MMU.NR52, Flags.NR52_SOUND_ENABLED)) {
                    sample = new Sample(
                            (square_1_rendered ? square_1.sample : 0),
                            (square_2_rendered ? square_2.sample : 0),
                            (wave_rendered ? wave.sample : 0),
                            (noise_rendered ? noise.sample : 0)
                    );
                } else {
                    sample = new Sample(0, 0, 0, 0);
                }
                sample_queue.offer(sample);
                sample_index++;
                if (debugger.isHooked(DebuggerMode.APU)) {
                    debug_sample_queue.offer(sample);
                    if (debug_sample_queue.size() > APULayer.DEBUG_SAMPLE_NUMBER)
                        debug_sample_queue.poll();
                }
            }
            if (sample_index % (APU.SAMPLE_RATE/10) == 0) {
                if (sample_queue.size() > 5000) {
                    gameboy.mode.CYCLES_PER_SAMPLE += .5;
                    adaptive_sample_rate_started = true;
                } else if (sample_queue.size() < 100 && adaptive_sample_rate_started) {
                    gameboy.mode.CYCLES_PER_SAMPLE -= .5;
                }
            }
            cycle -= gameboy.mode.CYCLES_PER_SAMPLE;
        }
    }


    public float getNextSample() {
        Sample s = sample_queue.poll();
        if (s != null) {
            last_sample = (s.getNormalizedValue() + last_sample) / 2;
            return last_sample;
        }
        last_sample /= 2;

        return last_sample;
    }

    public void reset() {
        last_sample = 0;
        sample_queue.clear();
        debug_sample_queue.clear();
        adaptive_sample_rate_started = false;
        sample_index = 0;
        cycle = 0;
        cycle_envelope = 0;
        cycle_length = 0;
        cycle_sweep = 0;
        square_1.reset();
        square_2.reset();
        wave.reset();
        noise.reset();
    }

    public void enableSquare1(boolean enabled) {
        square_1_rendered = enabled;
    }

    public void enableSquare2(boolean enabled) {
        square_2_rendered = enabled;
    }

    public void enableWave(boolean enabled) {
        wave_rendered = enabled;
    }

    public void enableNoise(boolean enabled) {
        noise_rendered = enabled;
    }

    public void setFilteringMode(PulseMode pulse_mode) {
        square_1.setPulseMode(pulse_mode);
        square_2.setPulseMode(pulse_mode);
    }
}
