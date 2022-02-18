package console.commands;

import console.Console;
import console.LogLevel;

/**
 * This class represent a concrete Command of type help
 */
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

    /**
     * Execute the command from a Console
     * @param console the Console instance executing the command
     */
    @Override
    public void execute(Console console) {
        for (Command command : Commands.all) {
            command.displayHelp(console);
            console.log(LogLevel.INFO, " ");
        }
    }

    /**
     * Return whether the Command is a valid one or not
     * @return is the Command valid
     */
    @Override
    public boolean validate() {
        return true;
    }
}
