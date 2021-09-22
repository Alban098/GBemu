package core.ppu;

import core.Flags;
import core.memory.MMU;
import core.cpu.LR35902;
import core.ppu.helper.BackgroundMaps;
import core.ppu.helper.ColorPalettes;
import core.ppu.helper.ColorShade;
import core.ppu.helper.TileCollection;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;

public class PPU {

    public static final int SCREEN_WIDTH = 160;
    public static final int SCREEN_HEIGHT = 144;

    private final ByteBuffer screen_buffer;
    private final ColorShade[][] background_buffer;
    private final MMU memory;

    private final ColorPalettes palettes;
    private ColorPalettes.ColorPalette bgPal;
    private final BackgroundMaps bgMaps;
    private final TileCollection tileList;

    private long cycles = 0;
    private boolean isFrameComplete;


    public PPU(MMU memory) {
        screen_buffer = BufferUtils.createByteBuffer(SCREEN_HEIGHT * SCREEN_WIDTH * 4);
        background_buffer = new ColorShade[SCREEN_HEIGHT][SCREEN_WIDTH];
        palettes = new ColorPalettes(memory);
        bgMaps = new BackgroundMaps(memory);
        tileList = new TileCollection(memory);
        this.memory = memory;
    }

    public ByteBuffer getScreenBuffer() {
        return screen_buffer;
    }

    public void clock() {
        if (!memory.readIORegisterBit(MMU.LCDC, Flags.LCDC_LCD_ON))
            return;
        cycles++;
        switch (memory.readLcdMode()) {
            case OAM -> processOam();
            case TRANSFER -> processTransfer();
            case H_BLANK -> processHBlank();
            case V_BLANK -> processVBlank();
        }
    }

    private void processVBlank() {
        if (cycles >= LR35902.CPU_CYCLES_PER_V_BLANK) {
            if (memory.readByte(MMU.LY, true) == SCREEN_HEIGHT) {
                memory.writeIORegisterBit(MMU.IF, Flags.IF_VBLANK_IRQ, true);
                screen_buffer.clear();
                for (ColorShade[] scanline : background_buffer) {
                    for (ColorShade colorShade : scanline) {
                        screen_buffer.put((byte) colorShade.getColor().getRed());
                        screen_buffer.put((byte) colorShade.getColor().getGreen());
                        screen_buffer.put((byte) colorShade.getColor().getBlue());
                        screen_buffer.put((byte) colorShade.getColor().getAlpha());
                    }
                }
                isFrameComplete = true;
                screen_buffer.flip();
            }

            if (memory.readByte(MMU.LY, true) == 153) {
                memory.writeRaw(MMU.LY, 0);
                memory.writeLcdMode(LCDMode.OAM);
            } else {
                memory.writeRaw(MMU.LY, memory.readByte(MMU.LY, true) + 1);
            }
            cycles -= LR35902.CPU_CYCLES_PER_V_BLANK;
        }
    }

    private void processHBlank() {
        if (cycles >= LR35902.CPU_CYCLES_PER_H_BLANK) {
            if (memory.readIORegisterBit(MMU.STAT, Flags.STAT_HBLANK_IRQ_ON))
                memory.writeIORegisterBit(MMU.IF, Flags.IF_LCD_STAT_IRQ, true);

            fillBackgroundLayer();
            fillWindowLayer();
            fillSpriteLayer();

            if (memory.readByte(MMU.LY, true) == SCREEN_HEIGHT - 1)
                memory.writeLcdMode(LCDMode.V_BLANK);
            else
                memory.writeLcdMode(LCDMode.OAM);

            memory.writeRaw(MMU.LY, memory.readByte(MMU.LY, true) + 1);
            cycles -= LR35902.CPU_CYCLES_PER_H_BLANK;
        }
    }

    private void processTransfer() {
        if (cycles >= LR35902.CPU_CYCLES_PER_TRANSFER) {
            memory.writeLcdMode(LCDMode.H_BLANK);
            cycles -= LR35902.CPU_CYCLES_PER_TRANSFER;
        }
    }

    private void processOam() {
        if (cycles >= LR35902.CPU_CYCLES_PER_OAM) {
            if (memory.readIORegisterBit(MMU.STAT, Flags.STAT_OAM_IRQ_ON))
                memory.writeIORegisterBit(MMU.IF, Flags.IF_LCD_STAT_IRQ, true);

            int lcd_y = memory.readByte(MMU.LY, true);
            int lcd_yc = memory.readByte(MMU.LYC, true);

            if (lcd_y == lcd_yc) {
                memory.writeIORegisterBit(MMU.STAT, Flags.STAT_COINCIDENCE_STATUS, true);
                if (memory.readIORegisterBit(MMU.STAT, Flags.STAT_COINCIDENCE_IRQ))
                    memory.writeIORegisterBit(MMU.IF, Flags.IF_LCD_STAT_IRQ, true);
            } else {
                memory.writeIORegisterBit(MMU.STAT, Flags.STAT_COINCIDENCE_STATUS, false);
            }
            memory.writeLcdMode(LCDMode.TRANSFER);
            cycles -= LR35902.CPU_CYCLES_PER_OAM;
        }
    }

    private void fillSpriteLayer() {
        //TODO
    }

    private void fillWindowLayer() {
        //TODO
    }

    private void fillBackgroundLayer() {
        if (!memory.readIORegisterBit(MMU.LCDC, Flags.LCDC_BG_ON))
            return;

        int lcdY = memory.readByte(MMU.LY, true);
        int scrollY = memory.readByte(MMU.SCY, true);
        int scrollX = memory.readByte(MMU.SCX, true);

        int bgTileYStart = ((scrollY + lcdY) >> 3) >= 0x1F ? ((scrollY + lcdY) >> 3) & 0x1F : ((scrollY + lcdY) >> 3);
        int bgTileYOffset = (scrollY + lcdY) & 0x7;

        int bgTileXStart = scrollX >> 3;
        int bgTileXOffset = scrollX & 0x7;

        bgPal = palettes.getBgPalette();
        BackgroundMaps.BackgroundMap currentBGMap = bgMaps.getBGMap();

        for (int i = 0; i <= 20; i++) {
            int tileId = bgTileXStart + i >= 0x1F ? (bgTileXStart + i) & 0x1F : bgTileXStart + i;
            int currentTileId = currentBGMap.data[bgTileYStart][tileId];
            TileCollection.Tile currentTile = tileList.getBGTile(currentTileId);

            for (int j = 0; j < 8; j++) {
                if (i == 0 && j < bgTileXOffset) continue;
                if (i == 20 && j >= bgTileXOffset) continue;

                ColorShade color = bgPal.colors[currentTile.data[bgTileYOffset][j]];
                background_buffer[lcdY][(i << 3) + j - bgTileXOffset] = color;
            }
        }
    }

    public void reset() {
        //TODO
    }

    public boolean isFrameComplete() {
        boolean result = isFrameComplete;
        isFrameComplete = false;
        return result;
    }
}
