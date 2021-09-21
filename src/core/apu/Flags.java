package core.apu;

public enum Flags {

    OUTPUT_VOLUME(0x7),
    CHANNEL_1_OUTPUT_1(0x1),
    CHANNEL_2_OUTPUT_1(0x2),
    CHANNEL_3_OUTPUT_1(0x4),
    CHANNEL_4_OUTPUT_1(0x8),
    CHANNEL_1_OUTPUT_2(0x10),
    CHANNEL_2_OUTPUT_2(0x20),
    CHANNEL_3_OUTPUT_2(0x40),
    CHANNEL_4_OUTPUT_2(0x80),
    CHANNEL_RESTART(0x80),
    CHANNEL_3_ON(0x80),
    CHANNEL_DUTY(0xC0), 
    CHANNEL_LENGTH_DATA(0x3F), 
    CHANNEL_LENGTH_STOP(0x40), 
    CHANNEL_ENVELOPE_SWEEP(0x7), 
    CHANNEL_ENVELOPE_VOLUME(0xF0), 
    CHANNEL_ENVELOPE_INC(0x08),
    CHANNEL_SWEEP_TIME(0x70),
    CHANNEL_SWEEP_DEC(0x08),
    CHANNEL_SWEEP_SHIFT(0x07),
    CHANNEL_FREQ(0x07),
    SOUND_1_ON(0x01),
    SOUND_2_ON(0x02),
    SOUND_3_ON(0x04),
    SOUND_4_ON(0x08),
    LEFT_ENABLE(0x80),
    RIGHT_ENABLE(0x08);
    private final int mask;

    Flags(int mask) {
        this.mask = mask;
    }

    public int getMask() {
        return mask;
    }
}
