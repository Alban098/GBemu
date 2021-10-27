package core.memory;

import core.Flags;
import core.GameBoy;
import core.cartridge.Cartridge;
import core.ppu.LCDMode;
import core.ppu.helper.IMMUListener;
import debug.Debugger;
import debug.DebuggerMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MMU {
    public static final int P1                  = 0xFF00;
    public static final int SB                  = 0xFF01;
    public static final int SC                  = 0xFF02;
    public static final int DIV                 = 0xFF04;
    public static final int TIMA                = 0xFF05;
    public static final int TMA                 = 0xFF06;
    public static final int TAC                 = 0xFF07;
    public static final int IF                  = 0xFF0F;
    public static final int NR10                = 0xFF10;
    public static final int NR11                = 0xFF11;
    public static final int NR12                = 0xFF12;
    public static final int NR13                = 0xFF13;
    public static final int NR14                = 0xFF14;
    public static final int NR21                = 0xFF16;
    public static final int NR22                = 0xFF17;
    public static final int NR23                = 0xFF18;
    public static final int NR24                = 0xFF19;
    public static final int NR30                = 0xFF1A;
    public static final int NR31                = 0xFF1B;
    public static final int NR32                = 0xFF1C;
    public static final int NR33                = 0xFF1D;
    public static final int NR34                = 0xFF1E;
    public static final int NR41                = 0xFF20;
    public static final int NR42                = 0xFF21;
    public static final int NR43                = 0xFF22;
    public static final int NR44                = 0xFF23;
    public static final int NR50                = 0xFF24;
    public static final int NR51                = 0xFF25;
    public static final int NR52                = 0xFF26;
    public static final int WAVE_PATTERN_START  = 0xFF30;
    public static final int LCDC                = 0xFF40;
    public static final int STAT                = 0xFF41;
    public static final int SCY                 = 0xFF42;
    public static final int SCX                 = 0xFF43;
    public static final int LY                  = 0xFF44;
    public static final int LYC                 = 0xFF45;
    public static final int DMA                 = 0xFF46;
    public static final int BGP                 = 0xFF47;
    public static final int OBP0                = 0xFF48;
    public static final int OBP1                = 0xFF49;
    public static final int WY                  = 0xFF4A;
    public static final int WX                  = 0xFF4B;
    public static final int CGB_KEY_1           = 0xFF4D;
    public static final int CGB_VRAM_BANK       = 0xFF4F;
    public static final int BOOTSTRAP_CONTROL   = 0xFF50;
    public static final int CGB_HDMA1           = 0xFF51;
    public static final int CGB_HDMA2           = 0xFF52;
    public static final int CGB_HDMA3           = 0xFF53;
    public static final int CGB_HDMA4           = 0xFF54;
    public static final int CGB_HDMA5           = 0xFF55;
    public static final int CGB_RP              = 0xFF56;
    public static final int CGB_BCPS_BCPI       = 0xFF68;
    public static final int CGB_BCPD_BGPD       = 0xFF69;
    public static final int CGB_OCPS_OBPI       = 0xFF6A;
    public static final int CGB_OCPD_OBPD       = 0xFF6B;
    public static final int CGB_WRAM_BANK       = 0xFF70;
    public static final int IE                  = 0xFFFF;

    public static final int IRQ_V_BLANK_VECTOR  = 0x40;
    public static final int IRQ_LCD_VECTOR      = 0x48;
    public static final int IRQ_TIMER_VECTOR    = 0x50;
    public static final int IRQ_SERIAL_VECTOR   = 0x58;
    public static final int IRQ_INPUT_VECTOR    = 0x60;


    public static final int BG_MAP0_START       = 0x9800;
    public static final int BG_MAP1_START       = 0x9C00;
    public static final int OAM_START           = 0xFE00;
    public static final int OAM_END             = 0xFE9F;

    public static final int TILE_BLOCK_START    = 0x8000;

    private Cartridge cartridge;
    private final GameBoy gameboy;
    private final Bootstrap bootstrap;
    private final int[] vram;
    private final int[] wram;
    private final int[] oam;
    private final int[] io_registers;
    private final int[] hram;
    private final int[] cgb_bg_palettes;
    private final int[] cgb_obj_palettes;

    private int ie;

    private int dma_remaining_cycles = 0;
    private int hdma_remaining_cycles = 0;
    private int hdma_block_left_total = 0;
    private int hdma_block_left_hblank = 0;
    private int hdma_current_source = 0;
    private int hdma_current_dest = 0;
    private StringBuilder serialOutput;
    private LCDMode ppuMode = LCDMode.H_BLANK;
    private final List<IMMUListener> listeners;
    private Debugger debugger;

    public MMU(GameBoy gb) {
        serialOutput = new StringBuilder();
        bootstrap = new Bootstrap(gb);
        vram = new int[0x4000];
        wram = new int[0x10000];
        oam = new int[0xA0];
        io_registers = new int[0x80];
        hram = new int[0xFF];
        cgb_bg_palettes = new int[64];
        cgb_obj_palettes = new int[64];
        ie = 0;
        listeners = new ArrayList<>();
        this.gameboy = gb;
        gameboy.getDebugger().link(this);
    }

    public boolean clock() {
        cartridge.clock();
        if (dma_remaining_cycles > 0) {
            dma_remaining_cycles--;
            return false;
        }
        if (gameboy.mode == GameBoy.Mode.CGB && hdma_block_left_total > 0) {
            if (readLcdMode() == LCDMode.H_BLANK) {
                if (hdma_block_left_hblank != 0) {
                    for (int i = 0; i < 0x10; i++)
                        writeRaw(hdma_current_dest++, readByte(hdma_current_source++, true));
                    hdma_block_left_total -= hdma_block_left_hblank;
                    hdma_block_left_hblank = 0x00;
                    io_registers[CGB_HDMA5 & 0x7F] = 0x80 | (hdma_block_left_total / 0x10) - 1;
                }
            } else {
                hdma_block_left_hblank = Math.min(hdma_block_left_total, 0x10);
            }
            return false;
        }
        if (gameboy.mode == GameBoy.Mode.CGB && hdma_remaining_cycles > 0) {
            hdma_remaining_cycles--;
            if (hdma_remaining_cycles == 0)
                io_registers[CGB_HDMA1 & 0x7F] = 0xFF;
                io_registers[CGB_HDMA2 & 0x7F] = 0xFF;
                io_registers[CGB_HDMA3 & 0x7F] = 0xFF;
                io_registers[CGB_HDMA4 & 0x7F] = 0xFF;
                io_registers[CGB_HDMA5 & 0x7F] = 0xFF;
            return false;
        }
        return true;
    }

    public void addListener(IMMUListener listener) {
        listeners.add(listener);
    }

    public void loadCart(String file) throws Exception {
        cartridge = new Cartridge(file, gameboy);
    }

    public int readByte(int addr) {
        return readByte(addr, false);
    }

    public int readByte(int addr, boolean fromPPU) {
        addr &= 0xFFFF;
        if (gameboy.mode == GameBoy.Mode.CGB && addr == CGB_VRAM_BANK)
            return io_registers[addr & 0x7F] | 0xFE;
        if (addr <= 0x7FFF) {
            if ((io_registers[BOOTSTRAP_CONTROL & 0x7F] & 0x1) != 1) {
                int data = bootstrap.readByte(addr);
                if (data != -1)
                    return data;
            }
            if (cartridge != null)
                return cartridge.read(addr);
            return 0x00;
        } else if (addr <= 0x9FFF) {
            if (gameboy.mode == GameBoy.Mode.CGB)
                return vram[(addr & 0x1FFF) + (0x2000 * (readByte(CGB_VRAM_BANK) & 0x01))];
            return vram[addr & 0x1FFF];
        } else if (addr <= 0xBFFF) {
            if (cartridge != null)
                return cartridge.read(addr);
            return 0x00;
        } else if (addr <= 0xDFFF) {
            if (gameboy.mode == GameBoy.Mode.CGB)
                return wram[(addr & 0x1FFF) | (0x2000 * (readByte(CGB_WRAM_BANK) & 0x07))];
            return wram[addr & 0x1FFF];
        } else if (addr <= 0xFE9F) {
            if (!fromPPU && (ppuMode == LCDMode.TRANSFER || ppuMode == LCDMode.OAM || dma_remaining_cycles > 0) && (readIORegisterBit(MMU.LCDC, Flags.LCDC_LCD_ON) || dma_remaining_cycles > 0))
                return 0xFF;
            return oam[(addr & 0xFF) % 0xA0];
        } else if (addr <= 0xFEFF) {
            if (ppuMode == LCDMode.OAM)
                return 0xFF;
            if (gameboy.mode == GameBoy.Mode.CGB)
                return (addr & 0xF0) | ((addr & 0xF0) >> 4);
            return 0xFF;
        } else if (addr <= 0xFF7F) {
            return io_registers[addr & 0x7F];
        } else if (addr <= 0xFFFE) {
            return hram[addr & 0x7F];
        } else {
            return ie;
        }
    }

    public void writeByte(int addr, int data) {
        addr &= 0xFFFF;
        data &= 0xFF;
        if (addr <= 0x7FFF) {
            if (cartridge != null)
                cartridge.write(addr, data);
        } else if (addr <= 0x9FFF) {
            if (gameboy.mode == GameBoy.Mode.CGB && (readByte(CGB_VRAM_BANK) & 0x01) == 0x1)
                vram[(addr & 0x1FFF) + (0x2000 * (readByte(CGB_VRAM_BANK) & 0x01))] = data;
            else
                vram[addr & 0x1FFF] = data;
        } else if (addr <= 0xBFFF) {
            if (cartridge != null)
                cartridge.write(addr, data);
        } else if (addr <= 0xDFFF) {
            if (gameboy.mode == GameBoy.Mode.CGB)
                wram[(addr & 0x1FFF) | (0x2000 * (readByte(CGB_WRAM_BANK) & 0x07))] = data;
            else
                wram[addr & 0x1FFF] = data;
        } else if (addr <= 0xFE9F) {
            oam[(addr & 0xFF) % 0xA0] = data;
        } else if (addr <= 0xFEFF) {
            return;
        } else if (addr <= 0xFF7F) {
            if (gameboy.mode == GameBoy.Mode.CGB && addr == CGB_HDMA5) {
                executeHdmaTransfer(data);
            }
            if (addr == SC && data == 0x81) {
                char c = (char) readByte(SB);
                serialOutput.append(c);
                writeByte(SC, 0);
            }
            if(addr == LY)
                io_registers[LY & 0x7F] = 0;
            else if(addr == DIV)
                io_registers[DIV & 0x7F] = 0;
            else if(addr == DMA)
                executeDmaTransfer(data);
            else if(addr == P1)
                io_registers[P1 & 0x7F] = (data & 0x30) | (io_registers[P1 & 0x7F] & 0xCF);
            else if(addr == STAT)
                io_registers[STAT & 0x7F] = (data & 0x78) | (io_registers[STAT & 0x7F] & 0x07);
            else if(addr == CGB_BCPD_BGPD) {
                cgb_bg_palettes[readByte(CGB_BCPS_BCPI) & Flags.CGB_BCPS_ADDR] = data & 0xFF;
                if ((readIORegisterBit(CGB_BCPS_BCPI, Flags.CGB_BCPS_AUTO_INC)))
                    writeRaw(CGB_BCPS_BCPI, Flags.CGB_BCPS_AUTO_INC | (((readByte(CGB_BCPS_BCPI) & Flags.CGB_BCPS_ADDR) + 1) & Flags.CGB_BCPS_ADDR));
            } else if(addr == CGB_OCPD_OBPD) {
                cgb_obj_palettes[readByte(CGB_OCPS_OBPI) & Flags.CGB_BCPS_ADDR] = data & 0xFF;
                if ((readIORegisterBit(CGB_OCPS_OBPI, Flags.CGB_BCPS_AUTO_INC)))
                    writeRaw(CGB_OCPS_OBPI, Flags.CGB_BCPS_AUTO_INC | (((readByte(CGB_OCPS_OBPI) & Flags.CGB_BCPS_ADDR) + 1) & Flags.CGB_BCPS_ADDR));
            } else
                io_registers[addr & 0x7F] = data;
        } else if (addr <= 0xFFFE) {
            hram[addr & 0x7F] = data;
        } else {
            ie = data;
        }
        for (IMMUListener listener : listeners)
            listener.onWriteToMMU(addr, data);

        if (debugger.isHooked(DebuggerMode.MEMORY))
            debugger.writeByte(addr, data);
    }

    private void executeHdmaTransfer(int data) {
        int source = (io_registers[CGB_HDMA1 & 0x7F] << 8) | io_registers[CGB_HDMA2 & 0x7F];
        source &= 0xFFF0;
        int dest = (io_registers[CGB_HDMA3 & 0x7F] << 8) | io_registers[CGB_HDMA4 & 0x7F];
        dest = 8000 | (dest & 0x1FF0);
        int length = ((data & 0x7F) + 1) * 0x10;

        if ((data & 0x80) == 0) { //General purpose HDMA
            hdma_remaining_cycles = length;
            for (int i = 0; i < length; i++)
                writeRaw(dest + i, readByte(source + i, true));
            writeRaw(CGB_HDMA1, 0xFF);
            writeRaw(CGB_HDMA2, 0xFF);
            writeRaw(CGB_HDMA3, 0xFF);
            writeRaw(CGB_HDMA4, 0xFF);
            writeRaw(CGB_HDMA5, 0xFF);
        } else {
            hdma_current_source = source;
            hdma_current_dest = dest;
            hdma_block_left_total = length;
            hdma_block_left_hblank = 0x10;
        }
    }

    public void writeRaw(int addr, int data) {
        addr &= 0xFFFF;
        data &= 0xFF;
        if (addr <= 0x7FFF) {
            if (cartridge != null)
                cartridge.write(addr, data);
        } else if (addr <= 0x9FFF) {
            if (gameboy.mode == GameBoy.Mode.CGB)
                vram[(addr & 0x1FFF) | (0x2000 * (readByte(CGB_VRAM_BANK) & 0x01))] = data;
            else
                vram[addr & 0x1FFF] = data;
        } else if (addr <= 0xBFFF) {
            if (cartridge != null)
                cartridge.write(addr, data);
        } else if (addr <= 0xDFFF) {
            if (gameboy.mode == GameBoy.Mode.CGB)
                wram[(addr & 0x1FFF) | (0x2000 * (readByte(CGB_WRAM_BANK) & 0x07))] = data;
            else
                wram[addr & 0x1FFF] = data;
        } else if (addr <= 0xFE9F) {
            oam[addr & 0xFF] = data;
        } else if (addr <= 0xFEFF) {
            return;
        } else if (addr <= 0xFF7F) {
            if (addr == SC && data == 0x81) {
                char c = (char) readByte(SB);
                serialOutput.append(c);
                writeByte(SC, 0);
            } else {
                io_registers[addr & 0x7F] = data;
            }
        } else if (addr <= 0xFFFE) {
            hram[addr & 0x7F] = data;
        } else {
            ie = data;
        }
        for (IMMUListener listener : listeners)
            listener.onWriteToMMU(addr, data);

        if (debugger.isHooked(DebuggerMode.MEMORY))
            debugger.writeByte(addr, data);
    }

    private void executeDmaTransfer(int data) {
        io_registers[DMA & 0x7F] = data & 0xFF;
        int start = data << 8;

        for (int i = 0x0; i <= 0x9F; i++)
            writeRaw(0xFE00 | i, readByte(start | i, true));
    }

    public boolean readIORegisterBit(int reg, int flag) {
        return (readByte(reg) & flag) == flag;
    }

    public void writeIORegisterBit(int register, int flag, boolean value) {
        int toWrite;
        if (value)
            toWrite = io_registers[register & 0x7F] | flag;
        else
            toWrite = io_registers[register & 0x7F] & ~flag;
        writeRaw(register, toWrite);
    }

    public LCDMode readLcdMode() {
        return ppuMode;
    }

    public void writeLcdMode(LCDMode lcdMode) {
        int lcdModeValue = lcdMode.getValue();
        writeIORegisterBit(STAT, Flags.STAT_MODE >> 1, ((lcdModeValue >> 1) & 0x1) == 0x1);
        writeIORegisterBit(STAT, Flags.STAT_MODE & 0x1, (lcdModeValue & 0x1) == 0x1);
        ppuMode = lcdMode;
    }

    public String getSerialOutput() {
        return serialOutput.toString();
    }

    public void flushSerialOutput() {
        serialOutput = new StringBuilder();
    }

    public void reset() {
        Arrays.fill(vram, 0);
        Arrays.fill(wram, 0);
        Arrays.fill(oam, 0);
        Arrays.fill(io_registers, 0);
        Arrays.fill(hram, 0);
        ie = 0;
    }

    public void saveCartridge() {
        cartridge.save();
    }

    public int readVRAM(int addr, int bank) {
        if (gameboy.mode != GameBoy.Mode.CGB)
            return readByte(addr, true);
        return vram[(addr & 0x1FFF) | (0x2000 * bank)];
    }

    public int readCGBPalette(boolean obj_pal, int addr) {
        if (gameboy.mode == GameBoy.Mode.CGB)
            return obj_pal ? cgb_obj_palettes[addr] : cgb_bg_palettes[addr];
        return 0x00;
    }

    public void loadBootstrap(GameBoy.Mode mode, String file) {
        bootstrap.loadBootstrap(mode, file);
    }

    public void linkDebugger(Debugger debugger) {
        this.debugger = debugger;
    }
}
