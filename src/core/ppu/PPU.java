package core.ppu;

import core.MMU;
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
        if (!memory.readIORegisterBit(MMU.IO_LCD_CONTROL, Flags.CONTROL_LCD_ON.getMask(), true))
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
            if (memory.readByte(MMU.IO_LCD_Y, true) == SCREEN_HEIGHT) {
                memory.writeIORegisterBit(MMU.IO_INTERRUPT_FLAG, core.cpu.Flags.IRQ_VBLANK.getMask(), true);
                screen_buffer.clear();
                for (ColorShade[] scanline : background_buffer) {
                    for (ColorShade colorShade : scanline) {
                        screen_buffer.put((byte) colorShade.getColor().getRed());
                        screen_buffer.put((byte) colorShade.getColor().getGreen());
                        screen_buffer.put((byte) colorShade.getColor().getBlue());
                        screen_buffer.put((byte) colorShade.getColor().getAlpha());
                    }
                }
                screen_buffer.flip();
            }

            if (memory.readByte(MMU.IO_LCD_Y, true) == 153) {
                memory.writeRaw(MMU.IO_LCD_Y, 0);
                memory.writeLcdMode(LCDMode.OAM);
            } else {
                memory.writeRaw(MMU.IO_LCD_Y, memory.readByte(MMU.IO_LCD_Y, true) + 1);
            }
            cycles -= LR35902.CPU_CYCLES_PER_V_BLANK;
        }
    }

    private void processHBlank() {
        if (cycles >= LR35902.CPU_CYCLES_PER_H_BLANK) {
            if (memory.readIORegisterBit(MMU.IO_LCD_STAT, Flags.STATUS_H_BLANK_IRQ_ON.getMask(), true))
                memory.writeIORegisterBit(MMU.IO_INTERRUPT_FLAG, core.cpu.Flags.IRQ_LCD_STAT.getMask(), true);

            fillBackgroundLayer();
            fillWindowLayer();
            fillSpriteLayer();

            if (memory.readByte(MMU.IO_LCD_Y, true) == SCREEN_HEIGHT - 1)
                memory.writeLcdMode(LCDMode.V_BLANK);
            else
                memory.writeLcdMode(LCDMode.OAM);

            memory.writeRaw(MMU.IO_LCD_Y, memory.readByte(MMU.IO_LCD_Y, true) + 1);
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
            if (memory.readIORegisterBit(MMU.IO_LCD_STAT, Flags.STATUS_OAM_IRQ_ON.getMask(), true))
                memory.writeIORegisterBit(MMU.IO_INTERRUPT_FLAG, core.cpu.Flags.IRQ_LCD_STAT.getMask(), true);

            int lcd_y = memory.readByte(MMU.IO_LCD_Y, true);
            int lcd_yc = memory.readByte(MMU.IO_LCD_YC, true);

            if (lcd_y == lcd_yc) {
                memory.writeIORegisterBit(MMU.IO_LCD_STAT, Flags.STATUS_COINCIDENCE.getMask(), true);
                if (memory.readIORegisterBit(MMU.IO_LCD_STAT, Flags.STATUS_LCD_Y_IRQ_ON.getMask(), true))
                    memory.writeIORegisterBit(MMU.IO_INTERRUPT_FLAG, core.cpu.Flags.IRQ_LCD_STAT.getMask(), true);
            } else {
                memory.writeIORegisterBit(MMU.IO_LCD_STAT, Flags.STATUS_COINCIDENCE.getMask(), false);
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
        if (!memory.readIORegisterBit(MMU.IO_LCD_CONTROL, Flags.CONTROL_BG_ON.getMask(), true))
            return;

        int lcdY = memory.readByte(MMU.IO_LCD_Y, true);
        int scrollY = memory.readByte(MMU.IO_SCROLL_Y, true);
        int scrollX = memory.readByte(MMU.IO_SCROLL_X, true);

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
}
