package gbemu.core.input;

import gbemu.core.Flags;
import gbemu.core.GameBoy;
import gbemu.core.memory.MMU;
import gbemu.settings.Button;

public class InputManager {

    private int requestedState = 0x00;
    private final MMU memory;

    public InputManager(GameBoy gameboy) {
        this.memory = gameboy.getMemory();
    }

    public void clock() {
        boolean irq = false;
        int newState = 0;
        if (!memory.readIORegisterBit(MMU.P1, Flags.P1_BUTTON)) {
            newState = requestedState & 0x0F;
            irq = ((memory.readByte(MMU.P1) & 0x0F) ^ newState) != 0;
        } else if (!memory.readIORegisterBit(MMU.P1, Flags.P1_DPAD)) {
            newState = (requestedState & 0xF0) >> 4;
            irq = ((memory.readByte(MMU.P1) & 0x0F) ^ newState) != 0;
        }
        if (irq) {
            memory.writeRaw(MMU.P1, (memory.readByte(MMU.P1) & 0xF0 | newState));
            memory.writeIORegisterBit(MMU.IF, Flags.IF_JOYPAD_IRQ, true);
        }
    }

    public synchronized void setButtonState(Button button, InputState state) {
        if (state == InputState.PRESSED)
            requestedState &= ~button.getMask();
        else
            requestedState |= button.getMask();
    }
}
