package gui.std;

import gbemu.core.GameBoy;
import gbemu.extension.cheats.CheatManager;
import gbemu.extension.cheats.GameSharkCode;
import imgui.ImGui;
import imgui.type.ImBoolean;
import imgui.type.ImString;

/**
 * This class represent the Cheats window
 * allowing user to add and remove GameShark cheats
 */
public class CheatsLayer extends Layer {

    private final GameBoy gameboy;
    private final CheatManager cheat_manager;
    private final ImString cheat_string = new ImString();
    private final ImString cheat_name = new ImString();

    /**
     * Create a new instance of the layer
     * @param gameboy the Game Boy where the cartridge is inserted
     * @param cheat_manager the Cheat Manager to link the Layer to
     */
    public CheatsLayer(GameBoy gameboy, CheatManager cheat_manager) {
        this.cheat_manager = cheat_manager;
        this.gameboy = gameboy;
    }

    /**
     * Render the layer to the screen
     * and propagate the cheats to the manager
     */
    public void render() {
        ImGui.begin("GameShark Cheats");
        ImGui.setWindowSize(340, 520);
        ImGui.sameLine(290);
        if (ImGui.button("Exit"))
            setVisible(false);
        ImGui.separator();
        ImGui.setNextItemOpen(true);
        ImGui.beginChild("Cheats", 340, 428);
        for (GameSharkCode cheat : cheat_manager.getCheats(gameboy.getGameId())) {
            ImBoolean enabled = new ImBoolean(cheat.isEnabled());
            if (ImGui.checkbox(cheat.getName(), enabled))
                cheat.setEnabled(enabled.get());
            ImGui.sameLine(180);
            ImGui.textColored(255, 255, 0, 255, cheat.getRawCheat());
            ImGui.sameLine(270);
            if (ImGui.button("Delete"))
                cheat_manager.removeCheat(gameboy.getGameId(), cheat);
        }
        ImGui.endChild();
        ImGui.separator();
        ImGui.pushItemWidth(70);
        if (!gameboy.getGameId().equals("")) {
            ImGui.inputText(": Name  ", cheat_name);
            ImGui.sameLine();
            if (ImGui.inputText(": Cheat    ", cheat_string))
                cheat_string.set(cheat_string.get().replaceAll("[^A-Fa-f0-9]*[ ]*", ""));
            ImGui.sameLine();
            if (ImGui.button("Add")) {
                cheat_string.set(cheat_string.get().replaceAll("[^A-Fa-f0-9]*[ ]*", ""));
                if (cheat_string.get().equals(""))
                    cheat_string.set("0");
                cheat_manager.addCheat(gameboy.getGameId(), cheat_name.get(), cheat_string.get());
                cheat_name.set("");
                cheat_string.set("");
            }
        } else {
            ImGui.textColored(255, 0, 0, 255, "Please load a game to access GameShark cheats !");
        }
        ImGui.end();
    }
}