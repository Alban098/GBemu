package core;

import core.cartridge.Cartridge;
import core.cpu.Flags;

public class Memory {

    public static final int IO_P1                = 0xFF00;
    public static final int IO_SERIAL_BUS        = 0xFF01;
    public static final int IO_SERIAL_CONTROL    = 0xFF02;
    public static final int IO_DIVIDER           = 0xFF04;
    public static final int IO_TIMA              = 0xFF05;
    public static final int IO_TMA               = 0xFF06;
    public static final int IO_TAC               = 0xFF07;
    public static final int IO_INTERRUPT_FLAG    = 0xFF0F;
    public static final int IO_SND_NR10          = 0xFF10;
    public static final int IO_SND_NR11          = 0xFF11;
    public static final int IO_SND_NR12          = 0xFF12;
    public static final int IO_SND_NR13          = 0xFF13;
    public static final int IO_SND_NR14          = 0xFF14;
    public static final int IO_SND_NR21          = 0xFF16;
    public static final int IO_SND_NR22          = 0xFF17;
    public static final int IO_SND_NR23          = 0xFF18;
    public static final int IO_SND_NR24          = 0xFF19;
    public static final int IO_SND_NR30          = 0xFF1A;
    public static final int IO_SND_NR31          = 0xFF1B;
    public static final int IO_SND_NR32          = 0xFF1C;
    public static final int IO_SND_NR33          = 0xFF1D;
    public static final int IO_SND_NR34          = 0xFF1E;
    public static final int IO_SND_NR41          = 0xFF20;
    public static final int IO_SND_NR42          = 0xFF21;
    public static final int IO_SND_NR43          = 0xFF22;
    public static final int IO_SND_NR44          = 0xFF23;
    public static final int IO_SND_NR50          = 0xFF24;
    public static final int IO_SND_NR51          = 0xFF25;
    public static final int IO_SND_NR52          = 0xFF26;
    public static final int IO_SND_WAVE_START    = 0xFF30;
    public static final int IO_LCDC              = 0xFF40;
    public static final int IO_LCD_STAT          = 0xFF41;
    public static final int IO_SCROLL_Y          = 0xFF42;
    public static final int IO_SCROLL_X          = 0xFF43;
    public static final int IO_LCD_Y             = 0xFF44;
    public static final int IO_LCD_YC            = 0xFF45;
    public static final int IO_DMA               = 0xFF46;
    public static final int IO_BGP               = 0xFF47;
    public static final int IO_OBP0              = 0xFF48;
    public static final int IO_OBP1              = 0xFF49;
    public static final int IO_WY                = 0xFF4A;
    public static final int IO_WX                = 0xFF4B;
    public static final int INTERRUPT_REG        = 0xFFFF;

    public static final int ROM_ADDR             = 0x0000;
    public static final int ROM_SBANK_ADDR       = 0x4000;
    public static final int VRAM_ADDR            = 0x8000;
    public static final int SRAM_ADDR            = 0xA000;
    public static final int GB_RAM               = 0xC000;
    public static final int UNUSED_1             = 0xE000;
    public static final int SPRITE_ATTRIBUTES    = 0xFE00;
    public static final int UNUSED_2             = 0xFEA0;
    public static final int IO_REGISTERS         = 0xFF00;
    public static final int UNUSED_3             = 0xFF4C;
    public static final int HIGH_RAM             = 0xFF80;

    private final int[] vram;
    private final int[] wram;
    private final int[] oam;
    private final int[] ioreg;
    private final int[] hram;
    private int ie_reg;

    private Cartridge cartridge;

    public Memory() {
        vram = new int[0x2000];
        wram = new int[0x2000];
        oam = new int[0xA0];
        ioreg = new int[0x80];
        hram = new int[0x7F];
        ie_reg = 0;
    }

    public void loadCart(String file) {
        cartridge = new Cartridge(file);
        init();
    }
    
    public void init() {}

    public void setInterrupt(Flags flag) {
        writeByte(IO_INTERRUPT_FLAG, readByte(IO_INTERRUPT_FLAG | flag.getMask()), true);
    }

    public int readByte(int addr) {
        addr &= 0xFFFF;
        if (addr == INTERRUPT_REG)
            return ie_reg;
        else if (addr >= HIGH_RAM)
            return hram[addr & 0x7F];
        else if (addr >= UNUSED_3)
            return 0x00;
        else if (addr >= IO_REGISTERS)
            return ioreg[addr & 0x7F];
        else if (addr >= UNUSED_2)
            return 0x00;
        else if (addr >= SPRITE_ATTRIBUTES)
            return oam[addr & 0xFF];
        else if (addr >= UNUSED_1)
            return wram[addr & 0x1FFF];
        else if (addr >= GB_RAM)
            return wram[addr & 0x1FFF];
        else if (addr >= SRAM_ADDR)
            return cartridge.readSRAM(addr & 0x1FFF);
        else if (addr >= VRAM_ADDR)
            return vram[addr & 0x1FFF];
        else if (addr >= ROM_ADDR)
            return cartridge.read(addr);
        else
            System.err.println(Integer.toHexString(addr)  + "not in range of Memory");
        return 0x00;
    }

    public void writeByte(int addr, int data) {
        writeByte(addr, data, false);
    }

    public void writeByte(int addr, int data, boolean force) {
        addr &= 0xFFFF;
        data &= 0xFF;
        int d;
        if (addr == IO_SERIAL_CONTROL && data == 0x81) {
            char c = (char) readByte(IO_SERIAL_BUS);
            System.out.print(c);
            writeByte(IO_SERIAL_CONTROL, 0);
        }

        switch (addr) {
            case IO_P1:
                if (force)
                    ioreg[addr & 0x7F] = data;
                else
                    ioreg[addr & 0x7F] = (ioreg[addr & 0x7F] & 0xCF) | (data & 0x30);
                break;

            case IO_LCD_STAT:
                if (force)
                    ioreg[addr & 0x7F] = data;
                else
                    ioreg[addr & 0x7F] = (ioreg[addr & 0x7F] & 0x07) | (data | 0xF8);
                break;

            case IO_DMA:
                d = 0x0;
                for (int s = data * 0x100; s < data * 0x100 + 0xA0; s++) {
                    oam[d] = readByte(s);
                    d++;
                }
                break;

            case IO_DIVIDER:
                if (force)
                    ioreg[addr & 0x7F] = data;
                else
                    ioreg[addr & 0x7F] = 0x00;
                break;

            default:
                if (addr >= INTERRUPT_REG)
                    ie_reg = data;
                else if (addr >= HIGH_RAM)
                    hram[addr & 0x7F] = data;
                else if (addr >= UNUSED_3)
                    return;
                else if (addr >= IO_REGISTERS)
                    ioreg[addr & 0x7F] = data;
                else if (addr >= UNUSED_2)
                    return;
                else if (addr >= SPRITE_ATTRIBUTES)
                    oam[addr & 0xFF] = data;
                else if (addr >= UNUSED_1)
                    wram[addr & 0x1FFF] = data;
                else if (addr >= GB_RAM)
                    wram[addr & 0x1FFF] = data;
                else if (addr >= SRAM_ADDR)
                    cartridge.writeSRAM(addr, data);
                else if (addr >= VRAM_ADDR)
                    vram[addr & 0x1FFF] = data;
                else if (addr >= ROM_ADDR)
                    cartridge.write(addr, data);
                else
                    System.err.println(Integer.toHexString(addr)  + "not in range of Memory");
                break;
        }
    }
}
