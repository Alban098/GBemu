package gbemu.core.apu.channels;

import gbemu.core.apu.APU;
import gbemu.core.apu.components.Envelope;
import gbemu.core.apu.components.LengthCounter;
import gbemu.core.memory.MMU;

public class SquareChannel {

    protected final MMU memory;
    private final LengthCounter length_counter;
    private final Envelope envelope;

    public int sample;

    protected int current_freq = 0;
    protected final int nrX1_register;
    private final int nrX2_register;
    protected final int nrX3_register;
    protected final int nrX4_register;
    private final int nr52_channel_on_flag;
    private final int nrX1_pattern_duty_flag;
    private final int nrX1_sound_length_flag;
    private final int nrX2_env_sweep_nb_flag;
    private final int nrX2_env_volume_flag;
    private final int nrX2_env_dir_flag;
    private final int nrX4_loop_flag;
    private final int nrX4_freq_high_flag;

    private int selected_duty = 0;
    private int sample_index = 0;
    protected int cycle_sample_update = 0;
    protected int cycle_count = 0;

    protected boolean running = false;

    public SquareChannel(MMU memory, int nrX1_register, int nrX2_register, int nrX3_register, int nrX4_register, int nr52_channel_on_flag, int nrX1_pattern_duty_flag, int nrX1_sound_length_flag, int nrX2_env_sweep_nb_flag, int nrX2_env_volume_flag, int nrX2_env_dir_flag, int nrX4_loop_flag, int nrX4_freq_high_flag) {
        this.memory = memory;
        this.nrX1_register = nrX1_register;
        this.nrX2_register = nrX2_register;
        this.nrX3_register = nrX3_register;
        this.nrX4_register = nrX4_register;
        this.nr52_channel_on_flag = nr52_channel_on_flag;
        this.nrX1_pattern_duty_flag = nrX1_pattern_duty_flag;
        this.nrX1_sound_length_flag = nrX1_sound_length_flag;
        this.nrX2_env_sweep_nb_flag = nrX2_env_sweep_nb_flag;
        this.nrX2_env_volume_flag = nrX2_env_volume_flag;
        this.nrX2_env_dir_flag = nrX2_env_dir_flag;
        this.nrX4_loop_flag = nrX4_loop_flag;
        this.nrX4_freq_high_flag = nrX4_freq_high_flag;
        this.length_counter = new LengthCounter();
        this.envelope = new Envelope();
    }

    public void clock() {
        cycle_count++;
        if (cycle_count >= cycle_sample_update) {
            sample_index++;
            if (sample_index > 7) sample_index = 0;

            updateSample();
            cycle_count -= cycle_sample_update;
        }
    }

    public void restart() {
        running = true;
        memory.writeIORegisterBit(MMU.NR52, nr52_channel_on_flag, true);
        selected_duty = (memory.readByte(nrX1_register) & nrX1_pattern_duty_flag) >> 6;

        int length = 64 - (memory.readByte(nrX1_register) & nrX1_sound_length_flag);
        boolean length_stop = memory.readIORegisterBit(nrX4_register, nrX4_loop_flag);
        length_counter.setLength(length, length_stop);

        int envelope_data = memory.readByte(nrX2_register);
        int envelope_ticks = envelope_data & nrX2_env_sweep_nb_flag;
        int envelope_volume = (envelope_data & nrX2_env_volume_flag) >> 4;
        boolean increase = (envelope_data & nrX2_env_dir_flag) != 0;
        envelope.setEnvelope(envelope_ticks, envelope_volume, increase);

        current_freq = getFrequency();
        cycle_sample_update = (2048 - current_freq) << 2;
        cycle_count = 0;
        sample_index = 0;

    }

    private int getFrequency() {
        int frequency_data = memory.readByte(nrX4_register);
        int frequency = memory.readByte(nrX3_register);
        frequency |= (frequency_data & nrX4_freq_high_flag) << 8;
        return frequency;
    }

    public void tickLength() {
        running = length_counter.clock();
        if (!running)
            memory.writeIORegisterBit(MMU.NR52, nr52_channel_on_flag, false);
    }

    public void tickEnvelope() {
        envelope.clock();
    }

    public void updateSample() {
        int duty_val = APU.WAVE_PATTERN[selected_duty][sample_index];
        sample = duty_val * envelope.getVolume();

        if (!running)
            sample = 0;
    }

    public void reset() {
        sample = 0;
        selected_duty = 0;
        sample_index = 0;
        cycle_sample_update = 0;
        cycle_count = 0;
        running = false;
        length_counter.reset();
        envelope.reset();
    }
}
