package core.ppu;

import core.BitUtils;
import core.Flags;
import core.GameBoy;
import core.memory.MMU;
import core.ppu.helper.ColorPalettes;
import core.ppu.helper.ColorShade;
import core.ppu.helper.Sprite;
import openGL.SwappingByteBuffer;

import java.awt.*;
import java.nio.ByteBuffer;
import java.util.*;

import static core.BitUtils.signedByte;

public class PPU {

    public static final int SCREEN_WIDTH = 160;
    public static final int SCREEN_HEIGHT = 144;
    private static final int MAX_SPRITES_PER_SCANLINE = 10;

    private final SwappingByteBuffer screen_buffer;
    private Collection<Sprite> sprites;

    private final MMU memory;
    private final GameBoy gameboy;

    private final ColorPalettes palettes;

    private long cycles = 0;
    private boolean isFrameComplete;
    private int off_cycles = 0;

    private final State state;

    public PPU(GameBoy gameboy) {
        this.gameboy = gameboy;
        this.memory = gameboy.getMemory();
        screen_buffer = new SwappingByteBuffer(SCREEN_HEIGHT * SCREEN_WIDTH * 4);
        palettes = new ColorPalettes(memory);
        sprites = new TreeSet<>();
        state = new State();
        gameboy.getDebugger().link(state);
    }

    public synchronized ByteBuffer getScreenBuffer() {
        return screen_buffer.getBuffer();
    }

    public void clock() {
        if (!memory.readIORegisterBit(MMU.LCDC, Flags.LCDC_LCD_ON)) {
            memory.writeLcdMode(LCDMode.H_BLANK);
            //prevent rendering routine from getting stuck when LCD is off
            off_cycles++;
            if (off_cycles >= gameboy.mode.cpu_cycles_per_frame) {
                screen_buffer.swap();
                isFrameComplete = true;
                off_cycles = 0;
            }
            return;
        }
        cycles++;

        switch (memory.readLcdMode()) {
            case OAM -> processOam();
            case TRANSFER -> processTransfer();
            case H_BLANK -> processHBlank();
            case V_BLANK -> processVBlank();
        }
    }

    private void processVBlank() {
        if (cycles >= gameboy.mode.cpu_cycles_per_vblank_scanline) {
            if (memory.readByte(MMU.LY, true) == SCREEN_HEIGHT) {
                memory.writeIORegisterBit(MMU.IF, Flags.IF_VBLANK_IRQ, true);
                isFrameComplete = true;
                screen_buffer.swap();
            }

            if (memory.readByte(MMU.LY, true) == 154) {
                memory.writeRaw(MMU.LY, 0);
                switchToLCDMode(LCDMode.OAM);
            } else {
                memory.writeRaw(MMU.LY, memory.readByte(MMU.LY, true) + 1);
            }
            cycles = 0;
        }
    }

