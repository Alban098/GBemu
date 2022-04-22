package gui.std.parameter;

import rendering.postprocessing.Parameter;
import rendering.postprocessing.ParameterType;

import java.util.HashMap;
import java.util.Map;

public abstract class ParameterView<T> {

    private static Map<ParameterType, ParameterView<?>> parameterView;

    private static void init() {
        parameterView = new HashMap<>();
        parameterView.put(ParameterType.FLOAT, new ParameterFloatView());
        parameterView.put(ParameterType.BOOLEAN, new ParameterBooleanView());
        parameterView.put(ParameterType.INTEGER, new ParameterIntegerView());
        parameterView.put(ParameterType.VEC2, new ParameterVec2View());
        parameterView.put(ParameterType.VEC3, new ParameterVec3View());
        parameterView.put(ParameterType.VEC4, new ParameterVec4View());
        parameterView.put(ParameterType.MAT2, new ParameterMat2View());
        parameterView.put(ParameterType.MAT3, new ParameterMat3View());
        parameterView.put(ParameterType.MAT4, new ParameterMat4View());
    }

    public static ParameterView<?> findView(ParameterType type) {
        if (parameterView == null)
            init();
        return parameterView.get(type);
    }

    public ParameterView() {}

    public abstract void render(Parameter<T> parameter);
}
