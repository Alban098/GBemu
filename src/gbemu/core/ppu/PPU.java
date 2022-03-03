package gbemu.core.ppu;

import gbemu.core.BitUtils;
import gbemu.core.Flags;
import gbemu.core.GameBoy;
import gbemu.core.memory.MMU;
import gbemu.core.ppu.helper.ColorPalettes;
import gbemu.core.ppu.helper.ColorShade;
import gbemu.core.ppu.helper.Sprite;
import glwrapper.SwappingByteBuffer;

import java.awt.*;
import java.nio.ByteBuffer;
import java.util.*;

import static gbemu.core.BitUtils.signedByte;

/**
 * This class represents the PPU, it handles everything relative to screen rendering
 */
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

    /**
     * Create a new PPU instance
     * @param gameboy the Game Boy to link to
     */
    public PPU(GameBoy gameboy) {
        this.gameboy = gameboy;
        this.memory = gameboy.getMemory();
        screen_buffer = new SwappingByteBuffer(SCREEN_HEIGHT * SCREEN_WIDTH * 4);
        palettes = new ColorPalettes(memory);
        sprites = new TreeSet<>();
        state = new State();
        gameboy.getDebugger().link(state);
    }

    /**
     * Return the screen buffer that can be displayed to the screen
     * @return the screen buffer
     */
    public synchronized ByteBuffer getScreenBuffer() {
        return screen_buffer.getBuffer();
    }

    /**
     * Return the screen buffer that can be displayed to the screen
     * @return the screen buffer
     */
    public synchronized boolean isScreenUpdated() {
        return screen_buffer.hasSwapped();
    }

    /**
     * Execute a cycle of the PPU and compute everything that happen during that cycle
     */
    public void clock() {
        if (!memory.readIORegisterBit(MMU.LCDC, Flags.LCDC_LCD_ON)) {
            memory.writeLcdMode(LCDMode.H_BLANK);
            //prevent rendering routine from getting stuck when LCD is off
            off_cycles++;
            if (off_cycles >= gameboy.mode.CYCLES_PER_FRAME) {
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

    /**
     * Execute a cycle when in LCDMode V_BLANK
     */
    private void processVBlank() {
        if (cycles >= gameboy.mode.CYCLES_PER_VBLANK_SCANLINE) {
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
        if (cycles >= gameboy.mode.CYCLES_PER_HBLANK) {

            //Register reads
            int y = memory.readByte(MMU.LY, true);
            int scx = memory.readByte(MMU.SCX, true);
            int scy = memory.readByte(MMU.SCY, true);
            int wx = memory.readByte(MMU.WX, true);
            int wy = memory.readByte(MMU.WY, true);
            int spriteSize = memory.readIORegisterBit(MMU.LCDC, Flags.LCDC_OBJ_SIZE) ? 16 : 8;
            boolean mode1 = memory.readIORegisterBit(MMU.LCDC, Flags.LCDC_BG_TILE_DATA);

            //Rendering banks base addresses
            int tile_bg_map_addr = (memory.readIORegisterBit(MMU.LCDC, Flags.LCDC_BG_TILE_MAP) ? MMU.BG_MAP1_START : MMU.BG_MAP0_START) | ((((y + scy) & 0xFF) & 0xF8) << 2);
            int tile_win_map_addr = (memory.readIORegisterBit(MMU.LCDC, Flags.LCDC_WINDOW_MAP) ? MMU.BG_MAP1_START : MMU.BG_MAP0_START) | (((y - wy) & 0xF8) << 2);

            ColorShade bg_color, win_color, sprite_color, final_color;

            //Temporary variables, declared here to increase reusability
            int tile_id_addr, tile_id, win_color_index, bg_color_index, sprite_color_index;
            boolean sprite_priority_flag = false;
            int cgb_tile_attr = 0, cgb_vram_bank, cgb_palette_nb;
            int priority; //0 = BG, 1 = Win, 2 = Sprite

            for (int x = 0; x < 160; x++) {
                bg_color = ColorShade.TRANSPARENT;
                win_color = ColorShade.TRANSPARENT;
                sprite_color = ColorShade.TRANSPARENT;
                win_color_index = 0;
                bg_color_index = 0;
                priority = 0;

                //Background
                tile_id_addr = tile_bg_map_addr | (((x + scx) & 0xFF) >> 3);
                if (gameboy.mode != GameBoy.Mode.CGB) {
                    if (memory.readIORegisterBit(MMU.LCDC, Flags.LCDC_BG_ON)) {
                        tile_id = memory.readVRAM(tile_id_addr, 0);
                        if (!mode1)
                            tile_id = signedByte(tile_id);
                        bg_color_index = getTileColorIndex(
                                0,
                                mode1 ? 0 : 2,
                                tile_id,
                                ((x + scx) & 0xFF) & 0x7,
                                ((y + scy) & 0xFF) & 0x7
                        );
                        bg_color = palettes.getBgPalette().colors[bg_color_index];
                    }
                } else {
                    cgb_tile_attr = memory.readVRAM(tile_id_addr, 1);
                    cgb_vram_bank = (cgb_tile_attr & Flags.CGB_TILE_VRAM_BANK) >> 3;
                    cgb_palette_nb = (cgb_tile_attr & Flags.CGB_TILE_PALETTE);
                    tile_id = memory.readVRAM(tile_id_addr, 0);
                    if (!mode1)
                        tile_id = signedByte(tile_id);
                    bg_color_index = getTileColorIndex(
                            cgb_vram_bank,
                            mode1 ? 0 : 2,
                            tile_id,
                            (cgb_tile_attr & Flags.CGB_TILE_HFLIP) == 0 ? ((x + scx) & 0xFF) & 0x7 : (((7 - x) + scx) & 0xFF) & 0x7,
                            (cgb_tile_attr & Flags.CGB_TILE_VFLIP) == 0 ? ((y + scy) & 0xFF) & 0x7 : (((7 - y) + scy) & 0xFF) & 0x7
                    );
                    bg_color = palettes.getCGBBgPalette(cgb_palette_nb).colors[bg_color_index];
                }

                //Window
                tile_id_addr = tile_win_map_addr | ((x - (wx - 7)) >> 3);
                if (gameboy.mode != GameBoy.Mode.CGB) {
                    if (memory.readIORegisterBit(MMU.LCDC, Flags.LCDC_WINDOW_ON) && (x - (wx - 7) >= 0) && (y - wy >= 0) && (x - wx <= 160) && (y - wy <= 144) && wx < 166 && wy < 143) {
                        tile_id = memory.readVRAM(tile_id_addr, 0);
                        if (!mode1)
                            tile_id = signedByte(tile_id);
                        win_color_index = getTileColorIndex(
                                0,
                                mode1 ? 0 : 2,
                                tile_id,
                                (x - (wx - 7)) & 0x7,
                                (y - wy) & 0x7
                        );
                        win_color = palettes.getBgPalette().colors[win_color_index];
                        priority = 1;
                    }
                } else {
                    if (memory.readIORegisterBit(MMU.LCDC, Flags.LCDC_WINDOW_ON) && (x - (wx - 7) >= 0) && (y - wy >= 0) && (x - wx <= 160) && (y - wy <= 144) && wx < 166 && wy < 143) {
                        cgb_tile_attr = memory.readVRAM(tile_win_map_addr, 1);
                        cgb_vram_bank = (cgb_tile_attr & Flags.CGB_TILE_VRAM_BANK) >> 3;
                        cgb_palette_nb = (cgb_tile_attr & Flags.CGB_TILE_PALETTE);
                        tile_id = memory.readByte(tile_id_addr, true);
                        if (!mode1)
                            tile_id = signedByte(tile_id);
                        win_color_index = getTileColorIndex(
                                cgb_vram_bank,
                                mode1 ? 0 : 2,
                                tile_id,
                                (cgb_tile_attr & Flags.CGB_TILE_HFLIP) == 0 ? (x - (wx - 7)) & 0x7 : ((7 - x) - (wx - 7)) & 0x7,
                                (cgb_tile_attr & Flags.CGB_TILE_VFLIP) == 0 ? (y - wy) & 0x7 : ((7 - y) - wy) & 0x7
                        );
                        win_color = palettes.getCGBBgPalette(cgb_palette_nb).colors[win_color_index];
                        priority = 1;
                    }
                }

                //Sprites
                if (memory.readIORegisterBit(MMU.LCDC, Flags.LCDC_OBJ_ON)) {
                    for (Sprite sprite : sprites) {
                        if (sprite.x() - 8 <= x && sprite.x() > x) {
                            sprite_priority_flag = (sprite.attributes() & Flags.SPRITE_ATTRIB_UNDER_BG)!= 0x00;
                            sprite_color_index = fetchSpriteColorIndex(x, y, spriteSize, sprite);

                            if (gameboy.mode == GameBoy.Mode.DMG)
                                sprite_color = ((sprite.attributes() & Flags.SPRITE_ATTRIB_PAL) == 0x00 ? palettes.getObjPalette0() : palettes.getObjPalette1()).colors[sprite_color_index];
                            else
                                sprite_color = palettes.getCGBObjPalette(sprite.attributes() & Flags.SPRITE_ATTRIB_CGB_PAL).colors[sprite_color_index];
                            if (sprite_color_index != 0) {
                                priority = 2;
                                break;
                            }
                        }
                    }
                }

                if (gameboy.mode == GameBoy.Mode.CGB && (cgb_tile_attr & Flags.CGB_TILE_PRIORITY) != 0 && priority != 1)
                    priority = 0;
                else if (sprite_priority_flag && priority == 2)
                    if (win_color != ColorShade.TRANSPARENT && win_color_index != 0)
                        priority = 1;
                    else if (bg_color != ColorShade.TRANSPARENT && bg_color_index != 0)
                        priority = 0;

                if (priority == 0)
                    final_color = bg_color;
                else if (priority == 1)
                    final_color = win_color;
                else
                    final_color = sprite_color;
                
                screen_buffer.put(final_color.getColor());
            }

            if (memory.readByte(MMU.LY, true) == SCREEN_HEIGHT - 1)
                switchToLCDMode(LCDMode.V_BLANK);
            else
                switchToLCDMode(LCDMode.OAM);

            memory.writeRaw(MMU.LY, memory.readByte(MMU.LY, true) + 1);
            cycles = 0;
        }
    }

    /**
     * Return the color index of a Sprite at a sampled coordinate
     * @param x the x coordinate inside the sprite (from left to right, 0 to 8)
     * @param y the y coordinate inside the sprite (from top to bottom, 0 to 8 or 16 depending on sprite_size)
     * @param sprite_size the sprite height (8 or 16)
     * @param sprite the sprite to load from
     * @return the color index at the sampled coordinate
     */
    private int fetchSpriteColorIndex(int x, int y, int sprite_size, Sprite sprite) {
        int sprite_sub_X = ((sprite.attributes() & Flags.SPRITE_ATTRIB_X_FLIP) != 0x00) ? 7 - (x - sprite.x() + 8) : (x - sprite.x() + 8);
        if (sprite_size == 8) { //8x8 mode
            int spriteSubY = ((sprite.attributes() & Flags.SPRITE_ATTRIB_Y_FLIP) != 0x00) ? 7 - (y - sprite.y() + 16) : (y - sprite.y() + 16);
            return getTileColorIndex(
                    gameboy.mode == GameBoy.Mode.CGB ? (sprite.attributes() & Flags.SPRITE_ATTRIB_CGB_VRAM_BANK) >> 3 : 0,
                    0,
                    sprite.tileId(),
                    sprite_sub_X,
                    spriteSubY
            );
        } else { //8x16 mode
            int sprite_sub_Y = ((sprite.attributes() & Flags.SPRITE_ATTRIB_Y_FLIP) != 0x00) ? 15 - (y - sprite.y() + 16) : (y - sprite.y() + 16);
            return getTileColorIndex(
                    gameboy.mode == GameBoy.Mode.CGB ? (sprite.attributes() & Flags.SPRITE_ATTRIB_CGB_VRAM_BANK) >> 3 : 0,
                    0,
                    sprite_sub_Y < 8 ? sprite.tileId() & 0xFE : sprite.tileId() | 1,
                    sprite_sub_X,
                    sprite_sub_Y
            );
        }
    }

    /**
     * Execute a cycle when in LCDMode TRANSFER
     */
    private void processTransfer() {
        if (cycles >= gameboy.mode.CYCLES_PER_TRANSFER) {
            switchToLCDMode(LCDMode.H_BLANK);
            cycles = 0;

            fetchSprites(sprites, memory.readByte(MMU.LY, true));
        }
    }

    /**
     * Execute a cycle when in LCDMode OAM
     */
    private void processOam() {
        if (cycles >= gameboy.mode.CYCLES_PER_OAM) {
            switchToLCDMode(LCDMode.TRANSFER);
            cycles = 0;

            //If CGB Mode, sprite priority is OAM Address and not X coords, so a TreeSet is not only high overhead but inaccurate to the official hardware
            switch (gameboy.mode) {
                case DMG -> { if (sprites instanceof ArrayList) sprites = new TreeSet<>(); }
                case CGB -> { if (sprites instanceof TreeSet) sprites = new ArrayList<>(); }
            }
        }
    }

    /**
     * Retrieve every sprite present on a scanline to a Collection
     * @param sprites the Collection to populate
     * @param y the y coordinate of the current scanline
     */
    private void fetchSprites(Collection<Sprite> sprites, int y) {
        int sprite_size = memory.readIORegisterBit(MMU.LCDC, Flags.LCDC_OBJ_SIZE) ? 16 : 8;
        int addr = MMU.OAM_START;
        int found_sprites = 0, sprite_Y;
        sprites.clear();

        while(found_sprites < MAX_SPRITES_PER_SCANLINE && addr < MMU.OAM_END) {
            sprite_Y = memory.readByte(addr++, true);
            if (sprite_Y - 16 <= y && sprite_Y - 16 + sprite_size > y) {
                sprites.add(new Sprite(sprite_Y, memory.readByte(addr++, true), memory.readByte(addr++, true), memory.readByte(addr++, true)));
                found_sprites++;
            } else {
                addr += 3;
            }
        }
    }

    /**
     * Switch between LCDMode and write the right values to Status and Interrupts Registers
     * @param mode the LCDMode to switch to
     */
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

    /**
     * Compute the OAM for the Debugger View
     * @param hovered_sprite_X the x coordinate of the sprite currently hoovered by the mouse
     * @param hovered_sprite_Y the y coordinate of the sprite currently hoovered by the mouse
     */
    public synchronized void computeOAM(int hovered_sprite_X, int hovered_sprite_Y) {
        state.getOAMBuffer().clear();
        int spriteSize = memory.readIORegisterBit(MMU.LCDC, Flags.LCDC_OBJ_SIZE) ? 16 : 8;
        ColorShade color;
        for (int y = 0; y < 144; y++) {
            Set<Sprite> sprites = new TreeSet<>();
            fetchSprites(sprites, y);
            for (int x = 0; x < 160; x++) {
                color = ColorShade.EMPTY;
                if (hovered_sprite_X >= 0 && hovered_sprite_Y >= 0 && ((BitUtils.inRange(x, hovered_sprite_X, hovered_sprite_X + 8) && (y == hovered_sprite_Y || y == hovered_sprite_Y + spriteSize)) || ((BitUtils.inRange(y, hovered_sprite_Y, hovered_sprite_Y + spriteSize) && (x == hovered_sprite_X || x == hovered_sprite_X + 8))))) {
                    state.getOAMBuffer().put(Color.GREEN);
                } else {
                    for (Sprite sprite : sprites) {
                        if (sprite.x() - 8 <= x && sprite.x() > x) {
                            int colorIndex = fetchSpriteColorIndex(x, y, spriteSize, sprite);
                            if (gameboy.mode != GameBoy.Mode.CGB) {
                                color = ((sprite.attributes() & Flags.SPRITE_ATTRIB_PAL) == 0x00 ? palettes.getObjPalette0() : palettes.getObjPalette1()).colors[colorIndex];
                            } else {
                                color = palettes.getCGBObjPalette(sprite.attributes() & Flags.SPRITE_ATTRIB_CGB_PAL).colors[colorIndex];
                            }
                            if (color == ColorShade.TRANSPARENT || colorIndex == 0) {
                                color = ColorShade.EMPTY;
                            } else {
                                break;
                            }
                        }
                    }
                    state.getOAMBuffer().put(color.getColor());
                }
            }
        }
        state.getOAMBuffer().swap();
    }

    /**
     * Compute the TileMaps for the Debugger View
     * @param show_viewport do we need to render the current viewport rectangle
     * @param selected_tile_map the currently selected TileMap (Map0 or Map1)
     * @param hovered_tile_X the x coordinate of the tile currently hoovered by the mouse
     * @param hovered_tile_Y the y coordinate of the tile currently hoovered by the mouse
     * @param show_grid do we need to render the grid
     */
    public synchronized void computeTileMaps(boolean show_viewport, int selected_tile_map, int hovered_tile_X, int hovered_tile_Y, boolean show_grid) {
        int i = 0, tile_id_addr, tile_id, cgb_tile_attr, cgb_vram_bank, cgb_palette_nb;
        boolean mode_1 = memory.readIORegisterBit(MMU.LCDC, Flags.LCDC_BG_TILE_DATA);
        int scx = memory.readByte(MMU.SCX, true);
        int scy = memory.readByte(MMU.SCY, true);
        ColorShade color;
        SwappingByteBuffer tile_map = state.getTileMapBuffers()[selected_tile_map];
        tile_map.clear();
        for (int y = 0; y < 256; y++) {
            for (int x = 0; x < 256; x++) {
                int cornerX = hovered_tile_X * 8;
                int cornerY = hovered_tile_Y * 8;
                if (hovered_tile_X >= 0 && hovered_tile_Y >= 0 && ((BitUtils.inRange(x, cornerX, cornerX + 8) && (y == cornerY || y == cornerY + 8)) || ((BitUtils.inRange(y, cornerY, cornerY + 8) && (x == cornerX || x == cornerX + 8))))) {
                    tile_map.put(Color.GREEN);
                } else if (show_viewport && (((memory.readByte(MMU.LCDC) & Flags.LCDC_BG_TILE_MAP) >> 3) == i && ((y == scy && (BitUtils.inRange(x, scx, scx + 160) || ((scx + 160) > 0xFF) & BitUtils.inRange(x, 0, (scx + 160) & 0xFF))) ||
                    (y == ((scy + 144) & 0xFF) && (BitUtils.inRange(x, scx, scx + 160) || ((scx + 160) > 0xFF) & BitUtils.inRange(x, 0, (scx + 160) & 0xFF))) ||
                    (x == scx && (BitUtils.inRange(y, scy, scy + 144) || ((scy + 144) > 0xFF) & BitUtils.inRange(y, 0, (scy + 144) & 0xFF))) ||
                    (x == ((scx + 160) & 0xFF) && (BitUtils.inRange(y, scy, scy + 144) || ((scy + 144) > 0xFF) & BitUtils.inRange(y, 0, (scy + 144) & 0xFF)))))) {
                    tile_map.put(Color.RED);
                } else if (show_grid && ((x & 0x7) == 0 || (y & 0x7) == 0)) {
                    tile_map.put(Color.LIGHT_GRAY);
                } else {
                    tile_id_addr = (selected_tile_map == 0 ? MMU.BG_MAP0_START : MMU.BG_MAP1_START) | (x >> 3) | ((y & 0xF8) << 2);
                    if (gameboy.mode == GameBoy.Mode.CGB) {
                        cgb_tile_attr = memory.readVRAM(tile_id_addr, 1);
                        cgb_vram_bank = (cgb_tile_attr & Flags.CGB_TILE_VRAM_BANK) >> 3;
                        cgb_palette_nb = (cgb_tile_attr & Flags.CGB_TILE_PALETTE);
                        tile_id = memory.readVRAM(tile_id_addr, 0);
                        if (!mode_1)
                            tile_id = signedByte(tile_id);
                        int color_index = getTileColorIndex(
                                cgb_vram_bank,
                                mode_1 ? 0 : 2,
                                tile_id,
                                (cgb_tile_attr & Flags.CGB_TILE_HFLIP) == 0 ? x & 0x7 : (7 - x) & 0x7,
                                (cgb_tile_attr & Flags.CGB_TILE_VFLIP) == 0 ? y & 0x7 : (7 - y) & 0x7
                        );
                        color = palettes.getCGBBgPalette(cgb_palette_nb).colors[color_index];
                    } else {
                        tile_id = memory.readByte(tile_id_addr, true);
                        if (!mode_1)
                            tile_id = signedByte(tile_id);
                        color = palettes.getBgPalette().colors[getTileColorIndex(0, mode_1 ? 0 : 2, tile_id, x & 0x7, y & 0x7)];
                    }

                    tile_map.put(color.getColor());
                }
            }
        }
        tile_map.swap();
    }

    /**
     * Compute the TileTables for the Debugger View
     * @param hovered_tile_X the x coordinate of the tile currently hoovered by the mouse
     * @param hovered_tile_Y the y coordinate of the tile currently hoovered by the mouse
     * @param show_grid do we need to render the grid
     */
    public synchronized void computeTileTables(int hovered_tile_X, int hovered_tile_Y, boolean show_grid) {
        /*
            We iterate over rows of tiles
            for each row we iterate over each tile
            for each tile we draw one line of it before going to the next
            this ensure that pixels are pushed to the ByteBuffer in the correct order
        */
        int x = 0, y = 0;
        SwappingByteBuffer table = state.getTileTableBuffer();
        for (int tileRow = 0; tileRow < 24; tileRow++) {
            for (int pixelY = 0; pixelY < 8; pixelY++) {
                for (int bank = 0; bank < 2; bank++) {
                    for (int tileId = 16 * (tileRow & 0x7); tileId < 16 * (tileRow & 0x7) + 16; tileId++) {
                        for (int pixelX = 0; pixelX < 8; pixelX++) {
                            int cornerX = hovered_tile_X * 8;
                            int cornerY = hovered_tile_Y * 8;
                            if (hovered_tile_X >= 0 && hovered_tile_Y >= 0 && ((BitUtils.inRange(x, cornerX, cornerX + 8) && (y == cornerY || y == cornerY + 8)) || ((BitUtils.inRange(y, cornerY, cornerY + 8) && (x == cornerX || x == cornerX + 8))))) {
                                table.put(Color.GREEN);
                            } else if (show_grid && ((x & 0x7) == 0 || (y & 0x7) == 0)) {
                                table.put(Color.LIGHT_GRAY);
                            } else {
                                Color color;
                                if (gameboy.mode == GameBoy.Mode.DMG && bank == 1) {
                                    color = Color.WHITE;
                                } else {
                                    switch (getTileColorIndex(bank, tileRow >> 3, tileId, pixelX, pixelY)) {
                                        case 1 -> color = Color.LIGHT_GRAY;
                                        case 2 -> color = Color.GRAY;
                                        case 3 -> color = Color.BLACK;
                                        default -> color = Color.WHITE;
                                    }
                                }
                                table.put(color);
                            }
                            x++;
                            if (x == 256) {
                                x = 0;
                                y++;
                            }
                        }
                    }
                }
            }
        }
        table.swap();
    }

    /**
     * Get the color index of a tile at specified 
     * @param vram_brank
     * @param tileBank
     * @param tileId
     * @param x
     * @param y
     * @return
     */
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

    public void renderTile(int tileId, int attr, ByteBuffer buffer, int bank, boolean mode1, GameBoy.Mode mode) {
        ColorShade color;
        buffer.clear();
        if (mode == GameBoy.Mode.CGB) {
            int cgb_vram_bank = (attr & Flags.CGB_TILE_VRAM_BANK) >> 3;
            int cgb_palette_nb = (attr & Flags.CGB_TILE_PALETTE);
            if (!mode1)
                tileId = signedByte(tileId);
            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 8; x++) {
                    int colorIndex = getTileColorIndex(
                            cgb_vram_bank,
                            mode1 ? 0 : 2,
                            tileId,
                            (attr & Flags.CGB_TILE_HFLIP) == 0 ? x & 0x7 : (7 - x) & 0x7,
                            (attr & Flags.CGB_TILE_VFLIP) == 0 ? y & 0x7 : (7 - y) & 0x7
                    );
                    color = palettes.getCGBBgPalette(cgb_palette_nb).colors[colorIndex];
                    buffer.put((byte) color.getColor().getRed());
                    buffer.put((byte) color.getColor().getGreen());
                    buffer.put((byte) color.getColor().getBlue());
                    buffer.put((byte) color.getColor().getAlpha());
                }
            }

        } else if (mode == GameBoy.Mode.DMG) {
            if (!mode1)
                tileId = signedByte(tileId);
            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 8; x++) {
                    color = palettes.getBgPalette().colors[getTileColorIndex(bank, mode1 ? 0 : 2, tileId, x & 0x7, y & 0x7)];
                    buffer.put((byte) color.getColor().getRed());
                    buffer.put((byte) color.getColor().getGreen());
                    buffer.put((byte) color.getColor().getBlue());
                    buffer.put((byte) color.getColor().getAlpha());
                }
            }
        } else if (mode == null) {
            if (!mode1)
                tileId = signedByte(tileId);
            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 8; x++) {
                    int gray;
                    switch (getTileColorIndex(bank, mode1 ? 0 : 2, tileId, x & 0x7, y & 0x7)) {
                        case 1 -> gray = Color.LIGHT_GRAY.getRed();
                        case 2 -> gray = Color.GRAY.getRed();
                        case 3 -> gray = Color.BLACK.getRed();
                        default -> gray = Color.WHITE.getRed();
                    }
                    buffer.put((byte) gray);
                    buffer.put((byte) gray);
                    buffer.put((byte) gray);
                    buffer.put((byte) 255);
                }
            }
        }
        buffer.flip();
    }
}
