package rendering.postprocessing.parameter;

import glwrapper.shader.uniform.UniformBoolean;

public class BooleanParameter extends Parameter {

    private Boolean value;

    public BooleanParameter(String name, Boolean value) {
        super(name);
        this.value = value;
    }

    public BooleanParameter(UniformBoolean uniform) {
        super(uniform.getName());
        this.value = uniform.getDefault();
    }

    @Override
    public Boolean getValue() {
        return value;
    }

    @Override
    public String getType() {
        return "BOOLEAN";
    }

    @Override
    public void accept(ParameterVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public Class<?> getUniformClass() {
        return UniformBoolean.class;
    }

    public void setValue(Boolean value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return Boolean.toString(value);
    }
}
