package core.apu.channels;

import core.Flags;
import core.apu.APU;
import core.apu.channels.component.Envelope;
import core.apu.channels.component.LengthCounter;
import core.memory.MMU;

public class NoiseChannel {

    private final MMU memory;
    private final LengthCounter lengthCounter;
    private final Envelope envelope;

    public int sample;

    private int cycleSampleUpdate = 0;
    private int cycleCount = 0;
    private int divisor = 0;
    private int lfsr = 0;

    private boolean widthFlag = false;
    private boolean running = false;

    public NoiseChannel(MMU memory) {
        this.memory = memory;
        this.lengthCounter = new LengthCounter();
        this.envelope = new Envelope();
    }

    public void clock(int mcycles) {
        cycleCount += mcycles;
        if (cycleCount >= cycleSampleUpdate) {
            updateSample();
            cycleCount -= cycleSampleUpdate;
        }
    }

    public void restart() {
        running = true;
        memory.writeIORegisterBit(MMU.NR52, Flags.NR52_CHANNEL_4_ON, true);

        int length = 64 - (memory.readByte(MMU.NR41) & Flags.NR41_SOUND_LENGTH);
        boolean lengthStop = memory.readIORegisterBit(MMU.NR44, Flags.NR44_LOOP_CHANNEL);
        lengthCounter.setLength(length, lengthStop);

        int envelopeData = memory.readByte(MMU.NR42);
        int envelopeTicks = envelopeData & Flags.NR42_ENVELOPE_SWEEP_NB;
        int envelopeVolume = (envelopeData & Flags.NR42_ENVELOPE_VOLUME) >> 4;
        boolean increase = (envelopeData & Flags.NR42_ENVELOPE_DIR) != 0;
        envelope.setEnvelope(envelopeTicks, envelopeVolume, increase);

        int noiseData = memory.readByte(MMU.NR43);
        divisor = APU.AUDIO_DIVISOR[noiseData & Flags.NR43_DIV_RATIO];
        widthFlag = (noiseData & Flags.NR43_COUNTER_WIDTH) != 0x00;
        lfsr = 0x7FFF;
        cycleSampleUpdate = (divisor << (((noiseData & Flags.NR43_SHIFT_CLK_FREQ) >> 4) + 1)) << 2;
        cycleCount = 0;

        updateSample();
    }

    public void tickLength() {
        running = lengthCounter.clock();
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
        if (widthFlag)
            lfsr = (xor << 6) | (lfsr & 0x7FBF);
        sample = ((lfsr & 0x01) == 0 ? 1 : 0) * envelope.getVolume();

        if (!running)
            sample = 0;
    }

}
