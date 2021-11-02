package gbemu.core.apu.channels;

import gbemu.core.apu.APU;
import gbemu.core.apu.components.Envelope;
import gbemu.core.apu.components.LengthCounter;
import gbemu.core.memory.MMU;

public class SquareChannel {

    protected final MMU memory;
    private final LengthCounter lengthCounter;
    private final Envelope envelope;

    public int sample;

    protected int currentFreq = 0;
    protected final int nrX1_register;
    private final int nrX2_register;
    protected final int nrX3_register;
    protected final int nrX4_register;
    private final int nr52_channelOnFlag;
    private final int nrX1_patternDutyFlag;
    private final int nrX1_soundLengthFlag;
    private final int nrX2_envSweepNbFlag;
    private final int nrX2_envVolumeFlag;
    private final int nrX2_envDirFlag;
    private final int nrX4_loopFlag;
    private final int nrX4_freqHighFlag;

    private int selectedDuty = 0;
    private int sampleIndex = 0;
    protected int cycleSampleUpdate = 0;
    protected int cycleCount = 0;

    protected boolean running = false;

    public SquareChannel(MMU memory, int nrX1_register, int nrX2_register, int nrX3_register, int nrX4_register, int nr52_channelOnFlag, int nrX1_patternDutyFlag, int nrX1_soundLengthFlag, int nrX2_envSweepNbFlag, int nrX2_envVolumeFlag, int nrX2_envDirFlag, int nrX4_loopFlag, int nrX4_freqHighFlag) {
        this.memory = memory;
        this.nrX1_register = nrX1_register;
        this.nrX2_register = nrX2_register;
        this.nrX3_register = nrX3_register;
        this.nrX4_register = nrX4_register;
        this.nr52_channelOnFlag = nr52_channelOnFlag;
        this.nrX1_patternDutyFlag = nrX1_patternDutyFlag;
        this.nrX1_soundLengthFlag = nrX1_soundLengthFlag;
        this.nrX2_envSweepNbFlag = nrX2_envSweepNbFlag;
        this.nrX2_envVolumeFlag = nrX2_envVolumeFlag;
        this.nrX2_envDirFlag = nrX2_envDirFlag;
        this.nrX4_loopFlag = nrX4_loopFlag;
        this.nrX4_freqHighFlag = nrX4_freqHighFlag;
        this.lengthCounter = new LengthCounter();
        this.envelope = new Envelope();
    }

    public void clock() {
        cycleCount++;
        if (cycleCount >= cycleSampleUpdate) {
            sampleIndex++;
            if (sampleIndex > 7) sampleIndex = 0;

            updateSample();
            cycleCount -= cycleSampleUpdate;
        }
    }

    public void restart() {
        running = true;
        memory.writeIORegisterBit(MMU.NR52, nr52_channelOnFlag, true);
        selectedDuty = (memory.readByte(nrX1_register) & nrX1_patternDutyFlag) >> 6;

        int length = 64 - (memory.readByte(nrX1_register) & nrX1_soundLengthFlag);
        boolean lengthStop = memory.readIORegisterBit(nrX4_register, nrX4_loopFlag);
        lengthCounter.setLength(length, lengthStop);

        int envelopeData = memory.readByte(nrX2_register);
        int envelopeTicks = envelopeData & nrX2_envSweepNbFlag;
        int envelopeVolume = (envelopeData & nrX2_envVolumeFlag) >> 4;
        boolean increase = (envelopeData & nrX2_envDirFlag) != 0;
        envelope.setEnvelope(envelopeTicks, envelopeVolume, increase);

        currentFreq = getFrequency();
        cycleSampleUpdate = (2048 - currentFreq) << 2;
        cycleCount = 0;
        sampleIndex = 0;

    }

    private int getFrequency() {
        int frequencyData = memory.readByte(nrX4_register);
        int frequency = memory.readByte(nrX3_register);
        frequency |= (frequencyData & nrX4_freqHighFlag) << 8;
        return frequency;
    }

    public void tickLength() {
        running = lengthCounter.clock();
        if (!running)
            memory.writeIORegisterBit(MMU.NR52, nr52_channelOnFlag, false);
    }

    public void tickEnvelope() {
        envelope.clock();
    }

    public void updateSample() {
        int dutyVal = APU.WAVE_PATTERN[selectedDuty][sampleIndex];
        sample = dutyVal * envelope.getVolume();

        if (!running)
            sample = 0;
    }

    public void reset() {
        sample = 0;
        selectedDuty = 0;
        sampleIndex = 0;
        cycleSampleUpdate = 0;
        cycleCount = 0;
        running = false;
        lengthCounter.reset();
        envelope.reset();
    }
}
