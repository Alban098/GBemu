package gui;

import core.apu.Sample;
import core.memory.MMU;
import debug.Debugger;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.extension.implot.ImPlot;
import imgui.extension.implot.flag.ImPlotAxisFlags;
import imgui.extension.implot.flag.ImPlotFlags;
import utils.Utils;

import java.util.Queue;


public class APULayer extends AbstractDebugLayer {

    public static final int DEBUG_SAMPLE_NUMBER = 368;
    private final Queue<Sample> sample_queue;
    private final Float[][] samples;
    private final Float[] xs;

    public APULayer(Debugger debugger) {
        super(debugger);
        this.sample_queue = debugger.getSampleQueue();
        this.samples = new Float[5][DEBUG_SAMPLE_NUMBER];
        xs = new Float[DEBUG_SAMPLE_NUMBER];
        for (int i = 0; i < DEBUG_SAMPLE_NUMBER; i++) {
            samples[0][i] = 0f;
            samples[1][i] = 0f;
            samples[2][i] = 0f;
            samples[3][i] = 0f;
            samples[4][i] = 0f;
            xs[i] = (float) i;
        }
    }

    public void render() {
        ImGui.begin("APU");
        int i = 0;
        for (Sample s : sample_queue) {
            if (i >= DEBUG_SAMPLE_NUMBER)
                break;
            samples[0][i] = s.square1() / 15f + 6f;
            samples[1][i] = s.square2() / 15f + 4.5f;
            samples[2][i] = s.wave() / 15f + 3f;
            samples[3][i] = s.noise() / 15f + 1.5f;
            samples[4][i] = s.getNormalizedValue();

            i++;
        }
        ImGui.setWindowSize(515, 360);
        if (ImGui.beginTabBar("tab")) {
            if (ImGui.beginTabItem("Registers")) {
                ImGui.textColored(255, 255, 0, 255, "  Channel 1 - Envelope & Sweep:");
                ImGui.sameLine(270);
                ImGui.textColored(255, 255, 0, 255, "  Channel 3 - Wave Reader:");
                inlineRegister(MMU.NR10, "NR10", debugger.readMemory(MMU.NR10));
                ImGui.sameLine(270);
                inlineRegister(MMU.NR30, "NR30", debugger.readMemory(MMU.NR30));
                inlineRegister(MMU.NR11, "NR11", debugger.readMemory(MMU.NR11));
                ImGui.sameLine(270);
                inlineRegister(MMU.NR31, "NR31", debugger.readMemory(MMU.NR31));
                inlineRegister(MMU.NR12, "NR12", debugger.readMemory(MMU.NR12));
                ImGui.sameLine(270);
                inlineRegister(MMU.NR32, "NR32", debugger.readMemory(MMU.NR32));
                inlineRegister(MMU.NR13, "NR13", debugger.readMemory(MMU.NR13));
                ImGui.sameLine(270);
                inlineRegister(MMU.NR33, "NR33", debugger.readMemory(MMU.NR33));
                inlineRegister(MMU.NR14, "NR14", debugger.readMemory(MMU.NR14));
                ImGui.sameLine(270);
                inlineRegister(MMU.NR34, "NR34", debugger.readMemory(MMU.NR34));
                ImGui.newLine();
                ImGui.separator();
                ImGui.textColored(255, 255, 0, 255, "  Channel 2 - Envelope:");
                ImGui.sameLine(270);
                ImGui.textColored(255, 255, 0, 255, "  Channel 4 -  Noise Generator:");
                inlineRegister(MMU.NR21, "NR21", debugger.readMemory(MMU.NR21));
                ImGui.sameLine(270);
                inlineRegister(MMU.NR41, "NR41", debugger.readMemory(MMU.NR41));
                inlineRegister(MMU.NR22, "NR22", debugger.readMemory(MMU.NR22));
                ImGui.sameLine(270);
                inlineRegister(MMU.NR42, "NR42", debugger.readMemory(MMU.NR42));
                inlineRegister(MMU.NR23, "NR23", debugger.readMemory(MMU.NR23));
                ImGui.sameLine(270);
                inlineRegister(MMU.NR43, "NR43", debugger.readMemory(MMU.NR43));
                inlineRegister(MMU.NR24, "NR24", debugger.readMemory(MMU.NR24));
                ImGui.sameLine(270);
                inlineRegister(MMU.NR44, "NR44", debugger.readMemory(MMU.NR44));
                ImGui.newLine();
                ImGui.separator();
                ImGui.textColored(255, 255, 0, 255, "  Control Registers");
                ImGui.sameLine(270);
                ImGui.textColored(255, 255, 0, 255, "  Wave Pattern ($FF30 - $FF3F):");
                inlineRegister(MMU.NR50, "NR50", debugger.readMemory(MMU.NR50));
                ImGui.sameLine(270);
                ImGui.text("   ");
                ImGui.sameLine();
                for (int offset = 0; offset < 4; offset++) {
                    int val = debugger.readMemory(MMU.WAVE_PATTERN_START + offset*2) | (debugger.readMemory(MMU.WAVE_PATTERN_START + offset*2 + 1) << 8);
                    ImGui.text(String.format("%04X", val));
                    ImGui.sameLine();
                }
                ImGui.newLine();
                inlineRegister(MMU.NR51, "NR51", debugger.readMemory(MMU.NR51));
                ImGui.sameLine(270);
                ImGui.text("   ");
                ImGui.sameLine();
                for (int offset = 0; offset < 4; offset++) {
                    int val = debugger.readMemory(MMU.WAVE_PATTERN_START + 0x08 + offset*2) | (debugger.readMemory(MMU.WAVE_PATTERN_START + 0x08 + offset*2 + 1) << 8);
                    ImGui.text(String.format("%04X", val));
                    ImGui.sameLine();
                }
                ImGui.newLine();
                inlineRegister(MMU.NR52, "NR52", debugger.readMemory(MMU.NR52));
                ImGui.endTabItem();
            }
            if (ImGui.beginTabItem("Spectrum Visualizer")) {
                ImPlot.setNextPlotLimits(0, DEBUG_SAMPLE_NUMBER, -0.2, 7.1, 1);
                int axisFlags = ImPlotAxisFlags.RangeFit | ImPlotAxisFlags.LockMax | ImPlotAxisFlags.LockMin | ImPlotAxisFlags.NoGridLines | ImPlotAxisFlags.NoDecorations;
                int flags = ImPlotFlags.NoMousePos | ImPlotFlags.NoLegend;
                if (ImPlot.beginPlot("Channels", "Time", "Intensity", new ImVec2(500, 300), flags, axisFlags, axisFlags)) {
                    ImPlot.plotLine("Square 1", xs, samples[0]);
                    ImPlot.plotLine("Square 2", xs, samples[1]);
                    ImPlot.plotLine("Wave", xs, samples[2]);
                    ImPlot.plotLine("Noise", xs, samples[3]);
                    ImPlot.plotLine("D.A.C", xs, samples[4]);
                    ImPlot.endPlot();
                }
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