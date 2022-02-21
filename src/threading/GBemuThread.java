package threading;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class regroups the common behaviour of all Threaded workload
 */
public abstract class GBemuThread extends Thread {

    //Flag signifying if the Thread should exit
    protected final AtomicBoolean should_exit;

    /**
     * Create a new Thread with the exit flag reset
     */
    protected GBemuThread() {
        should_exit = new AtomicBoolean(false);
    }

    /**
     * Flags the thread to be killed after next loop
     */
    public synchronized void kill() {
        should_exit.set(true);
        //Wake up the thread for it to be able to exit
        notify();
    }
    
}
