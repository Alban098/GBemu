package core;

import core.apu.APU;
import core.cpu.LR35902;
import core.input.InputManager;
import core.input.State;
import core.input.Button;
import core.memory.MMU;
import core.ppu.PPU;
import debug.BreakPoint;
import debug.Debugger;
import debug.DebuggerMode;
import debug.Logger;

import java.util.HashMap;
import java.util.Map;

public class GameBoy {

    public static final boolean ENABLE_BOOTSTRAP = true;
    private boolean hasCartridge = false;
    public Mode mode = Mode.DMG;
    private long mcycles = 0;

    private final MMU memory;
    private final LR35902 cpu;
    private final PPU ppu;
    private final APU apu;
    private final Timer timer;
    private final InputManager inputManager;
    private final Debugger debugger;

    private GameBoyState currentState;
    private boolean half_exec_step = false;

    public GameBoy() {
        debugger = new Debugger(this);
        memory = new MMU(this);
        cpu = new LR35902(this);
        ppu = new PPU(this);
        apu = new APU(this);
        timer = new Timer(this);
        inputManager = new InputManager(this);
        currentState = GameBoyState.RUNNING;
    }

    public Debugger getDebugger() {
        return debugger;
    }


    public void insertCartridge(String file) throws Exception {
        memory.loadCart(file);
        hasCartridge = true;
        reset();
        debugger.init();
    }

    public void reset() {
        memory.reset();
        cpu.reset();
        ppu.reset();
        apu.reset();
        memory.writeRaw(MMU.P1, 0xCF);
        memory.writeRaw(MMU.SB, 0x00);
        memory.writeRaw(MMU.SC, 0x7E);
        memory.writeRaw(MMU.DIV, 0xAB);
        memory.writeRaw(MMU.TIMA, 0x00);
        memory.writeRaw(MMU.TMA, 0x00);
        memory.writeRaw(MMU.TAC, 0xF8);
        memory.writeRaw(MMU.IF, 0xE1);
        memory.writeRaw(MMU.NR10, 0x80);
        memory.writeRaw(MMU.NR11, 0xBF);
        memory.writeRaw(MMU.NR12, 0xF3);
        memory.writeRaw(MMU.NR13, 0xFF);
        memory.writeRaw(MMU.NR14, 0xBF);
        memory.writeRaw(MMU.NR21, 0x3F);
        memory.writeRaw(MMU.NR22, 0x00);
        memory.writeRaw(MMU.NR23, 0xFF);
        memory.writeRaw(MMU.NR24, 0xBF);
        memory.writeRaw(MMU.NR30, 0x7F);
        memory.writeRaw(MMU.NR31, 0xFF);
        memory.writeRaw(MMU.NR32, 0x9F);
        memory.writeRaw(MMU.NR33, 0xFF);
        memory.writeRaw(MMU.NR34, 0xBF);
        memory.writeRaw(MMU.NR41, 0xFF);
        memory.writeRaw(MMU.NR42, 0x00);
        memory.writeRaw(MMU.NR43, 0x00);
        memory.writeRaw(MMU.NR44, 0xBF);
        memory.writeRaw(MMU.NR50, 0x77);
        memory.writeRaw(MMU.NR51, 0xF3);
        memory.writeRaw(MMU.NR52, 0xF1);
        memory.writeRaw(MMU.LCDC, 0x91);
        memory.writeRaw(MMU.STAT, 0x85);
        memory.writeRaw(MMU.SCX, 0x00);
        memory.writeRaw(MMU.SCY, 0x00);
        memory.writeRaw(MMU.LY, 0x00);
        memory.writeRaw(MMU.LYC, 0x00);
        memory.writeRaw(MMU.DMA, 0xFF);
        memory.writeRaw(MMU.BGP, 0xFC);
        memory.writeRaw(MMU.OBP0, 0xFF);
        memory.writeRaw(MMU.OBP1, 0xFF);
        memory.writeRaw(MMU.WX, 0x00);
        memory.writeRaw(MMU.WY, 0x00);
        memory.writeRaw(MMU.IE, 0x00);
        Logger.log(Logger.Type.INFO, "Emulation reset");
        cpu.init();
    }

