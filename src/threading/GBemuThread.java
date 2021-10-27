package threading;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class GBemuThread extends Thread {

    protected final AtomicBoolean shouldExit;

    protected GBemuThread() {
        shouldExit = new AtomicBoolean(false);
    }

    public synchronized void kill() {
        shouldExit.set(true);
        notify();
    }
    
}
