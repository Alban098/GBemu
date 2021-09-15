package core;

import core.cartridge.Cartridge;
import core.cpu.Flags;

public class Memory {

    public static final int MEM_IO_REGISTER      = 0xFF00;
    public static final int MEM_SERIAL_SB        = 0xFF01;
    public static final int MEM_SERIAL_SC        = 0xFF02;
    public static final int MEM_DIVIDER          = 0xFF04;
    public static final int MEM_TIMA             = 0xFF05;
    public static final int MEM_TMA              = 0xFF06;
    public static final int MEM_TIMER_CONTROL    = 0xFF07;
    public static final int MEM_IRQ_FLAGS        = 0xFF0F;
    public static final int MEM_SOUND_NR10       = 0xFF10;
    public static final int MEM_SOUND_NR11       = 0xFF11;
    public static final int MEM_SOUND_NR12       = 0xFF12;
    public static final int MEM_SOUND_NR13       = 0xFF13;
    public static final int MEM_SOUND_NR14       = 0xFF14;
    public static final int MEM_SOUND_NR21       = 0xFF16;
    public static final int MEM_SOUND_NR22       = 0xFF17;
    public static final int MEM_SOUND_NR23       = 0xFF18;
    public static final int MEM_SOUND_NR24       = 0xFF19;
    public static final int MEM_SOUND_NR30       = 0xFF1A;
    public static final int MEM_SOUND_NR31       = 0xFF1B;
    public static final int MEM_SOUND_NR32       = 0xFF1C;
    public static final int MEM_SOUND_NR33       = 0xFF1D;
    public static final int MEM_SOUND_NR34       = 0xFF1E;
    public static final int MEM_SOUND_NR41       = 0xFF20;
    public static final int MEM_SOUND_NR42       = 0xFF21;
    public static final int MEM_SOUND_NR43       = 0xFF22;
    public static final int MEM_SOUND_NR44       = 0xFF23;
    public static final int MEM_SOUND_NR50       = 0xFF24;
    public static final int MEM_SOUND_NR51       = 0xFF25;
    public static final int MEM_SOUND_NR52       = 0xFF26;
    public static final int MEM_SOUND_WAVE_START = 0xFF30;
    public static final int MEM_LCDC             = 0xFF40;
    public static final int MEM_LCD_STAT         = 0xFF41;
    public static final int MEM_SCROLL_Y         = 0xFF42;
    public static final int MEM_SCROLL_X         = 0xFF43;
    public static final int MEM_LCD_Y            = 0xFF44;
    public static final int MEM_LCD_YC           = 0xFF45;
    public static final int MEM_DMA              = 0xFF46;
    public static final int MEM_BGP              = 0xFF47;
    public static final int MEM_OBP0             = 0xFF48;
    public static final int MEM_OBP1             = 0xFF49;
    public static final int MEM_WY               = 0xFF4A;
    public static final int MEM_WX               = 0xFF4B;
    public static final int MEM_IE               = 0xFFFF;

    public static final int MEM_ROM_START        = 0x0000;
    public static final int MEM_ROM_BANKED_START = 0x4000;
    public static final int MEM_VRAM_START       = 0x8000;
    public static final int MEM_VRAM_TILES       = 0x8000;
    public static final int MEM_VRAM_TILES_B1    = 0x8800;
    public static final int MEM_VRAM_TILES_B2    = 0x9000;
    public static final int MEM_VRAM_MAP1        = 0x9800;
    public static final int MEM_VRAM_MAP2        = 0x9C00;
    public static final int MEM_RAM_EXTERNAL     = 0xA000;
    public static final int MEM_RAM_INTERNAL     = 0xC000;
    public static final int MEM_RAM_MIRROR       = 0xE000;
    public static final int MEM_SPRITE_ATTR_TBL  = 0xFE00;
    public static final int MEM_HIGH_RAM         = 0xFF80;

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
    
