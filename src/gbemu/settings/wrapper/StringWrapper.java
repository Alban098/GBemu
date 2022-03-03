package gbemu.settings.wrapper;

public class StringWrapper implements ISerializable {

    private String value;

    public StringWrapper() {
        value = "";
    }

    public StringWrapper(String value) {
        this.value = value;
    }

    public void wrap(String value) {
        this.value = value;
    }

    public String unwrap() {
        return value;
    }

    @Override
    public String serialize() {
        return value;
    }

    @Override
    public void deserialize(String str) {
        value = str;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof StringWrapper)
            return value.equals(((StringWrapper)obj).value);
        return false;
    }
}