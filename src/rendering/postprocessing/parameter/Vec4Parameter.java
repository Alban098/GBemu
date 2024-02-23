package rendering.postprocessing.parameter;

import glwrapper.shader.uniform.UniformVec4;
import org.joml.Vector4f;

public class Vec4Parameter extends Parameter {

    private final Vector4f value;

    public Vec4Parameter(String name, Vector4f value) {
        super(name);
        this.value = value;
    }

    public Vec4Parameter(UniformVec4 uniform) {
        super(uniform.getName());
        this.value = uniform.getDefault();
    }

    @Override
    public Vector4f getValue() {
        return value;
    }

    @Override
    public String getType() {
        return "VEC4";
    }

    @Override
    public void accept(ParameterVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public Class<?> getUniformClass() {
        return UniformVec4.class;
    }

    @Override
    public String toString() {
        return value.x + ";" + value.y + ";" + value.z + ";" + value.w;
    }
}
