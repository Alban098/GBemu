package gbemu.settings.wrapper;

public class FloatWrapper implements ISerializable {

    private Float value;

    public FloatWrapper() {
        value = 0f;
    }

    public FloatWrapper(Float value) {
        this.value = value;
    }

    public void wrap(float value) {
        this.value = value;
    }

    public float unwrap() {
        return value;
    }

    @Override
    public String serialize() {
        return value.toString();
    }

    @Override
    public void deserialize(String str) {
        try {
            value = Float.parseFloat(str);
        } catch(NumberFormatException ex) {
            value = 0f;
        }
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FloatWrapper)
            return value.equals(((FloatWrapper)obj).value);
        return false;
    }
}