package core.ppu;

public enum Flags {
    CONTROL_LCD_ON(0x80),
    CONTROL_WINDOW_MAP(0x40),
    CONTROL_WINDOW_ON(0x20),
    CONTROL_BG_DATA(0x10),
    CONTROL_BG_MAP(0x08),
    CONTROL_OBJ_SIZE(0x04),
    CONTROL_OBJ_ON(0x02),
    CONTROL_BG_ON(0x01),

    STATUS_LCD_Y_IRQ_ON(0x40),
    STATUS_OAM_IRQ_ON(0x20),
    STATUS_V_BLANK_IRQ_ON(0x10),
    STATUS_H_BLANK_IRQ_ON(0x08),
    STATUS_COINCIDENCE(0x04),
    STATUS_MODE_HIGH(0x02),
    STATUS_MODE_LOW(0x01);

    private final int mask;

    Flags(int mask) {
        this.mask = mask;
    }

    public int getMask() {
        return mask;
    }
}
