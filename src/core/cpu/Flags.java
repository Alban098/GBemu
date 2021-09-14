package core.cpu;

public enum Flags {
    ZERO(0b10000000),
    SUBTRACT(0b01000000),
    HALF_CARRY(0b00100000),
    CARRY(0b00010000);

    private int mask;

    Flags(int mask) {
        this.mask = mask;
    }

    public int getMask() {
        return mask;
    }
}
