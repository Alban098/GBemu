package rendering.postprocessing.parameter;

import glwrapper.shader.uniform.UniformVec3;
import org.joml.Vector3f;

public class Vec3Parameter extends Parameter {

    private final Vector3f value;

    public Vec3Parameter(String name, Vector3f value) {
        super(name);
        this.value = value;
    }

    public Vec3Parameter(UniformVec3 uniform) {
        super(uniform.getName());
        this.value = uniform.getDefault();
    }

    @Override
    public Vector3f getValue() {
        return value;
    }

    @Override
    public String getType() {
        return "VEC3";
    }

    @Override
    public void accept(ParameterVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public Class<?> getUniformClass() {
        return UniformVec3.class;
    }

    @Override
    public String toString() {
        return value.x + ";" + value.y + ";" + value.z;
    }
}
