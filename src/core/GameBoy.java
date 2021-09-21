package core;

import core.apu.APU;
import core.cpu.LR35902;
import core.memory.MMU;
import core.ppu.PPU;

public class GameBoy {

    private final MMU memory;
    private final LR35902 cpu;
    private final PPU ppu;
    private final APU apu;
    private final Timer timer;
    private GameBoyState currentState = GameBoyState.PAUSED;

    public GameBoy(String bootstap) {
        memory = new MMU(bootstap, this);
        cpu = new LR35902(memory, this);
        ppu = new PPU(memory);
        apu = new APU(memory);
        timer = new Timer(memory);
        reset();
    }

    public void insertCartridge(String file) {
        memory.loadCart(file);
        cpu.init();
    }

    public void reset() {
        cpu.reset();
        ppu.reset();
        memory.writeRaw(MMU.IF, 0xE1);
        memory.writeRaw(MMU.IE, 0x00);
        memory.writeRaw(MMU.STAT, 0x81);
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

    public void clock() {
        cpu.clock();
        ppu.clock();
        apu.clock();
        timer.clock();
    }

    public void executeInstruction(int nb_instr) {
        for (int i = 0; i < nb_instr; i++) {
            while (!cpu.clock()) {
                ppu.clock();
                apu.clock();
                timer.clock();
            }
        }
    }

    public String getSerialOutput() {
        return memory.getSerialOutput();
    }

    public void flushSerialOutput() {
        memory.flushSerialOutput();
    }

    public void executeFrame() {
        while(!ppu.isFrameComplete() && currentState == GameBoyState.RUNNING)
            clock();
    }

    public void forceFrame() {
        while(!ppu.isFrameComplete() && currentState == GameBoyState.DEBUG)
            clock();
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

    public void addBreakpoint(int addr) {
        cpu.addBreakpoint(addr);
    }

    public void removeBreakpoint(int addr) {
        cpu.removeBreakpoint(addr);
    }

    public void addMemoryBreakpoint(int addr) {
        memory.addBreakpoint(addr);
    }
}
