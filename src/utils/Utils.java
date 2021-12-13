package utils;

import imgui.ImGui;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * This class regroups useful methods used all over the project
 */
public class Utils {

    private static final Map<Integer, String> keys = new HashMap<>();

    static {
        keys.put(GLFW.GLFW_KEY_APOSTROPHE, "'");
        keys.put(GLFW.GLFW_KEY_COMMA, ",");
        keys.put(GLFW.GLFW_KEY_MINUS, "-");
        keys.put(GLFW.GLFW_KEY_PERIOD, "Period");
        keys.put(GLFW.GLFW_KEY_SLASH, "/");
        keys.put(GLFW.GLFW_KEY_0, "0");
        keys.put(GLFW.GLFW_KEY_1, "1");
        keys.put(GLFW.GLFW_KEY_2, "2");
        keys.put(GLFW.GLFW_KEY_3, "3");
        keys.put(GLFW.GLFW_KEY_4, "4");
        keys.put(GLFW.GLFW_KEY_5, "5");
        keys.put(GLFW.GLFW_KEY_6, "6");
        keys.put(GLFW.GLFW_KEY_7, "7");
        keys.put(GLFW.GLFW_KEY_8, "8");
        keys.put(GLFW.GLFW_KEY_9, "9");
        keys.put(GLFW.GLFW_KEY_SEMICOLON, ";");
        keys.put(GLFW.GLFW_KEY_EQUAL, "=");
        keys.put(GLFW.GLFW_KEY_A, "A");
        keys.put(GLFW.GLFW_KEY_B, "B");
        keys.put(GLFW.GLFW_KEY_C, "C");
        keys.put(GLFW.GLFW_KEY_D, "D");
        keys.put(GLFW.GLFW_KEY_E, "E");
        keys.put(GLFW.GLFW_KEY_F, "F");
        keys.put(GLFW.GLFW_KEY_G, "G");
        keys.put(GLFW.GLFW_KEY_H, "H");
        keys.put(GLFW.GLFW_KEY_I, "I");
        keys.put(GLFW.GLFW_KEY_J, "J");
        keys.put(GLFW.GLFW_KEY_K, "K");
        keys.put(GLFW.GLFW_KEY_L, "L");
        keys.put(GLFW.GLFW_KEY_M, "M");
        keys.put(GLFW.GLFW_KEY_N, "N");
        keys.put(GLFW.GLFW_KEY_O, "O");
        keys.put(GLFW.GLFW_KEY_P, "P");
        keys.put(GLFW.GLFW_KEY_Q, "Q");
        keys.put(GLFW.GLFW_KEY_R, "R");
        keys.put(GLFW.GLFW_KEY_S, "S");
        keys.put(GLFW.GLFW_KEY_T, "T");
        keys.put(GLFW.GLFW_KEY_U, "U");
        keys.put(GLFW.GLFW_KEY_V, "V");
        keys.put(GLFW.GLFW_KEY_W, "W");
        keys.put(GLFW.GLFW_KEY_X, "X");
        keys.put(GLFW.GLFW_KEY_Y, "Y");
        keys.put(GLFW.GLFW_KEY_Z, "Z");
        keys.put(GLFW.GLFW_KEY_LEFT_BRACKET, "[");
        keys.put(GLFW.GLFW_KEY_BACKSLASH, "\\");
        keys.put(GLFW.GLFW_KEY_RIGHT_BRACKET, "]");
        keys.put(GLFW.GLFW_KEY_GRAVE_ACCENT, "è");
        keys.put(GLFW.GLFW_KEY_WORLD_1, "World 1");
        keys.put(GLFW.GLFW_KEY_WORLD_2, "World 2");
        keys.put(GLFW.GLFW_KEY_LEFT, "Left");
        keys.put(GLFW.GLFW_KEY_RIGHT, "Right");
        keys.put(GLFW.GLFW_KEY_UP, "Up");
        keys.put(GLFW.GLFW_KEY_DOWN, "Down");
        keys.put(GLFW.GLFW_KEY_PAUSE, "Pause");
        keys.put(GLFW.GLFW_KEY_F1, "F1");
        keys.put(GLFW.GLFW_KEY_F2, "F2");
        keys.put(GLFW.GLFW_KEY_F3, "F3");
        keys.put(GLFW.GLFW_KEY_F4, "F4");
        keys.put(GLFW.GLFW_KEY_F5, "F5");
        keys.put(GLFW.GLFW_KEY_F6, "F6");
        keys.put(GLFW.GLFW_KEY_F7, "F7");
        keys.put(GLFW.GLFW_KEY_F8, "F8");
        keys.put(GLFW.GLFW_KEY_F9, "F9");
        keys.put(GLFW.GLFW_KEY_F10, "F10");
        keys.put(GLFW.GLFW_KEY_F11, "F11");
        keys.put(GLFW.GLFW_KEY_F12, "F12");
        keys.put(GLFW.GLFW_KEY_DELETE, "Delete");
        keys.put(GLFW.GLFW_KEY_ESCAPE, "Escape");
        keys.put(GLFW.GLFW_KEY_BACKSPACE, "Back Space");
        keys.put(GLFW.GLFW_KEY_SPACE, "Space");
        keys.put(GLFW.GLFW_KEY_ENTER, "Enter");
        keys.put(GLFW.GLFW_KEY_TAB, "Tab");
        keys.put(GLFW.GLFW_KEY_INSERT, "Insert");
        keys.put(GLFW.GLFW_KEY_PAGE_UP, "Pg Up");
        keys.put(GLFW.GLFW_KEY_PAGE_DOWN, "Pg Down");
        keys.put(GLFW.GLFW_KEY_HOME, "Home");
        keys.put(GLFW.GLFW_KEY_END, "End");
        keys.put(GLFW.GLFW_KEY_KP_0, "Num 0");
        keys.put(GLFW.GLFW_KEY_KP_1, "Num 1");
        keys.put(GLFW.GLFW_KEY_KP_2, "Num 2");
        keys.put(GLFW.GLFW_KEY_KP_3, "Num 3");
        keys.put(GLFW.GLFW_KEY_KP_4, "Num 4");
        keys.put(GLFW.GLFW_KEY_KP_5, "Num 5");
        keys.put(GLFW.GLFW_KEY_KP_6, "Num 6");
        keys.put(GLFW.GLFW_KEY_KP_7, "Num 7");
        keys.put(GLFW.GLFW_KEY_KP_8, "Num 8");
        keys.put(GLFW.GLFW_KEY_KP_9, "Num 9");
        keys.put(GLFW.GLFW_KEY_KP_DECIMAL, "Num 10");
        keys.put(GLFW.GLFW_KEY_KP_DIVIDE, "Num 11");
        keys.put(GLFW.GLFW_KEY_KP_MULTIPLY, "Num *");
        keys.put(GLFW.GLFW_KEY_KP_SUBTRACT, "Num -");
        keys.put(GLFW.GLFW_KEY_KP_ADD, "Num +");
        keys.put(GLFW.GLFW_KEY_KP_ENTER, "Num Enter");
        keys.put(GLFW.GLFW_KEY_KP_EQUAL, "Num =");
        keys.put(GLFW.GLFW_KEY_MENU, "Menu");
    }

