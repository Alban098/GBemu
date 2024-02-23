package rendering.postprocessing.parameter;

import glwrapper.shader.uniform.UniformFloat;

public class FloatParameter extends Parameter {

    private Float value;

    public FloatParameter(String name, Float value) {
        super(name);
        this.value = value;
    }

    public FloatParameter(UniformFloat uniform) {
        super(uniform.getName());
        this.value = uniform.getDefault();
    }

    @Override
    public Float getValue() {
        return value;
    }

    @Override
    public String getType() {
        return "FLOAT";
    }

    @Override
    public void accept(ParameterVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public Class<?> getUniformClass() {
        return UniformFloat.class;
    }

    public void setValue(Float value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return Float.toString(value);
    }
}
