package threading;

import core.GameBoy;
import core.GameBoyState;
import debug.DebuggerMode;

public class GameBoyThread extends Thread {

    private boolean shouldExit = false;
    private final GameBoy gameboy;
    private boolean requestedFrame = false;
    private int requestedInstructions = 0;

    public GameBoyThread(GameBoy gameboy) {
        this.gameboy = gameboy;
    }

    @Override
    public void run() {
        try {
            while(!shouldExit) {
                if (gameboy.hasCartridge()) {
                    if (gameboy.isDebuggerHooked(DebuggerMode.CPU)) {
                        if (gameboy.getState() == GameBoyState.RUNNING)
                            gameboy.executeFrames();
                        if (gameboy.getState() == GameBoyState.DEBUG) {
                            if (requestedInstructions != 0) {
                                gameboy.executeInstructions(requestedInstructions, true);
                                requestedInstructions = 0;
                            }
                            if (requestedFrame) {
                                gameboy.forceFrame();
                                requestedFrame = false;
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

    public synchronized void requestOneFrame() {
        requestedFrame = true;
    }

    public void requestInstructions(int nb) {
        requestedInstructions = nb;
    }

    public void shouldExit() {
        shouldExit = true;
    }
}
