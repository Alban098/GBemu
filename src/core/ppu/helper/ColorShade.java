package core.ppu.helper;

import java.awt.*;

public record ColorShade(Color color) {

    public static final ColorShade WHITE = new ColorShade(new Color(0xE0, 0xF8, 0xD0, 0xFF));
    public static final ColorShade LIGHT_GRAY = new ColorShade(new Color(0x88, 0xC0, 0x70, 0xFF));
    public static final ColorShade DARK_GRAY = new ColorShade(new Color(0x34, 0x58, 0x66, 0xFF));
    public static final ColorShade BLACK = new ColorShade(new Color(0x08, 0x18, 0x20, 0xFF));
    public static final ColorShade TRANSPARENT = new ColorShade(new Color(0xFF, 0xFF, 0xFF, 0x00));
    public static final ColorShade EMPTY = new ColorShade(new Color(0xFF, 0xFF, 0xFF, 0xFF));

    public Color getColor() {
        return color;
    }

    public static ColorShade get(int data) {
        return switch (data & 0x3) {
            case 0 -> WHITE;
            case 1 -> LIGHT_GRAY;
            case 2 -> DARK_GRAY;
            case 3 -> BLACK;
            default -> TRANSPARENT;
        };
    }
}
