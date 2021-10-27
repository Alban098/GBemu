package threading;

import core.GameBoy;
import core.GameBoyState;
import debug.DebuggerMode;

import java.util.concurrent.atomic.AtomicBoolean;

public class GameBoyThread extends GBemuThread {

    private final GameBoy gameboy;
    private final AtomicBoolean requestedFrame;
    private int requestedInstructions = 0;

    public GameBoyThread(GameBoy gameboy) {
        super();
        this.gameboy = gameboy;
        requestedFrame = new AtomicBoolean(false);
    }

    @Override
    public void run() {
        try {
            while(!shouldExit.get()) {
                if (gameboy.hasCartridge()) {
                    if (gameboy.isDebuggerHooked(DebuggerMode.CPU)) {
                        if (gameboy.getState() == GameBoyState.RUNNING)
                            gameboy.executeFrames();
                        if (gameboy.getState() == GameBoyState.DEBUG) {
                            if (requestedInstructions != 0) {
                                gameboy.executeInstructions(requestedInstructions, true);
                                requestedInstructions = 0;
                            }
                            if (requestedFrame.get()) {
                                gameboy.forceFrame();
                                requestedFrame.set(false);
                            }
                        }
                    } else {
                        gameboy.executeFrames();
                    }
                }
                synchronized (this) {
                    wait();
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void requestOneFrame() {
        requestedFrame.set(true);
    }

    public void requestInstructions(int nb) {
        requestedInstructions = nb;
    }
}
