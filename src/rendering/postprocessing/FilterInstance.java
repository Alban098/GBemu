package rendering.postprocessing;

import rendering.postprocessing.parameter.Parameter;

public class FilterInstance {

    private final Filter filter;
    private final Parameter[] parameters;

    public FilterInstance(Filter filter) {
        this.filter = filter;
        this.parameters = filter.getDefaultParameters();
    }

    public FilterInstance(Filter filter, Parameter[] parameters) {
        this.filter = filter;
        if (parameters != null)
            this.parameters = parameters;
        else
            this.parameters = filter.getDefaultParameters();
    }

    @Override
    public String toString() {
        return filter.toString();
    }

    public Filter getFilter() {
        return filter;
    }

    public Parameter[] getParameters() {
        return parameters;
    }


}