    public void init() {
        writeByte(MEM_IO_REGISTER, 0x2F, true);
        writeByte(MEM_SOUND_NR10, 0x80, true);
        writeByte(MEM_SOUND_NR11, 0xBF, true);
        writeByte(MEM_SOUND_NR12, 0xF3, true);
        writeByte(MEM_SOUND_NR14, 0xBF, true);
        writeByte(MEM_SOUND_NR21, 0x3F, true);
        writeByte(MEM_SOUND_NR24, 0xBF, true);
        writeByte(MEM_SOUND_NR30, 0x7F, true);
        writeByte(MEM_SOUND_NR31, 0xFF, true);
        writeByte(MEM_SOUND_NR32, 0x9F, true);
        writeByte(MEM_SOUND_NR34, 0xBF, true);
        writeByte(MEM_SOUND_NR41, 0xFF, true);
        writeByte(MEM_SOUND_NR44, 0xBF, true);
        writeByte(MEM_SOUND_NR50, 0x77, true);
        writeByte(MEM_SOUND_NR51, 0xF3, true);
        writeByte(MEM_SOUND_NR52, 0xF1, true);
        writeByte(MEM_LCDC, 0x91, true);
        writeByte(MEM_BGP, 0xFC, true);
        writeByte(MEM_OBP0, 0xFF, true);
        writeByte(MEM_OBP1, 0xFF, true);
    }

    public void setInterrupt(Flags flag) {
        writeByteRdOnly(MEM_IRQ_FLAGS, readByte(MEM_IRQ_FLAGS | flag.getMask()));
    }

    public int readByte(int addr) {
        addr &= 0xFFFF;
        if (addr >= MEM_IE)
            return ie_reg;
        else if (addr >= MEM_HIGH_RAM)
            return hram[addr - MEM_HIGH_RAM];
        else if (addr >= MEM_IO_REGISTER)
            return ioreg[addr - MEM_IO_REGISTER];
        else if (addr >= 0xFEA0)
            return 0xFF;
        else if (addr >= MEM_SPRITE_ATTR_TBL)
            return oam[addr - MEM_SPRITE_ATTR_TBL];
        else if (addr >= MEM_RAM_MIRROR)
            return wram[addr - MEM_RAM_MIRROR];
        else if (addr >= MEM_RAM_INTERNAL)
            return wram[addr - MEM_RAM_INTERNAL];

        if (addr >= MEM_RAM_EXTERNAL)
            return cartridge.read(addr);
        else if (addr >= MEM_VRAM_TILES)
            return vram[addr - MEM_VRAM_TILES];
        else if (addr >= MEM_ROM_START)
            return cartridge.read(addr);
        else
            System.err.println(Integer.toHexString(addr)  + "not in range of Memory");
        return 0x00;
    }

    public void writeByteRdOnly(int addr, int data) {
        writeByte(addr, data, false);
    }

    public void writeByte(int addr, int data, boolean force) {
        int d;
        switch (addr) {
            case MEM_IO_REGISTER:
                if (force)
                    ioreg[addr - MEM_IO_REGISTER] = data;
                else
                    ioreg[addr - MEM_IO_REGISTER] = (ioreg[addr - MEM_IO_REGISTER] & 0xCF) | (data & 0x30);
                break;

            case MEM_LCD_STAT:
                if (force)
                    ioreg[addr - MEM_IO_REGISTER] = data;
                else
                    ioreg[addr - MEM_IO_REGISTER] = (ioreg[addr - MEM_IO_REGISTER] & 0x07) | (data | 0xF8);
                break;

            case MEM_DMA:
                d = 0x0;
                for (int s = data * 0x100; s < data * 0x100 + 0xA0; s++) {
                    oam[d] = readByte(s);
                    d++;
                }
                break;

            case MEM_DIVIDER:
                if (force)
                    ioreg[addr - MEM_IO_REGISTER] = data;
                else
                    ioreg[addr - MEM_IO_REGISTER] = 0x00;
                break;

            default:
                if (addr >= MEM_IE)
                    ie_reg = data;
                else if (addr >= MEM_HIGH_RAM)
                    hram[addr - MEM_HIGH_RAM] = data;
                else if (addr >= MEM_IO_REGISTER)
                    ioreg[addr - MEM_IO_REGISTER] = data;
                else if (addr >= 0xFEA0)
                    return;
                else if (addr >= MEM_SPRITE_ATTR_TBL)
                    oam[addr - MEM_SPRITE_ATTR_TBL] = data;
                else if (addr >= MEM_RAM_MIRROR)
                    wram[addr - MEM_RAM_MIRROR] = data;
                else if (addr >= MEM_RAM_INTERNAL)
                    wram[addr - MEM_RAM_INTERNAL] = data;
                if (addr >= MEM_RAM_EXTERNAL)
                    cartridge.write(addr, data);
                else if (addr >= MEM_VRAM_TILES)
                    vram[addr - MEM_VRAM_TILES] = data;
                else if (addr >= MEM_ROM_START)
                    cartridge.write(addr, data);
                else
                    System.err.println(Integer.toHexString(addr)  + "not in range of Memory");
                break;
        }
    }
}
