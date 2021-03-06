package threading;

import console.Console;
import console.LogLevel;
import gbemu.core.GameBoy;
import gbemu.core.GameBoyState;
import gbemu.extension.debug.DebuggerMode;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class represent a Thread running the emulator
 */
public class GameBoyThread extends GBemuThread {

    private final GameBoy gameboy;
    private final AtomicBoolean requested_frame;
    private int requested_instructions = 0;

    /**
     * Create a new Game Boy Thread
     * @param gameboy the Game Boy to run
     */
    public GameBoyThread(GameBoy gameboy) {
        super();
        this.gameboy = gameboy;
        requested_frame = new AtomicBoolean(false);
    }

    /**
     * Run the Game Boy Thread
     */
    @Override
    public void run() {
        try {
            //While the emulator is running
            while(!should_exit.get()) {
                //If a Cartridge is inserted
                if (gameboy.hasCartridge()) {
                    //If the game Boy has a Debugger hooked up to the CPU
                    if (gameboy.isDebuggerHooked(DebuggerMode.CPU)) {
                        //Compute debugging features
                        if (gameboy.getState() == GameBoyState.RUNNING)
                            gameboy.executeFrames();
                        if (gameboy.getState() == GameBoyState.DEBUG) {
                            if (requested_instructions != 0) {
                                gameboy.executeInstructions(requested_instructions, true);
                                requested_instructions = 0;
                            }
                            if (requested_frame.get()) {
                                gameboy.debugFrame();
                                requested_frame.set(false);
                            }
                        }
                    //Otherwise, run as normal
                    } else {
                        gameboy.executeFrames();
                    }
                }
                //Wait for the next frame to be requested
                synchronized (this) {
                    wait();
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            Console.getInstance().log(LogLevel.ERROR, "Emulation Thread crashed !");
        }
    }

    /**
     * Notify that the Debugger has requested a new frame to be computed
     */
    public void requestOneFrame() {
        requested_frame.set(true);
    }

    /**
     * Notify that the Debugger has request for some instruction to be executed
     * @param nb the number of instruction to execute
     */
    public void requestInstructions(int nb) {
        requested_instructions = nb;
    }
}
