package rendering.postprocessing.parameter;

import glwrapper.shader.uniform.UniformMat4;
import org.joml.Matrix4f;

public class Mat4Parameter extends Parameter {

    private final Matrix4f value;

    public Mat4Parameter(String name, Matrix4f value) {
        super(name);
        this.value = value;
    }

    public Mat4Parameter(UniformMat4 uniform) {
        super(uniform.getName());
        this.value = uniform.getDefault();
    }

    @Override
    public Matrix4f getValue() {
        return value;
    }

    @Override
    public String getType() {
        return "MAT4";
    }

    @Override
    public void accept(ParameterVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public Class<?> getUniformClass() {
        return UniformMat4.class;
    }

    @Override
    public String toString() {
        return value.m00() + ";" + value.m01() + ";" + value.m02() + ";" + value.m03() + ";"
                + value.m10() + ";" + value.m11() + ";" + value.m12() + ";" + value.m13() + ";"
                + value.m20() + ";" + value.m21() + ";" + value.m22() + ";" + value.m23() + ";"
                + value.m30() + ";" + value.m31() + ";" + value.m32() + ";" + value.m33();
    }
}
