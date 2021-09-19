package core.ppu;

public enum LCDMode {
    H_BLANK(0),
    V_BLANK(1),
    OAM(2),
    TRANSFER(3);

    private final int value;

    LCDMode(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

}
