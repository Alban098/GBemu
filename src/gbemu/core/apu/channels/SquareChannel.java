package gbemu.core.apu.channels;

import gbemu.core.apu.APU;
import gbemu.core.apu.components.Envelope;
import gbemu.core.apu.components.LengthCounter;
import gbemu.core.apu.components.Oscillator;
import gbemu.core.memory.MMU;

public class SquareChannel {

    protected final MMU memory;
    private final LengthCounter length_counter;
    private final Envelope envelope;
    protected final Oscillator oscillator;

    private double time = 0;
    public float sample;
    private PulseMode pulse_mode;

    protected int current_freq_raw = 0;
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
        this.oscillator = new Oscillator();
    }

    public void clock(double time) {
        cycle_count++;
        if (cycle_count >= cycle_sample_update) {
            sample_index++;
            if (sample_index > 7) sample_index = 0;

            updateSample();
            cycle_count -= cycle_sample_update;
        }
        this.time += time;
    }

    public void restart() {
        running = true;
        memory.writeIORegisterBit(MMU.NR52, nr52_channel_on_flag, true);
        selected_duty = (memory.readByte(nrX1_register) & nrX1_pattern_duty_flag) >> 6;
        switch (selected_duty) {
            case 0 -> oscillator.duty_cycle = 0.125f;
            case 1 -> oscillator.duty_cycle = 0.250f;
            case 2 -> oscillator.duty_cycle = 0.500f;
            case 3 -> oscillator.duty_cycle = 0.750f;
        }

        int length = 64 - (memory.readByte(nrX1_register) & nrX1_sound_length_flag);
        boolean length_stop = memory.readIORegisterBit(nrX4_register, nrX4_loop_flag);
        length_counter.setLength(length, length_stop);

        int envelope_data = memory.readByte(nrX2_register);
        int envelope_ticks = envelope_data & nrX2_env_sweep_nb_flag;
        int envelope_volume = (envelope_data & nrX2_env_volume_flag) >> 4;
        boolean increase = (envelope_data & nrX2_env_dir_flag) != 0;
        envelope.setEnvelope(envelope_ticks, envelope_volume, increase);

        updateFrequency();

        cycle_count = 0;
        sample_index = 0;
    }

    private void updateFrequency() {
        int frequency_data = memory.readByte(nrX4_register);
        int frequency = memory.readByte(nrX3_register);
        frequency |= (frequency_data & nrX4_freq_high_flag) << 8;
        current_freq_raw = frequency;
        cycle_sample_update = (2048 - current_freq_raw) << 2;
        oscillator.frequency = 131072f/(2048 - frequency);
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
        switch (pulse_mode) {
            case RAW -> {
                int duty_val = APU.WAVE_PATTERN[selected_duty][sample_index];
                sample = duty_val * envelope.getVolume();
            }
            case SIN_FILTERED -> {
                oscillator.amplitude = envelope.getVolume();
                sample = oscillator.sample(time);
            }
        }
        if (!running)
            sample = 0;
    }

    public PulseMode getPulseMode() {
        return pulse_mode;
    }

    public void setPulseMode(PulseMode pulse_mode) {
        this.pulse_mode = pulse_mode;
    }

    public void reset() {
        sample = 0;
        selected_duty = 0;
        oscillator.duty_cycle = 0.125f;
        sample_index = 0;
        cycle_sample_update = 0;
        cycle_count = 0;
        running = false;
        length_counter.reset();
        envelope.reset();
    }
}
