package core;

import core.cartridge.Cartridge;
import core.ppu.Flags;
import core.ppu.LCDMode;

public class MMU {

    public static final int IO_P1                = 0xFF00;
    public static final int IO_SERIAL_BUS        = 0xFF01;
    public static final int IO_SERIAL_CONTROL    = 0xFF02;
    public static final int IO_INTERNAL_CLK_LOW  = 0xFF03;
    public static final int IO_DIVIDER           = 0xFF04;
    public static final int IO_LCD_STAT          = 0xFF41;
    public static final int IO_LCD_Y             = 0xFF44;
    public static final int IO_DMA               = 0xFF46;


    private Cartridge cartridge;
    private LCDMode ppuMode = LCDMode.H_BLANK;
    private final int[] memory;

    public MMU() {
        memory = new int[0x10000];
    }

    public void loadCart(String file) {
        cartridge = new Cartridge(file);
    }

    public int readByte(int addr) {
        return readByte(addr, false);
    }

    public int readByte(int addr, boolean fromPPU) {
        if(addr <= 0x3FFF)
            return cartridge.read(addr);
        else if(addr <= 0x7FFF)
            return cartridge.read(addr);
        else if(addr <= 0x9FFF && !fromPPU && ppuMode == LCDMode.TRANSFER)
            return 0xFF;
        else if(addr >= 0xFE00 && addr <= 0xFE9F && !fromPPU && (ppuMode == LCDMode.TRANSFER || ppuMode == LCDMode.OAM))
            return 0xFF;
        else
            return memory[addr];
    }

    public void writeByte(int addr, int data) {
        addr &= 0xFFFF;
        data &= 0xFF;

        if (addr == IO_SERIAL_CONTROL && data == 0x81) {
            char c = (char) readByte(IO_SERIAL_BUS);
            System.out.print(c);
            writeByte(IO_SERIAL_CONTROL, 0);
        }

        if(addr == IO_LCD_Y)
            memory[addr] = 0;
        else if(addr == IO_DIVIDER) {
            memory[IO_INTERNAL_CLK_LOW] = 0;
            memory[addr] = 0;
        }
        else if(addr == IO_DMA)
            executeDmaTransfer(data);
        else if(addr == IO_P1)
            memory[IO_P1] = (data & 0x30) | (memory[IO_P1] & 0xCF);
        else if(addr == IO_LCD_STAT)
            memory[IO_LCD_STAT] = (data & 0x7C) | (memory[IO_LCD_STAT] & 0x03);
        else if(addr >= 0x2000 && addr <= 0x3FFF)
            cartridge.switchRomBank(data);
        else
            memory[addr] = data;
    }

    private void executeDmaTransfer(int data) {
        memory[IO_DMA] = data;
        int start = data << 8;

        for (int i = 0x0; i <= 0x9F; i++)
            writeRaw(0xFE00 | i, readByte(start | i, true));
    }

    public void writeRaw(int addr, int data) {
        memory[addr] = data & 0xFF;
    }

    public boolean readIORegisterBit(int reg, int flag, boolean ppuAccess) {
        return (readByte(reg, ppuAccess) & flag) == flag;
    }

    public void writeIORegisterBit(int register, int flag, boolean value) {
        if(value)
            memory[register] |= flag;
        else
            memory[register] &= ~flag;
    }

    public LCDMode readLcdMode() {
        return ppuMode;
    }

    public void writeLcdMode(LCDMode lcdMode) {
        int lcdModeValue = lcdMode.getValue();
        writeIORegisterBit(IO_LCD_STAT, Flags.STATUS_MODE_HIGH.getMask(), ((lcdModeValue >> 1) & 0x1) == 0x1);
        writeIORegisterBit(IO_LCD_STAT, Flags.STATUS_MODE_LOW.getMask(), (lcdModeValue & 0x1) == 0x1);
        ppuMode = lcdMode;
    }

    public String toString(int highByte) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0x0; i <= 0xF; i++)
            sb.append(String.format("%02X ", readByte((highByte << 4) | i, true)));
        return sb.toString();
    }
}
