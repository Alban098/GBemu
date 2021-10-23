package gui;

import core.memory.MMU;
import core.ppu.PPU;
import core.ppu.helper.ColorShade;
import core.ppu.helper.Sprite;
import debug.Debugger;
import debug.DebuggerMode;
import imgui.ImGui;
import openGL.Texture;

public class PPULayer extends AbstractDebugLayer {

    private Texture[] tileMaps;
    private Texture[][] tileTables;
    private Texture oam;
    private boolean cgbMode = false;

    public PPULayer(Debugger debugger) {
        super(debugger);
    }

    public void initTextures() {
        tileMaps = new Texture[]{
                new Texture(256, 256),
                new Texture(256, 256)
        };
        tileTables = new Texture[][]{
                {
                        new Texture(128, 64),
                        new Texture(128, 64),
                        new Texture(128, 64)
                },
                {
                        new Texture(128, 64),
                        new Texture(128, 64),
                        new Texture(128, 64)
                }
        };
        oam = new Texture(PPU.SCREEN_WIDTH, PPU.SCREEN_HEIGHT);
    }

    public void render() {
        ImGui.begin("PPU");
        if (ImGui.beginTabBar("tab")) {
            if (ImGui.beginTabItem("Tile Data")) {
                debugger.setHooked(DebuggerMode.TILES, true);
                debugger.setHooked(DebuggerMode.PALETTES, false);
                debugger.setHooked(DebuggerMode.TILEMAPS, false);
                debugger.setHooked(DebuggerMode.OAMS, false);
                if (cgbMode) {
                    tileTables[0][0].load(debugger.getPpuState().getTileTableBuffers()[0]);
                    tileTables[0][1].load(debugger.getPpuState().getTileTableBuffers()[1]);
                    tileTables[0][2].load(debugger.getPpuState().getTileTableBuffers()[2]);
                    tileTables[1][0].load(debugger.getPpuState().getTileTableBuffers()[3]);
                    tileTables[1][1].load(debugger.getPpuState().getTileTableBuffers()[4]);
                    tileTables[1][2].load(debugger.getPpuState().getTileTableBuffers()[5]);
                    ImGui.setWindowSize(535, 450);
                    ImGui.image(tileTables[0][0].getID(), 128 * 2, 64 * 2);
                    ImGui.sameLine();
                    ImGui.image(tileTables[1][0].getID(), 128 * 2, 64 * 2);
                    ImGui.image(tileTables[0][1].getID(), 128 * 2, 64 * 2);
                    ImGui.sameLine();
                    ImGui.image(tileTables[1][1].getID(), 128 * 2, 64 * 2);
                    ImGui.image(tileTables[0][2].getID(), 128 * 2, 64 * 2);
                    ImGui.sameLine();
                    ImGui.image(tileTables[1][2].getID(), 128 * 2, 64 * 2);
                } else {
                    tileTables[0][0].load(debugger.getPpuState().getTileTableBuffers()[0]);
                    tileTables[0][1].load(debugger.getPpuState().getTileTableBuffers()[1]);
                    tileTables[0][2].load(debugger.getPpuState().getTileTableBuffers()[2]);
                    ImGui.setWindowSize(270, 450);
                    ImGui.image(tileTables[0][0].getID(), 128 * 2, 64 * 2);
                    ImGui.image(tileTables[0][1].getID(), 128 * 2, 64 * 2);
                    ImGui.image(tileTables[0][2].getID(), 128 * 2, 64 * 2);
                }

                ImGui.endTabItem();
            }
            if (ImGui.beginTabItem("Palettes")) {
                debugger.setHooked(DebuggerMode.TILES, false);
                debugger.setHooked(DebuggerMode.PALETTES, true);
                debugger.setHooked(DebuggerMode.TILEMAPS, false);
                debugger.setHooked(DebuggerMode.OAMS, false);
                if (cgbMode) {
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
                    ImGui.setWindowSize(267, 140);
                    ImGui.newLine();
                    drawPalette("BGP", debugger.readMemorySnapshot(MMU.BGP));
                    drawPalette("OBP0", debugger.readMemorySnapshot(MMU.OBP0));
                    drawPalette("OBP1", debugger.readMemorySnapshot(MMU.OBP1));

                }
                ImGui.endTabItem();
            }
            if (ImGui.beginTabItem("Tile Maps")) {
                debugger.setHooked(DebuggerMode.TILES, false);
                debugger.setHooked(DebuggerMode.PALETTES, false);
                debugger.setHooked(DebuggerMode.TILEMAPS, true);
                debugger.setHooked(DebuggerMode.OAMS, false);
                tileMaps[0].load(debugger.getPpuState().getTileMapBuffers()[0]);
                tileMaps[1].load(debugger.getPpuState().getTileMapBuffers()[1]);
                ImGui.setWindowSize(535, 315);
                ImGui.image(tileMaps[0].getID(), 256, 256);
                ImGui.sameLine();
                ImGui.image(tileMaps[1].getID(), 256, 256);
                ImGui.endTabItem();
            }
            if (ImGui.beginTabItem("OAM")) {
                debugger.setHooked(DebuggerMode.TILES, false);
                debugger.setHooked(DebuggerMode.PALETTES, false);
                debugger.setHooked(DebuggerMode.TILEMAPS, false);
                debugger.setHooked(DebuggerMode.OAMS, true);
                oam.load(debugger.getPpuState().getOAMBuffer());
                ImGui.setWindowSize(1068, 485);
                ImGui.newLine();
                int addr = MMU.OAM_START;
                ImGui.beginGroup();
                ImGui.newLine();
                for (int i = 0; i < 5; i++) {
                    for (int j = 0; j < 8; j++) {
                        ImGui.sameLine();
                        Sprite sprite = new Sprite(debugger.readMemorySnapshot(addr++), debugger.readMemorySnapshot(addr++), debugger.readMemorySnapshot(addr++), debugger.readMemorySnapshot(addr++));
                        ImGui.beginChild("sprite" + (8 * i + j), 65, 65);
                        ImGui.textColored(255, 255, 0, 255, "   Y:");
                        ImGui.sameLine();
                        if (sprite.y() != 0) ImGui.text(String.format("%02X", sprite.y()));
                        else ImGui.textColored(128, 128, 128, 255, String.format("%02X", sprite.y()));
                        ImGui.textColored(255, 255, 0, 255, "   X:");
                        ImGui.sameLine();
                        if (sprite.x() != 0) ImGui.text(String.format("%02X", sprite.x()));
                        else ImGui.textColored(128, 128, 128, 255, String.format("%02X", sprite.x()));
                        ImGui.textColored(255, 255, 0, 255, "Tile:");
                        ImGui.sameLine();
                        if (sprite.tileId() != 0) ImGui.text(String.format("%02X", sprite.tileId()));
                        else ImGui.textColored(128, 128, 128, 255, String.format("%02X", sprite.tileId()));
                        ImGui.textColored(255, 255, 0, 255, "Attr:");
                        ImGui.sameLine();
                        if (sprite.attributes() != 0) ImGui.text(String.format("%02X", sprite.attributes()));
                        else ImGui.textColored(128, 128, 128, 255, String.format("%02X", sprite.attributes()));
                        ImGui.endChild();
                    }
                    if (i < 4) {
                        ImGui.newLine();
                        ImGui.newLine();
                    }
                }
                ImGui.endGroup();
                ImGui.sameLine();
                ImGui.image(oam.getID(), (int) (160*2.85), (int) (144*2.85));
                ImGui.endTabItem();
            }
        }
        ImGui.endTabBar();
        ImGui.end();
    }

    private void drawPalette(String name, int pal) {
        float[] color = {0f, 0f, 0f, 1f};
        for (int col = 0; col < 4; col++) {
            int id = (pal & (0x3 << (col << 1))) >> (col << 1);
            color[0] = ColorShade.get(id).getColor().getRed() / 255f;
            color[1] = ColorShade.get(id).getColor().getGreen() / 255f;
            color[2] = ColorShade.get(id).getColor().getBlue() / 255f;
            ImGui.colorButton("Color " + col, color);
            ImGui.sameLine();
        }
        ImGui.text(name);
    }

    private void drawColorPalette(String name, int pal, boolean obj_pal) {
        float[] color = {0f, 0f, 0f, 1f};
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

    public void setCgbMode(boolean cgbMode) {
        this.cgbMode = cgbMode;
    }

    public void cleanUp() {
        oam.cleanUp();
        for (Texture tex : tileMaps)
            tex.cleanUp();
        for (Texture[] texs : tileTables)
            for (Texture tex : texs)
                tex.cleanUp();
    }
}