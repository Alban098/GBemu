package gui.std.parameter;

import imgui.ImGui;
import org.joml.Vector4f;
import rendering.postprocessing.Parameter;

public class ParameterVec4View extends ParameterView<Vector4f> {

    private final float[] buffer = new float[4];

    public ParameterVec4View() {
        super();
    }

    @Override
    public void render(Parameter<Vector4f> parameter) {
        buffer[0] = parameter.value.x;
        buffer[1] = parameter.value.y;
        buffer[2] = parameter.value.z;
        buffer[3] = parameter.value.z;
        ImGui.textColored(255, 255, 0, 255, parameter.name);
        ImGui.sameLine();
        if (ImGui.inputFloat2("##" + parameter.name, buffer))
            parameter.value.set(buffer);
    }
}
