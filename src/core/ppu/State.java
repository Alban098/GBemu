package core.ppu;

import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;

public class State {

    private final ByteBuffer[] tileMaps;
    private final ByteBuffer[] tileTables;
    private final ByteBuffer oam_buffer;
    private final PPU ppu;

    public State(PPU ppu) {
        this.ppu = ppu;
        oam_buffer = BufferUtils.createByteBuffer(PPU.SCREEN_HEIGHT * PPU.SCREEN_WIDTH * 4);
        tileMaps = new ByteBuffer[]{
                BufferUtils.createByteBuffer(256 * 256 * 4),
                BufferUtils.createByteBuffer(256 * 256 * 4)
        };
        tileTables = new ByteBuffer[]{
                BufferUtils.createByteBuffer(128 * 64 * 4),
                BufferUtils.createByteBuffer(128 * 64 * 4),
                BufferUtils.createByteBuffer(128 * 64 * 4),
                BufferUtils.createByteBuffer(128 * 64 * 4),
                BufferUtils.createByteBuffer(128 * 64 * 4),
                BufferUtils.createByteBuffer(128 * 64 * 4)
        };
    }

    public ByteBuffer getOAMBuffer() {
        return oam_buffer;
    }

    public ByteBuffer[] getTileMapBuffers() {
        return tileMaps;
    }

    public ByteBuffer[] getTileTableBuffers() {
        return tileTables;
    }

    public void clearBuffers() {
        oam_buffer.clear();
        tileMaps[0].clear();
        tileMaps[1].clear();
        tileTables[0].clear();
        tileTables[1].clear();
        tileTables[2].clear();
        tileTables[3].clear();
        tileTables[4].clear();
        tileTables[5].clear();
    }
}
