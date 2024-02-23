package rendering.postprocessing.parameter;

import glwrapper.shader.uniform.*;

public class CreateParameterVisitor implements UniformVisitor {

    Parameter result;

    @Override
    public void visit(UniformBoolean uniform) {
        result = new BooleanParameter(uniform);
    }

    @Override
    public void visit(UniformFloat uniform) {
        result = new FloatParameter(uniform);
    }

    @Override
    public void visit(UniformInteger uniform) {
        result = new IntegerParameter(uniform);
    }

    @Override
    public void visit(UniformVec2 uniform) {
        result = new Vec2Parameter(uniform);
    }

    @Override
    public void visit(UniformVec3 uniform) {
        result = new Vec3Parameter(uniform);
    }

    @Override
    public void visit(UniformVec4 uniform) {
        result = new Vec4Parameter(uniform);
    }

    @Override
    public void visit(UniformMat2 uniform) {
        result = new Mat2Parameter(uniform);
    }

    @Override
    public void visit(UniformMat3 uniform) {
        result = new Mat3Parameter(uniform);
    }

    @Override
    public void visit(UniformMat4 uniform) {
        result = new Mat4Parameter(uniform);
    }

    public Parameter getResult() {
        return result;
    }
}
