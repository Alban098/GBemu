package gui;

import core.cheats.CheatManager;
import core.cheats.GameSharkCode;
import debug.Debugger;
import imgui.ImGui;
import imgui.type.ImBoolean;
import imgui.type.ImString;

public class CheatsLayer extends AbstractDebugLayer {

    private final CheatManager cheatManager;
    private final ImString cheatString = new ImString();
    private final ImString cheatName = new ImString();

    public CheatsLayer(Debugger debugger, CheatManager cheatManager) {
        super(debugger);
        this.cheatManager = cheatManager;
    }

    public void render() {
        ImGui.begin("GameShark Cheats");
        ImGui.setWindowSize(340, 490);

        ImGui.setNextItemOpen(true);
        ImGui.beginChild("Cheats", 340, 428);
        for (GameSharkCode cheat : cheatManager.getCheats(debugger.getGameId())) {
            ImBoolean enabled = new ImBoolean(cheat.isEnabled());
            if (ImGui.checkbox(cheat.getName(), enabled))
                cheat.setEnabled(enabled.get());
            ImGui.sameLine(180);
            ImGui.textColored(255, 255, 0, 255, cheat.getRawCheat());
            ImGui.sameLine(270);
            if (ImGui.button("Delete"))
                cheatManager.removeCheat(debugger.getGameId(), cheat);
        }
        ImGui.endChild();
        ImGui.separator();
        ImGui.pushItemWidth(70);
        if (!debugger.getGameId().equals("")) {
            ImGui.inputText(": Name  ", cheatName);
            ImGui.sameLine();
            if (ImGui.inputText(": Cheat    ", cheatString))
                cheatString.set(cheatString.get().replaceAll("[^A-Fa-f0-9]*[ ]*", ""));
            ImGui.sameLine();
            if (ImGui.button("Add")) {
                cheatString.set(cheatString.get().replaceAll("[^A-Fa-f0-9]*[ ]*", ""));
                if (cheatString.get().equals(""))
                    cheatString.set("0");
                cheatManager.addCheat(debugger.getGameId(), cheatName.get(), cheatString.get());
                cheatName.set("");
                cheatString.set("");
            }
        } else {
            ImGui.textColored(255, 0, 0, 255, "Please load a game to access GameShark cheats !");
        }
        ImGui.end();
    }
}