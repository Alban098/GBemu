package gui.std.parameter;

import imgui.ImGui;
import imgui.type.ImBoolean;
import rendering.postprocessing.Parameter;

public class ParameterBooleanView extends ParameterView<Boolean> {

    private final ImBoolean buffer;

    public ParameterBooleanView() {
        super();
        buffer = new ImBoolean();
    }

    @Override
    public void render(Parameter<Boolean> parameter) {
        buffer.set(parameter.value);
        ImGui.textColored(255, 255, 0, 255, parameter.name);
        ImGui.sameLine();
        if (ImGui.checkbox("##" + parameter.name, buffer))
            parameter.value = buffer.get();
    }
}
