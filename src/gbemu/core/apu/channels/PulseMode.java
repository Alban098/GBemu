package gbemu.core.apu.channels;

public enum PulseMode {
    RAW("Raw"),
    SIN_FILTERED("Sinus filtering");

    private final String name;

    PulseMode(String name) {
        this.name = name;
    }

    public static final String[] names = {RAW.name, SIN_FILTERED.name};

    public static PulseMode get(int ordinal) {
        return values()[ordinal];
    }
}
