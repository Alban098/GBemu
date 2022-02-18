package console.commands;

import console.Console;
import console.LogLevel;

public class GetCommand extends Command {

    /**
     * Create a Command from its raw String representation
     *
     * @param raw the raw String command
     */
    public GetCommand(String raw) {
        super(raw);
        this.command = "get";
    }

    @Override
    public boolean validate() {
        try {
            if (args.size() != 1)
                throw new Exception("Invalid number of arguments, expected : 2");
            int addr = Integer.decode("0x" + args.get(0));
            if (addr < 0 || addr > 0xFFFF)
                throw new Exception("address out of range (0x0000 - 0xFFFF");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void execute(Console console) {
        try {
            if (args.size() != 1)
                throw new Exception("Invalid number of arguments, expected : 1");
            int addr = Integer.decode("0x" + args.get(0));
            if (addr < 0 || addr > 0xFFFF)
                throw new Exception("address out of range (0x0000 - 0xFFFF");
            console.getDebugger().readMemory(addr);
            console.log(LogLevel.INFO, "value at 0x" + args.get(0) + " : " + String.format("0x%02X", console.getDebugger().readMemory(addr)));
        } catch (Exception e) {
            console.log(LogLevel.ERROR,"Error reading memory : " + e.getMessage());
        }
    }

    @Override
    public void displayHelp(Console console) {
        console.log(LogLevel.INFO, "================= get =================");
        console.log(LogLevel.INFO, "Read an address from mapped memory");
        console.log(LogLevel.INFO, " get (addr)");
        console.log(LogLevel.INFO, " addr : address in hex, ex:C5F6");
    }
}
