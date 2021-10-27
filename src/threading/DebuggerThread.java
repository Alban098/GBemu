package threading;

import debug.Debugger;

public class DebuggerThread extends GBemuThread {

    private final Debugger debugger;

    public DebuggerThread(Debugger debugger) {
        super();
        this.debugger = debugger;
    }

    @Override
    public void run() {
        try {
            while(!shouldExit.get()) {
                debugger.clock();
                synchronized (this) {
                    wait();
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
