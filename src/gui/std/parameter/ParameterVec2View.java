package gui.std.parameter;

import imgui.ImGui;
import imgui.type.ImFloat;
import org.joml.Vector2f;
import rendering.postprocessing.Parameter;

public class ParameterVec2View extends ParameterView<Vector2f> {

    private final float[] buffer = new float[2];

    public ParameterVec2View() {
        super();
    }

    @Override
    public void render(Parameter<Vector2f> parameter) {
        buffer[0] = parameter.value.x;
        buffer[1] = parameter.value.y;
        ImGui.textColored(255, 255, 0, 255, parameter.name);
        ImGui.sameLine();
        if (ImGui.inputFloat2("##" + parameter.name, buffer))
            parameter.value.set(buffer);
    }
}
