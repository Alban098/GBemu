package core;

import core.cpu.LR35902;
import core.memory.MMU;

public class Timer {

    private final MMU memory;
    private boolean timaStarted = false;
    private boolean lastPulse = false;
    private boolean pendingIrq = false;

    public Timer(MMU memory) {
        this.memory = memory;
    }

    public void clock() {
        int clock = (((memory.readByte(MMU.DIV) << 8) | memory.readByte(MMU.IO_INTERNAL_CLK_LOW)) + 1) & 0xFFFF;
        memory.writeRaw(MMU.DIV, (clock & 0xFF00) >> 8);
        memory.writeRaw(MMU.IO_INTERNAL_CLK_LOW, clock & 0x00FF);

        computeTIMA(clock);
    }

    public void computeTIMA(int clock) {
        if (!timaStarted)
            timaStarted = true;

        int bit = (LR35902.CPU_CYCLES_PER_SEC / getTimerFreq()) >> 1;
        triggerTimerOverflowIrq();
        boolean currentPulse = (clock & bit) != 0;
        if (lastPulse && !currentPulse) {
            int timer = (memory.readByte(MMU.TIMA) + 1) & 0xFF;
            pendingIrq = timer == 0x00;
            memory.writeByte(MMU.TIMA, timer);
        }
        lastPulse = currentPulse;
    }

    public int getTimerFreq() {
        int freq = memory.readByte(MMU.TAC) & Flags.TAC_CLOCK;
        return switch (freq) {
            case 0 -> 4096;
            case 1 -> 262144;
            case 2 -> 65536;
            case 3 -> 16384;
            default -> 0;
        };
    }

    public void triggerTimerOverflowIrq() {
        if (pendingIrq) {
            pendingIrq = false;
            memory.writeByte(MMU.TIMA, memory.readByte(MMU.TIMA));
            memory.writeIORegisterBit(MMU.IF, Flags.IF_TIMER_IRQ, true);
        }
    }
}