    public MMU getMemory() {
        return memory;
    }

    public LR35902 getCpu() {
        return cpu;
    }

    public PPU getPpu() {
        return ppu;
    }

    public APU getApu() {
        return apu;
    }

    public void executeInstruction() {
        int opcode_mcycles = Integer.MAX_VALUE;
        while (opcode_mcycles > 0) {
            switch (mode) {
                case CGB -> {
                    if (memory.clock()) {
                        opcode_mcycles = cpu.execute();
                        timer.clock();
                        if (half_exec_step) {
                            ppu.clock();
                            apu.clock();
                            inputManager.clock();
                        }
                        half_exec_step = !half_exec_step;
                    }
                }
                case DMG -> {
                    if (memory.clock()) {
                        opcode_mcycles = cpu.execute();
                        timer.clock();
                        ppu.clock();
                        apu.clock();
                        inputManager.clock();
                    }
                }
            }

            mcycles++;
            if (mcycles >= mode.cpu_cycles_per_second * 10L) {
                memory.saveCartridge();
                mcycles -= mode.cpu_cycles_per_second * 10L;
            }
        }
    }

    public void executeInstructions(int nb_instr, boolean force) {
        for (int i = 0; i < nb_instr && (currentState == GameBoyState.RUNNING || force); i++)
            executeInstruction();
    }

    public String getSerialOutput() {
        return memory.getSerialOutput();
    }

    public void flushSerialOutput() {
        memory.flushSerialOutput();
    }

    public void executeFrame() {
        while(ppu.isFrameIncomplete() && currentState == GameBoyState.RUNNING)
            executeInstruction();
    }

    public void forceFrame() {
        while(ppu.isFrameIncomplete() && currentState == GameBoyState.DEBUG)
            executeInstruction();
    }

    public GameBoyState getState() {
        return currentState;
    }

    public void setState(GameBoyState state) {
        this.currentState = state;
    }

    public float getNextSample() {
        return apu.getNextSample();
    }

    public void setButtonState(Button button, State state) {
        inputManager.setButtonState(button, state);
    }

    public boolean hasCartridge() {
        return hasCartridge;
    }

    public boolean isDebuggerHooked(DebuggerMode mode) {
        return debugger.isHooked(mode);
    }

    public enum Mode {
        DMG(4194304, 70224, 208, 456, 80, 168),
        CGB(8388608, 70224, 208, 456, 80, 168);

        public final int cpu_cycles_per_second;
        public final int cpu_cycles_per_frame;
        public final int cpu_cycles_per_hblank;
        public final int cpu_cycles_per_vblank_scanline; //divide because VBlank is 10 scanline long
        public final int cpu_cycles_per_oam;
        public final int cpu_cycles_per_transfer;
        public final int cpu_cycles_256HZ = 4194304 / 256;
        public final int cpu_cycles_128HZ = 4194304 / 128;
        public final int cpu_cycles_64HZ = 4194304 / 64;
        public float cpu_cycles_per_sample = 4194304f / APU.SAMPLE_RATE;

        Mode(int cpu_cycles_per_second, int cpu_cycles_per_frame, int cpu_cycles_per_hblank, int cpu_cycles_per_vblank_scanline, int cpu_cycles_per_oam, int cpu_cycles_per_transfer) {
            this.cpu_cycles_per_second = cpu_cycles_per_second;
            this.cpu_cycles_per_frame = cpu_cycles_per_frame;
            this.cpu_cycles_per_hblank = cpu_cycles_per_hblank;
            this.cpu_cycles_per_vblank_scanline = cpu_cycles_per_vblank_scanline;
            this.cpu_cycles_per_oam = cpu_cycles_per_oam;
            this.cpu_cycles_per_transfer = cpu_cycles_per_transfer;
        }
    }
}
