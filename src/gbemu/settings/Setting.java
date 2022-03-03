package gbemu.settings;

import gbemu.settings.wrapper.ISerializable;

import java.awt.*;
import java.util.Map;
import java.util.function.Consumer;

/**
 * This class represent a Settings that can be modified and applied to the emulator
 * @param <T> the Type of the Settings parameter
 */
public class Setting<T extends ISerializable> {

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
    public String serialize() {
        return value.serialize();
    }

    /**
     * Load a value from a serialized representation
     * @param val the serialized representation
     */
    public void deserialize(String val) {
        if (val != null)
            value.deserialize(val);
    }
}