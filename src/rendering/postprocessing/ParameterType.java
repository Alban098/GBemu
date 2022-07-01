package rendering.postprocessing;

import glwrapper.shader.uniform.*;

import java.util.Locale;

public enum ParameterType {
    BOOLEAN(UniformBoolean.class),
    INTEGER(UniformInteger.class),
    FLOAT(UniformFloat.class),
    VEC2(UniformVec2.class),
    VEC3(UniformVec3.class),
    VEC4(UniformVec4.class),
    MAT2(UniformMat2.class),
    MAT3(UniformMat3.class),
    MAT4(UniformMat4.class);

    private final Class<?> uniformClass;

    ParameterType(Class<?> uniformClass) {
        this.uniformClass = uniformClass;
    }

    public Class<?> getUniformClass() {
        return uniformClass;
    }

    public static ParameterType get(String type) {
        return switch (type.toLowerCase(Locale.ROOT)) {
            case "boolean" -> BOOLEAN;
            case "integer" -> INTEGER;
            case "float" -> FLOAT;
            case "vec2" -> VEC2;
            case "vec3" -> VEC3;
            case "vec4" -> VEC4;
            case "mat2" -> MAT2;
            case "mat3" -> MAT3;
            case "mat4" -> MAT4;
            default -> null;
        };
    }
}
