package rendering.postprocessing;

import glwrapper.shader.uniform.*;
import org.joml.*;
import rendering.postprocessing.parameter.Parameter;

public class LoadUniformVisitor implements UniformVisitor {


    private Object valueToLoad;

    @Override
    public void visit(UniformBoolean uniform) {
        uniform.loadBoolean((Boolean) valueToLoad);
    }

    @Override
    public void visit(UniformFloat uniform) {
        uniform.loadFloat((Float) valueToLoad);
    }

    @Override
    public void visit(UniformInteger uniform) {
        uniform.loadInteger((Integer) valueToLoad);
    }

    @Override
    public void visit(UniformVec2 uniform) {
        uniform.loadVec2((Vector2f) valueToLoad);
    }

    @Override
    public void visit(UniformVec3 uniform) {
        uniform.loadVec3((Vector3f) valueToLoad);
    }

    @Override
    public void visit(UniformVec4 uniform) {
        uniform.loadVec4((Vector4f) valueToLoad);
    }

    @Override
    public void visit(UniformMat2 uniform) {
        uniform.loadMatrix((Matrix2f) valueToLoad);
    }

    @Override
    public void visit(UniformMat3 uniform) {
        uniform.loadMatrix((Matrix3f) valueToLoad);
    }

    @Override
    public void visit(UniformMat4 uniform) {
        uniform.loadMatrix((Matrix4f) valueToLoad);
    }

    @Override
    public Parameter getResult() {
        return null;
    }

    public void setValue(Object value) {
        this.valueToLoad = value;
    }
}
