package gbemu.settings;

public class Palette {

    public static final Palette[] palettes = {
            new Palette("Default", 0xE0F8D0, 0x88c070, 0x346856, 0x081820),
            new Palette("RedRose", 0xCC3D50, 0x991F27, 0x591616, 0x260F0D),
            new Palette("Aqua", 0x668FCC, 0x244AB3, 0x141F66, 0x141433),
            new Palette("Internal Yellow", 0xD0E040, 0xA0A830, 0x607028, 0x384828),
            new Palette("Deadbeat", 0xE2E8BD, 0xAF986F, 0x9C8277, 0x6B6066),
            new Palette("Gumball", 0xFFFFFF, 0x2CE8F5, 0xFF0044, 0x193C3E),
            new Palette("Kirokaze", 0xE2f3E4, 0x94E344, 0x46878F, 0x332C50),
            new Palette("AYY4", 0xF1F2DA, 0xFFCE96, 0xFF7777, 0x00303B),
            new Palette("Mist", 0xC4C0C2, 0x5AB9A8, 0x1E606E, 0x2D1B00),
            new Palette("Wish", 0x8BE5FF, 0x5608FCF, 0x7550E8, 0x622E4C),
            new Palette("Demichrome", 0xE9EFEC, 0xA0A08B, 0x555568, 0x211E20),
            new Palette("Gold", 0xCFAB51, 0x9D654C, 0x4D222C, 0x210B1B),
            new Palette("Grayscale", 0xFFFFFF, 0xAAAAAA, 0x555555, 0x000000),
    };

    private final String name;
    private final int[] colors;

    public Palette(String name, int color0, int color1, int color2, int color3) {
        this.name = name;
        colors = new int[]{color0, color1, color2, color3};
    }

    public String getName() {
        return name;
    }

    public int[] getColors() {
        return colors;
    }
}