package gbemu.core;

import gbemu.core.memory.MMU;

/**
 * This class represent an abstraction of the component in charge of keeping trace of CPU cycles
 * to trigger Timer Interrupts to the CPU
 */
public class Timer {

    private final MMU memory;
    private int clock_div = 0;
    private int clock_tima = 0;
    private int pending_overflow = -1;

    /**
     * Create a new Timer Component
     * @param gameboy the Game Boy to link to
     */
    public Timer(GameBoy gameboy) {
        this.memory = gameboy.getMemory();
    }

    /**
     * Update the timer, called every CPU cycles
     */
    public void clock() {
        clock_div++;
        clock_tima++;
        if (pending_overflow >= 0)
            triggerInterrupt();
        if (clock_div > 256) {
            memory.writeRaw(MMU.DIV, (memory.readByte(MMU.DIV) + 1) & 0xFF);
            clock_div -= 256;
        }
        if (memory.readIORegisterBit(MMU.TAC, Flags.TAC_ENABLED)) {
            int clockLength = getTimerFreqDivider();
            while (clock_tima >= clockLength) {
                int tima = memory.readByte(MMU.TIMA);
                tima = (tima + 1) & 0xFF;
                memory.writeRaw(MMU.TIMA, tima);
                if (tima == 0x00)
                    pending_overflow = 4;
                clock_tima -= clockLength;
            }
        }
    }

    /**
     * Get the right frequency divider that will ensure the right TIMA timer length
     * @return the timer frequency divider
     */
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

    /**
     * Trigger an interrupt by setting the Timer_IRQ flag of the IF Register to HIGH
     * also reset the TIME Register to the TMA value
     */
    public void triggerInterrupt() {
        if (pending_overflow <= 0) {
            memory.writeByte(MMU.TIMA, memory.readByte(MMU.TMA));
            memory.writeIORegisterBit(MMU.IF, Flags.IF_TIMER_IRQ, true);
        }
        pending_overflow--;
    }
}