    /**
     * Convert a RAW string to a multiline one
     * @param input the RAW input
     * @param lineLength number of character in each line
     * @return the prettified output
     */
    public static String getPrettifiedOutput(String input, int lineLength) {
        StringBuilder sb = new StringBuilder();
        for (String line : input.lines().toList()) {
            if (line.length() > lineLength) {
                for (int i = 1; i <= line.length(); i++) {
                    sb.append(line.charAt(i-1));
                    if (i % lineLength == 0)
                        sb.append("\n");
                }
                sb.append("\n");
            } else {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * Convert from an N bits unsigned Integer to its binary representation
     * @param value the RAW value to convert
     * @param nbBits how many bits to consider for conversion
     * @return the N bits String representation
     */
    public static String binaryString(int value, int nbBits) {
        StringBuilder s = new StringBuilder();
        int mask = 0x1 << (nbBits - 1);
        for (int i = 0; i < nbBits; i++) {
            if (i != 0 && i % 4 == 0)
                s.append(" ");
            s.append(((value & mask) != 0) ? "1" : "0");
            value <<= 1;
        }
        return s.toString();
    }

    public static int getPressedKey() {
        GLFW.glfwPollEvents();
        for (int keycode : keys.keySet()) {
            if (ImGui.isKeyDown(keycode) && isValidKey(keycode))
                return keycode;
        }
        return -1;
    }

    public static boolean isValidKey(int keycode) {
       return keys.containsKey(keycode);
    }

    public static String getKeyName(int keycode) {
        String name = GLFW.glfwGetKeyName(keycode, GLFW.GLFW_KEY_UNKNOWN);
        if (name == null)
            return keys.get(keycode);
        return name.toUpperCase(Locale.ROOT);
    }
}
