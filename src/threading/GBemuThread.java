package threading;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class regroups the common behaviour of all Threaded workload
 */
public abstract class GBemuThread extends Thread {

    //Flag signifying if the Thread should exit
    protected final AtomicBoolean shouldExit;

    /**
     * Create a new Thread with the exit flag reset
     */
    protected GBemuThread() {
        shouldExit = new AtomicBoolean(false);
    }

    /**
     * Flags the thread to be killed after next loop
     */
    public synchronized void kill() {
        shouldExit.set(true);
        //Wake up the thread for it to be able to exit
        notify();
    }
    
}
