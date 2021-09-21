package core.ppu.helper;

import core.MMU;

public class ColorPalettes implements IMMUListener {

    private final ColorPalette bgPalette;
    private final ColorPalette objPalette0;
    private final ColorPalette objPalette1;

    public ColorPalettes(MMU memory) {
        memory.addListener(this);
        bgPalette = new ColorPalette();
        objPalette0 = new ColorPalette();
        objPalette1 = new ColorPalette();
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

    public void onWriteToMMU(int addr, int data) {
        switch (addr) {
            case MMU.IO_BG_PAL -> updatePalette(bgPalette, data, false);
            case MMU.IO_OBJ_PAL0 -> updatePalette(objPalette0, data, true);
            case MMU.IO_OBJ_PAL1 -> updatePalette(objPalette1, data, true);
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
    }
}
