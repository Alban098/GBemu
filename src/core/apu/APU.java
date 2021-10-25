package core.apu;

import audio.AudioEngine;
import core.Flags;
import core.GameBoy;
import core.apu.channels.NoiseChannel;
import core.apu.channels.SweepingSquareChannel;
import core.apu.channels.WaveChannel;
import core.memory.MMU;
import core.apu.channels.SquareChannel;
import core.ppu.helper.IMMUListener;
import debug.Debugger;
import debug.DebuggerMode;
import gui.APULayer;

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

    private final Queue<Sample> sampleQueue;
    private final Queue<Sample> debugSampleQueue;

    private final SweepingSquareChannel square1;
    private final SquareChannel square2;
    private final WaveChannel wave;
    private final NoiseChannel noise;

    private double cycle = 0;
    private int cycleLength = 0;
    private int cycleEnvelope = 0;
    private int cycleSweep = 0;

    private long sampleIndex = 0;
    private boolean adaptiveSampleRateStarted = false;

    private float lastSample = 0;
    private boolean square1Rendered = true;
    private boolean square2Rendered = true;
    private boolean waveRendered = true;
    private boolean noiseRendered = true;

    public APU(GameBoy gameboy) {
        this.gameboy = gameboy;
        gameboy.getMemory().addListener(this);
        sampleQueue = new ConcurrentLinkedQueue<>();
        debugSampleQueue = new ConcurrentLinkedQueue<>();
        square1 = new SweepingSquareChannel(gameboy.getMemory(), MMU.NR11, MMU.NR12, MMU.NR13, MMU.NR14, Flags.NR52_CHANNEL_1_ON, Flags.NR11_PATTERN_DUTY, Flags.NR11_SOUND_LENGTH, Flags.NR12_ENVELOPE_SWEEP_NB, Flags.NR12_ENVELOPE_VOLUME, Flags.NR12_ENVELOPE_DIR, Flags.NR14_LOOP_CHANNEL, Flags.NR14_FREQ_HIGH);
        square2 = new SquareChannel(gameboy.getMemory(), MMU.NR21, MMU.NR22, MMU.NR23, MMU.NR24, Flags.NR52_CHANNEL_2_ON, Flags.NR21_PATTERN_DUTY, Flags.NR21_SOUND_LENGTH, Flags.NR22_ENVELOPE_SWEEP_NB, Flags.NR22_ENVELOPE_VOLUME, Flags.NR22_ENVELOPE_DIR, Flags.NR24_LOOP_CHANNEL, Flags.NR24_FREQ_HIGH);
        wave = new WaveChannel(gameboy.getMemory());
        noise = new NoiseChannel(gameboy.getMemory());
        debugger = gameboy.getDebugger();
        debugger.link(debugSampleQueue);
    }

    public void clock() {
        clockLength();
        clockEnvelope();
        clockSweep();
        clockChannels();
        clockSamples();
    }

    public void onWriteToMMU(int addr, int data) {
        switch(addr) {
            case MMU.NR14 -> {
                if ((data & Flags.NR14_RESTART) != 0)
                    square1.restart();
            }
            case MMU.NR24 -> {
                if ((data & Flags.NR24_RESTART) != 0)
                    square2.restart();
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
        cycleLength++;
        while (cycleLength >= gameboy.mode.cpu_cycles_256HZ) {
            square1.tickLength();
            square2.tickLength();
            wave.tickLength();
            noise.tickLength();
            cycleLength -= gameboy.mode.cpu_cycles_256HZ;
        }
    }

    private void clockEnvelope() {
        cycleEnvelope++;
        while (cycleEnvelope >= gameboy.mode.cpu_cycles_64HZ) {
            square1.tickEnvelope();
            square2.tickEnvelope();
            noise.tickEnvelope();
            cycleEnvelope -= gameboy.mode.cpu_cycles_64HZ;
        }
    }

    private void clockSweep() {
        cycleSweep++;
        while (cycleSweep >= gameboy.mode.cpu_cycles_128HZ) {
            square1.tickSweep();
            cycleSweep -= gameboy.mode.cpu_cycles_128HZ;
        }
    }

    private void clockChannels() {
        square1.clock();
        square2.clock();
        wave.clock();
        noise.clock();
    }

    private void clockSamples() {
        cycle++;
        while (cycle >= gameboy.mode.cpu_cycles_per_sample) {
            if (AudioEngine.getInstance().isStarted()) {
                Sample sample;
                if (gameboy.getMemory().readIORegisterBit(MMU.NR52, Flags.NR52_SOUND_ENABLED)) {
                    sample = new Sample(
                            (square1Rendered ? square1.sample : 0),
                            (square2Rendered ? square2.sample : 0),
                            (waveRendered ? wave.sample : 0),
                            (noiseRendered ? noise.sample : 0)
                    );
                } else {
                    sample = new Sample(0, 0, 0, 0);
                }
                sampleQueue.offer(sample);
                sampleIndex++;
                if (debugger.isHooked(DebuggerMode.APU)) {
                    debugSampleQueue.offer(sample);
                    if (debugSampleQueue.size() > APULayer.DEBUG_SAMPLE_NUMBER)
                        debugSampleQueue.poll();
                }
            }
            if (sampleIndex % (APU.SAMPLE_RATE/10) == 0) {
                if (sampleQueue.size() > 5000) {
                    gameboy.mode.cpu_cycles_per_sample += .5;
                    adaptiveSampleRateStarted = true;
                } else if (sampleQueue.size() < 100 && adaptiveSampleRateStarted) {
                    gameboy.mode.cpu_cycles_per_sample -= .5;
                }
            }
            cycle -= gameboy.mode.cpu_cycles_per_sample;
        }
    }


    public float getNextSample() {
        Sample s = sampleQueue.poll();
        if (s != null) {
            lastSample = (s.getNormalizedValue() + lastSample) / 2;
            return lastSample;
        }
        lastSample /= 2;

        return lastSample;
    }

    public void reset() {
        lastSample = 0;
        sampleQueue.clear();
        debugSampleQueue.clear();
        adaptiveSampleRateStarted = false;
        sampleIndex = 0;
        cycle = 0;
        cycleEnvelope = 0;
        cycleLength = 0;
        cycleSweep = 0;
        square1.reset();
        square2.reset();
        wave.reset();
        noise.reset();
    }

    public void enableSquare1(boolean enabled) {
        square1Rendered = enabled;
    }

    public void enableSquare2(boolean enabled) {
        square2Rendered = enabled;
    }

    public void enableWave(boolean enabled) {
        waveRendered = enabled;
    }

    public void enableNoise(boolean enabled) {
        noiseRendered = enabled;
    }
}
