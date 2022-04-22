package gui.std.parameter;

import imgui.ImGui;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import rendering.postprocessing.Parameter;

public class ParameterMat4View extends ParameterView<Matrix4f> {

    private static final int SIZE = 4;
    private final float[][] buffer = new float[SIZE][SIZE];

    public ParameterMat4View() {
        super();
    }

    @Override
    public void render(Parameter<Matrix4f> parameter) {
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                buffer[row][col] = parameter.value.get(col, row);
            }
        }
        ImGui.textColored(255, 255, 0, 255, parameter.name);
        for (int row = 0; row < SIZE; row++) {
            if (ImGui.inputFloat3("##row_" + row + "_" + parameter.name, buffer[row])) {
                for (int r = 0; r < SIZE; r++) {
                    for (int c = 0; c < SIZE; c++) {
                        parameter.value.set(c, r, buffer[r][c]);
                    }
                }
            }
        }
    }
}
