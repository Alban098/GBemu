package threading;

import console.Console;
import console.LogLevel;
import gbemu.extension.debug.Debugger;

/**
 * This class represent a Thread running a Debugger
 */
public class DebuggerThread extends GBemuThread {

    private final Debugger debugger;

    /**
     * Create a new Debugger Thread
     * @param debugger the Debugger to run
     */
    public DebuggerThread(Debugger debugger) {
        super();
        this.debugger = debugger;
    }

    /**
     * Run the Debugger Thread
     */
    @Override
    public void run() {
        try {
            //While the debugger is active
            while(!should_exit.get()) {
                //Update the debugger
                debugger.clock();
                //Wait for the next debugger frame to be requested
                synchronized (this) {
                    wait();
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            Console.getInstance().log(LogLevel.ERROR, "Debugger Thread crashed !");
        }
    }
}
