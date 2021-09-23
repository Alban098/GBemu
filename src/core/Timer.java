package core;

import core.memory.MMU;

public class Timer {

    private final MMU memory;
    private int clockDiv = 0;
    private int clockTima = 0;
    private int pendingOverflow = -1;

    public Timer(MMU memory) {
        this.memory = memory;
    }

    public void clock(int mcycles) {
        clockDiv += mcycles;
        clockTima += mcycles;
        if (pendingOverflow >= 0)
            triggerInterrupt(mcycles);
        if (clockDiv > 256) {
            memory.writeRaw(MMU.DIV, (memory.readByte(MMU.DIV) + 1) & 0xFF);
            clockDiv -= 256;
        }
        if (memory.readIORegisterBit(MMU.TAC, Flags.TAC_ENABLED)) {
            int clockLength = getTimerFreqDivider();
            while (clockTima >= clockLength) {
                int tima = memory.readByte(MMU.TIMA);
                tima = (tima + 1) & 0xFF;
                memory.writeRaw(MMU.TIMA, tima);
                if (tima == 0x00)
                    pendingOverflow = 4;
                clockTima -= clockLength;
            }
        }
    }

    public int getTimerFreqDivider() {
        int freq = memory.readByte(MMU.TAC) & Flags.TAC_CLOCK;
        return switch (freq) {
            case 0 -> 1024;
            case 1 -> 16;
            case 2 -> 64;
            case 3 -> 256;
            default -> 1;
        };
    }

    public void triggerInterrupt(int mcycles) {
        if (pendingOverflow <= 0) {
            memory.writeByte(MMU.TIMA, memory.readByte(MMU.TMA));
            memory.writeIORegisterBit(MMU.IF, Flags.IF_TIMER_IRQ, true);
        }
        pendingOverflow -= mcycles;
    }
}
