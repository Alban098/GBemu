package gbemu.settings.wrapper;

import java.awt.*;

public class ColorWrapper implements ISerializable {

    private Color value;

    public ColorWrapper() {
        value = new Color(0);
    }

    public ColorWrapper(Color value) {
        this.value = value;
    }

    public ColorWrapper(int r, int g, int b, int a) {
        value = new Color(r, g, b, a);
    }

    public ColorWrapper(int r, int g, int b) {
        value = new Color(r, g, b);
    }

    public ColorWrapper(int rgb) {
        value = new Color(rgb);
    }

    public void wrap(Color value) {
        this.value = value;
    }

    public Color unwrap() {
        return value;
    }

    @Override
    public String serialize() {
        return String.valueOf(value.getRGB());
    }

    @Override
    public void deserialize(String str) {
        try {
            value = new Color(Integer.parseInt(str));
        } catch (NumberFormatException ex) {
            value = new Color(0);
        }
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ColorWrapper)
            return value.equals(((ColorWrapper)obj).value);
        return false;
    }
}