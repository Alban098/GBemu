package core.ppu.helper;

import java.awt.*;

public enum ColorShade {
    WHITE(new Color(0xE0,0xF8,0xD0,0xFF)),
    LIGHT_GRAY(new Color(0x88,0xC0,0x70,0xFF)),
    DARK_GRAY(new Color(0x34,0x58,0x66,0xFF)),
    BLACK(new Color(0x08,0x18,0x20,0xFF)),
    TRANSPARENT(new Color(0xFF,0xFF,0xFF, 0x00)),
    EMPTY(new Color(0xFF,0xFF,0xFF, 0xFF));

    private final Color color;

    ColorShade(Color color) {
        this.color = color;
    }

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
