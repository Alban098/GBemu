package console.commands;

import console.Console;
import console.LogLevel;
import gbemu.extension.debug.BreakPoint;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class represent a concrete Command of type break
 */
public class BreakCommand extends Command {

    /**
     * Create a Command from its raw String representation
     *
     * @param raw the raw String command
     */
    public BreakCommand(String raw) {
        super(raw);
        this.command = "break";
        this.args = new ArrayList<>();
        String[] split = raw.split(" ");
        args.addAll(Arrays.asList(split).subList(1, split.length));;
    }

    /**
     * Return whether the Command is a valid one or not
     * @return is the Command valid
     */
    @Override
    public boolean validate() {
        if (args.size() > 0) {
            switch (args.get(0)) {
                case "-m" -> {
                    if ("-r".equals(args.get(1))) {
                        try {
                            int decode = Integer.decode("0x" + args.get(0));
                            return true;
                        } catch (Exception e) {
                            return false;
                        }
                    } else {
                        try {
                            int decode = Integer.decode("0x" + args.get(1));
                            return true;
                        } catch (Exception e) {
                            return false;
                        }
                    }
                }
                case "-r" -> {
                    try {
                        int decode = Integer.decode("0x" + args.get(1));
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                }
                default -> {
                    try {
                        int decode = Integer.decode("0x" + args.get(0));
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Execute the command from a Console
     * @param console the Console instance executing the command
     */
    @Override
    public void execute(Console console) {
        switch (args.get(0)) {
            case "-m" -> {
                if ("-r".equals(args.get(1))) {
                    try {
                        console.getDebugger().removeBreakpoint(Integer.decode("0x" + args.get(2)));
                        console.log(LogLevel.INFO, "Breakpoint removed");
                    } catch (Exception e) {
                        console.log(LogLevel.ERROR, "Error removing breakpoint : " + e.getMessage());
                    }
                } else {
                    try {
                        BreakPoint.Type level = BreakPoint.Type.WRITE;
                        if ("/r".equals(args.get(2)))
                            level = BreakPoint.Type.READ;

                        console.getDebugger().addBreakpoint(Integer.decode("0x" + args.get(1)), level);
                        console.log(LogLevel.INFO, "Breakpoint created");
                    } catch (Exception e) {
                        console.log(LogLevel.ERROR, "Error creating breakpoint : " + e.getMessage());
                    }
                }
            }
            case "-r" -> {
                try {
                    console.getDebugger().removeBreakpoint(Integer.decode("0x" + args.get(1)));
                    console.log(LogLevel.INFO,  "Breakpoint removed");
                } catch (Exception e) {
                    console.log(LogLevel.ERROR,"Error removing breakpoint : " + e.getMessage());
                }
            }
            default -> {
                try {
                    console.getDebugger().addBreakpoint(Integer.decode("0x" + args.get(0)), BreakPoint.Type.EXEC);
                    console.log(LogLevel.INFO,  "Breakpoint created");
                } catch (Exception e) {
                    console.log(LogLevel.ERROR,"Error creating breakpoint : " + e.getMessage());
                }
            }
        }
    }

    /**
     * Print the manual of the Command to a Console
     * @param console the Console instance to print to
     */
    @Override
    public void displayHelp(Console console) {
        console.log(LogLevel.INFO, "================= break =================");
        console.log(LogLevel.INFO, "Add or remove breakpoints");
        console.log(LogLevel.INFO, " break (-r)/(-m)/(-m -r) (addr) [/r or /w if -m]");
        console.log(LogLevel.INFO, " -r : remove breakpoint at addr");
        console.log(LogLevel.INFO, " -m : add memory breakpoint at addr");
        console.log(LogLevel.INFO, " -m -r : remove memory breakpoint at addr");
        console.log(LogLevel.INFO, " /r : memory breakpoint on read to addr (only if -m)");
        console.log(LogLevel.INFO, " /w : memory breakpoint on write to addr (only if -m)");
        console.log(LogLevel.INFO, " addr : address in hex, ex:C5F6");
    }
}