    private void processHBlank() {
        //This may be reworked if LCDC.tileTable can be change mid scanline, but I don't think so ??
        if (cycles >= gameboy.mode.cpu_cycles_per_hblank) {

            //Register reads
            int y = memory.readByte(MMU.LY, true);
            int scx = memory.readByte(MMU.SCX, true);
            int scy = memory.readByte(MMU.SCY, true);
            int wx = memory.readByte(MMU.WX, true);
            int wy = memory.readByte(MMU.WY, true);
            int spriteSize = memory.readIORegisterBit(MMU.LCDC, Flags.LCDC_OBJ_SIZE) ? 16 : 8;
            boolean mode1 = memory.readIORegisterBit(MMU.LCDC, Flags.LCDC_BG_TILE_DATA);

            //Rendering banks base addresses
            int tileBGMapAddr = (memory.readIORegisterBit(MMU.LCDC, Flags.LCDC_BG_TILE_MAP) ? MMU.BG_MAP1_START : MMU.BG_MAP0_START) | ((((y + scy) & 0xFF) & 0xF8) << 2);
            int tileWINMapAddr = (memory.readIORegisterBit(MMU.LCDC, Flags.LCDC_WINDOW_MAP) ? MMU.BG_MAP1_START : MMU.BG_MAP0_START) | (((y - wy) & 0xF8) << 2);

            ColorShade bgColor, winColor, spriteColor, finalColor;

            //Temporary variables, declared here to increase reusability
            int tileIdAddr, tileId, winColorIndex, bgColorIndex, spriteColorIndex;
            boolean spritePriorityFlag = false;
            int cgb_tile_attr = 0, cgb_vram_bank, cgb_palette_nb;
            int priority; //0 = BG, 1 = Win, 2 = Sprite

            for (int x = 0; x < 160; x++) {
                bgColor = ColorShade.TRANSPARENT;
                winColor = ColorShade.TRANSPARENT;
                spriteColor = ColorShade.TRANSPARENT;
                winColorIndex = 0;
                bgColorIndex = 0;
                priority = 0;

                //Background
                tileIdAddr = tileBGMapAddr | (((x + scx) & 0xFF) >> 3);
                if (gameboy.mode != GameBoy.Mode.CGB) {
                    if (memory.readIORegisterBit(MMU.LCDC, Flags.LCDC_BG_ON)) {
                        tileId = memory.readVRAM(tileIdAddr, 0);
                        if (!mode1)
                            tileId = signedByte(tileId);
                        bgColorIndex = getTileColorIndex(
                                0,
                                mode1 ? 0 : 2,
                                tileId,
                                ((x + scx) & 0xFF) & 0x7,
                                ((y + scy) & 0xFF) & 0x7
                        );
                        bgColor = palettes.getBgPalette().colors[bgColorIndex];
                    }
                } else {
                    cgb_tile_attr = memory.readVRAM(tileIdAddr, 1);
                    cgb_vram_bank = (cgb_tile_attr & Flags.CGB_TILE_VRAM_BANK) >> 3;
                    cgb_palette_nb = (cgb_tile_attr & Flags.CGB_TILE_PALETTE);
                    tileId = memory.readVRAM(tileIdAddr & 0x7FFF, 0);
                    if (!mode1)
                        tileId = signedByte(tileId);
                    bgColorIndex = getTileColorIndex(
                            cgb_vram_bank,
                            mode1 ? 0 : 2,
                            tileId,
                            (cgb_tile_attr & Flags.CGB_TILE_HFLIP) == 0 ? ((x + scx) & 0xFF) & 0x7 : (((7 - x) + scx) & 0xFF) & 0x7,
                            (cgb_tile_attr & Flags.CGB_TILE_VFLIP) == 0 ? ((y + scy) & 0xFF) & 0x7 : (((7 - y) + scy) & 0xFF) & 0x7
                    );
                    bgColor = palettes.getCGBBgPalette(cgb_palette_nb).colors[bgColorIndex];
                }

                //Window
                tileIdAddr = tileWINMapAddr | ((x - (wx - 7)) >> 3);
                if (gameboy.mode != GameBoy.Mode.CGB) {
                    if (memory.readIORegisterBit(MMU.LCDC, Flags.LCDC_WINDOW_ON) && (x - (wx - 7) >= 0) && (y - wy >= 0) && (x - wx <= 160) && (y - wy <= 144) && wx < 166 && wy < 143) {
                        tileId = memory.readVRAM(tileIdAddr, 0);
                        if (!mode1)
                            tileId = signedByte(tileId);
                        winColorIndex = getTileColorIndex(
                                0,
                                mode1 ? 0 : 2,
                                tileId,
                                (x - (wx - 7)) & 0x7,
                                (y - wy) & 0x7
                        );
                        winColor = palettes.getBgPalette().colors[winColorIndex];
                        priority = 1;
                    }
                } else {
                    if (memory.readIORegisterBit(MMU.LCDC, Flags.LCDC_WINDOW_ON) && (x - (wx - 7) >= 0) && (y - wy >= 0) && (x - wx <= 160) && (y - wy <= 144) && wx < 166 && wy < 143) {
                        cgb_tile_attr = memory.readVRAM(tileWINMapAddr, 1);
                        cgb_vram_bank = (cgb_tile_attr & Flags.CGB_TILE_VRAM_BANK) >> 3;
                        cgb_palette_nb = (cgb_tile_attr & Flags.CGB_TILE_PALETTE);
                        tileId = memory.readByte(tileIdAddr, true);
                        if (!mode1)
                            tileId = signedByte(tileId);
                        winColorIndex = getTileColorIndex(
                                cgb_vram_bank,
                                mode1 ? 0 : 2,
                                tileId,
                                (cgb_tile_attr & Flags.CGB_TILE_HFLIP) == 0 ? (x - (wx - 7)) & 0x7 : ((7 - x) - (wx - 7)) & 0x7,
                                (cgb_tile_attr & Flags.CGB_TILE_VFLIP) == 0 ? (y - wy) & 0x7 : ((7 - y) - wy) & 0x7
                        );
                        winColor = palettes.getCGBBgPalette(cgb_palette_nb).colors[winColorIndex];
                        priority = 1;
                    }
                }

                //Sprites
                if (memory.readIORegisterBit(MMU.LCDC, Flags.LCDC_OBJ_ON)) {
                    for (Sprite sprite : sprites) {
                        if (sprite.x() - 8 <= x && sprite.x() > x) {
                            spritePriorityFlag = (sprite.attributes() & Flags.SPRITE_ATTRIB_UNDER_BG)!= 0x00;
                            spriteColorIndex = fetchSpriteColorIndex(x, y, spriteSize, sprite);

                            if (gameboy.mode == GameBoy.Mode.DMG)
                                spriteColor = ((sprite.attributes() & Flags.SPRITE_ATTRIB_PAL) == 0x00 ? palettes.getObjPalette0() : palettes.getObjPalette1()).colors[spriteColorIndex];
                            else
                                spriteColor = palettes.getCGBObjPalette(sprite.attributes() & Flags.SPRITE_ATTRIB_CGB_PAL).colors[spriteColorIndex];
                            if (spriteColorIndex != 0) {
                                priority = 2;
                                break;
                            }
                        }
                    }
                }

                if (gameboy.mode == GameBoy.Mode.CGB && (cgb_tile_attr & Flags.CGB_TILE_PRIORITY) != 0 && priority != 1)
                    priority = 0;
                else if (spritePriorityFlag && priority == 2)
                    if (winColor != ColorShade.TRANSPARENT && winColorIndex != 0)
                        priority = 1;
                    else if (bgColor != ColorShade.TRANSPARENT && bgColorIndex != 0)
                        priority = 0;

                if (priority == 0)
                    finalColor = bgColor;
                else if (priority == 1)
                    finalColor = winColor;
                else
                    finalColor = spriteColor;
                
                screen_buffer.put(finalColor.getColor());
            }

            if (memory.readByte(MMU.LY, true) == SCREEN_HEIGHT - 1)
                switchToLCDMode(LCDMode.V_BLANK);
            else
                switchToLCDMode(LCDMode.OAM);

            memory.writeRaw(MMU.LY, memory.readByte(MMU.LY, true) + 1);
            cycles = 0;
        }
    }

