package gbemu.core.apu.channels;

import gbemu.core.Flags;
import gbemu.core.apu.components.LengthCounter;
import gbemu.core.memory.MMU;

public class WaveChannel {

    private final MMU memory;
    private final LengthCounter lengthCounter;

    public int sample;

    private int sampleIndex = 0;
    private int cycleSampleUpdate = 0;
    private int cycleCount = 0;

    private boolean running = false;

    public WaveChannel(MMU memory) {
        this.memory = memory;
        this.lengthCounter = new LengthCounter();
    }

    public void clock() {
        cycleCount++;
        if (cycleCount >= cycleSampleUpdate) {
            sampleIndex++;
            if (sampleIndex > 31) sampleIndex = 0;

            updateSample();
            cycleCount -= cycleSampleUpdate;
        }
    }

    public void restart() {
        running = true;
        memory.writeIORegisterBit(MMU.NR52, Flags.NR52_CHANNEL_3_ON, true);

        int length = 256 - memory.readByte(MMU.NR31);
        boolean lengthStop = memory.readIORegisterBit(MMU.NR34, Flags.NR34_LOOP_CHANNEL);
        lengthCounter.setLength(length, lengthStop);

        cycleSampleUpdate = (2048 - getFrequency()) << 1;
        cycleCount = 0;
        sampleIndex = 0;

    }

    private int getFrequency() {
        int frequencyData = memory.readByte(MMU.NR34);
        int frequency = memory.readByte(MMU.NR33);
        frequency |= (frequencyData & Flags.NR34_FREQ_HIGH) << 8;
        return frequency;
    }

    public void tickLength() {
        running = lengthCounter.clock();
        if (!running)
            memory.writeIORegisterBit(MMU.NR52, Flags.NR52_CHANNEL_3_ON, false);
    }

    public void updateSample() {
        sample = memory.readByte(MMU.WAVE_PATTERN_START | (sampleIndex >> 1));
        if ((sampleIndex & 0x1) == 0x1) sample &= 0x0F;
        else sample = (sample & 0xF0) >> 4;
        switch ((memory.readByte(MMU.NR32) & Flags.NR32_OUTPUT_LEVEL) >> 5) {
            case 0 -> sample = 0;
            case 2 -> sample *= .5f;
            case 3 -> sample *= .25f;
        }
        if (!running || !memory.readIORegisterBit(MMU.NR30, Flags.NR30_CHANNEL_ON))
            sample = 0;
    }

    public void reset() {
        sample = 0;
        running = false;
        sampleIndex = 0;
        cycleSampleUpdate = 0;
        cycleCount = 0;
        lengthCounter.reset();
    }
}
