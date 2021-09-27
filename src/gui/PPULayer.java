package gui;

import core.GameBoy;
import core.memory.MMU;
import core.ppu.helper.Sprite;
import imgui.ImGui;
import openGL.Texture;

public class PPULayer extends AbstractDebugLayer {

    private Texture[] tileMaps;
    private Texture[] tileTables;
    private Texture oam;

    public PPULayer(GameBoy gameboy) {
        super(gameboy);
    }

    public void render() {
        ImGui.begin("PPU");
        if (ImGui.beginTabBar("tab")) {
            if (ImGui.beginTabItem("Tile Data")) {
                ImGui.setWindowSize(270, 450);
                ImGui.image(tileTables[0].getID(), 128 * 2, 64 * 2);
                ImGui.image(tileTables[1].getID(), 128 * 2, 64 * 2);
                ImGui.image(tileTables[2].getID(), 128 * 2, 64 * 2);
                ImGui.endTabItem();
            }
            if (ImGui.beginTabItem("Tile Maps")) {
                ImGui.setWindowSize(535, 315);
                ImGui.image(tileMaps[0].getID(), 256, 256);
                ImGui.sameLine();
                ImGui.image(tileMaps[1].getID(), 256, 256);
                ImGui.endTabItem();
            }
            if (ImGui.beginTabItem("OAM")) {
                ImGui.setWindowSize(1068, 485);
                ImGui.newLine();
                int addr = MMU.OAM_START;
                ImGui.beginGroup();
                ImGui.newLine();
                for (int i = 0; i < 5; i++) {
                    for (int j = 0; j < 8; j++) {
                        ImGui.sameLine();
                        Sprite sprite = new Sprite(gameboy.getMemory().readByte(addr++, true), gameboy.getMemory().readByte(addr++, true), gameboy.getMemory().readByte(addr++, true), gameboy.getMemory().readByte(addr++, true));
                        ImGui.beginChild("sprite" + (8 * i + j), 65, 65);
                        ImGui.textColored(255, 255, 0, 255, "   Y:");
                        ImGui.sameLine();
                        if (sprite.y != 0) ImGui.text(String.format("%02X", sprite.y));
                        else ImGui.textColored(128, 128, 128, 255, String.format("%02X", sprite.y));
                        ImGui.textColored(255, 255, 0, 255, "   X:");
                        ImGui.sameLine();
                        if (sprite.x != 0) ImGui.text(String.format("%02X", sprite.x));
                        else ImGui.textColored(128, 128, 128, 255, String.format("%02X", sprite.x));
                        ImGui.textColored(255, 255, 0, 255, "Tile:");
                        ImGui.sameLine();
                        if (sprite.tileId != 0) ImGui.text(String.format("%02X", sprite.tileId));
                        else ImGui.textColored(128, 128, 128, 255, String.format("%02X", sprite.tileId));
                        ImGui.textColored(255, 255, 0, 255, "Attr:");
                        ImGui.sameLine();
                        if (sprite.attributes != 0) ImGui.text(String.format("%02X", sprite.attributes));
                        else ImGui.textColored(128, 128, 128, 255, String.format("%02X", sprite.attributes));
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

    public void linkTextures(Texture[] tileMaps_textures, Texture[] tileTables_textures, Texture oam_texture) {
        this.tileMaps = tileMaps_textures;
        this.tileTables = tileTables_textures;
        this.oam = oam_texture;
    }
}