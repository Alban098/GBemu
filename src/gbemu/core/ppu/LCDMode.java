package gbemu.core.ppu;

/**
 * This enum contains all the possible LCDMode the PPU can be in
 */
public enum LCDMode {
    H_BLANK(0),
    V_BLANK(1),
    OAM(2),
    TRANSFER(3);

    private final int value;

    /**
     * Create a new LCDMode
     * @param value the index of the LCDMode
     */
    LCDMode(int value) {
        this.value = value;
    }

    /**
     * Return the index of the LCDMode
     * @return the LCDMode's index
     */
    public int getValue() {
        return value;
    }

}
