package core.apu.channels;

import core.Flags;
import core.memory.MMU;

public class SweepingSquareChannel extends SquareChannel {


    private int elapsedSweepTime = 0;
    private int sweepTime = 0;
    private boolean sweepDecreasing = false;
    private int sweepShift = 0;

    public SweepingSquareChannel(MMU memory, int nrX1_register, int nrX2_register, int nrX3_register, int nrX4_register, int nr52_channelOnFlag, int nrX1_patternDutyFlag, int nrX1_soundLengthFlag, int nrX2_envSweepNbFlag, int nrX2_envVolumeFlag, int nrX2_envDirFlag, int nrX4_loopFlag, int nrX4_freqHighFlag) {
        super(memory, nrX1_register, nrX2_register, nrX3_register, nrX4_register, nr52_channelOnFlag, nrX1_patternDutyFlag, nrX1_soundLengthFlag, nrX2_envSweepNbFlag, nrX2_envVolumeFlag, nrX2_envDirFlag, nrX4_loopFlag, nrX4_freqHighFlag);
    }

    @Override
    public void restart() {
        super.restart();
        elapsedSweepTime = 0;
        int sweepData = memory.readByte(MMU.NR10);
        sweepTime = (sweepData & Flags.NR10_SWEEP_TIME) >> 4;
        sweepDecreasing = (sweepData & Flags.NR10_SWEEP_MODE) != 0;
        sweepShift = sweepData & Flags.NR10_SWEEP_SHIFT_NB;
    }

    public void tickSweep() {
        if (sweepTime == 0)
            return;
        if (elapsedSweepTime <= sweepTime)
            elapsedSweepTime++;
        if (elapsedSweepTime == sweepTime) {
            int sweepCorrection = sweepDecreasing ? -1 : 1;
            int sweepChange = (currentFreq >> sweepShift) * sweepCorrection;

            if (sweepDecreasing && sweepChange > currentFreq)
                elapsedSweepTime = 0;
            else if (!sweepDecreasing && sweepChange + currentFreq > 2047)
                running = false;
            else {
                currentFreq += sweepChange;
                cycleSampleUpdate = (2048 - currentFreq) >> 2;
                cycleCount = 0;
                elapsedSweepTime = 0;
                setFrequency(currentFreq);
            }
        }
    }

    private void setFrequency(int freq) {
        int frequencyData = memory.readByte(nrX4_register);
        memory.writeByte(nrX4_register, (frequencyData & 0xF8) | ((freq & 0x700) >> 8));
        memory.writeByte(nrX3_register, freq & 0xFF);
    }

    @Override
    public void reset() {
        super.reset();
        elapsedSweepTime = 0;
        sweepShift = 0;
        sweepTime = 0;
        sweepDecreasing = false;
    }
}
