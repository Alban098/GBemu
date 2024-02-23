package gui.std;

import imgui.ImGui;
import imgui.type.ImInt;
import rendering.Direction;
import rendering.Renderer;
import rendering.postprocessing.Filter;
import rendering.postprocessing.FilterInstance;
import rendering.postprocessing.parameter.Parameter;
import rendering.postprocessing.parameter.ParameterVisitor;

import java.util.List;

/**
 * This class represent the Setting window
 * allowing user to change the emulator's behaviour
 */
public class PostProcessingLayer extends Layer {

    private final String[] filters;
    private final Renderer renderer;
    private final ImInt combo_filters_index;

    /**
     * Create a new instance of the Layer
     */
    public PostProcessingLayer(Renderer renderer) {
        super();
        this.renderer = renderer;
        combo_filters_index = new ImInt();
        filters = Filter.getNames();
    }

    /**
     * Render the layer to the screen
     * and propagate user inputs to the emulator
     */
    public void render() {
        ImGui.begin("Post-Processing");
        ImGui.setWindowSize(360, 310);
        if (ImGui.button("Save Effects")) {
            renderer.savePipeline();
        }
        ImGui.sameLine(305);
        if (ImGui.button("Exit"))
            setVisible(false);
        ImGui.separator();
        ImGui.combo("Filters", combo_filters_index, filters);
        ImGui.sameLine();
        if (ImGui.button("Add")) {
            renderer.add(new FilterInstance(Filter.get(filters[combo_filters_index.get()])));
        }
        ImGui.separator();
        List<FilterInstance> applied_filters = renderer.getFilters();
        for (int index = 0; index < applied_filters.size(); index++) {
            FilterInstance filter = applied_filters.get(index);
            boolean expended = ImGui.treeNode(filter.toString() + "##" + filter.hashCode());
            if (ImGui.isItemHovered()) {
                showToolTip(filter);
            }
            if (expended) {
                ParameterVisitor visitor = new RenderParameterVisitor();
                for (Parameter parameter : filter.getParameters()) {
                    parameter.accept(visitor);
                }
                if (index > 0) {
                    if (ImGui.button("/\\##" + filter.hashCode())) {
                        renderer.moveFilter(filter, Direction.UP);
                    }
                    ImGui.sameLine();
                }
                if (index < applied_filters.size() - 1) {
                    if (ImGui.button("\\/##" + filter.hashCode())) {
                        renderer.moveFilter(filter, Direction.DOWN);
                    }
                }
                ImGui.sameLine(290);
                if (ImGui.button("Delete##" + filter.hashCode())) {
                    renderer.delete(filter);
                }
                ImGui.treePop();
            }
            ImGui.separator();
        }
        renderer.commitFilters();
        ImGui.end();
    }

    private void showToolTip(FilterInstance filter) {
        ImGui.beginTooltip();
        ImGui.textColored(255, 0, 255, 255, filter.getFilter().getName());
        ImGui.text(filter.getFilter().getDescription());
        ImGui.endTooltip();
    }
}
