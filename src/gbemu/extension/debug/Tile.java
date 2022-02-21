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
    public int map_addr;
    public int tile_addr;
    public int bank;
    public ByteBuffer render_target;

    /**
     * Create an Empty tile
     */
    public Tile() {
        x = 0;
        y = 0;
        id = 0;
        attrib = 0;
        map_addr = 0;
        bank = 0;
        tile_addr = 0;
        render_target = BufferUtils.createByteBuffer(64 * 4);
    }

    /**
     * Fill a tile, leave the render target untouched
     * @param x tile x
     * @param y tile y
     * @param map_addr tile address on tile map
     * @param tile_addr tile adress on tile tables
     * @param id tile id
     * @param attrib tile attribute
     * @param bank tile VRAM bank
     */
    public void fill(int x, int y, int map_addr, int tile_addr, int id, int attrib, int bank) {
        this.x = x;
        this.y = y;
        this.map_addr = map_addr;
        this.tile_addr = tile_addr;
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
        this.map_addr = tile.map_addr;
        this.tile_addr = tile.tile_addr;
        this.id = tile.id;
        this.attrib = tile.attrib;
        this.bank = tile.bank;
        this.render_target.clear();
        this.render_target.put(tile.render_target);
        this.render_target.flip();
    }
}
