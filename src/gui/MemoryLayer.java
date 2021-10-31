package gui;

import core.Flags;
import core.memory.MMU;
import debug.Debugger;
import imgui.ImGui;
import imgui.type.ImBoolean;
import imgui.type.ImString;
import utils.Utils;

public class MemoryLayer extends AbstractDebugLayer {

    private static final int HIGHLIGH_DURATION = 64;
    private int highlight = -1;
    private int highlight_cooldown = 0;
    private final ImString goTo = new ImString();
    private final ImBoolean gradient = new ImBoolean();
    private final int[] currentPage = new int[1];

    public MemoryLayer(Debugger debugger) {
        super(debugger);
    }

    public void render() {
        ImGui.begin("Memory");
        if (ImGui.beginTabBar("tab")) {
            if (ImGui.beginTabItem("Memory")) {
                ImGui.setWindowSize(595, 415);
                ImGui.pushItemWidth(100);
                if (ImGui.inputText(": Search Address", goTo)) {
                    goTo.set(goTo.get().replaceAll("[^A-Fa-f0-9]*[ ]*", ""));
                    if (goTo.get().equals(""))
                        goTo.set("0");
                    highlight = Integer.decode("0x" + goTo.get());
                    currentPage[0] = (highlight & 0xFF00) >> 4;
                    highlight_cooldown = HIGHLIGH_DURATION;
                }
                ImGui.sameLine(500);
                ImGui.checkbox("Gradient", gradient);
                ImGui.separator();
                ImGui.textColored(0, 255, 255, 255, "ROM");
                ImGui.sameLine();
                ImGui.text(String.format("= $%02X", debugger.getROMBank()));
                ImGui.sameLine();
                ImGui.textColored(0, 255, 255, 255, "  RAM");
                ImGui.sameLine();
                ImGui.text(String.format("= $%02X", debugger.getRAMBank()));
                ImGui.sameLine();
                ImGui.textColored(0, 255, 255, 255, "  VRAM");
                ImGui.sameLine();
                ImGui.text(String.format("= $%02X", debugger.getVRAMBank()));
                ImGui.sameLine();
                ImGui.textColored(0, 255, 255, 255, "  WRAM");
                ImGui.sameLine();
                ImGui.text(String.format("= $%02X", debugger.getWRAMBank()));
                ImGui.separator();
                ImGui.text("           ");
                for (int i = 0x0; i <= 0xF; i++) {
                    ImGui.sameLine();
                    ImGui.textColored(255, 255, 0, 255, String.format("%02X", i));
                }
                for (int i = 0x0; i <= 0xF; i++) {
                    int addr = (currentPage[0] << 4) + (i << 4);
                    ImGui.textColored(255, 0, 155, 255, debugger.getSector(addr) + ":");
                    ImGui.sameLine();
                    ImGui.textColored(255, 255, 0, 255, String.format("%04X ", addr));
                    for (int data = 0x0; data <= 0xF; data++) {
                        ImGui.sameLine();
                        addr = (currentPage[0] << 4) + (i << 4) | data;
                        int read = debugger.readMemory(addr);
                        if (addr == highlight) {
                            if (read == 0x00)
                                ImGui.textColored(128 + 128 * highlight_cooldown / HIGHLIGH_DURATION, 128 - 128 * highlight_cooldown / HIGHLIGH_DURATION, 128 - 128 * highlight_cooldown / HIGHLIGH_DURATION, 255, String.format("%02X", read));
                            else
                                ImGui.textColored(255, 255 - 255 * highlight_cooldown / HIGHLIGH_DURATION, 255 - 255 * highlight_cooldown / HIGHLIGH_DURATION, 255, String.format("%02X", read));
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
                    for (int data = 0x0; data <= 0xF; data++) {
                        char read = (char) debugger.readMemory((currentPage[0] << 4) + (i << 4) | data);
                        dataString.append(read < 0x20 ? "." : read);
                    }
                    ImGui.sameLine();
                    ImGui.text(dataString.toString());
                }
                ImGui.pushItemWidth(450);
                ImGui.sliderInt(" ", currentPage, 0, 0xFF0, String.format("%03X0", currentPage[0]));
                ImGui.endTabItem();
            }
            if (ImGui.beginTabItem("I/O Map")) {
                ImGui.setWindowSize(505, 440);
                ImGui.textColored(255, 255, 0, 255, "  Interrupts:");
                ImGui.sameLine(270);
                ImGui.textColored(255, 255, 0, 255, "  LCD Registers:");
                inlineRegister(MMU.IE, "IE  ", debugger.readMemory(MMU.IE));
                ImGui.sameLine(270);
                inlineRegister(MMU.LCDC, "LCDC", debugger.readMemory(MMU.LCDC));
                inlineRegister(MMU.IF, "IF  ", debugger.readMemory(MMU.IF));
                ImGui.sameLine(270);
                inlineRegister(MMU.STAT, "STAT", debugger.readMemory(MMU.STAT));
                int enabled = debugger.readMemory(MMU.IE) & debugger.readMemory(MMU.IF);
                ImGui.textColored(0, 255, 255, 255, "    VBLANK ");
                ImGui.sameLine();
                ImGui.textColored((enabled & Flags.IE_VBLANK_IRQ) == 0 ? 255 : 0, (enabled & Flags.IF_VBLANK_IRQ) == 0 ? 0 : 255, 0, 255, (enabled & Flags.IF_VBLANK_IRQ) == 0 ? "OFF" : "ON ");
                ImGui.sameLine();
                ImGui.textColored(255, 0, 255, 255, " IE:");
                ImGui.sameLine();
                ImGui.text((debugger.readMemory(MMU.IE) & Flags.IE_VBLANK_IRQ) != 0 ? "1" : "0");
                ImGui.sameLine();
                ImGui.textColored(255, 0, 255, 255, "IF:");
                ImGui.sameLine();
                ImGui.text((debugger.readMemory(MMU.IF) & Flags.IF_VBLANK_IRQ) != 0 ? "1" : "0");
                ImGui.sameLine(270);
                inlineRegister(MMU.SCY, "SCY ", debugger.readMemory(MMU.SCY));
                ImGui.textColored(0, 255, 255, 255, "    STAT   ");
                ImGui.sameLine();
                ImGui.textColored((enabled & Flags.IE_LCD_STAT_IRQ) == 0 ? 255 : 0, (enabled & Flags.IF_LCD_STAT_IRQ) == 0 ? 0 : 255, 0, 255, (enabled & Flags.IF_LCD_STAT_IRQ) == 0 ? "OFF" : "ON ");
                ImGui.sameLine();
                ImGui.textColored(255, 0, 255, 255, " IE:");
                ImGui.sameLine();
                ImGui.text((debugger.readMemory(MMU.IE) & Flags.IE_LCD_STAT_IRQ) != 0 ? "1" : "0");
                ImGui.sameLine();
                ImGui.textColored(255, 0, 255, 255, "IF:");
                ImGui.sameLine();
                ImGui.text((debugger.readMemory(MMU.IF) & Flags.IF_LCD_STAT_IRQ) != 0 ? "1" : "0");
                ImGui.sameLine(270);
                inlineRegister(MMU.SCX, "SCX ", debugger.readMemory(MMU.SCX));
                ImGui.textColored(0, 255, 255, 255, "    TIMER  ");
                ImGui.sameLine();
                ImGui.textColored((enabled & Flags.IE_TIMER_IRQ) == 0 ? 255 : 0, (enabled & Flags.IF_TIMER_IRQ) == 0 ? 0 : 255, 0, 255, (enabled & Flags.IF_TIMER_IRQ) == 0 ? "OFF" : "ON ");
                ImGui.sameLine();
                ImGui.textColored(255, 0, 255, 255, " IE:");
                ImGui.sameLine();
                ImGui.text((debugger.readMemory(MMU.IE) & Flags.IE_TIMER_IRQ) != 0 ? "1" : "0");
                ImGui.sameLine();
                ImGui.textColored(255, 0, 255, 255, "IF:");
                ImGui.sameLine();
                ImGui.text((debugger.readMemory(MMU.IF) & Flags.IF_TIMER_IRQ) != 0 ? "1" : "0");
                ImGui.sameLine(270);
                inlineRegister(MMU.LY, "LY  ", debugger.readMemory(MMU.LY));
                ImGui.textColored(0, 255, 255, 255, "    SERIAL ");
                ImGui.sameLine();
                ImGui.textColored((enabled & Flags.IE_SERIAL_IRQ) == 0 ? 255 : 0, (enabled & Flags.IF_SERIAL_IRQ) == 0 ? 0 : 255, 0, 255, (enabled & Flags.IF_SERIAL_IRQ) == 0 ? "OFF" : "ON ");
                ImGui.sameLine();
                ImGui.textColored(255, 0, 255, 255, " IE:");
                ImGui.sameLine();
                ImGui.text((debugger.readMemory(MMU.IE) & Flags.IE_SERIAL_IRQ) != 0 ? "1" : "0");
                ImGui.sameLine();
                ImGui.textColored(255, 0, 255, 255, "IF:");
                ImGui.sameLine();
                ImGui.text((debugger.readMemory(MMU.IF) & Flags.IF_SERIAL_IRQ) != 0 ? "1" : "0");
                ImGui.sameLine(270);
                inlineRegister(MMU.LYC, "LYC ", debugger.readMemory(MMU.LYC));
                ImGui.textColored(0, 255, 255, 255, "    JOYPAD ");
                ImGui.sameLine();
                ImGui.textColored((enabled & Flags.IE_JOYPAD_IRQ) == 0 ? 255 : 0, (enabled & Flags.IF_JOYPAD_IRQ) == 0 ? 0 : 255, 0, 255, (enabled & Flags.IF_JOYPAD_IRQ) == 0 ? "OFF" : "ON ");
                ImGui.sameLine();
                ImGui.textColored(255, 0, 255, 255, " IE:");
                ImGui.sameLine();
                ImGui.text((debugger.readMemory(MMU.IE) & Flags.IE_JOYPAD_IRQ) != 0 ? "1" : "0");
                ImGui.sameLine();
                ImGui.textColored(255, 0, 255, 255, "IF:");
                ImGui.sameLine();
                ImGui.text((debugger.readMemory(MMU.IF) & Flags.IF_JOYPAD_IRQ) != 0 ? "1" : "0");
                ImGui.sameLine(270);
                inlineRegister(MMU.DMA, "DMA ", debugger.readMemory(MMU.DMA));
                ImGui.textColored(255, 255, 0, 255, "  Timer:");
                ImGui.sameLine(270);
                inlineRegister(MMU.BGP, "BGP ", debugger.readMemory(MMU.BGP));
                inlineRegister(MMU.DIV, "DIV ", debugger.readMemory(MMU.DIV));
                ImGui.sameLine(270);
                inlineRegister(MMU.OBP0, "OBP0", debugger.readMemory(MMU.OBP0));
                inlineRegister(MMU.TIMA, "TIMA", debugger.readMemory(MMU.TIMA));
                ImGui.sameLine(270);
                inlineRegister(MMU.OBP1, "OBP1", debugger.readMemory(MMU.OBP1));
                inlineRegister(MMU.TMA, "TMA ", debugger.readMemory(MMU.TMA));
                ImGui.sameLine(270);
                inlineRegister(MMU.WY, "WY  ", debugger.readMemory(MMU.WY));
                inlineRegister(MMU.TAC, "TAC ", debugger.readMemory(MMU.TAC));
                ImGui.sameLine(270);
                inlineRegister(MMU.WX, "WX  ", debugger.readMemory(MMU.WX));
                ImGui.textColored(255, 255, 0, 255, "  GBC:");
                ImGui.sameLine(270);
                ImGui.textColored(255, 255, 0, 255, "  GBC LCD:");
                inlineRegister(MMU.CGB_KEY_1, "KEY1", debugger.readMemory(MMU.CGB_KEY_1));
                ImGui.sameLine(270);
                inlineRegister(MMU.CGB_BCPS_BCPI, "BCPS", debugger.readMemory(MMU.CGB_BCPS_BCPI));
                inlineRegister(MMU.CGB_WRAM_BANK, "SVBK", debugger.readMemory(MMU.CGB_WRAM_BANK));
                ImGui.sameLine(270);
                inlineRegister(MMU.CGB_BCPD_BGPD, "BCPD", debugger.readMemory(MMU.CGB_BCPD_BGPD));
                ImGui.textColored(255, 255, 0, 255, "  GBC HDMA:");
                ImGui.sameLine(270);
                inlineRegister(MMU.CGB_OCPS_OBPI, "OCPS", debugger.readMemory(MMU.CGB_OCPS_OBPI));
                ImGui.textColored(0, 255, 255, 255, String.format("    $%04X", MMU.CGB_HDMA1) + "-" + String.format("$%04X", MMU.CGB_HDMA2));
                ImGui.sameLine();
                ImGui.textColored(255, 0, 255, 255, "SOURCE");
                ImGui.sameLine();
                ImGui.text(String.format("$%04X", (debugger.readMemory(MMU.CGB_HDMA1) << 8) | (debugger.readMemory(MMU.CGB_HDMA2))));
                ImGui.sameLine(270);
                inlineRegister(MMU.CGB_OCPD_OBPD, "OCPD", debugger.readMemory(MMU.CGB_OCPD_OBPD));
                ImGui.textColored(0, 255, 255, 255, String.format("    $%04X", MMU.CGB_HDMA3) + "-" + String.format("$%04X", MMU.CGB_HDMA4));
                ImGui.sameLine();
                ImGui.textColored(255, 0, 255, 255, "DEST  ");
                ImGui.sameLine();
                ImGui.text(String.format("$%04X", (debugger.readMemory(MMU.CGB_HDMA3) << 8) | (debugger.readMemory(MMU.CGB_HDMA4))));
                ImGui.sameLine(270);
                inlineRegister(MMU.CGB_VRAM_BANK, "VBK ", debugger.readMemory(MMU.CGB_VRAM_BANK));
                inlineRegister(MMU.CGB_HDMA5, "LEN ", debugger.readMemory(MMU.CGB_HDMA5));
                ImGui.sameLine(270);
                ImGui.textColored(255, 255, 0, 255, "  SERIAL:");
                ImGui.textColored(255, 255, 0, 255, "  INPUT:");
                ImGui.sameLine(270);
                inlineRegister(MMU.SB, "SB  ", debugger.readMemory(MMU.SB));
                inlineRegister(MMU.P1, "JOYB", debugger.readMemory(MMU.P1));
                ImGui.sameLine(270);
                inlineRegister(MMU.SC, "SC  ", debugger.readMemory(MMU.SC));

                ImGui.endTabItem();
            }
            ImGui.endTabBar();
        }
        ImGui.end();
    }

    private void inlineRegister(int addr, String name, int value) {
        ImGui.textColored(0, 255, 255, 255, String.format("    $%04X", addr));
        ImGui.sameLine();
        ImGui.textColored(255, 0, 255, 255, name);
        ImGui.sameLine();
        ImGui.text(String.format("$%02X", value) + "(" + Utils.binaryString(value, 8) + ")");
    }
}