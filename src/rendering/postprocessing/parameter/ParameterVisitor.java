package rendering.postprocessing.parameter;

public interface ParameterVisitor {
    void visit(BooleanParameter parameter);
    void visit(IntegerParameter parameter);
    void visit(FloatParameter parameter);
    void visit(Vec2Parameter parameter);
    void visit(Vec3Parameter parameter);
    void visit(Vec4Parameter parameter);
    void visit(Mat2Parameter parameter);
    void visit(Mat3Parameter parameter);
    void visit(Mat4Parameter parameter);
}
