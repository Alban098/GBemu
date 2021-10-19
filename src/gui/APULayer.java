package gui;

import core.GameBoy;
import core.apu.Sample;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.extension.implot.ImPlot;
import imgui.extension.implot.flag.ImPlotAxisFlags;
import imgui.extension.implot.flag.ImPlotFlags;
import java.util.Queue;


public class APULayer extends AbstractDebugLayer {

    public static final int DEBUG_SAMPLE_NUMBER = 368;
    private final Queue<Sample> sample_queue;
    private final Float[][] samples;
    private final Float[] xs;

    public APULayer(GameBoy gameBoy) {
        super(gameBoy);
        this.sample_queue = gameBoy.getApu().getDebugSampleQueue();
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
            samples[0][i] = s.square1() / 15f + 6f;
            samples[1][i] = s.square2() / 15f + 4.5f;
            samples[2][i] = s.wave() / 15f + 3f;
            samples[3][i] = s.noise() / 15f + 1.5f;
            samples[4][i] = s.getNormalizedValue();
            i++;
        }
        ImGui.setWindowSize(515, 335);
        ImPlot.setNextPlotLimits(0, DEBUG_SAMPLE_NUMBER, -0.2,7.1,1);
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
        ImGui.end();
    }
}