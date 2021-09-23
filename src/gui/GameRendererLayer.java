package gui;

import core.ppu.PPU;
import imgui.ImGui;
import openGL.Texture;

public class GameRendererLayer {

    public void imgui(Texture texture) {
        ImGui.begin("GameRenderer");
        ImGui.text(ImGui.getIO().getFramerate() + "fps");
        ImGui.beginChild("GameRenderer");
        ImGui.image(texture.getID(), PPU.SCREEN_WIDTH*5, PPU.SCREEN_WIDTH*5);
        ImGui.endChild();

        ImGui.end();
    }
}