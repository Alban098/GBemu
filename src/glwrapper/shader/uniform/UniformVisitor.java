package glwrapper.shader.uniform;

import rendering.postprocessing.parameter.Parameter;

public interface UniformVisitor {

    void visit(UniformBoolean uniform);
    void visit(UniformFloat uniform);
    void visit(UniformInteger uniform);
    void visit(UniformVec2 uniform);
    void visit(UniformVec3 uniform);
    void visit(UniformVec4 uniform);
    void visit(UniformMat2 uniform);
    void visit(UniformMat3 uniform);
    void visit(UniformMat4 uniform);
    Parameter getResult();
}