    private int fetchSpriteColorIndex(int x, int y, int spriteSize, Sprite sprite) {
        int spriteSubX = ((sprite.attributes() & Flags.SPRITE_ATTRIB_X_FLIP) != 0x00) ? 7 - (x - sprite.x() + 8) : (x - sprite.x() + 8);
        if (spriteSize == 8) { //8x8 mode
            int spriteSubY = ((sprite.attributes() & Flags.SPRITE_ATTRIB_Y_FLIP) != 0x00) ? 7 - (y - sprite.y() + 16) : (y - sprite.y() + 16);
            return getTileColorIndex(
                    gameboy.mode == GameBoy.Mode.CGB ? (sprite.attributes() & Flags.SPRITE_ATTRIB_CGB_VRAM_BANK) >> 3 : 0,
                    0,
                    sprite.tileId(),
                    spriteSubX,
                    spriteSubY
            );
        } else { //8x16 mode
            int spriteSubY = ((sprite.attributes() & Flags.SPRITE_ATTRIB_Y_FLIP) != 0x00) ? 15 - (y - sprite.y() + 16) : (y - sprite.y() + 16);
            return getTileColorIndex(
                    gameboy.mode == GameBoy.Mode.CGB ? (sprite.attributes() & Flags.SPRITE_ATTRIB_CGB_VRAM_BANK) >> 3 : 0,
                    0,
                    spriteSubY < 8 ? sprite.tileId() & 0xFE : sprite.tileId() | 1,
                    spriteSubX,
                    spriteSubY
            );
        }
    }

    private void processTransfer() {
        if (cycles >= gameboy.mode.cpu_cycles_per_transfer) {
            switchToLCDMode(LCDMode.H_BLANK);
            cycles = 0;

            fetchSprites(sprites, memory.readByte(MMU.LY, true));
        }
    }

