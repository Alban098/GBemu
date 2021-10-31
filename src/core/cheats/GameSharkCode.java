package core.cheats;

public class GameSharkCode {

    private final int type;
    private final int address;
    private final int value;
    private final String rawCheat;
    private final String name;
    private boolean enabled;

    public GameSharkCode(String name, String cheat, int type, int address, int value) {
        this.rawCheat = cheat;
        this.name = name.length() >= 20 ? name.substring(0, 20) : name;
        this.type = type;
        this.address = address;
        this.value = value;
        this.enabled = false;
    }

    public String getName() {
        return name;
    }

    public String getRawCheat() {
        return rawCheat;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getType() {
        return type;
    }

    public int getAddress() {
        return address;
    }

    public int getValue() {
        return value;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof GameSharkCode)
            return ((GameSharkCode)obj).name.equals(name) && ((GameSharkCode)obj).rawCheat.equals(rawCheat);
        return false;
    }
}
