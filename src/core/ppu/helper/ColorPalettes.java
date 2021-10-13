package core.ppu.helper;

import core.memory.MMU;

import java.awt.*;

//TODO Optimize CGB palette management
public class ColorPalettes implements IMMUListener {

    private final MMU memory;

    private final ColorPalette bgPalette;
    private final ColorPalette objPalette0;
    private final ColorPalette objPalette1;
    private final ColorPalette[] cgbPaletteBuffer;

    public ColorPalettes(MMU memory) {
        this.memory = memory;
        memory.addListener(this);
        bgPalette = new ColorPalette();
        objPalette0 = new ColorPalette();
        objPalette1 = new ColorPalette();
        cgbPaletteBuffer = new ColorPalette[16];
        for (int i = 0; i < 16; i++)
            cgbPaletteBuffer[i] = new ColorPalette();
        updatePalette(bgPalette, memory.readByte(MMU.BGP), false);
        updatePalette(objPalette0, memory.readByte(MMU.OBP0), false);
        updatePalette(objPalette1, memory.readByte(MMU.OBP1), false);
    }

    public ColorPalette getBgPalette() {
        return bgPalette;
    }

    public ColorPalette getObjPalette0() {
        return objPalette0;
    }

    public ColorPalette getObjPalette1() {
        return objPalette1;
    }

    public ColorPalette getCGBBgPalette(int paletteId) {
        return cgbPaletteBuffer[paletteId];
    }

    public ColorPalette getCGBObjPalette(int paletteId) {
        return cgbPaletteBuffer[paletteId + 8];
    }

    public void onWriteToMMU(int addr, int data) {
        switch (addr) {
            case MMU.BGP -> updatePalette(bgPalette, data, false);
            case MMU.OBP0 -> updatePalette(objPalette0, data, true);
            case MMU.OBP1 -> updatePalette(objPalette1, data, true);
            case MMU.CGB_BCPD_BGPD -> calculateCGBPalettes(false);
            case MMU.CGB_OCPD_OBPD -> calculateCGBPalettes(true);
        }
    }

    private void calculateCGBPalettes(boolean obj_pal) {
        int r, g, b;
        for (int pal = 0; pal < 8; pal++) {
            for (int i = 0; i < 4; i++) {
                int rgb555 = memory.readPalette(obj_pal, pal * 8 + i * 2) | (memory.readPalette(obj_pal, pal * 8 + i * 2 + 1) << 8);
                r = (rgb555 & 0b000000000011111);
                g = (rgb555 & 0b000001111100000) >> 5;
                b = (rgb555 & 0b111110000000000) >> 10;
                if (obj_pal)
                    cgbPaletteBuffer[8 + pal].colors[i] = new ColorShade(new Color((int) (r / 32.0 * 255) & 0xFF, (int) (g / 32.0 * 255) & 0xFF, (int) (b / 32.0 * 255) & 0xFF));
                else
                    cgbPaletteBuffer[pal].colors[i] = new ColorShade(new Color((int) (r / 32.0 * 255) & 0xFF, (int) (g / 32.0 * 255) & 0xFF, (int) (b / 32.0 * 255) & 0xFF));
            }
        }
    }

    public void updatePalette(ColorPalette palette, int data, boolean hasTransparent) {
        palette.colors[0] = hasTransparent ? ColorShade.TRANSPARENT : ColorShade.get(data & 0x3);
        palette.colors[1] = ColorShade.get((data & 0xC) >> 2);
        palette.colors[2] = ColorShade.get((data & 0x30) >> 4);
        palette.colors[3] = ColorShade.get((data & 0xC0) >> 6);
    }

    public static class ColorPalette {
        public final ColorShade[] colors = new ColorShade[4];

        public ColorPalette() {
            for (int i = 0; i < 4; i++)
                colors[i] = ColorShade.TRANSPARENT;
        }
    }
}
