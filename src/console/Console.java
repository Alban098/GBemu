package console;

import debug.BreakPoint;
import debug.Debugger;

import java.awt.*;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Console {

    public static final int MAX_LINES = 100;
    private static Console instance;

    private Debugger debugger;
    private final Queue<Line> lines;

    public static Console getInstance() {
        if (instance == null)
            instance = new Console();
        return instance;
    }

    public void link(Debugger debugger) {
        this.debugger = debugger;
    }

    private Console() {
        lines = new ConcurrentLinkedQueue<>();
    }
    
    public void interpret(Command command) {
        switch (command.command) {
            case "break" -> {
                switch (command.args.get(0)) {
                    case "-m" -> {
                        if ("-r".equals(command.args.get(1))) {
                            try {
                                debugger.removeBreakpoint(Integer.decode("0x" + command.args.get(2)));
                                log(Type.INFO, "Breakpoint removed");
                            } catch (Exception e) {
                                log(Type.ERROR, "Error removing breakpoint : " + e.getMessage());
                            }
                        } else {
                            try {
                                BreakPoint.Type type = BreakPoint.Type.WRITE;
                                if ("/r".equals(command.args.get(2)))
                                    type = BreakPoint.Type.READ;

                                debugger.addBreakpoint(Integer.decode("0x" + command.args.get(1)), type);
                                log(Type.INFO, "Breakpoint created");
                            } catch (Exception e) {
                                log(Type.ERROR, "Error creating breakpoint : " + e.getMessage());
                            }
                        }
                    }
                    case "-r" -> {
                        try {
                            debugger.removeBreakpoint(Integer.decode("0x" + command.args.get(1)));
                            log(Type.INFO,  "Breakpoint removed");
                        } catch (Exception e) {
                            log(Type.ERROR,"Error removing breakpoint : " + e.getMessage());
                        }
                    }
                    default -> {
                        try {
                            debugger.addBreakpoint(Integer.decode("0x" + command.args.get(0)), BreakPoint.Type.EXEC);
                            log(Type.INFO,  "Breakpoint created");
                        } catch (Exception e) {
                            log(Type.ERROR,"Error creating breakpoint : " + e.getMessage());
                        }
                    }
                }
            }
            case "help" -> {
                log(Type.INFO, "================= break =================");
                log(Type.INFO, " break (-r)/(-m)/(-m -r) addr [/r or /w if -m]");
                log(Type.INFO, " -r : remove breakpoint at addr");
                log(Type.INFO, " -m : add memory breakpoint at addr");
                log(Type.INFO, " -m -r : remove memory breakpoint at addr");
                log(Type.INFO, " /r : memory breakpoint on read to addr (only if -m)");
                log(Type.INFO, " /w : memory breakpoint on write to addr (only if -m)");
                log(Type.INFO, " addr : address in hex, ex:C5F6");
            }
            default -> log(Type.WARNING, "Unknown command !");
        }
    }

    public void log(Type type, String string) {
        switch (type) {
            case ERROR -> lines.offer(new Line(Color.RED, string));
            case WARNING -> lines.offer(new Line(Color.ORANGE, string));
            case INFO -> lines.offer(new Line(Color.GREEN, string));
            case INPUT -> lines.offer(new Line(Color.WHITE, string));
        }
        if (lines.size() > MAX_LINES)
            lines.poll();
    }

    public Queue<Line> getLines() {
        return lines;
    }

    public record Line(Color color, String content) {

        public Color getColor() {
            return color;
        }

        public String getContent() {
            return content;
        }
    }

    public enum Type {
        ERROR,
        WARNING,
        INFO,
        INPUT
    }
}
