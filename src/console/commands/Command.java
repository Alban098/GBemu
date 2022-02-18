package console.commands;

import console.Console;
import console.LogLevel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class represent a Command as inputted in the Console Window
 * holding the command and its arguments if present
 */
public abstract class Command {

    protected String command;
    protected List<String> args;

    /**
     * Create a Command from its raw String representation
     * @param raw the raw String command
     */
    public Command(String raw) {
        String[] split = raw.split(" ");
        args = new ArrayList<>();
        args.addAll(Arrays.asList(split).subList(1, split.length));
    }

    /**
     * Execute the command from a Console
     * @param console the Console instance executing the command
     */
    public abstract void execute(Console console);

    /**
     * Return whether the Command is a valid one or not
     * @return is the Command valid
     */
    public abstract boolean validate();

    /**
     * Print the manual of the Command to a Console
     * @param console the Console instance to print to
     */
    public void displayHelp(Console console) {
        console.log(LogLevel.WARNING, "No help for this command");
    }

    /**
     * Create an instance of Command of the appropriate type
     * @param string the raw String representation
     * @return a valid instance of Command, null otherwise
     */
    public static Command build(String string) {
        String[] split = string.split(" ");
        switch (split[0]) {
            case "help" -> {
                Command command = new HelpCommand(string);
                return command.validate() ? command : null;
            }
            case "break" -> {
                Command command = new BreakCommand(string);
                return command.validate() ? command : null;
            }
            case "set" -> {
                Command command = new SetCommand(string);
                return command.validate() ? command : null;
            }
            case "get" -> {
                Command command = new GetCommand(string);
                return command.validate() ? command : null;
            }
            default -> { return null; }
        }
    }
}