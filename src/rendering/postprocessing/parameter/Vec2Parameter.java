package rendering.postprocessing.parameter;

import glwrapper.shader.uniform.UniformVec2;
import org.joml.Vector2f;

public class Vec2Parameter extends Parameter {

    private final Vector2f value;

    public Vec2Parameter(String name, Vector2f value) {
        super(name);
        this.value = value;
    }

    public Vec2Parameter(UniformVec2 uniform) {
        super(uniform.getName());
        this.value = uniform.getDefault();
    }

    @Override
    public Vector2f getValue() {
        return value;
    }

    @Override
    public String getType() {
        return "VEC2";
    }

    @Override
    public void accept(ParameterVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public Class<?> getUniformClass() {
        return UniformVec2.class;
    }

    @Override
    public String toString() {
        return value.x + ";" + value.y;
    }
}
