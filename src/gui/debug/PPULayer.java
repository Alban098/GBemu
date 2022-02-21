package gui.debug;

import gbemu.core.Flags;
import gbemu.core.memory.MMU;
import gbemu.core.ppu.PPU;
import gbemu.core.ppu.helper.ColorShade;
import gbemu.core.ppu.helper.Sprite;
import gbemu.extension.debug.Debugger;
import gbemu.extension.debug.DebuggerMode;
import gbemu.extension.debug.Tile;
import imgui.ImGui;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import glwrapper.Texture;

import java.awt.*;

/**
 * This class represent the DebugLayer in charge of displaying the current state of the PPU
 * nametables, OAMS, palettes ...
 */
public class PPULayer extends DebugLayer {

    private Texture tile_map;
    private Texture tile_tables;
    private Texture oam;
    private Texture tile_texture;
    private boolean cgb_mode = false;
    private final Tile tile;

    private final ImInt selected_map = new ImInt();
    private final ImBoolean show_viewport = new ImBoolean();
    private final ImBoolean tile_map_grid = new ImBoolean();
    private final ImBoolean tile_tables_grid = new ImBoolean();

    /**
     * Create a new instance of PPULayer
     * @param debugger the debugger to link to
     */
    public PPULayer(Debugger debugger) {
        super(debugger);
        tile = new Tile();
        show_viewport.set(true);
    }

    /**
     * Create the needed Textures
     */
    public void initTextures() {
        tile_map = new Texture(256,256);
        tile_tables = new Texture(256, 192);
        tile_texture = new Texture(8, 8);
        oam = new Texture(PPU.SCREEN_WIDTH, PPU.SCREEN_HEIGHT);
    }

