package core.ppu;

import openGL.SwappingByteBuffer;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;

public class State {

    private final SwappingByteBuffer[] tileMaps;
    private final SwappingByteBuffer[] tileTables;
    private final SwappingByteBuffer oam_buffer;

    public State() {
        oam_buffer = new SwappingByteBuffer(PPU.SCREEN_HEIGHT * PPU.SCREEN_WIDTH * 4);
        tileMaps = new SwappingByteBuffer[]{
                new SwappingByteBuffer(256 * 256 * 4),
                new SwappingByteBuffer(256 * 256 * 4)
        };
        tileTables = new SwappingByteBuffer[]{
                new SwappingByteBuffer(128 * 64 * 4),
                new SwappingByteBuffer(128 * 64 * 4),
                new SwappingByteBuffer(128 * 64 * 4),
                new SwappingByteBuffer(128 * 64 * 4),
                new SwappingByteBuffer(128 * 64 * 4),
                new SwappingByteBuffer(128 * 64 * 4)
        };
    }

    public SwappingByteBuffer getOAMBuffer() {
        return oam_buffer;
    }

    public SwappingByteBuffer[] getTileMapBuffers() {
        return tileMaps;
    }

    public SwappingByteBuffer[] getTileTableBuffers() {
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
