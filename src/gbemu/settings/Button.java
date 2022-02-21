package gbemu.settings;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.*;

/**
 * This enum contains all Game Boy buttons
 */
public enum Button {
    A(0x02),
    B(0x01),
    START(0x08),
    SELECT(0x04),
    UP(0x40),
    DOWN(0x80),
    LEFT(0x20),
    RIGHT(0x10);

    private final int mask;
    private static final Map<Button, Integer> keyboard_map = new HashMap<>();

    /**
     * Create a new instance of Button
     * @param mask the mask of the button
     */
    Button(int mask) {
        this.mask = mask;
    }

    /**
     * Return a Button from its name, null if not found
     * @param s the name of the button
     * @return the button with specified name, null if not found
     */
    public static Button get(String s) {
        for (Button button : values())
            if (button.name().equals(s))
                return button;
        return null;
    }

    /**
     * Return the Button's mask
     * @return the mask of the Button
     */
    public int getMask() {
        return mask;
    }

    /**
     * Return a map of all the mapped inputs
     * @return the map of all inputs
     */
    public static Map<Button, Integer> getKeyboardMap() {
        if (keyboard_map.isEmpty()) {
            keyboard_map.put(START, GLFW_KEY_K);
            keyboard_map.put(SELECT, GLFW_KEY_L);
            keyboard_map.put(A, GLFW_KEY_I);
            keyboard_map.put(B, GLFW_KEY_O);
            keyboard_map.put(UP, GLFW_KEY_W);
            keyboard_map.put(DOWN, GLFW_KEY_S);
            keyboard_map.put(LEFT, GLFW_KEY_A);
            keyboard_map.put(RIGHT, GLFW_KEY_D);
        }
        return keyboard_map;
    }
}
