package core.ppu;

import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;
import java.util.Random;

public class PPU {

    public static final int SCREEN_WIDTH = 160;
    public static final int SCREEN_HEIGHT = 144;

    private final ByteBuffer screen_buffer;
    private final ByteBuffer screen_buffer_tmp;

    private int scanline = 0;
    private int pixel = 0;
    private long cycle = 0;

    public PPU() {
        screen_buffer = BufferUtils.createByteBuffer(SCREEN_HEIGHT * SCREEN_WIDTH * 4);
        screen_buffer_tmp = BufferUtils.createByteBuffer(SCREEN_HEIGHT * SCREEN_WIDTH * 4);
    }

    public ByteBuffer getScreenBuffer() {
        return screen_buffer;
    }

    public void clock() {
        //TODO
    }

}