    /**
     * Render the layer to the screen
     */
    public void render() {
        ImGui.begin("PPU");
        if (ImGui.beginTabBar("tab")) {
            if (ImGui.beginTabItem("Tile Data")) {
                debugger.setHooked(DebuggerMode.TILES, true);
                debugger.setHooked(DebuggerMode.PALETTES, false);
                debugger.setHooked(DebuggerMode.TILEMAPS, false);
                debugger.setHooked(DebuggerMode.OAMS, false);
                Point tileCoords = getHoveredTile();
                tileCoords.x >>= 1;
                tileCoords.y >>= 1;
                synchronized (debugger) {
                    debugger.setHoveredTileOnTables(tileCoords.x, tileCoords.y);
                    tile_tables.load(debugger.getTileTableBuffer().getBuffer());
                }
                ImGui.setWindowSize(670, 445);
                ImGui.image(tile_tables.getID(), 256 * 2, 192 * 2);
                ImGui.sameLine();
                ImGui.beginChild("Tile", 140, 260);
                if (tileCoords.x <= 0x20 && tileCoords.x >= 0x0 && tileCoords.y <= 0x17 && tileCoords.y >= 0x0) {
                    tile.fill(debugger.getTileTableHoveredTile());
                } else {
                    tile.fill(0, 0, 0, 0, 0, 0, 0);
                    tile.render_target.clear();
                    for (int i = 0; i < 256; i++)
                        tile.render_target.put((byte) 255);
                    tile.render_target.flip();
                }
                tile_texture.load(tile.render_target);
                ImGui.image(tile_texture.getID(), 128, 128);
                ImGui.textColored(0, 255, 255, 255, "Tile Address:");
                ImGui.sameLine();
                ImGui.text(String.format("$%04X", tile.tile_addr));
                ImGui.textColored(0, 255, 255, 255, "Tile Index:  ");
                ImGui.sameLine();
                ImGui.text(String.format("$%02X", tile.id));
                ImGui.separator();
                if (ImGui.checkbox("Grid", tile_tables_grid)) {
                    debugger.enableTileTablesGrid(tile_tables_grid.get());
                }
                ImGui.endChild();
                ImGui.endTabItem();
            }
            if (ImGui.beginTabItem("Palettes")) {
                debugger.setHooked(DebuggerMode.TILES, false);
                debugger.setHooked(DebuggerMode.PALETTES, true);
                debugger.setHooked(DebuggerMode.TILEMAPS, false);
                debugger.setHooked(DebuggerMode.OAMS, false);
                if (cgb_mode) {
                    ImGui.setWindowSize(370, 240);
                    for (int pal = 0; pal < 8; pal++) {
                        ImGui.newLine();
                        drawColorPalette("BG", pal, false);
                        ImGui.text("<- BG " +  pal + " | OBJ " + pal + " ->");
                        ImGui.sameLine();
                        drawColorPalette("OBJ", pal, true);
                        ImGui.newLine();
                    }
                } else {
                    ImGui.setWindowSize(267, 123);
                    synchronized (debugger) {
                        ImGui.newLine();
                        drawPalette(debugger.readMemory(MMU.BGP));
                        ImGui.text("<- BGP");
                        ImGui.newLine();
                        drawPalette(debugger.readMemory(MMU.OBP1));
                        ImGui.text("<- OBP0");
                        ImGui.newLine();
                        drawPalette(debugger.readMemory(MMU.OBP1));
                        ImGui.text("<- OBP1");
                    }
                }
                ImGui.endTabItem();
            }
            if (ImGui.beginTabItem("Tile Maps")) {
                debugger.setHooked(DebuggerMode.TILES, false);
                debugger.setHooked(DebuggerMode.PALETTES, false);
                debugger.setHooked(DebuggerMode.TILEMAPS, true);
                debugger.setHooked(DebuggerMode.OAMS, false);
                Point tile_coords = getHoveredTile();
                tile_coords.x >>= 1;
                tile_coords.y >>= 1;
                synchronized (debugger) {
                    debugger.setHoveredTileOnMap(tile_coords.x, tile_coords.y);
                    debugger.selectTileMap(selected_map.get());
                    tile_map.load(debugger.getTileMapBuffer(selected_map.get()).getBuffer());
                }
                ImGui.setWindowSize(670, 571);
                ImGui.image(tile_map.getID(), 512, 512);
                ImGui.sameLine();
                ImGui.beginChild("Tile", 140, 400);
                if (tile_coords.x <= 0x1F && tile_coords.x >= 0x0 && tile_coords.y <= 0x1F && tile_coords.y >= 0x0) {
                    tile.fill(debugger.getTileMapHoveredTile());
                } else {
                    tile.fill(0, 0, 0, 0, 0, 0, 0);
                    tile.render_target.clear();
                    for (int i = 0; i < 256; i++)
                        tile.render_target.put((byte) 255);
                    tile.render_target.flip();
                }
                tile_texture.load(tile.render_target);
                ImGui.image(tile_texture.getID(), 128, 128);
                ImGui.textColored(0, 255, 255, 255, "X:");
                ImGui.sameLine();
                ImGui.text(String.format("$%02X",tile.x));
                ImGui.sameLine(50);
                ImGui.textColored(0, 255, 255, 255, " Tile:  ");
                ImGui.sameLine();
                ImGui.text(String.format("$%02X", tile.id));

                ImGui.textColored(0, 255, 255, 255, "Y:");
                ImGui.sameLine();
                ImGui.text(String.format("$%02X", tile.y));
                ImGui.sameLine(50);
                ImGui.textColored(0, 255, 255, 255, " Attrib:");
                ImGui.sameLine();
                ImGui.text(cgb_mode ? String.format("$%02X", tile.attrib) : "---");

                ImGui.textColored(0, 255, 255, 255, "Map Address: ");
                ImGui.sameLine();
                ImGui.text(String.format("$%04X", tile.map_addr));
                ImGui.textColored(0, 255, 255, 255, "Tile Address:");
                ImGui.sameLine();
                ImGui.text(String.format("$%04X", tile.tile_addr));
                if (cgb_mode) {
                    ImGui.separator();
                    boolean xflip = (tile.attrib & Flags.CGB_TILE_HFLIP) != 0;
                    boolean yflip = (tile.attrib & Flags.CGB_TILE_HFLIP) != 0;
                    boolean priority = (tile.attrib & Flags.CGB_TILE_HFLIP) != 0;
                    int pal = (tile.attrib & Flags.CGB_TILE_PALETTE);
                    ImGui.textColored(0, 255, 255, 255, "Flip X:");
                    ImGui.sameLine();
                    ImGui.textColored(xflip ? 0 : 255, xflip ? 255 : 0, 0, 255, xflip ? "1" : "0");
                    ImGui.sameLine(70);
                    ImGui.textColored(0, 255, 255, 255, " Pal: ");
                    ImGui.sameLine();
                    ImGui.text(String.valueOf(pal));
                    ImGui.textColored(0, 255, 255, 255, "Flip Y:");
                    ImGui.sameLine();
                    ImGui.textColored(yflip ? 0 : 255, yflip ? 255 : 0, 0, 255, yflip ? "1" : "0");
                    ImGui.sameLine(70);
                    ImGui.textColored(0, 255, 255, 255, " Bank:");
                    ImGui.sameLine();
                    ImGui.text(String.valueOf((tile.attrib & Flags.CGB_TILE_VRAM_BANK) >> 3));
                    ImGui.textColored(0, 255, 255, 255, "Priority:");
                    ImGui.sameLine();
                    ImGui.textColored(priority ? 0 : 255, priority ? 255 : 0, 0, 255, priority ? "1" : "0");
                }
                ImGui.separator();
                ImGui.separator();
                if (ImGui.checkbox("Show Viewport", show_viewport)) {
                    debugger.enableViewport(show_viewport.get());
                }
                if (ImGui.checkbox("Grid", tile_map_grid)) {
                    debugger.enableTileMapGrid(tile_map_grid.get());
                }
                ImGui.combo("Map", selected_map, new String[]{"9800", "9C00"});
                debugger.selectTileMap(selected_map.get());
                ImGui.endChild();
                ImGui.endTabItem();
            }
            if (ImGui.beginTabItem("OAM")) {
                debugger.setHooked(DebuggerMode.TILES, false);
                debugger.setHooked(DebuggerMode.PALETTES, false);
                debugger.setHooked(DebuggerMode.TILEMAPS, false);
                debugger.setHooked(DebuggerMode.OAMS, true);
                synchronized (debugger) {
                    oam.load(debugger.getOAMBuffer().getBuffer());
                }
                ImGui.setWindowSize(760, 485);
                int addr = MMU.OAM_START;
                ImGui.newLine();
                synchronized (debugger) {
                    ImGui.beginChild("OAMs", 285, 400);
                    boolean hovered = false;
                    for (int oam = 0; oam < 40; oam++) {
                        ImGui.beginGroup();
                        Sprite sprite = new Sprite(debugger.readMemory(addr++), debugger.readMemory(addr++), debugger.readMemory(addr++), debugger.readMemory(addr++));
                        ImGui.textColored(255, 255, 0, 255, String.format("$%02X:", oam));

                        ImGui.sameLine();
                        ImGui.textColored(0, 255, 255, 255, "Y:");
                        ImGui.sameLine();
                        if (sprite.y() != 0) ImGui.text(String.format("%02X", sprite.y()));
                        else ImGui.textColored(128, 128, 128, 255, String.format("%02X", sprite.y()));

                        ImGui.sameLine();
                        ImGui.textColored(0, 255, 255, 255, " X:");
                        ImGui.sameLine();
                        if (sprite.x() != 0) ImGui.text(String.format("%02X", sprite.x()));
                        else ImGui.textColored(128, 128, 128, 255, String.format("%02X", sprite.x()));

                        ImGui.sameLine();
                        ImGui.textColored(0, 255, 255, 255, " Tile:");
                        ImGui.sameLine();
                        if (sprite.tileId() != 0) ImGui.text(String.format("%02X", sprite.tileId()));
                        else ImGui.textColored(128, 128, 128, 255, String.format("%02X", sprite.tileId()));

                        ImGui.sameLine();
                        ImGui.textColored(0, 255, 255, 255, " Attr:");
                        ImGui.sameLine();
                        if (sprite.attributes() != 0) ImGui.text(String.format("%02X", sprite.attributes()));
                        else ImGui.textColored(128, 128, 128, 255, String.format("%02X", sprite.attributes()));
                        boolean priority = (sprite.attributes() & Flags.SPRITE_ATTRIB_UNDER_BG) != 0;
                        boolean x_flip = (sprite.attributes() & Flags.SPRITE_ATTRIB_X_FLIP) != 0;
                        boolean y_flip = (sprite.attributes() & Flags.SPRITE_ATTRIB_X_FLIP) != 0;
                        int dmg_pal = (sprite.attributes() & Flags.SPRITE_ATTRIB_PAL) != 0 ? 1 : 0;
                        int bank = (sprite.attributes() & Flags.SPRITE_ATTRIB_CGB_VRAM_BANK) != 0 ? 1 : 0;
                        int pal = (sprite.attributes() & Flags.SPRITE_ATTRIB_CGB_PAL);
                        ImGui.textColored(255, 0, 255, 255, "     Palette:");
                        ImGui.sameLine();
                        if (!cgb_mode)
                            ImGui.text(dmg_pal == 1 ? "OBP1 " : "OBP0 ");
                        else
                            ImGui.text("OBJ " + pal);
                        ImGui.sameLine();
                        ImGui.textColored(255, 0, 255, 255, "     VRAM Bank:");
                        ImGui.sameLine();
                        ImGui.text(String.valueOf(bank));
                        ImGui.textColored(priority ? 0 : 255, priority ? 255 : 0, 0, 255, "     Priority");
                        ImGui.sameLine();
                        ImGui.textColored(x_flip ? 0 : 255, x_flip ? 255 : 0, 0, 255, "     X-Flip");
                        ImGui.sameLine();
                        ImGui.textColored(y_flip ? 0 : 255, y_flip ? 255 : 0, 0, 255, "     Y-Flip");
                        ImGui.separator();
                        ImGui.endGroup();
                        if (ImGui.isItemHovered()) {
                            hovered = true;
                            debugger.setHoveredSprite(sprite.x() - 8, sprite.y() - 16);
                        }
                    }
                    if (!hovered)
                        debugger.setHoveredSprite(-1, -1);
                    ImGui.endChild();
                }
                ImGui.sameLine();
                ImGui.image(oam.getID(), 444, 400);
                ImGui.endTabItem();
            }
        }
        ImGui.endTabBar();
        ImGui.end();
    }

