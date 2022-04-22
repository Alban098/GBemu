package gui.std;

import gbemu.settings.Palette;
import gbemu.settings.SettingIdentifiers;
import gbemu.settings.SettingsContainer;
import gui.std.parameter.ParameterView;
import imgui.ImGui;
import imgui.type.ImInt;
import rendering.Renderer;
import rendering.postprocessing.Filter;
import rendering.postprocessing.FilterInstance;
import rendering.postprocessing.Parameter;

/**
 * This class represent the Setting window
 * allowing user to change the emulator's behaviour
 */
public class PostProcessingLayer extends Layer {

    private final SettingsContainer settings_container;
    private final String[] filters;
    private final Renderer renderer;
    private ImInt combo_filters_index;

    /**
     * Create a new instance of the Layer
     * @param settings_container the container linked to the layer
     */
    public PostProcessingLayer(SettingsContainer settings_container, Renderer renderer) {
        super();
        this.renderer = renderer;
        combo_filters_index = new ImInt();
        filters = Filter.getNames();
        this.settings_container = settings_container;
    }

    /**
     * Render the layer to the screen
     * and propagate user inputs to the emulator
     */
    public void render() {
        ImGui.begin("Post-Processing");
        ImGui.setWindowSize(350, 310);
        if (ImGui.button("Save Effects")) {}
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
        for (FilterInstance filter : renderer.getFilters()) {
            if (ImGui.treeNode(filter.toString() + "##" + filter.hashCode())) {
                for (Parameter<?> parameter : filter.getParameters()) {
                    ParameterView view = ParameterView.findView(parameter.type);
                    view.render(parameter);
                }
                if (ImGui.button("Delete##" + filter.hashCode())) {
                    renderer.delete(filter);
                }
                ImGui.treePop();
            }
            ImGui.separator();
        }
        ImGui.end();
    }
}