package gbemu.settings;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.*;

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
    private static final Map<Button, Integer> keyboardMap = new HashMap<>();

    Button(int mask) {
        this.mask = mask;
    }

    public static Button get(String s) {
        for (Button button : values())
            if (button.name().equals(s))
                return button;
        return null;
    }

    public int getMask() {
        return mask;
    }

    public static Map<Button, Integer> getKeyboardMap() {
        if (keyboardMap.isEmpty()) {
            keyboardMap.put(START, GLFW_KEY_K);
            keyboardMap.put(SELECT, GLFW_KEY_L);
            keyboardMap.put(A, GLFW_KEY_I);
            keyboardMap.put(B, GLFW_KEY_O);
            keyboardMap.put(UP, GLFW_KEY_W);
            keyboardMap.put(DOWN, GLFW_KEY_S);
            keyboardMap.put(LEFT, GLFW_KEY_A);
            keyboardMap.put(RIGHT, GLFW_KEY_D);
        }
        return keyboardMap;
    }
}
