package gui.std.parameter;

import imgui.ImGui;
import org.joml.Matrix3f;
import rendering.postprocessing.Parameter;

public class ParameterMat3View extends ParameterView<Matrix3f> {

    private static final int SIZE = 3;
    private final float[][] buffer = new float[SIZE][SIZE];

    public ParameterMat3View() {
        super();
    }

    @Override
    public void render(Parameter<Matrix3f> parameter) {
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                buffer[row][col] = parameter.value.get(col, row);
            }
        }
        ImGui.textColored(255, 255, 0, 255, parameter.name);
        ImGui.sameLine();
        ImGui.beginGroup();
        for (int row = 0; row < SIZE; row++) {
            if (ImGui.inputFloat3("##row_" + row + "_" + parameter.name, buffer[row])) {
                for (int r = 0; r < SIZE; r++) {
                    for (int c = 0; c < SIZE; c++) {
                        parameter.value.set(c, r, buffer[r][c]);
                    }
                }
            }
        }
        ImGui.endGroup();
    }
}
