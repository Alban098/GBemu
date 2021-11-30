package gbemu.settings;

import java.awt.*;
import java.util.Map;
import java.util.function.Consumer;

public class Setting<T> {

    private final SettingIdentifiers identifier;
    private T value;
    private final Consumer<Setting<T>> renderer;

    public Setting(SettingIdentifiers identifier, T defaultValue, Consumer<Setting<T>> renderer) {
        this.identifier = identifier;
        this.value = defaultValue;
        this.renderer = renderer;
    }

    public T getValue() {
        return value;
    }

    public Setting<T> setValue(T value) {
        this.value = value;
        return this;
    }

    public SettingIdentifiers getIdentifier() {
        return identifier;
    }

    public Class<?> getType() {
        return value.getClass();
    }

    public void process() {
        renderer.accept(this);
    }

    public String serializedValue() {
        if (value instanceof Color)
            return String.valueOf(((Color) value).getRGB());
        else if (value instanceof Map) {
            StringBuilder val = new StringBuilder();
            for (Map.Entry entry : (((Map<Button, Integer>) value).entrySet()))
                val.append(((Button)entry.getKey()).name()).append(":").append(entry.getValue()).append(";");
            val.deleteCharAt(val.length()-1);
            return val.toString();
        } else
            return value.toString();
    }

    public void setSerializedValue(String val) {
        if (val != null) {
            if (value instanceof Color)
                value = (T) new Color(Integer.parseInt(val));
            else if (value instanceof Map) {
                ((Map<Button, Integer>)value).clear();
                for (String split : val.split(";")) {
                    String[] tuple = split.split(":");
                    ((Map<Button, Integer>)value).put(Button.get(tuple[0]), Integer.parseInt(tuple[1]));
                }
            }
            else if (value instanceof Float)
                value = (T) Float.valueOf(val);
            else if (value instanceof Boolean)
                value = (T) Boolean.valueOf(val);
            else if (value instanceof Integer)
                value = (T) Integer.valueOf(val);
            else if (value instanceof String)
                value = (T) val;
        }
    }
}