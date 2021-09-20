package gui;

import core.GameBoy;
import imgui.ImGui;



public class SerialOutputLayer {


    public void imgui(GameBoy gameBoy) {
        ImGui.begin("Serial Output");
        ImGui.setWindowSize(515, 192);
        ImGui.beginChild("Scrolling", 500, 130);

        ImGui.textColored(0, 255, 0, 255, getPrettifiedOutput(gameBoy.getSerialOutput(), 69));
        ImGui.endChild();
        if (ImGui.button("Clear", 500, 18))
            gameBoy.flushSerialOutput();
        ImGui.end();
    }

    private static String getPrettifiedOutput(String input, int lineLength) {
        StringBuilder sb = new StringBuilder();
        for (String line : input.lines().toList()) {
            if (line.length() > lineLength) {
                for (int i = 1; i <= line.length(); i++) {
                    sb.append(line.charAt(i-1));
                    if (i % lineLength == 0)
                        sb.append("\n");
                }
                sb.append("\n");
            } else {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

}