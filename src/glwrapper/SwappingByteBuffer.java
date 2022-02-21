package glwrapper;

import org.lwjgl.BufferUtils;

import java.awt.*;
import java.nio.ByteBuffer;

/**
 * This class represent a Dual Buffer, Buffer can be swapped
 * doing so allow one buffer to be filled while another one is accessed by the graphical API
 * thus eliminating flickering if trying to display a buffer that is currently being filled
 * it eliminates synchronisation problems in the Display loop
 */
public class SwappingByteBuffer {

    //The 2 Buffers used for flip flop
    private final ByteBuffer[] buffers;
    //Index of the buffer being filled
    private int current_buffer;

    /**
     * Create a new SwappingByteBuffer of specified capacity
     * @param capacity the buffer capacity in byte
     */
    public SwappingByteBuffer(int capacity) {
        buffers = new ByteBuffer[]{BufferUtils.createByteBuffer(capacity), BufferUtils.createByteBuffer(capacity)};
        current_buffer = 0;
    }

    /**
     * Return the buffer flagged as full
     * @return the full buffer
     */
    public synchronized ByteBuffer getBuffer() {
        return buffers[(current_buffer + 1) & 0x1];
    }

    /**
     * Swap the two buffer, setting the current buffer as full, thus flipping it
     * and setting the other buffer as the current one, ready to be filled
     */
    public synchronized void swap() {
        buffers[current_buffer].flip();
        current_buffer = (current_buffer + 1) & 0x1;
        buffers[current_buffer].clear();
    }

    /**
     * Put a color to the current buffer if there is space left inside
     * Color is pushed as 4 8bits values (one for each channel)
     * @param color the color to be pushed to the buffer
     */
    public void put(Color color) {
        if (buffers[current_buffer].position() < buffers[current_buffer].limit()) {
            buffers[current_buffer].put((byte) color.getRed());
            buffers[current_buffer].put((byte) color.getGreen());
            buffers[current_buffer].put((byte) color.getBlue());
            buffers[current_buffer].put((byte) color.getAlpha());
        }
    }

    /**
     * Clear the 2 buffers
     */
    public synchronized void clear() {
        buffers[0].clear();
        buffers[1].clear();
    }
}
