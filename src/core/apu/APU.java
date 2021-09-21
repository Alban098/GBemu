package core.apu;

import core.MMU;
import core.apu.channels.SquareChannel;
import core.cpu.LR35902;
import core.ppu.helper.IMMUListener;

public class APU implements IMMUListener {

    public static final int SAMPLE_RATE = 44100;
    public static final int[][] WAVE_PATTERN = {
            { 0, 0, 0, 0, 0, 0, 0, 1 },
            { 1, 0, 0, 0, 0, 0, 0, 1 },
            { 1, 0, 0, 0, 0, 1, 1, 1 },
            { 0, 1, 1, 1, 1, 1, 1, 0 }
    };

    private final MMU memory;
    private final SquareChannel square1;

    private int cycle = 0;
    private int cycleLength = 0;
    private int cycleEnvelope = 0;
    private int cycleSweep = 0;
    private int sampleCounter;

    private int volumeLeft = 0;
    private int volumeRight= 0;
    boolean square1OutputLeft = false;
    boolean square1OutputRight = false;

    boolean leftEnabled = false;
    boolean rightEnabled = true;

    public APU(MMU memory) {
        this.memory = memory;
        memory.addListener(this);
        square1 = new SquareChannel(memory);
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
                leftEnabled = (data & Flags.LEFT_ENABLE.getMask()) != 0;
                rightEnabled = (data & Flags.RIGHT_ENABLE.getMask()) != 0;
                volumeLeft = 8 - (data & Flags.OUTPUT_VOLUME.getMask());
                volumeRight = 8 - ((data >> 4) & Flags.OUTPUT_VOLUME.getMask());
            }
            case MMU.NR51 -> {
                square1OutputLeft = (data & Flags.CHANNEL_1_OUTPUT_1.getMask()) != 0;
                square1OutputRight = (data & Flags.CHANNEL_1_OUTPUT_2.getMask()) != 0;
            }
            case MMU.NR14 -> {
                if ((data & Flags.CHANNEL_RESTART.getMask()) != 0)
                    square1.restart();
            }
        }
    }


    private void clockLength() {
        cycleLength++;
        if (cycleLength >= LR35902.CPU_CYCLES_256HZ) {
            square1.tickLength();
            cycleLength -= LR35902.CPU_CYCLES_256HZ;
        }
    }

    private void clockEnvelope() {
        cycleEnvelope++;
        if (cycleLength >= LR35902.CPU_CYCLES_64HZ) {
            square1.tickEnvelope();
            cycleLength -= LR35902.CPU_CYCLES_64HZ;
        }
    }

    private void clockSweep() {
        cycleSweep++;
        if (cycleLength >= LR35902.CPU_CYCLES_128HZ) {
            square1.tickSweep();
            cycleLength -= LR35902.CPU_CYCLES_128HZ;
        }
    }

    private void clockChannels() {
        square1.clock();
    }

    private void clockSamples() {
        cycle++;
        if (cycle >= LR35902.CPU_CYCLES_PER_SAMPLE) {
            //TODO generate sample
            sampleCounter++;
            if (sampleCounter >= 4096) {
                sampleCounter = 0;
            }
            cycle -= LR35902.CPU_CYCLES_PER_SAMPLE;
        }
    }


    public float getNextSample() {
        return 0;
    }
}
