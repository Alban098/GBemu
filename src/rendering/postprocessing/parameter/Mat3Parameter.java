package rendering.postprocessing.parameter;

import glwrapper.shader.uniform.UniformMat3;
import org.joml.Matrix3f;

public class Mat3Parameter extends Parameter {

    private final Matrix3f value;

    public Mat3Parameter(String name, Matrix3f value) {
        super(name);
        this.value = value;
    }

    public Mat3Parameter(UniformMat3 uniform) {
        super(uniform.getName());
        this.value = uniform.getDefault();
    }

    @Override
    public Matrix3f getValue() {
        return value;
    }

    @Override
    public String getType() {
        return "MAT3";
    }

    @Override
    public void accept(ParameterVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public Class<?> getUniformClass() {
        return UniformMat3.class;
    }

    @Override
    public String toString() {
        return value.m00 + ";" + value.m01 + ";" + value.m02 + ";"
                + value.m10 + ";" + value.m11 + ";" + value.m12 + ";"
                + value.m20 + ";" + value.m21 + ";" + value.m22;
    }
}
