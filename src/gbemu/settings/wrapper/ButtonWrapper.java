package gbemu.settings.wrapper;

import gbemu.settings.Button;

public class ButtonWrapper implements ISerializable {

    private Button value;

    public ButtonWrapper() {
        value = null;
    }

    public ButtonWrapper(Button value) {
        this.value = value;
    }

    public void wrap(Button value) {
        this.value = value;
    }

    public Button unwrap() {
        return value;
    }

    @Override
    public String serialize() {
        return value.name();
    }

    @Override
    public void deserialize(String str) {
        value = Button.get(str);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ButtonWrapper)
            return value.equals(((ButtonWrapper)obj).value);
        return false;
    }
}