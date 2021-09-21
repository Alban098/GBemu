package core.ppu.helper;

import core.MMU;
import core.ppu.Flags;

import static core.BitUtils.signedByte;

public class TileCollection implements IMMUListener {

    private final MMU memory;

    private final Tile[] tile0 = new Tile[0x100];
    private final Tile[] tile1 = new Tile[0x100];

    private boolean tile0Updated = false;
    private boolean tile1Updated = false;

    public TileCollection(MMU memory) {
        this.memory = memory;
        memory.addListener(this);
    }

    public Tile getBGTile(int tileId) {
        return memory.readIORegisterBit(MMU.IO_LCD_CONTROL, Flags.CONTROL_BG_DATA.getMask(), true) ? getTile(MMU.TILE_DATA1_START, tileId) : getTile(MMU.TILE_DATA0_START, signedByte(tileId) + 128);
    }

    public void onWriteToMMU(int addr, int data) {
        if (addr >= MMU.TILE_DATA0_START && addr <= MMU.TILE_DATA0_END)
            tile0Updated = true;
        else if (addr >= MMU.TILE_DATA1_START && addr <= MMU.TILE_DATA1_END)
            tile1Updated = true;
    }

    private Tile getTile(int addr, int tileNr) {
        if (addr == MMU.TILE_DATA0_START) {
            if (tile0Updated) {
                updateTile(tile0, addr);
                tile0Updated = false;
            }
            return tile0[tileNr];
        } else if (addr == MMU.TILE_DATA1_START) {
            if (tile1Updated) {
                updateTile(tile1, addr);
                tile1Updated = false;
            }
            return tile1[tileNr];
        }
        return null;
    }

    private void updateTile(Tile[] tiles, int addr) {
        for (int i = 0; i < 0xFF; i++) {
            if (tiles[i] == null)
                tiles[i] = new Tile();
            tiles[i].id = i;

            int tileAddr = (addr | (i << 4)) & 0xFFFF;
            for (int j = 0; j < 8; j++) {
                int lineData1 = memory.readByte(tileAddr | (j << 1), true);
                int lineData2 = memory.readByte(tileAddr | (j << 1) | 1, true);
                for (int k = 7; k >= 0; k--) {
                    int low = (lineData1 >> k) & 0x01;
                    int high = (lineData2 >> k) & 0x01;
                    tiles[i].data[j][Math.abs(k - 7)] = (high << 1) | low;
                }
            }
        }
    }

    public static class Tile {
        public int id;
        public final int[][] data = new int[8][8];
    }
}
