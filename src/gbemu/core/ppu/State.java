package gbemu.core.ppu;

import glwrapper.SwappingByteBuffer;

/**
 * This class represent a state of the PPU with all its framebuffers
 * can be used to take a snapshot of the PPU
 */
public class State {

    private final SwappingByteBuffer[] tile_maps_buffers;
    private final SwappingByteBuffer tile_tables_buffer;
    private final SwappingByteBuffer oam_buffer;

    /**
     * Create a new state and initialize the necessary Buffers
     */
    public State() {
        oam_buffer = new SwappingByteBuffer(PPU.SCREEN_HEIGHT * PPU.SCREEN_WIDTH * 4);
        tile_maps_buffers = new SwappingByteBuffer[]{
                new SwappingByteBuffer(256 * 256 * 4),
                new SwappingByteBuffer(256 * 256 * 4)
        };
        tile_tables_buffer = new SwappingByteBuffer(256 * 192 * 4);
    }

    /**
     * Return the OAM Buffer
     * @return the OAM Buffer
     */
    public SwappingByteBuffer getOAMBuffer() {
        return oam_buffer;
    }

    /**
     * Return the TileMap Buffers
     * @return the TileMap Buffers as an Array
     */
    public SwappingByteBuffer[] getTileMapBuffers() {
        return tile_maps_buffers;
    }

    /**
     * Return the TileTable Buffer
     * @return the TileTable Buffer
     */
    public SwappingByteBuffer getTileTableBuffer() {
        return tile_tables_buffer;
    }

    /**
     * Clear all the Buffers
     */
    public void clearBuffers() {
        oam_buffer.clear();
        tile_maps_buffers[0].clear();
        tile_maps_buffers[1].clear();
        tile_tables_buffer.clear();
    }
}
