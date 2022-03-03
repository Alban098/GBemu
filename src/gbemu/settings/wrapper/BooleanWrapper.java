package gbemu.settings.wrapper;

public class BooleanWrapper implements ISerializable {

    private Boolean value;

    public BooleanWrapper() {
        value = false;
    }

    public BooleanWrapper(Boolean value) {
        this.value = value;
    }

    public void wrap(boolean value) {
        this.value = value;
    }

    public boolean unwrap() {
        return value;
    }

    @Override
    public String serialize() {
        return value.toString();
    }

    @Override
    public void deserialize(String str) {
        value = Boolean.parseBoolean(str);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BooleanWrapper)
            return value.equals(((BooleanWrapper)obj).value);
        return false;
    }
}
