package core;

import core.cpu.LR35902;
import core.ppu.PPU;

public class GameBoy {

    private final MMU memory;
    private final LR35902 cpu;
    private final PPU ppu;

    public GameBoy() {
        memory = new MMU();
        cpu = new LR35902(memory);
        ppu = new PPU();
    }

    public void insertCartridge(String file) {
        memory.loadCart(file);
        cpu.init();
        //TODO Reset
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

    public void clock() {
        cpu.clock();
        ppu.clock();
    }

    public void executeInstruction(int nb_instr) {
        for (int i = 0; i < nb_instr; i++)
            while (!cpu.clock())
                ppu.clock();
    }

    public String getSerialOutput() {
        return memory.getSerialOutput();
    }

    public void flushSerialOutput() {
        memory.flushSerialOutput();
    }
}
