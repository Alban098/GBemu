package threading;

import core.GameBoy;
import core.GameBoyState;
import debug.Debugger;
import debug.DebuggerMode;

public class DebuggerThread extends Thread {

    private boolean shouldExit = false;
    private final Debugger debugger;

    public DebuggerThread(Debugger debugger) {
        this.debugger = debugger;
    }

    @Override
    public void run() {
        try {
            while(!shouldExit) {
                debugger.clock();
                synchronized (this) {
                    wait();
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void shouldExit() {
        shouldExit = true;
    }
}
