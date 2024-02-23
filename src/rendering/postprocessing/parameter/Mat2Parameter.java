package rendering.postprocessing.parameter;

import glwrapper.shader.uniform.UniformMat2;
import org.joml.Matrix2f;

public class Mat2Parameter extends Parameter {

    private final Matrix2f value;

    public Mat2Parameter(String name, Matrix2f value) {
        super(name);
        this.value = value;
    }

    public Mat2Parameter(UniformMat2 uniform) {
        super(uniform.getName());
        this.value = uniform.getDefault();
    }

    @Override
    public Matrix2f getValue() {
        return value;
    }

    @Override
    public String getType() {
        return "MAT2";
    }

    @Override
    public void accept(ParameterVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public Class<?> getUniformClass() {
        return UniformMat2.class;
    }

    @Override
    public String toString() {
        return value.m00 + ";" + value.m01 + ";"
                + value.m10 + ";" + value.m11;
    }
}
