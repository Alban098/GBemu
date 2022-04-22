package gui.std.parameter;

import imgui.ImGui;
import imgui.type.ImFloat;
import rendering.postprocessing.Parameter;

public class ParameterFloatView extends ParameterView<Float> {

    private final ImFloat buffer;

    public ParameterFloatView() {
        super();
        buffer = new ImFloat();
    }

    @Override
    public void render(Parameter<Float> parameter) {
        buffer.set(parameter.value);
        ImGui.textColored(255, 255, 0, 255, parameter.name);
        ImGui.sameLine();
        if (ImGui.inputFloat("##" + parameter.name, buffer))
            parameter.value = buffer.get();
    }
}
