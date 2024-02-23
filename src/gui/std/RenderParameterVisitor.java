package gui.std;

import imgui.ImGui;
import imgui.type.ImBoolean;
import imgui.type.ImFloat;
import imgui.type.ImInt;
import rendering.postprocessing.parameter.*;

import java.util.HashMap;

public class RenderParameterVisitor implements ParameterVisitor {

    private static final HashMap<String, Object> BUFFERS;

    static {
        BUFFERS = new HashMap<>();
        BUFFERS.put("FLOAT", new ImFloat());
        BUFFERS.put("BOOLEAN", new ImBoolean());
        BUFFERS.put("INTEGER", new ImInt());
        BUFFERS.put("VEC2", new float[2]);
        BUFFERS.put("VEC3", new float[3]);
        BUFFERS.put("VEC4", new float[4]);
        BUFFERS.put("MAT2", new float[2][2]);
        BUFFERS.put("MAT3", new float[3][3]);
        BUFFERS.put("MAT4", new float[4][4]);
    }

    @Override
    public void visit(BooleanParameter parameter) {
        ImBoolean buffer = (ImBoolean) BUFFERS.get(parameter.getType());
        buffer.set(parameter.getValue());
        ImGui.textColored(255, 255, 0, 255, parameter.getName());
        ImGui.sameLine();
        if (ImGui.checkbox("##" + parameter.getName(), buffer))
            parameter.setValue(buffer.get());
    }

    @Override
    public void visit(IntegerParameter parameter) {
        ImInt buffer = (ImInt) BUFFERS.get(parameter.getType());
        buffer.set(parameter.getValue());
        ImGui.textColored(255, 255, 0, 255, parameter.getName());
        ImGui.sameLine();
        if (ImGui.inputInt("##" + parameter.getName(), buffer))
            parameter.setValue(buffer.get());
    }

    @Override
    public void visit(FloatParameter parameter) {
        ImFloat buffer = (ImFloat) BUFFERS.get(parameter.getType());
        buffer.set(parameter.getValue());
        ImGui.textColored(255, 255, 0, 255, parameter.getName());
        ImGui.sameLine();
        if (ImGui.inputFloat("##" + parameter.getName(), buffer))
            parameter.setValue(buffer.get());
    }

    @Override
    public void visit(Vec2Parameter parameter) {
        float[] buffer = (float[]) BUFFERS.get(parameter.getType());
        buffer[0] = parameter.getValue().x;
        buffer[1] = parameter.getValue().y;
        ImGui.textColored(255, 255, 0, 255, parameter.getName());
        ImGui.sameLine();
        if (ImGui.inputFloat2("##" + parameter.getName(), buffer))
            parameter.getValue().set(buffer);
    }

    @Override
    public void visit(Vec3Parameter parameter) {
        float[] buffer = (float[]) BUFFERS.get(parameter.getType());
        buffer[0] = parameter.getValue().x;
        buffer[1] = parameter.getValue().y;
        buffer[2] = parameter.getValue().z;
        ImGui.textColored(255, 255, 0, 255, parameter.getName());
        ImGui.sameLine();
        if (ImGui.inputFloat3("##" + parameter.getName(), buffer))
            parameter.getValue().set(buffer);
    }

    @Override
    public void visit(Vec4Parameter parameter) {
        float[] buffer = (float[]) BUFFERS.get(parameter.getType());
        buffer[0] = parameter.getValue().x;
        buffer[1] = parameter.getValue().y;
        buffer[2] = parameter.getValue().z;
        buffer[3] = parameter.getValue().w;
        ImGui.textColored(255, 255, 0, 255, parameter.getName());
        ImGui.sameLine();
        if (ImGui.inputFloat4("##" + parameter.getName(), buffer))
            parameter.getValue().set(buffer);
    }

    @Override
    public void visit(Mat2Parameter parameter) {
        float[][] buffer = (float[][]) BUFFERS.get(parameter.getType());
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 2; col++) {
                buffer[row][col] = parameter.getValue().get(col, row);
            }
        }
        ImGui.textColored(255, 255, 0, 255, parameter.getName());
        for (int row = 0; row < 2; row++) {
            if (ImGui.inputFloat2("##row_" + row + "_" + parameter.getName(), buffer[row])) {
                for (int r = 0; r < 2; r++) {
                    for (int c = 0; c < 2; c++) {
                        parameter.getValue().set(c, r, buffer[r][c]);
                    }
                }
            }
        }
    }

    @Override
    public void visit(Mat3Parameter parameter) {
        float[][] buffer = (float[][]) BUFFERS.get(parameter.getType());
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                buffer[row][col] = parameter.getValue().get(col, row);
            }
        }
        ImGui.textColored(255, 255, 0, 255, parameter.getName());
        for (int row = 0; row < 3; row++) {
            if (ImGui.inputFloat2("##row_" + row + "_" + parameter.getName(), buffer[row])) {
                for (int r = 0; r < 3; r++) {
                    for (int c = 0; c < 3; c++) {
                        parameter.getValue().set(c, r, buffer[r][c]);
                    }
                }
            }
        }
    }

    @Override
    public void visit(Mat4Parameter parameter) {
        float[][] buffer = (float[][]) BUFFERS.get(parameter.getType());
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                buffer[row][col] = parameter.getValue().get(col, row);
            }
        }
        ImGui.textColored(255, 255, 0, 255, parameter.getName());
        for (int row = 0; row < 4; row++) {
            if (ImGui.inputFloat2("##row_" + row + "_" + parameter.getName(), buffer[row])) {
                for (int r = 0; r < 4; r++) {
                    for (int c = 0; c < 4; c++) {
                        parameter.getValue().set(c, r, buffer[r][c]);
                    }
                }
            }
        }
    }
}
