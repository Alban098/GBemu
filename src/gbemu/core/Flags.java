package gbemu.core;

public class Flags {

    //CPU Flags
    public static final int Z = 0x80;
    public static final int N = 0x40;
    public static final int H = 0x20;
    public static final int C = 0x10;

    public static final int TAC_ENABLED = 0x04;
    public static final int TAC_CLOCK   = 0x03;

    public static final int SC_TRANSFER_START = 0x80;
    public static final int SC_CLK_SPEED      = 0x02;
    public static final int SC_SHIFT_CLK      = 0x01;

    public static final int IE_JOYPAD_IRQ   = 0x10;
    public static final int IE_LCD_STAT_IRQ = 0x02;
    public static final int IE_TIMER_IRQ    = 0x04;
    public static final int IE_SERIAL_IRQ   = 0x08;
    public static final int IE_VBLANK_IRQ   = 0x01;

    public static final int IF_JOYPAD_IRQ   = 0x10;
    public static final int IF_LCD_STAT_IRQ = 0x02;
    public static final int IF_TIMER_IRQ    = 0x04;
    public static final int IF_SERIAL_IRQ   = 0x08;
    public static final int IF_VBLANK_IRQ   = 0x01;

    public static final int LCDC_LCD_ON       = 0x80;
    public static final int LCDC_WINDOW_MAP   = 0x40;
    public static final int LCDC_WINDOW_ON    = 0x20;
    public static final int LCDC_BG_TILE_DATA = 0x10;
    public static final int LCDC_BG_TILE_MAP  = 0x08;
    public static final int LCDC_OBJ_SIZE     = 0x04;
    public static final int LCDC_OBJ_ON       = 0x02;
    public static final int LCDC_BG_ON        = 0x01;

    public static final int STAT_COINCIDENCE_IRQ    = 0x40;
    public static final int STAT_OAM_IRQ_ON         = 0x20;
    public static final int STAT_VBLANK_IRQ_ON      = 0x10;
    public static final int STAT_HBLANK_IRQ_ON      = 0x08;
    public static final int STAT_COINCIDENCE_STATUS = 0x04;
    public static final int STAT_MODE               = 0x03;

    public static final int NR10_SWEEP_TIME     = 0x70;
    public static final int NR10_SWEEP_MODE     = 0x08;
    public static final int NR10_SWEEP_SHIFT_NB = 0x07;

    public static final int NR11_PATTERN_DUTY = 0xC0;
    public static final int NR11_SOUND_LENGTH = 0x3F;

    public static final int NR12_ENVELOPE_VOLUME   = 0xF0;
    public static final int NR12_ENVELOPE_DIR      = 0x08;
    public static final int NR12_ENVELOPE_SWEEP_NB = 0x07;

    public static final int NR14_RESTART      = 0x80;
    public static final int NR14_LOOP_CHANNEL = 0x40;
    public static final int NR14_FREQ_HIGH    = 0x07;

    public static final int NR21_PATTERN_DUTY = 0xC0;
    public static final int NR21_SOUND_LENGTH = 0x3F;

    public static final int NR22_ENVELOPE_VOLUME   = 0xF0;
    public static final int NR22_ENVELOPE_DIR      = 0x08;
    public static final int NR22_ENVELOPE_SWEEP_NB = 0x07;

    public static final int NR24_RESTART      = 0x80;
    public static final int NR24_LOOP_CHANNEL = 0x40;
    public static final int NR24_FREQ_HIGH    = 0x07;

    public static final int NR30_CHANNEL_ON = 0x80;

    public static final int NR32_OUTPUT_LEVEL = 0x60;

    public static final int NR34_RESTART      = 0x80;
    public static final int NR34_LOOP_CHANNEL = 0x40;
    public static final int NR34_FREQ_HIGH    = 0x07;

    public static final int NR41_SOUND_LENGTH = 0x3F;

    public static final int NR42_ENVELOPE_VOLUME   = 0xF0;
    public static final int NR42_ENVELOPE_DIR      = 0x08;
    public static final int NR42_ENVELOPE_SWEEP_NB = 0x07;

    public static final int NR43_SHIFT_CLK_FREQ = 0xF0;
    public static final int NR43_COUNTER_WIDTH  = 0x08;
    public static final int NR43_DIV_RATIO      = 0x07;

    public static final int NR44_RESTART      = 0x80;
    public static final int NR44_LOOP_CHANNEL = 0x40;

    public static final int NR50_LEFT_SPEAKER_ON  = 0x80;
    public static final int NR50_LEFT_VOLUME      = 0x70;
    public static final int NR50_RIGHT_SPEAKER_ON = 0x08;
    public static final int NR50_RIGHT_VOLUME     = 0x07;

    public static final int NR51_CHANNEL_4_LEFT  = 0x80;
    public static final int NR51_CHANNEL_3_LEFT  = 0x40;
    public static final int NR51_CHANNEL_2_LEFT  = 0x20;
    public static final int NR51_CHANNEL_1_LEFT  = 0x10;
    public static final int NR51_CHANNEL_4_RIGHT  = 0x08;
    public static final int NR51_CHANNEL_3_RIGHT  = 0x04;
    public static final int NR51_CHANNEL_2_RIGHT  = 0x02;
    public static final int NR51_CHANNEL_1_RIGHT  = 0x01;

    public static final int NR52_SOUND_ENABLED = 0x80;
    public static final int NR52_CHANNEL_4_ON  = 0x08;
    public static final int NR52_CHANNEL_3_ON  = 0x04;
    public static final int NR52_CHANNEL_2_ON  = 0x02;
    public static final int NR52_CHANNEL_1_ON  = 0x01;

    public static final int SPRITE_ATTRIB_UNDER_BG = 0x80;
    public static final int SPRITE_ATTRIB_Y_FLIP = 0x40;
    public static final int SPRITE_ATTRIB_X_FLIP = 0x20;
    public static final int SPRITE_ATTRIB_PAL = 0x10;

    public static final int P1_BUTTON = 0x20;
    public static final int P1_DPAD = 0x10;

    public static final int CGB_BCPS_AUTO_INC = 0x80;
    public static final int CGB_BCPS_ADDR = 0x3F;
    public static final int CGB_TILE_VRAM_BANK = 0x08;
    public static final int CGB_TILE_PALETTE = 0x07;
    public static final int CGB_TILE_HFLIP = 0x20;
    public static final int CGB_TILE_VFLIP = 0x40;
    public static final int CGB_TILE_PRIORITY = 0x80;
    public static final int SPRITE_ATTRIB_CGB_VRAM_BANK = 0x08;
    public static final int SPRITE_ATTRIB_CGB_PAL = 0x07;
    public static final int CGB_KEY_1_SPEED = 0x80;
    public static final int CGB_KEY_1_SWITCH = 0x01;
}
