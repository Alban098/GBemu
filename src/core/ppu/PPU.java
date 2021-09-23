package core.ppu;

import com.sun.jna.ptr.ByteByReference;
import core.Flags;
import core.GameBoy;
import core.GameBoyState;
import core.memory.MMU;
import core.cpu.LR35902;
import core.ppu.helper.BackgroundMaps;
import core.ppu.helper.ColorPalettes;
import core.ppu.helper.ColorShade;
import core.ppu.helper.TileCollection;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;

import static core.BitUtils.signedByte;

public class PPU {

    public static final int SCREEN_WIDTH = 160;
    public static final int SCREEN_HEIGHT = 144;

    private final ByteBuffer screen_buffer;
    private final ByteBuffer[] tileMaps;
    private final ByteBuffer[] tileTables;
    private final ColorShade[][] background_buffer;
    private final MMU memory;

    private final ColorPalettes palettes;
    private ColorPalettes.ColorPalette bgPal;
    private final BackgroundMaps bgMaps;
    private final TileCollection tileList;

    private long cycles = 0;
    private boolean isFrameComplete;
    private int off_cycles = 0;


    public PPU(MMU memory) {
        screen_buffer = BufferUtils.createByteBuffer(SCREEN_HEIGHT * SCREEN_WIDTH * 4);
        tileMaps = new ByteBuffer[]{
                BufferUtils.createByteBuffer(256 * 256 * 4),
                BufferUtils.createByteBuffer(256 * 256 * 4)
        };
        tileTables = new ByteBuffer[]{
                BufferUtils.createByteBuffer(128 * 64 * 4),
                BufferUtils.createByteBuffer(128 * 64 * 4),
                BufferUtils.createByteBuffer(128 * 64 * 4)
        };
        background_buffer = new ColorShade[SCREEN_HEIGHT][SCREEN_WIDTH];
        palettes = new ColorPalettes(memory);
        bgMaps = new BackgroundMaps(memory);
        tileList = new TileCollection(memory);
        this.memory = memory;
    }

    public ByteBuffer getScreenBuffer() {
        return screen_buffer;
    }

    public void clock(int mcycles) {
        if (!memory.readIORegisterBit(MMU.LCDC, Flags.LCDC_LCD_ON)) {
            //prevent rendering routine from getting stuck when LCD is off
            off_cycles+= mcycles;
            if (off_cycles >= LR35902.CPU_CYCLES_PER_FRAME) {
                screen_buffer.clear();
                if (GameBoy.DEBUG) {
                    computeTileTables();
                    computeTileMaps();
                }
                isFrameComplete = true;
                off_cycles -= LR35902.CPU_CYCLES_PER_FRAME;
            }
            return;
        }
        cycles += mcycles;
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
                if (GameBoy.DEBUG) {
                    computeTileTables();
                    computeTileMaps();
                }
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

    private void computeTileMaps() {
        int i = 0;
        boolean mode1;
        for (ByteBuffer tileMap : tileMaps) {
            tileMap.clear();
            for (int y = 0; y < 256; y++) {
                for (int x = 0; x < 256; x++) {
                    int tileAttribAddr = (i == 0 ? MMU.BG_MAP0_START : MMU.BG_MAP1_START) | (x >> 3) | ((y & 0xF8) << 2);
                    int tileId = memory.readByte(tileAttribAddr, true);
                    mode1 = memory.readIORegisterBit(MMU.LCDC, Flags.LCDC_BG_TILE_DATA);
                    if (!mode1)
                        tileId = signedByte(tileId);

                    ColorShade color = getTileColor(mode1 ? 0 : 1, tileId, x & 0x7, y & 0x7);
                    tileMap.put((byte) color.getColor().getRed());
                    tileMap.put((byte) color.getColor().getGreen());
                    tileMap.put((byte) color.getColor().getBlue());
                    tileMap.put((byte) 255);
                }
            }
            tileMap.flip();
            i++;
        }
    }

    private void computeTileTables() {
        /*
            We iterate over rows of tiles
            for each row we iterate over each tile
            for each tile we draw one line of it before going to the next
            this ensure that pixels are pushed to the ByteBuffer in the correct order
        */
        int i = 0;
        for (ByteBuffer tileTable : tileTables) {
            tileTable.clear();
            //Iterate over tiles rows
            for (int tileRow = 0; tileRow < 8; tileRow++) {
                //Iterate over rows of pixels
                for (int y = 0; y < 8; y++) {
                    //Iterate over tiles of the row
                    for (int tileId = 16 * tileRow; tileId < 16 * tileRow + 16; tileId++) {
                        //Iterate over each pixel of the tile line
                        for (int x = 0; x < 8; x++) {
                            ColorShade color = getTileColor(i, tileId, x, y);
                            tileTable.put((byte) color.getColor().getRed());
                            tileTable.put((byte) color.getColor().getGreen());
                            tileTable.put((byte) color.getColor().getBlue());
                            tileTable.put((byte) 255);
                        }
                    }
                }
            }
            tileTable.flip();
            i++;
        }
    }

    private ColorShade getTileColor(int tileBank, int tileId, int x, int y) {
        int tileAddr = 0x8000 + 0x800 * tileBank + tileId * 16;
        int low = memory.readByte(tileAddr | (y << 1), true);
        int high = memory.readByte(tileAddr | (y << 1) | 1, true);
        int colorIndex = (((high >> (7 - x)) & 0x1) << 1) | ((low >> (7 - x)) & 0x1);
        return ColorShade.get((memory.readByte(MMU.BGP) >> colorIndex * 2) & 0x3);
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

    public ByteBuffer[] getTileMaps() {
        return tileMaps;
    }

    public ByteBuffer[] getTileTables() {
        return tileTables;
    }
}