    private void processOam() {
        if (cycles >= gameboy.mode.cpu_cycles_per_oam) {
            switchToLCDMode(LCDMode.TRANSFER);
            cycles = 0;

            //If CGB Mode, sprite priority is OAM Address and not X coords, so a TreeSet is not only high overhead but inaccurate to the official hardware
            switch (gameboy.mode) {
                case DMG -> { if (sprites instanceof ArrayList) sprites = new TreeSet<>(); }
                case CGB -> { if (sprites instanceof TreeSet) sprites = new ArrayList<>(); }
            }
        }
    }

    private void fetchSprites(Collection<Sprite> sprites, int y) {
        int spriteSize = memory.readIORegisterBit(MMU.LCDC, Flags.LCDC_OBJ_SIZE) ? 16 : 8;
        int addr = MMU.OAM_START;
        int foundSprites = 0, spriteY;
        sprites.clear();

        while(foundSprites < MAX_SPRITES_PER_SCANLINE && addr < MMU.OAM_END) {
            spriteY = memory.readByte(addr++, true);
            if (spriteY - 16 <= y && spriteY - 16 + spriteSize > y) {
                sprites.add(new Sprite(spriteY, memory.readByte(addr++, true), memory.readByte(addr++, true), memory.readByte(addr++, true)));
                foundSprites++;
            } else {
                addr += 3;
            }
        }
    }

    private void switchToLCDMode(LCDMode mode) {
        if (mode == LCDMode.V_BLANK) {
            memory.writeLcdMode(LCDMode.V_BLANK);
            if (memory.readIORegisterBit(MMU.STAT, Flags.STAT_VBLANK_IRQ_ON))
                memory.writeIORegisterBit(MMU.IF, Flags.IF_LCD_STAT_IRQ, true);
        } else if (mode == LCDMode.OAM) {
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
        } else if (mode == LCDMode.H_BLANK) {
            memory.writeLcdMode(LCDMode.H_BLANK);
            if (memory.readIORegisterBit(MMU.STAT, Flags.STAT_HBLANK_IRQ_ON))
                memory.writeIORegisterBit(MMU.IF, Flags.IF_LCD_STAT_IRQ, true);
        } else if (mode == LCDMode.TRANSFER) {
            memory.writeLcdMode(LCDMode.TRANSFER);
        }
    }

    public synchronized void computeOAM() {
        state.getOAMBuffer().clear();
        int spriteSize = memory.readIORegisterBit(MMU.LCDC, Flags.LCDC_OBJ_SIZE) ? 16 : 8;
        ColorShade color;
        for (int y = 0; y < 144; y++) {
            Set<Sprite> sprites = new TreeSet<>();
            fetchSprites(sprites, y);

            for (int x = 0; x < 160; x++) {
                color = ColorShade.EMPTY;
                for (Sprite sprite : sprites) {
                    if (sprite.x() - 8 <= x && sprite.x() > x) {
                        int colorIndex = fetchSpriteColorIndex(x, y, spriteSize, sprite);
                        if (gameboy.mode != GameBoy.Mode.CGB)
                            color = ((sprite.attributes() & Flags.SPRITE_ATTRIB_PAL) == 0x00 ? palettes.getObjPalette0() : palettes.getObjPalette1()).colors[colorIndex];
                        else
                            color = palettes.getCGBObjPalette(sprite.attributes() & Flags.SPRITE_ATTRIB_CGB_PAL).colors[colorIndex];
                        if (color == ColorShade.TRANSPARENT || colorIndex == 0)
                            color = ColorShade.EMPTY;
                        else
                            break;
                    }
                }
                state.getOAMBuffer().put(color.getColor());
            }
        }
        state.getOAMBuffer().swap();
    }