    /**
     * Draw a palette to the screen
     * @param pal the palette id to render
     */
    private void drawPalette(int pal) {
        float[] color = {0f, 0f, 0f, 1f};
        for (int col = 0; col < 4; col++) {
            int id = (pal & (0x3 << (col << 1))) >> (col << 1);
            color[0] = ColorShade.get(id).getColor().getRed() / 255f;
            color[1] = ColorShade.get(id).getColor().getGreen() / 255f;
            color[2] = ColorShade.get(id).getColor().getBlue() / 255f;
            ImGui.sameLine();
            ImGui.colorButton("Color " + col, color);
            ImGui.sameLine();
        }
    }

    /**
     * Draw a CGB palette to the screen
     * @param name the name of the palette
     * @param pal the id of the palette
     * @param obj_pal is it an object palette
     */
    private void drawColorPalette(String name, int pal, boolean obj_pal) {
        float[] color = {0f, 0f, 0f, 1f};
        synchronized (debugger) {
            for (int col = 0; col < 4; col++) {
                int rgb555 = (debugger.readCGBPalette(obj_pal, 8 * pal + col * 2 + 1) << 8) | debugger.readCGBPalette(obj_pal, 8 * pal + col * 2);
                color[0] = (float) ((rgb555 & 0b000000000011111) / 32.0);
                color[1] = (float) (((rgb555 & 0b000001111100000) >> 5) / 32.0);
                color[2] = (float) (((rgb555 & 0b111110000000000) >> 10) / 32.0);
                ImGui.sameLine();
                ImGui.colorButton(name + " " + pal + " Color " + col + " : " + String.format("#%04X", rgb555), color);
                ImGui.sameLine();
            }
        }
    }

    /**
     * Activate / deactivate CGB Mode
     * @param cgb_mode activate CGB
     */
    public void setCgbMode(boolean cgb_mode) {
        this.cgb_mode = cgb_mode;
    }

    public void cleanUp() {
        oam.cleanUp();
        tile_map.cleanUp();
        tile_tables.cleanUp();
    }

    public Point getHoveredTile() {
        return new Point((int) ((ImGui.getMousePosX() - ImGui.getWindowPosX() - 8) / 8), (int) ((ImGui.getMousePosY() - ImGui.getWindowPosY() - 50) / 8));
    }
}