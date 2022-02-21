package gbemu.extension.cheats;

/**
 * This class represent a GameShark Code that can be applied to the current game
 */
public class GameSharkCode {

    private final int type;
    private final int address;
    private final int value;
    private final String raw_cheat;
    private final String name;
    private boolean enabled;

    /**
     * Create a new GameShark code
     * @param name the name of the code
     * @param cheat the string representation of the code
     * @param type the type of the code
     * @param address the address affected by the code
     * @param value the value to write at the address
     */
    public GameSharkCode(String name, String cheat, int type, int address, int value) {
        this.raw_cheat = cheat;
        this.name = name.length() >= 20 ? name.substring(0, 20) : name;
        this.type = type;
        this.address = address;
        this.value = value;
        this.enabled = false;
    }

    /**
     * Return the name of the code
     * @return the name of the code
     */
    public String getName() {
        return name;
    }

    /**
     * Return the raw representation of the code
     * @return the raw representation of the code
     */
    public String getRawCheat() {
        return raw_cheat;
    }

    /**
     * Return whether the code is enabled or not
     * @return is the code enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Return the type of the code (for now only 1 is supported)
     * @return the type of the code
     */
    public int getType() {
        return type;
    }

    /**
     * Return the targeted address of the code
     * @return the targeted address
     */
    public int getAddress() {
        return address;
    }

    /**
     * Return the targeted value of the code
     * @return the targeted value
     */
    public int getValue() {
        return value;
    }

    /**
     * Enabled or Disable the code
     * @param enabled should the code be enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Return whether the passed code is the same as this one
     * @param obj the code to compare to
     * @return are the two code identical
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof GameSharkCode)
            return ((GameSharkCode)obj).name.equals(name) && ((GameSharkCode)obj).raw_cheat.equals(raw_cheat);
        return false;
    }
}
