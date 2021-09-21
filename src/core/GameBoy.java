package core;

import core.apu.APU;
import core.cpu.LR35902;
import core.ppu.PPU;

import javax.sound.sampled.SourceDataLine;

public class GameBoy {

    private final MMU memory;
    private final LR35902 cpu;
    private final PPU ppu;
    private final APU apu;
    private final Timer timer;
    private GameBoyState currentState = GameBoyState.PAUSED;

    public GameBoy(String bootstap) {
        memory = new MMU(bootstap);
        cpu = new LR35902(memory);
        ppu = new PPU(memory);
        apu = new APU(memory);
        timer = new Timer(memory);
    }

    public void insertCartridge(String file) {
        memory.loadCart(file);
        cpu.init();
        //TODO Reset
    }

    public void reset() {
        cpu.reset();
        ppu.reset();
        memory.writeByte(0xFF00, 0xCF);  // Joypad 1100 1111 (No Buttons pressed)
        memory.writeByte(0xFF05, 0x0);   // TIMA
        memory.writeByte(0xFF06, 0x0);   // TMA
        memory.writeByte(0xFF07, 0x0);   // TAC
        memory.writeByte(0xFF10, 0x80);  // NR10
        memory.writeByte(0xFF11, 0xBF);  // NR11
        memory.writeByte(0xFF12, 0xF3);  // NR12
        memory.writeByte(0xFF14, 0xBF);  // NR14
        memory.writeByte(0xFF16, 0x3F);  // NR21
        memory.writeByte(0xFF17, 0x00);  // NR22
        memory.writeByte(0xFF19, 0xBF);  // NR24
        memory.writeByte(0xFF1A, 0x7F);  // NR30
        memory.writeByte(0xFF1B, 0xFF);  // NR31
        memory.writeByte(0xFF1C, 0x9F);  // NR32
        memory.writeByte(0xFF1E, 0xBF);  // NR33
        memory.writeByte(0xFF20, 0xFF);  // NR41
        memory.writeByte(0xFF21, 0x00);  // NR42
        memory.writeByte(0xFF22, 0x00);  // NR43
        memory.writeByte(0xFF23, 0xBF);  // NR30
        memory.writeByte(0xFF24, 0x77);  // NR50
        memory.writeByte(0xFF25, 0xF3);  // NR51
        memory.writeByte(0xFF26, 0xF1);  // NR52
        memory.writeByte(0xFF40, 0x91);  // LCDC
        memory.writeByte(0xFF42, 0x00);  // SCY
        memory.writeByte(0xFF43, 0x00);  // SCX
        memory.writeByte(0xFF44, 0x00);  // LY
        memory.writeByte(0xFF45, 0x00);  // LYC
        memory.writeByte(0xFF47, 0xFC);  // BGP
        memory.writeByte(0xFF48, 0xFF);  // OBP0
        memory.writeByte(0xFF49, 0xFF);  // OBP1
        memory.writeByte(0xFF4A, 0x00);  // WY
        memory.writeByte(0xFF4B, 0x00);  // WX
        memory.writeByte(0xFF0F, 0xE0);  // IF
        memory.writeByte(0xFFFF, 0x00);  // IE
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
        while(!ppu.isFrameComplete())
            clock();
    }

    public GameBoyState getState() {
        return currentState;
    }

    public void setState(GameBoyState state) {
        this.currentState = state;
    }

    public float getLastSample() {
        return apu.getNextSample();
    }
}