    public synchronized void computeTileMaps() {
        int i = 0, tileIdAddr, tileId, cgb_tile_attr, cgb_vram_bank, cgb_palette_nb;
        boolean mode1 = memory.readIORegisterBit(MMU.LCDC, Flags.LCDC_BG_TILE_DATA);
        int scx = memory.readByte(MMU.SCX, true);
        int scy = memory.readByte(MMU.SCY, true);
        ColorShade color;
        for (SwappingByteBuffer tileMap : state.getTileMapBuffers()) {
            tileMap.clear();
            for (int y = 0; y < 256; y++) {
                for (int x = 0; x < 256; x++) {
                    if (((memory.readByte(MMU.LCDC) & Flags.LCDC_BG_TILE_MAP) >> 3) == i && ((y == scy && (BitUtils.inRange(x, scx, scx + 160) || ((scx + 160) > 0xFF) & BitUtils.inRange(x, 0, (scx + 160) & 0xFF))) ||
                        (y == ((scy + 144) & 0xFF) && (BitUtils.inRange(x, scx, scx + 160) || ((scx + 160) > 0xFF) & BitUtils.inRange(x, 0, (scx + 160) & 0xFF))) ||
                        (x == scx && (BitUtils.inRange(y, scy, scy + 144) || ((scy + 144) > 0xFF) & BitUtils.inRange(y, 0, (scy + 144) & 0xFF))) ||
                        (x == ((scx + 160) & 0xFF) && (BitUtils.inRange(y, scy, scy + 144) || ((scy + 144) > 0xFF) & BitUtils.inRange(y, 0, (scy + 144) & 0xFF))))) {
                        tileMap.put(Color.RED);
                    } else {
                        tileIdAddr = (i == 0 ? MMU.BG_MAP0_START : MMU.BG_MAP1_START) | (x >> 3) | ((y & 0xF8) << 2);
                        if (gameboy.mode == GameBoy.Mode.CGB) {
                            cgb_tile_attr = memory.readVRAM(tileIdAddr, 1);
                            cgb_vram_bank = (cgb_tile_attr & Flags.CGB_TILE_VRAM_BANK) >> 3;
                            cgb_palette_nb = (cgb_tile_attr & Flags.CGB_TILE_PALETTE);
                            tileId = memory.readVRAM(tileIdAddr - 0x8000, 0);
                            if (!mode1)
                                tileId = signedByte(tileId);
                            int colorIndex = getTileColorIndex(
                                    cgb_vram_bank,
                                    mode1 ? 0 : 2,
                                    tileId,
                                    (cgb_tile_attr & Flags.CGB_TILE_HFLIP) == 0 ? x & 0x7 : (7 - x) & 0x7,
                                    (cgb_tile_attr & Flags.CGB_TILE_VFLIP) == 0 ? y & 0x7 : (7 - y) & 0x7
                            );
                            color = palettes.getCGBBgPalette(cgb_palette_nb).colors[colorIndex];
                        } else {
                            tileId = memory.readByte(tileIdAddr, true);
                            if (!mode1)
                                tileId = signedByte(tileId);
                            color = palettes.getBgPalette().colors[getTileColorIndex(0, mode1 ? 0 : 2, tileId, x & 0x7, y & 0x7)];
                        }

                        tileMap.put(color.getColor());
                    }
                }
            }
            tileMap.swap();
            i++;
        }
    }

    public synchronized void computeTileTables() {
        /*
            We iterate over rows of tiles
            for each row we iterate over each tile
            for each tile we draw one line of it before going to the next
            this ensure that pixels are pushed to the ByteBuffer in the correct order
        */
        int i = 0;
        for (SwappingByteBuffer tileTable : state.getTileTableBuffers()) {
            if (gameboy.mode == GameBoy.Mode.DMG && i == 4)
                break;
            tileTable.clear();
            //Iterate over tiles rows
            for (int tileRow = 0; tileRow < 8; tileRow++) {
                //Iterate over rows of pixels
                for (int y = 0; y < 8; y++) {
                    //Iterate over tiles of the row
                    for (int tileId = 16 * tileRow; tileId < 16 * tileRow + 16; tileId++) {
                        //Iterate over each pixel of the tile line
                        for (int x = 0; x < 8; x++) {
                            ColorShade color = palettes.getBgPalette().colors[getTileColorIndex((i > 2) ? 1 : 0, i % 3, tileId, x, y)];
                            tileTable.put(color.getColor());
                        }
                    }
                }
            }
            tileTable.swap();
            i++;
        }
    }

    private int getTileColorIndex(int vram_brank, int tileBank, int tileId, int x, int y) {
        int tileAddr = MMU.TILE_BLOCK_START + 0x800 * tileBank + tileId * 16;
        int low = memory.readVRAM(tileAddr | (y << 1), vram_brank);
        int high = memory.readVRAM(tileAddr | (y << 1) | 1, vram_brank);
        return (((high >> (7 - x)) & 0x1) << 1) | ((low >> (7 - x)) & 0x1);
    }

    public void reset() {
        screen_buffer.clear();
        state.clearBuffers();
        isFrameComplete = false;
        cycles = 0;
        off_cycles = 0;
    }

    public boolean isFrameIncomplete() {
        boolean result = isFrameComplete;
        isFrameComplete = false;
        return !result;
    }

    public void setGamma(float gamma) {
        palettes.setGamma(gamma);
    }
}
