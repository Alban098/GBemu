package core.ppu.helper;

import core.MMU;
import core.ppu.Flags;

public class BackgroundMaps implements IMMUListener {

    private final MMU memory;

    private final BackgroundMap map0;
    private final BackgroundMap map1;

    private boolean map0Updated = false;
    private boolean map1Updated = false;

    public BackgroundMaps(MMU memory) {
        this.memory = memory;
        map0 = new BackgroundMap();
        map1 = new BackgroundMap();
        memory.addListener(this);
    }

    public void onWriteToMMU(int addr, int data) {
        if (addr >= MMU.BG_MAP0_START && addr <= MMU.BG_MAP0_END)
            map0Updated = true;
        else if (addr >= MMU.BG_MAP1_START && addr <= MMU.BG_MAP1_END)
            map1Updated = true;
    }

    public BackgroundMap getBGMap() {
        return getMap(Flags.CONTROL_BG_MAP.getMask());
    }

    public BackgroundMap getWindowMap() {
        return getMap(Flags.CONTROL_WINDOW_MAP.getMask());
    }

    private BackgroundMap getMap(int flags) {
        int addr = memory.readIORegisterBit(MMU.IO_LCD_CONTROL, flags, true) ? MMU.BG_MAP1_START : MMU.BG_MAP0_START;
        if (addr == MMU.BG_MAP0_START) {
            if (map0Updated) {
                loadMap(map0, addr);
                map0Updated = false;
            }
            return map0;
        } else {
            if (map1Updated) {
                loadMap(map1, addr);
                map1Updated = false;
            }
            return map1;
        }
    }

    private void loadMap(BackgroundMap map, int addr) {
        for (int i = 0; i < 0x400; i++)
            map.data[i >> 5][i & 0x1F] = memory.readByte(addr + i, true);
    }

    public static class BackgroundMap {
        public final int[][] data = new int[32][32];
    }
}
