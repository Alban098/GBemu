package gui.std.parameter;

import imgui.ImGui;
import org.joml.Vector3f;
import rendering.postprocessing.Parameter;

public class ParameterVec3View extends ParameterView<Vector3f> {

    private final float[] buffer = new float[3];

    public ParameterVec3View() {
        super();
    }

    @Override
    public void render(Parameter<Vector3f> parameter) {
        buffer[0] = parameter.value.x;
        buffer[1] = parameter.value.y;
        buffer[2] = parameter.value.z;
        ImGui.textColored(255, 255, 0, 255, parameter.name);
        ImGui.sameLine();
        if (ImGui.inputFloat2("##" + parameter.name, buffer))
            parameter.value.set(buffer);
    }
}
