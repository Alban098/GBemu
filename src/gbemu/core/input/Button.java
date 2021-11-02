package gbemu.core.input;

public enum Button {
    START(0x08),
    SELECT(0x04),
    A(0x02),
    B(0x01),
    DOWN(0x80),
    UP(0x40),
    LEFT(0x20),
    RIGHT(0x10);

    private final int mask;

    Button(int mask) {
        this.mask = mask;
    }

    public int getMask() {
        return mask;
    }
}
