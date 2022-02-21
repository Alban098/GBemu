package gbemu.core.apu.channels;

import gbemu.core.Flags;
import gbemu.core.apu.components.LengthCounter;
import gbemu.core.memory.MMU;

public class WaveChannel {

    private final MMU memory;
    private final LengthCounter length_counter;

    public int sample;

    private int sample_index = 0;
    private int cycle_sample_update = 0;
    private int cycle_count = 0;

    private boolean running = false;

    public WaveChannel(MMU memory) {
        this.memory = memory;
        this.length_counter = new LengthCounter();
    }

    public void clock() {
        cycle_count++;
        if (cycle_count >= cycle_sample_update) {
            sample_index++;
            if (sample_index > 31) sample_index = 0;

            updateSample();
            cycle_count -= cycle_sample_update;
        }
    }

    public void restart() {
        running = true;
        memory.writeIORegisterBit(MMU.NR52, Flags.NR52_CHANNEL_3_ON, true);

        int length = 256 - memory.readByte(MMU.NR31);
        boolean length_stop = memory.readIORegisterBit(MMU.NR34, Flags.NR34_LOOP_CHANNEL);
        length_counter.setLength(length, length_stop);

        cycle_sample_update = (2048 - getFrequency()) << 1;
        cycle_count = 0;
        sample_index = 0;

    }

    private int getFrequency() {
        int frequency_data = memory.readByte(MMU.NR34);
        int frequency = memory.readByte(MMU.NR33);
        frequency |= (frequency_data & Flags.NR34_FREQ_HIGH) << 8;
        return frequency;
    }

    public void tickLength() {
        running = length_counter.clock();
        if (!running)
            memory.writeIORegisterBit(MMU.NR52, Flags.NR52_CHANNEL_3_ON, false);
    }

    public void updateSample() {
        sample = memory.readByte(MMU.WAVE_PATTERN_START | (sample_index >> 1));
        if ((sample_index & 0x1) == 0x1) sample &= 0x0F;
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
        sample_index = 0;
        cycle_sample_update = 0;
        cycle_count = 0;
        length_counter.reset();
    }
}
