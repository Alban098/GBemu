package rendering.postprocessing;

public class Parameter<T> {

    public final String name;
    public T value;
    public final ParameterType type;

    public Parameter(String name, T value, ParameterType type) {
        this.name = name;
        this.value = value;
        this.type = type;
    }
}
