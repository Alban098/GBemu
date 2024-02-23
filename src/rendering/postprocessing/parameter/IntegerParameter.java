package rendering.postprocessing.parameter;

import glwrapper.shader.uniform.UniformInteger;

public class IntegerParameter extends Parameter {

    private Integer value;

    public IntegerParameter(String name, Integer value) {
        super(name);
        this.value = value;
    }

    public IntegerParameter(UniformInteger uniform) {
        super(uniform.getName());
        this.value = uniform.getDefault();
    }

    @Override
    public Integer getValue() {
        return value;
    }

    @Override
    public String getType() {
        return "INTEGER";
    }

    @Override
    public void accept(ParameterVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public Class<?> getUniformClass() {
        return UniformInteger.class;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }
}
