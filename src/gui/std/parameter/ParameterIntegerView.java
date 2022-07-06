package gui.std.parameter;

import imgui.ImGui;
import imgui.type.ImFloat;
import imgui.type.ImInt;
import rendering.postprocessing.Parameter;

public class ParameterIntegerView extends ParameterView<Integer> {

    private final ImInt buffer;

    public ParameterIntegerView() {
        super();
        buffer = new ImInt();
    }

    @Override
    public void render(Parameter<Integer> parameter) {
        buffer.set(parameter.value);
        ImGui.textColored(255, 255, 0, 255, parameter.name);
        ImGui.sameLine();
        if (ImGui.inputInt("##" + parameter.name, buffer))
            parameter.value = buffer.get();
    }
}
