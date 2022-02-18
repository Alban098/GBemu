package gbemu.settings;

import java.awt.*;
import java.util.Map;
import java.util.function.Consumer;

/**
 * This class represent a Settings that can be modified and applied to the emulator
 * @param <T> the Type of the Settings parameter
 */
public class Setting<T> {

    private final SettingIdentifiers identifier;
    private T value;
    private final Consumer<Setting<T>> renderer;

    /**
     * Create a new Setting
     * @param identifier the Setting's Identifier
     * @param defaultValue the Setting's default value
     * @param renderer the Setting's rendering callback
     */
    public Setting(SettingIdentifiers identifier, T defaultValue, Consumer<Setting<T>> renderer) {
        this.identifier = identifier;
        this.value = defaultValue;
        this.renderer = renderer;
    }

    /**
     * Return the Setting's current value
     * @return the Setting's current value
     */
    public T getValue() {
        return value;
    }

    /**
     * Set the Setting's value
     * @param value the new value
     * @return the current Setting (useful for chaining calls)
     */
    public Setting<T> setValue(T value) {
        this.value = value;
        return this;
    }

    /**
     * Return the Setting's Identifier
     * @return the Setting's Identifier
     */
    public SettingIdentifiers getIdentifier() {
        return identifier;
    }

    /**
     * Return the Type of the Setting's parameter
     * @return the Setting's parameter Type
     */
    public Class<?> getType() {
        return value.getClass();
    }

    /**
     * Render the Setting to the Settings Layer
     */
    public void process() {
        renderer.accept(this);
    }

    /**
     * Serialize the Settings to be saved to a file
     * @return the serialized Setting
     */
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

    /**
     * Load a value from a serialized representation
     * @param val the serialized representation
     */
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