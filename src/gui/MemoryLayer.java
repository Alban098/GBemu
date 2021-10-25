package gui;

import debug.Debugger;
import imgui.ImGui;
import imgui.type.ImBoolean;
import imgui.type.ImString;
import threading.DebuggerThread;

public class MemoryLayer extends AbstractDebugLayer {

    private static final int HIGHLIGH_DURATION = 64;
    private final DebuggerThread debuggerThread;
    private int highlight = -1;
    private int highlight_cooldown = 0;
    private final ImString goTo = new ImString();
    private final ImBoolean gradient = new ImBoolean();
    private final int[] currentPage = new int[1];

    public MemoryLayer(Debugger debugger, DebuggerThread debuggerThread) {
        super(debugger);
        this.debuggerThread = debuggerThread;
    }

    public void render() {
        ImGui.begin("Memory");
        ImGui.setWindowSize(555, 400);
        ImGui.sameLine();
        if (ImGui.button("Previous Page"))
            currentPage[0] = (currentPage[0] - 0x010) & 0xFFF;
        ImGui.sameLine(185);
        ImGui.pushItemWidth(100);
        ImGui.inputText("", goTo);
        ImGui.sameLine();
        if (ImGui.button("Search Address")) {
            highlight = Integer.decode("0x" + goTo.get().replaceAll(" ", ""));
            currentPage[0] = (highlight & 0xFF00) >> 4;
            highlight_cooldown = HIGHLIGH_DURATION;
        }
        ImGui.sameLine(467);
        if (ImGui.button("Next Page"))
            currentPage[0] = (currentPage[0] + 0x010) & 0xFFF;
        ImGui.separator();
        ImGui.checkbox("Gradient", gradient);
        ImGui.separator();
        ImGui.text("     ");
        for (int i = 0x0; i <= 0xF; i++) {
            ImGui.sameLine();
            ImGui.textColored(255, 255, 0, 255, String.format("%02X", i));
        }
        for (int i = 0x0; i <= 0xF; i++) {
            ImGui.textColored(255, 255, 0, 255, String.format("%04X ", (currentPage[0] << 4) + (i << 4)));
            for(int data = 0x0; data <= 0xF; data++) {
                ImGui.sameLine();
                int addr = (currentPage[0] << 4) + (i << 4) | data;
                int read = debugger.readMemorySnapshot(addr);
                if (addr == highlight) {
                    if (read == 0x00)
                        ImGui.textColored(128 + 128 * highlight_cooldown/HIGHLIGH_DURATION, 128 - 128 * highlight_cooldown/HIGHLIGH_DURATION, 128 - 128 * highlight_cooldown/HIGHLIGH_DURATION, 255, String.format("%02X", read));
                    else
                        ImGui.textColored(255, 255 - 255 * highlight_cooldown/HIGHLIGH_DURATION, 255 - 255 * highlight_cooldown/HIGHLIGH_DURATION, 255, String.format("%02X", read));
                    if (highlight_cooldown-- == 0)
                        highlight = -1;
                } else if (!gradient.get()) {
                    if (read == 0x00)
                        ImGui.textColored(128, 128, 128, 255, String.format("%02X", read));
                    else
                        ImGui.text(String.format("%02X", read));
                } else
                    ImGui.textColored(read, read, read, 255, String.format("%02X", read));
            }
            ImGui.sameLine();
            ImGui.text(" | ");
            StringBuilder dataString = new StringBuilder();
            for(int data = 0x0; data <= 0xF; data++) {
                char read = (char)debugger.readMemorySnapshot((currentPage[0] << 4) + (i << 4) | data);
                dataString.append(read < 0x20 ? "." : read);
            }
            ImGui.sameLine();
            ImGui.text(dataString.toString());
        }
        ImGui.pushItemWidth(538);
        ImGui.sliderInt(" ", currentPage, 0, 0xFF0, String.format("%03X0", currentPage[0]));
        ImGui.end();
    }
}