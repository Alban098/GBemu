package gbemu.core.apu.channels;

import gbemu.core.Flags;
import gbemu.core.memory.MMU;

public class SweepingSquareChannel extends SquareChannel {


    private int elapsed_sweep_time = 0;
    private int sweep_time = 0;
    private boolean sweep_decreasing = false;
    private int sweep_shift = 0;

    public SweepingSquareChannel(MMU memory, int nrX1_register, int nrX2_register, int nrX3_register, int nrX4_register, int nr52_channel_on_flag, int nrX1_pattern_duty_flag, int nrX1_sound_length_flag, int nrX2_env_sweep_nb_flag, int nrX2_env_volume_flag, int nrX2_env_dir_flag, int nrX4_loop_flag, int nrX4_freq_high_flag) {
        super(memory, nrX1_register, nrX2_register, nrX3_register, nrX4_register, nr52_channel_on_flag, nrX1_pattern_duty_flag, nrX1_sound_length_flag, nrX2_env_sweep_nb_flag, nrX2_env_volume_flag, nrX2_env_dir_flag, nrX4_loop_flag, nrX4_freq_high_flag);
    }

    @Override
    public void restart() {
        super.restart();
        elapsed_sweep_time = 0;
        int sweep_data = memory.readByte(MMU.NR10);
        sweep_time = (sweep_data & Flags.NR10_SWEEP_TIME) >> 4;
        sweep_decreasing = (sweep_data & Flags.NR10_SWEEP_MODE) != 0;
        sweep_shift = sweep_data & Flags.NR10_SWEEP_SHIFT_NB;
    }

    public void tickSweep() {
        if (sweep_time == 0)
            return;
        if (elapsed_sweep_time <= sweep_time)
            elapsed_sweep_time++;
        if (elapsed_sweep_time == sweep_time) {
            int sweepCorrection = sweep_decreasing ? -1 : 1;
            int sweepChange = (current_freq >> sweep_shift) * sweepCorrection;

            if (sweep_decreasing && sweepChange > current_freq)
                elapsed_sweep_time = 0;
            else if (!sweep_decreasing && sweepChange + current_freq > 2047)
                running = false;
            else {
                current_freq += sweepChange;
                cycle_sample_update = (2048 - current_freq) >> 2;
                cycle_count = 0;
                elapsed_sweep_time = 0;
                setFrequency(current_freq);
            }
        }
    }

    private void setFrequency(int freq) {
        int frequency_data = memory.readByte(nrX4_register);
        memory.writeByte(nrX4_register, (frequency_data & 0xF8) | ((freq & 0x700) >> 8));
        memory.writeByte(nrX3_register, freq & 0xFF);
    }

    @Override
    public void reset() {
        super.reset();
        elapsed_sweep_time = 0;
        sweep_shift = 0;
        sweep_time = 0;
        sweep_decreasing = false;
    }
}
