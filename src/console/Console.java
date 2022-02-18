package console;

import console.commands.Command;
import gbemu.extension.debug.Debugger;

import java.awt.*;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * This class represent a Console interpreter
 * that can consume Commands and apply them to the Emulator
 */
public class Console {

    public static final int MAX_LINES = 100;
    private static Console instance;

    private Debugger debugger;
    private final Queue<Line> lines;

    /**
     * Return the current Console instance
     * creating it if necessary
     * @return the current Console instance
     */
    public static Console getInstance() {
        if (instance == null)
            instance = new Console();
        return instance;
    }

    /**
     * Return the linked Debugger
     * @return the linked Debugger
     */
    public Debugger getDebugger() {
        return debugger;
    }

    /**
     * Link a debugger to the instance
     * @param debugger the Debugger to link
     */
    public void link(Debugger debugger) {
        this.debugger = debugger;
    }

    /**
     * Create a new Console
     */
    private Console() {
        lines = new ConcurrentLinkedQueue<>();
    }

    /**
     * Interpret and apply a Command
     * @param command the command to interpret
     */
    public void interpret(Command command) {
        if (command.validate())
            command.execute(this);
    }

    /**
     * Log a message to the console
     * @param level the type of message, it affects the display color
     * @param string the message to log
     */
    public void log(LogLevel level, String string) {
        switch (level) {
            case ERROR -> lines.offer(new Line(Color.RED, string));
            case WARNING -> lines.offer(new Line(Color.ORANGE, string));
            case INFO -> lines.offer(new Line(Color.GREEN, string));
            case INPUT -> lines.offer(new Line(Color.WHITE, string));
        }
        //If there is to many lines, discard the oldest one
        while (lines.size() > MAX_LINES)
            lines.poll();
    }

    /**
     * Return the lines of the current Console
     * @return a Queue of all lines
     */
    public Queue<Line> getLines() {
        return lines;
    }

    /**
     * A Record storing a line, and it's color
     */
    public record Line(Color color, String content) {

        public Color getColor() {
            return color;
        }

        public String getContent() {
            return content;
        }
    }
}
