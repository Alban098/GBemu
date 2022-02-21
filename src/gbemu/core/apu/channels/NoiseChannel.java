package gbemu.core.apu.channels;

import gbemu.core.Flags;
import gbemu.core.apu.APU;
import gbemu.core.apu.components.Envelope;
import gbemu.core.apu.components.LengthCounter;
import gbemu.core.memory.MMU;

public class NoiseChannel {

    private final MMU memory;
    private final LengthCounter length_counter;
    private final Envelope envelope;

    public int sample;

    private int cycle_sample_update = 0;
    private int cycle_count = 0;
    private int lfsr = 0;

    private boolean width_flag = false;
    private boolean running = false;

    public NoiseChannel(MMU memory) {
        this.memory = memory;
        this.length_counter = new LengthCounter();
        this.envelope = new Envelope();
    }

    public void clock() {
        cycle_count++;
        if (cycle_count >= cycle_sample_update) {
            updateSample();
            cycle_count -= cycle_sample_update;
        }
    }

    public void restart() {
        running = true;
        memory.writeIORegisterBit(MMU.NR52, Flags.NR52_CHANNEL_4_ON, true);

        int length = 64 - (memory.readByte(MMU.NR41) & Flags.NR41_SOUND_LENGTH);
        boolean length_stop = memory.readIORegisterBit(MMU.NR44, Flags.NR44_LOOP_CHANNEL);
        length_counter.setLength(length, length_stop);

        int envelope_data = memory.readByte(MMU.NR42);
        int envelope_ticks = envelope_data & Flags.NR42_ENVELOPE_SWEEP_NB;
        int envelope_volume = (envelope_data & Flags.NR42_ENVELOPE_VOLUME) >> 4;
        boolean increase = (envelope_data & Flags.NR42_ENVELOPE_DIR) != 0;
        envelope.setEnvelope(envelope_ticks, envelope_volume, increase);

        int noise_data = memory.readByte(MMU.NR43);
        width_flag = (noise_data & Flags.NR43_COUNTER_WIDTH) != 0x00;
        lfsr = 0x7FFF;
        cycle_sample_update = (APU.AUDIO_DIVISOR[noise_data & Flags.NR43_DIV_RATIO] << (((noise_data & Flags.NR43_SHIFT_CLK_FREQ) >> 4) + 1)) << 2;
        cycle_count = 0;

        updateSample();
    }

    public void tickLength() {
        running = length_counter.clock();
        if (!running)
            memory.writeIORegisterBit(MMU.NR52, Flags.NR52_CHANNEL_4_ON, false);
    }

    public void tickEnvelope() {
        envelope.clock();
    }

    public void updateSample() {
        int b0 = lfsr & 0x01;
        int b1 = (lfsr & 0x02) >> 1;
        int xor = b0 ^ b1;
        lfsr = (xor << 14) | (lfsr >> 1);
        if (width_flag)
            lfsr = (xor << 6) | (lfsr & 0x7FBF);
        sample = ((lfsr & 0x01) == 0 ? 1 : 0) * envelope.getVolume();

        if (!running)
            sample = 0;
    }

    public void reset() {
        sample = 0;
        running = false;
        cycle_sample_update = 0;
        cycle_count = 0;
        lfsr = 0;
        width_flag = false;
        length_counter.reset();
        envelope.reset();
    }
}
