package core.ppu;

import core.MMU;
import core.cpu.LR35902;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;

public class PPU {

    public static final int SCREEN_WIDTH = 160;
    public static final int SCREEN_HEIGHT = 144;

    private final ByteBuffer screen_buffer;
    private final ByteBuffer screen_buffer_tmp;
    private final MMU memory;

    private long cycles = 0;

    public PPU(MMU memory) {
        screen_buffer = BufferUtils.createByteBuffer(SCREEN_HEIGHT * SCREEN_WIDTH * 4);
        screen_buffer_tmp = BufferUtils.createByteBuffer(SCREEN_HEIGHT * SCREEN_WIDTH * 4);
        this.memory = memory;
    }

    public ByteBuffer getScreenBuffer() {
        return screen_buffer;
    }

    public void clock() {
        if (!memory.readIORegisterBit(MMU.IO_LCD_CONTROL, Flags.CONTROL_LCD_ON.getMask(), true))
            return;
        cycles++;
        switch (memory.readLcdMode()) {
            case OAM -> processOam();
            case TRANSFER -> processTransfer();
            case H_BLANK -> processHBlank();
            case V_BLANK -> processVBlank();
        }
    }

    private void processVBlank() {
        if (cycles >= LR35902.CPU_CYCLES_PER_V_BLANK) {
            if (memory.readByte(MMU.IO_LCD_Y, true) == 144) {
                memory.writeIORegisterBit(MMU.IO_INTERRUPT_FLAG, core.cpu.Flags.IRQ_VBLANK.getMask(), true);
                screen_buffer_tmp.flip();
                screen_buffer.clear();
                screen_buffer.put(screen_buffer_tmp);
                screen_buffer.flip();
                //Ready to be displayed
            }

            if (memory.readByte(MMU.IO_LCD_Y, true) == 153) {
                memory.writeRaw(MMU.IO_LCD_Y, 0);
                memory.writeLcdMode(LCDMode.OAM);
            } else {
                memory.writeRaw(MMU.IO_LCD_Y, memory.readByte(MMU.IO_LCD_Y, true) + 1);
            }
            cycles -= LR35902.CPU_CYCLES_PER_V_BLANK;
        }
    }

    private void processHBlank() {
        if (cycles >= LR35902.CPU_CYCLES_PER_H_BLANK) {
            if (memory.readIORegisterBit(MMU.IO_LCD_STAT, Flags.STATUS_H_BLANK_IRQ_ON.getMask(), true))
                memory.writeIORegisterBit(MMU.IO_INTERRUPT_FLAG, core.cpu.Flags.IRQ_LCD_STAT.getMask(), true);

            fillBackgroundLayer();
            fillWindowLayer();
            fillSpriteLayer();

            if (memory.readByte(MMU.IO_LCD_Y, true) == 143)
                memory.writeLcdMode(LCDMode.V_BLANK);
            else
                memory.writeLcdMode(LCDMode.OAM);

            memory.writeRaw(MMU.IO_LCD_Y, memory.readByte(MMU.IO_LCD_Y, true) + 1);
            cycles -= LR35902.CPU_CYCLES_PER_H_BLANK;
        }
    }

    private void processTransfer() {
        if (cycles >= LR35902.CPU_CYCLES_PER_TRANSFER) {
            memory.writeLcdMode(LCDMode.H_BLANK);
            cycles -= LR35902.CPU_CYCLES_PER_TRANSFER;
        }
    }

    private void processOam() {
        if (cycles >= LR35902.CPU_CYCLES_PER_OAM) {
            if (memory.readIORegisterBit(MMU.IO_LCD_STAT, Flags.STATUS_OAM_IRQ_ON.getMask(), true))
                memory.writeIORegisterBit(MMU.IO_INTERRUPT_FLAG, core.cpu.Flags.IRQ_LCD_STAT.getMask(), true);

            int lcd_y = memory.readByte(MMU.IO_LCD_Y, true);
            int lcd_yc = memory.readByte(MMU.IO_LCD_YC, true);

            if (lcd_y == lcd_yc) {
                memory.writeIORegisterBit(MMU.IO_LCD_STAT, Flags.STATUS_COINCIDENCE.getMask(), true);
                if (memory.readIORegisterBit(MMU.IO_LCD_STAT, Flags.STATUS_LCD_Y_IRQ_ON.getMask(), true))
                    memory.writeIORegisterBit(MMU.IO_INTERRUPT_FLAG, core.cpu.Flags.IRQ_LCD_STAT.getMask(), true);
            } else {
                memory.writeIORegisterBit(MMU.IO_LCD_STAT, Flags.STATUS_COINCIDENCE.getMask(), false);
            }
            memory.writeLcdMode(LCDMode.TRANSFER);
            cycles -= LR35902.CPU_CYCLES_PER_OAM;
        }
    }

    private void fillSpriteLayer() {
        //TODO
    }

    private void fillWindowLayer() {
        //TODO
    }

    private void fillBackgroundLayer() {
        //TODO
    }

    public void reset() {
        //TODO
    }
}
