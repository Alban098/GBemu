package core.apu;

import core.Flags;
import core.GameBoy;
import core.apu.channels.NoiseChannel;
import core.apu.channels.SweepingSquareChannel;
import core.apu.channels.WaveChannel;
import core.memory.MMU;
import core.apu.channels.SquareChannel;
import core.cpu.LR35902;
import core.ppu.helper.IMMUListener;
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
    private boolean adaptativeSampleRateStarted = false;

    private int volumeLeft = 0;
    private int volumeRight= 0;
    boolean square1OutputLeft = false;
    boolean square1OutputRight = false;

    boolean leftEnabled = false;
    boolean rightEnabled = false;

    private float lastSample = 0;

    public APU(MMU memory) {
        memory.addListener(this);
        sampleQueue = new ConcurrentLinkedQueue<>();
        debugSampleQueue = new ConcurrentLinkedQueue<>();
        square1 = new SweepingSquareChannel(memory, MMU.NR11, MMU.NR12, MMU.NR13, MMU.NR14, Flags.NR52_CHANNEL_1_ON, Flags.NR11_PATTERN_DUTY, Flags.NR11_SOUND_LENGTH, Flags.NR12_ENVELOPE_SWEEP_NB, Flags.NR12_ENVELOPE_VOLUME, Flags.NR12_ENVELOPE_DIR, Flags.NR14_LOOP_CHANNEL, Flags.NR14_FREQ_HIGH);
        square2 = new SquareChannel(memory, MMU.NR21, MMU.NR22, MMU.NR23, MMU.NR24, Flags.NR52_CHANNEL_1_ON, Flags.NR21_PATTERN_DUTY, Flags.NR21_SOUND_LENGTH, Flags.NR22_ENVELOPE_SWEEP_NB, Flags.NR22_ENVELOPE_VOLUME, Flags.NR22_ENVELOPE_DIR, Flags.NR24_LOOP_CHANNEL, Flags.NR24_FREQ_HIGH);
        wave = new WaveChannel(memory);
        noise = new NoiseChannel(memory);
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
            case MMU.NR50 -> {
                leftEnabled = (data & Flags.NR50_LEFT_SPEAKER_ON) != 0;
                rightEnabled = (data & Flags.NR50_RIGHT_SPEAKER_ON) != 0;
                volumeLeft = 8 - (data & Flags.NR50_LEFT_VOLUME);
                volumeRight = 8 - ((data >> 4) & Flags.NR50_RIGHT_VOLUME);
            }
            case MMU.NR51 -> {
                square1OutputLeft = (data & Flags.NR51_CHANNEL_1_LEFT) != 0;
                square1OutputRight = (data & Flags.NR51_CHANNEL_1_RIGHT) != 0;
            }
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
        if (cycleLength >= LR35902.CPU_CYCLES_256HZ) {
            square1.tickLength();
            square2.tickLength();
            wave.tickLength();
            noise.tickLength();
            cycleLength -= LR35902.CPU_CYCLES_256HZ;
        }
    }

    private void clockEnvelope() {
        cycleEnvelope++;
        if (cycleEnvelope >= LR35902.CPU_CYCLES_64HZ) {
            square1.tickEnvelope();
            square2.tickEnvelope();
            noise.tickEnvelope();
            cycleEnvelope -= LR35902.CPU_CYCLES_64HZ;
        }
    }

    private void clockSweep() {
        cycleSweep++;
        if (cycleSweep >= LR35902.CPU_CYCLES_128HZ) {
            square1.tickSweep();
            cycleSweep -= LR35902.CPU_CYCLES_128HZ;
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
        if (cycle >= LR35902.CPU_CYCLES_PER_SAMPLE) {
            Sample sample = new Sample(square1.sample, square2.sample, wave.sample, noise.sample);
            sampleQueue.offer(sample);
            if (sampleIndex % (APU.SAMPLE_RATE/10) == 0) {
                if (sampleQueue.size() > 5000) {
                    LR35902.CPU_CYCLES_PER_SAMPLE += .5;
                    adaptativeSampleRateStarted = true;
                } else if (sampleQueue.size() < 100 && adaptativeSampleRateStarted) {
                    LR35902.CPU_CYCLES_PER_SAMPLE -= .5;
                }
            }
            if (GameBoy.DEBUG) {
                debugSampleQueue.offer(sample);
                if (debugSampleQueue.size() > APULayer.DEBUG_SAMPLE_NUMBER)
                    debugSampleQueue.poll();
            }
            sampleIndex++;
            cycle -= LR35902.CPU_CYCLES_PER_SAMPLE;
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

    public Queue<Sample> getDebugSampleQueue() {
        return debugSampleQueue;
    }

    public void reset() {
        lastSample = 0;
        sampleQueue.clear();
        debugSampleQueue.clear();
        adaptativeSampleRateStarted = false;
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

}
