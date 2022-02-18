package console.commands;

import console.Console;
import console.LogLevel;

import java.util.ArrayList;
import java.util.Arrays;

public class SetCommand extends Command {

    /**
     * Create a Command from its raw String representation
     *
     * @param raw the raw String command
     */
    public SetCommand(String raw) {
        super(raw);
        this.command = "set";
        this.args = new ArrayList<>();
        String[] split = raw.split(" ");
        args.addAll(Arrays.asList(split).subList(1, split.length));;
    }

    @Override
    public boolean validate() {
        try {
            if (args.size() != 2)
                throw new Exception("Invalid number of arguments, expected : 2");
            int addr = Integer.decode("0x" + args.get(0));
            int val = Integer.decode("0x" + args.get(1));
            if (addr < 0 || addr > 0xFFFF)
                throw new Exception("address out of range (0x0000 - 0xFFFF");
            if (val < 0 || val > 0xFF)
                throw new Exception("value out of range (0x00 - 0xFF");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void execute(Console console) {
        try {
            if (args.size() != 2)
                throw new Exception("Invalid number of arguments, expected : 2");
            int addr = Integer.decode("0x" + args.get(0));
            int val = Integer.decode("0x" + args.get(1));
            if (addr < 0 || addr > 0xFFFF)
                throw new Exception("address out of range (0x0000 - 0xFFFF");
            if (val < 0 || val > 0xFF)
                throw new Exception("value out of range (0x00 - 0xFF");
            console.getDebugger().writeMemory(addr, val);
            console.log(LogLevel.INFO,  "0x" + args.get(1) + " written to " + "0x" + args.get(0));
        } catch (Exception e) {
            console.log(LogLevel.ERROR,"Error writing to memory : " + e.getMessage());
        }
    }

    @Override
    public void displayHelp(Console console) {
        console.log(LogLevel.INFO, "================= set =================");
        console.log(LogLevel.INFO, "Write a value to Mapped Memory (does not trigger \"on write events\"");
        console.log(LogLevel.INFO, "also not working on ROM");
        console.log(LogLevel.INFO, " set (addr) (val)");
        console.log(LogLevel.INFO, " addr : address in hex, ex:C5F6");
        console.log(LogLevel.INFO, " addr : value in hex, ex:F5");
    }
}
