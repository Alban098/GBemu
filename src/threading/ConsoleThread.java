package threading;

import console.Command;
import console.Console;
import console.Type;
import debug.Debugger;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * This class represent a Thread running a Console
 */
public class ConsoleThread extends GBemuThread {

    private final Console console;
    private final Queue<Command> pendingCommands;

    /**
     * Create a new Console Thread
     * @param debugger the debugger to link to the console
     */
    public ConsoleThread(Debugger debugger) {
        super();
        console = Console.getInstance();
        console.link(debugger);
        pendingCommands = new ConcurrentLinkedQueue<>();
    }

    /**
     * Run the Console thread
     */
    @Override
    public void run() {
        try {
            //While the console is active
            while(!shouldExit.get()) {
                //Consume all command in order of submission
                while(!pendingCommands.isEmpty())
                    console.interpret(pendingCommands.poll());
                //Wait for the thread to be waked up
                synchronized (this) {
                    wait();
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            Console.getInstance().log(Type.ERROR, "Console Thread crashed !");
        }
    }

    /**
     * Offer a Command to be consumed when possible
     * @param command the command to be consumed
     */
    public void offerCommand(Command command) {
        pendingCommands.offer(command);
    }
}
