package gbemu.settings.wrapper;

public class IntegerWrapper implements ISerializable {

    private Integer value;

    public IntegerWrapper() {
        value = 0;
    }

    public IntegerWrapper(Integer value) {
        this.value = value;
    }

    public void wrap(int value) {
        this.value = value;
    }

    public int unwrap() {
        return value;
    }

    @Override
    public String serialize() {
        return value.toString();
    }

    @Override
    public void deserialize(String str) {
        try {
            value = Integer.parseInt(str);
        } catch(NumberFormatException ex) {
            value = 0;
        }
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof IntegerWrapper)
            return value.equals(((IntegerWrapper)obj).value);
        return false;
    }
}