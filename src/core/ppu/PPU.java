package core.ppu;

import core.Flags;
import core.GameBoy;
import core.memory.MMU;
import core.cpu.LR35902;
import core.ppu.helper.ColorPalettes;
import core.ppu.helper.ColorShade;
import core.ppu.helper.Sprite;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;

import static core.BitUtils.signedByte;

public class PPU {

    public static final int SCREEN_WIDTH = 160;
    public static final int SCREEN_HEIGHT = 144;
    private static final int MAX_SPRITES_PER_SCANLINE = 10;

    private final ByteBuffer screen_buffer;
    private final ByteBuffer[] tileMaps;
    private final ByteBuffer[] tileTables;
    private final MMU memory;

    private final ColorPalettes palettes;

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
        palettes = new ColorPalettes(memory);
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
        if (cycles >= LR35902.CPU_CYCLES_PER_V_BLANK_SCANLINE) {

            if (memory.readByte(MMU.LY, true) == SCREEN_HEIGHT) {
                memory.writeIORegisterBit(MMU.IF, Flags.IF_VBLANK_IRQ, true);
                if (GameBoy.DEBUG) {
                    computeTileTables();
                    computeTileMaps();
                }
                isFrameComplete = true;
                screen_buffer.flip();
            }

            if (memory.readByte(MMU.LY, true) == 154) {
                screen_buffer.clear();
                memory.writeRaw(MMU.LY, 0);
                memory.writeLcdMode(LCDMode.OAM);
                if (memory.readIORegisterBit(MMU.STAT, Flags.STAT_OAM_IRQ_ON))
                    memory.writeIORegisterBit(MMU.IF, Flags.IF_LCD_STAT_IRQ, true);
                if (memory.readByte(MMU.LY, true) == memory.readByte(MMU.LYC, true)) {
                    memory.writeIORegisterBit(MMU.STAT, Flags.STAT_COINCIDENCE_STATUS, true);
                    if (memory.readIORegisterBit(MMU.STAT, Flags.STAT_COINCIDENCE_IRQ))
                        memory.writeIORegisterBit(MMU.IF, Flags.IF_LCD_STAT_IRQ, true);
                } else {
                    memory.writeIORegisterBit(MMU.STAT, Flags.STAT_COINCIDENCE_STATUS, false);
                }
            } else {
                memory.writeRaw(MMU.LY, memory.readByte(MMU.LY, true) + 1);
            }
            cycles -= LR35902.CPU_CYCLES_PER_V_BLANK_SCANLINE;
        }
    }

    private void processHBlank() {
        //This may be reworked if LCDC.tileTable can be change mid scanline, but I don't think so ??
        if (cycles >= LR35902.CPU_CYCLES_PER_H_BLANK) {

            //Register reads
            int y = memory.readByte(MMU.LY);
            int scx = memory.readByte(MMU.SCX);
            int scy = memory.readByte(MMU.SCY);
            int wx = memory.readByte(MMU.WX);
            int wy = memory.readByte(MMU.WY);
            int spriteSize = memory.readIORegisterBit(MMU.LCDC, Flags.LCDC_OBJ_SIZE) ? 16 : 8;
            boolean mode1 = memory.readIORegisterBit(MMU.LCDC, Flags.LCDC_BG_TILE_DATA);

            //Rendering banks base addresses
            int tileBGMapAddr = (memory.readIORegisterBit(MMU.LCDC, Flags.LCDC_BG_TILE_MAP) ? MMU.BG_MAP1_START : MMU.BG_MAP0_START) | ((((y + scy) & 0xFF) & 0xF8) << 2);
            int tileWINMapAddr = (memory.readIORegisterBit(MMU.LCDC, Flags.LCDC_WINDOW_MAP) ? MMU.BG_MAP1_START : MMU.BG_MAP0_START) | (((y - wy) & 0xF8) << 2);
            int oamAddr = MMU.OAM_START;

            ColorShade bgColor = ColorShade.TRANSPARENT, winColor = ColorShade.TRANSPARENT, spriteColor = ColorShade.TRANSPARENT, finalColor;

            //Temporary variables, declared here to increase reusability
            int tileIdAddr, tileId, spriteY, spriteSubX, spriteSubY;
            boolean spriteFlipX, spriteFlipY;
            Sprite[] sprites = new Sprite[PPU.MAX_SPRITES_PER_SCANLINE];
            for (int i = 0; i < PPU.MAX_SPRITES_PER_SCANLINE; i++)
                sprites[i] = new Sprite();

            /*
                Sprites fetch
                TODO sprites X priority
                Sprites priority when Xs are equal is ensure by the reverse filling of the sprites array
            */
            int foundSprites = PPU.MAX_SPRITES_PER_SCANLINE - 1;
            while(foundSprites >= 0 && oamAddr < MMU.OAM_END) {
                spriteY = memory.readByte(oamAddr++);
                if (spriteY - 16 <= y && spriteY - 16 + spriteSize > y) {
                    sprites[foundSprites].y = spriteY;
                    sprites[foundSprites].x = memory.readByte(oamAddr++);
                    sprites[foundSprites].tileId = memory.readByte(oamAddr++);
                    sprites[foundSprites].attributes = memory.readByte(oamAddr++);
                    foundSprites++;
                } else {
                    oamAddr += 3;
                }
            }

            for (int x = 0; x < 160; x++) {
                //Background
                if (memory.readIORegisterBit(MMU.LCDC, Flags.LCDC_BG_ON)) {
                    tileIdAddr = tileBGMapAddr | (((x + scx) & 0xFF) >> 3);
                    tileId = memory.readByte(tileIdAddr, true);
                    if (!mode1)
                        tileId = signedByte(tileId);
                    bgColor = getTileColor(
                            palettes.getBgPalette(),
                            mode1 ? 0 : 2,
                            tileId,
                            ((x + scx) & 0xFF) & 0x7,
                            ((y + scy) & 0xFF) & 0x7
                    );
                }

                //Window
                if (memory.readIORegisterBit(MMU.LCDC, Flags.LCDC_WINDOW_ON) && (x - wx - 7> 0) && (y - wy > 0) && (x - wx < 160) && (y - wy < 144) && wx < 166 && wy < 143) {
                    tileIdAddr = tileWINMapAddr | ((x - wx - 7) >> 3);
                    tileId = memory.readByte(tileIdAddr, true);
                    if (!mode1)
                        tileId = signedByte(tileId);
                    winColor = getTileColor(
                            palettes.getBgPalette(),
                            mode1 ? 0 : 2,
                            tileId,
                            (x - wx - 7) & 0x7,
                            (y - wy) & 0x7
                    );
                }

                //Sprites
                if (memory.readIORegisterBit(MMU.LCDC, Flags.LCDC_OBJ_ON)) {
                    for (Sprite sprite : sprites) {
                        if (sprite.x - 8 <= x && sprite.x >= x && ((sprite.attributes & Flags.SPRITE_ATTRIB_UNDER_BG) == 0x00)) {
                            spriteFlipX = (sprite.attributes & Flags.SPRITE_ATTRIB_X_FLIP) != 0x00;
                            spriteFlipY = (sprite.attributes & Flags.SPRITE_ATTRIB_Y_FLIP) != 0x00;
                            spriteSubX = spriteFlipX ? 7 - (x - sprite.x + 8) : (x - sprite.x + 8);
                            if (spriteSize == 8) { //8x8 mode
                                spriteSubY = spriteFlipY ? 7 - (y - sprite.y + 16) : (y - sprite.y + 16);
                                spriteColor = getTileColor(
                                        (sprite.attributes & Flags.SPRITE_ATTRIB_PAL) == 0x00 ? palettes.getObjPalette0() : palettes.getObjPalette1(),
                                        0,
                                        sprite.tileId,
                                        spriteSubX,
                                        spriteSubY
                                );
                            } else { //8x16 mode
                                spriteSubY = spriteFlipY ? 15 - (y - sprite.y + 16) : (y - sprite.y + 16);
                                spriteColor = getTileColor(
                                        (sprite.attributes & Flags.SPRITE_ATTRIB_PAL) == 0x00 ? palettes.getObjPalette0() : palettes.getObjPalette1(),
                                        0,
                                        spriteSubY < 8 ? sprite.tileId & 0xFE : sprite.tileId | 1,
                                        spriteSubX,
                                        spriteSubY
                                );
                            }
                        }
                    }
                }

                if (spriteColor == ColorShade.TRANSPARENT) {
                    if (winColor == ColorShade.TRANSPARENT) {
                        finalColor = bgColor;
                    } else {
                            finalColor = winColor;
                    }
                } else {
                    finalColor = spriteColor;
                }

                screen_buffer.put((byte) finalColor.getColor().getRed());
                screen_buffer.put((byte) finalColor.getColor().getGreen());
                screen_buffer.put((byte) finalColor.getColor().getBlue());
                screen_buffer.put((byte) 255);
            }

            if (memory.readByte(MMU.LY, true) == SCREEN_HEIGHT - 1) {
                memory.writeLcdMode(LCDMode.V_BLANK);
                if (memory.readIORegisterBit(MMU.STAT, Flags.STAT_VBLANK_IRQ_ON))
                    memory.writeIORegisterBit(MMU.IF, Flags.IF_LCD_STAT_IRQ, true);
            } else {
                memory.writeLcdMode(LCDMode.OAM);
                if (memory.readIORegisterBit(MMU.STAT, Flags.STAT_OAM_IRQ_ON))
                    memory.writeIORegisterBit(MMU.IF, Flags.IF_LCD_STAT_IRQ, true);
                if (memory.readByte(MMU.LY, true) == memory.readByte(MMU.LYC, true)) {
                    memory.writeIORegisterBit(MMU.STAT, Flags.STAT_COINCIDENCE_STATUS, true);
                    if (memory.readIORegisterBit(MMU.STAT, Flags.STAT_COINCIDENCE_IRQ))
                        memory.writeIORegisterBit(MMU.IF, Flags.IF_LCD_STAT_IRQ, true);
                } else {
                    memory.writeIORegisterBit(MMU.STAT, Flags.STAT_COINCIDENCE_STATUS, false);
                }
            }

            memory.writeRaw(MMU.LY, memory.readByte(MMU.LY, true) + 1);
            cycles -= LR35902.CPU_CYCLES_PER_H_BLANK;
        }
    }

    private void processTransfer() {
        if (cycles >= LR35902.CPU_CYCLES_PER_TRANSFER) {
            memory.writeLcdMode(LCDMode.H_BLANK);
            if (memory.readIORegisterBit(MMU.STAT, Flags.STAT_HBLANK_IRQ_ON))
                memory.writeIORegisterBit(MMU.IF, Flags.IF_LCD_STAT_IRQ, true);
            cycles -= LR35902.CPU_CYCLES_PER_TRANSFER;
        }
    }

    private void processOam() {
        if (cycles >= LR35902.CPU_CYCLES_PER_OAM) {
            memory.writeLcdMode(LCDMode.TRANSFER);
            cycles -= LR35902.CPU_CYCLES_PER_OAM;
        }
    }

    private void computeTileMaps() {
        int i = 0, tileIdAddr, tileId;
        boolean mode1 = memory.readIORegisterBit(MMU.LCDC, Flags.LCDC_BG_TILE_DATA);
        for (ByteBuffer tileMap : tileMaps) {
            tileMap.clear();
            for (int y = 0; y < 256; y++) {
                for (int x = 0; x < 256; x++) {
                    tileIdAddr = (i == 0 ? MMU.BG_MAP0_START : MMU.BG_MAP1_START) | (x >> 3) | ((y & 0xF8) << 2);
                    tileId = memory.readByte(tileIdAddr, true);
                    if (!mode1)
                        tileId = signedByte(tileId);

                    ColorShade color = getTileColor(palettes.getBgPalette(), mode1 ? 0 : 2, tileId, x & 0x7, y & 0x7);
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
                            ColorShade color = getTileColor(palettes.getBgPalette(), i, tileId, x, y);
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

    private ColorShade getTileColor(ColorPalettes.ColorPalette palette, int tileBank, int tileId, int x, int y) {
        int tileAddr = 0x8000 + 0x800 * tileBank + tileId * 16;
        int low = memory.readByte(tileAddr | (y << 1), true);
        int high = memory.readByte(tileAddr | (y << 1) | 1, true);
        int colorIndex = (((high >> (7 - x)) & 0x1) << 1) | ((low >> (7 - x)) & 0x1);
        return palette.colors[colorIndex];
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
