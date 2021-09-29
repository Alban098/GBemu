package core.memory;

import core.Flags;
import core.GameBoy;
import core.GameBoyState;
import core.cartridge.Cartridge;
import core.ppu.LCDMode;
import core.ppu.helper.IMMUListener;
import debug.Logger;

import java.util.*;

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
    public static final int BOOTSTRAP_CONTROL   = 0xFF50;
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

    private final List<IMMUListener> listeners;
    private final GameBoy gameboy;

    private Cartridge cartridge;
    private LCDMode ppuMode = LCDMode.H_BLANK;
    private final int[] memory;
    private StringBuilder serialOutput;

    private final Set<Integer> breakpoints;
    private int dma_remaining_cycles = 0;

    public MMU(GameBoy gb) {
        gameboy = gb;
        memory = new int[0x10000];
        breakpoints = new HashSet<>();
        serialOutput = new StringBuilder();
        listeners = new ArrayList<>();
        loadBootstrap();
    }

    public void addListener(IMMUListener listener) {
        listeners.add(listener);
    }

    public void loadCart(String file) throws Exception {
        cartridge = new Cartridge(file);
    }

    private void loadBootstrap() {
        System.arraycopy(GameBoy.BOOTSTRAP, 0, memory, 0, 0x0100);
    }

    public void clock() {
        if (dma_remaining_cycles > 0) {
            dma_remaining_cycles--;
        }
    }

    public int readByte(int addr) {
        return readByte(addr, false);
    }

    public int readByte(int addr, boolean fromPPU) {
        addr &= 0xFFFF;
        if (addr <= 0xFF && memory[BOOTSTRAP_CONTROL] != 1)
            return memory[addr];
        else if (addr <= 0x7FFF)
            return cartridge.read(addr);
        else if (addr <= 0x9FFF && !fromPPU && ppuMode == LCDMode.TRANSFER && readIORegisterBit(MMU.LCDC, Flags.LCDC_LCD_ON))
            return 0xFF;
        else if (addr >= 0xFE00 && addr <= 0xFE9F && !fromPPU && (ppuMode == LCDMode.TRANSFER || ppuMode == LCDMode.OAM || dma_remaining_cycles > 0) && (readIORegisterBit(MMU.LCDC, Flags.LCDC_LCD_ON) || dma_remaining_cycles > 0))
            return 0xFF;
        else
            return memory[addr];
    }

    public void writeByte(int addr, int data) {
        addr &= 0xFFFF;
        data &= 0xFF;
        if (addr == SC && data == 0x81) {
            char c = (char) readByte(SB);
            serialOutput.append(c);
            writeByte(SC, 0);
        }
        if (addr == SB)
            System.out.println("");

        if(addr == LY)
            memory[addr] = 0;
        else if(addr == DIV)
            memory[addr] = 0;
        else if(addr == DMA)
            executeDmaTransfer(data);
        else if(addr == P1)
            memory[P1] = (data & 0x30) | (memory[P1] & 0xCF);
        else if(addr == STAT)
            memory[STAT] = (data & 0x78) | (memory[STAT] & 0x07);
        else if(addr <= 0x3FFF)
            cartridge.write(addr, data);
        /*
        //VRAM Lock during transfer causes bugs for some reason
        else if (addr >= 0x8000 && addr <= 0x9FFF && (ppuMode == LCDMode.TRANSFER) && readIORegisterBit(MMU.LCDC, Flags.LCDC_LCD_ON))
            return;
        */
        else if (addr >= 0xFE00 && addr <= 0xFE9F && (ppuMode == LCDMode.TRANSFER || ppuMode == LCDMode.OAM || dma_remaining_cycles > 0) && (readIORegisterBit(MMU.LCDC, Flags.LCDC_LCD_ON) || dma_remaining_cycles > 0))
            return;
        else
            memory[addr] = data;

        for (IMMUListener listener : listeners)
            listener.onWriteToMMU(addr, data);
    }

    private void executeDmaTransfer(int data) {
        memory[DMA] = data;
        int start = data << 8;

        for (int i = 0x0; i <= 0x9F; i++)
            writeRaw(0xFE00 | i, readByte(start | i, true));
    }

    public void writeRaw(int addr, int data) {
        memory[addr] = data & 0xFF;
    }

    public boolean readIORegisterBit(int reg, int flag) {
        return (readByte(reg) & flag) == flag;
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

    public void addBreakpoint(int addr) {
        breakpoints.add(addr);
    }

    public void reset() {
        Arrays.fill(memory, 0);
        loadBootstrap();
    }

    public void removeBreakpoint(int addr) {
        breakpoints.remove(addr);
    }
}
