package console.commands;

import console.Console;
import console.LogLevel;

public class HelpCommand extends Command {

    /**
     * Create a Command from its raw String representation
     *
     * @param raw the raw String command
     */
    public HelpCommand(String raw) {
        super(raw);
        this.command = "help";
        this.args = null;
    }

    @Override
    public void execute(Console console) {
        for (Command command : Commands.all) {
            command.displayHelp(console);
            console.log(LogLevel.INFO, " ");
        }
    }

    @Override
    public boolean validate() {
        return true;
    }
}
