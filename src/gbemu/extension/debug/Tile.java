package gbemu.extension.debug;

import org.lwjgl.BufferUtils;
import java.nio.ByteBuffer;

/**
 * This class represent a Tile used by the debugger and debugging UIs
 */
public class Tile {

    public int x;
    public int y;
    public int id;
    public int attrib;
    public int mapAddr;
    public int tileAddr;
    public int bank;
    public ByteBuffer renderTarget;

    /**
     * Create an Empty tile
     */
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

    /**
     * Fill a tile, leave the render target untouched
     * @param x tile x
     * @param y tile y
     * @param mapAddr tile address on tile map
     * @param tileAddr tile adress on tile tables
     * @param id tile id
     * @param attrib tile attribute
     * @param bank tile VRAM bank
     */
    public void fill(int x, int y, int mapAddr, int tileAddr, int id, int attrib, int bank) {
        this.x = x;
        this.y = y;
        this.mapAddr = mapAddr;
        this.tileAddr = tileAddr;
        this.id = id;
        this.attrib = attrib;
        this.bank = bank;
    }

    /**
     * Fill a tile with the content of another, including the render target that become ready to render
     * @param tile the tile to copy
     */
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
