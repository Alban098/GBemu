package debug;


import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;

public class Tile {

    public int x;
    public int y;
    public int id;
    public int attrib;
    public int mapAddr;
    public int tileAddr;
    public int bank;
    public ByteBuffer renderTarget;

    public Tile() {
        x = 0;
        y = 0;
        id = 0;
        attrib = 0;
        mapAddr = 0;
        bank = 0;
        tileAddr = 0;
        renderTarget = BufferUtils.createByteBuffer(64 * 4);
    }

    public void fill(int x, int y, int mapAddr, int tileAddr, int id, int attrib, int bank) {
        this.x = x;
        this.y = y;
        this.mapAddr = mapAddr;
        this.tileAddr = tileAddr;
        this.id = id;
        this.attrib = attrib;
        this.bank = bank;
    }

    public void fill(Tile tile) {
        this.x = tile.x;
        this.y = tile.y;
        this.mapAddr = tile.mapAddr;
        this.tileAddr = tile.tileAddr;
        this.id = tile.id;
        this.attrib = tile.attrib;
        this.bank = tile.bank;
        this.renderTarget.clear();
        this.renderTarget.put(tile.renderTarget);
        this.renderTarget.flip();
    }
}
