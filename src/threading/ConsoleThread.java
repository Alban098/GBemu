package threading;

import console.Command;
import console.Console;
import debug.Debugger;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConsoleThread extends GBemuThread {

    private final Console console;
    private final Queue<Command> pendingCommands;

    public ConsoleThread(Debugger debugger) {
        super();
        console = Console.getInstance();
        console.link(debugger);
        pendingCommands = new ConcurrentLinkedQueue<>();
    }

    @Override
    public void run() {
        try {
            while(!shouldExit.get()) {
                while(!pendingCommands.isEmpty())
                    console.interpret(pendingCommands.poll());
                synchronized (this) {
                    wait();
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void offerCommand(Command command) {
        pendingCommands.offer(command);
    }
}
