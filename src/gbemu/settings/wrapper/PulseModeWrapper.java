package gbemu.settings.wrapper;

import gbemu.core.apu.channels.PulseMode;

public class PulseModeWrapper implements ISerializable {

    private PulseMode value;

    public PulseModeWrapper() {
        value = PulseMode.RAW;
    }

    public PulseModeWrapper(PulseMode value) {
        this.value = value;
    }

    public void wrap(PulseMode value) {
        this.value = value;
    }

    public PulseMode unwrap() {
        return value;
    }

    @Override
    public String serialize() {
        return value.toString();
    }

    @Override
    public void deserialize(String str) {
        try {
            value = PulseMode.valueOf(str);
        } catch(IllegalArgumentException ex) {
            value = PulseMode.RAW;
        }
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PulseModeWrapper)
            return value.equals(((PulseModeWrapper)obj).value);
        return false;
    }
}