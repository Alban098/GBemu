package console.commands;

public class Commands {

    public static final Command HELP_COMMAND = new HelpCommand("");
    public static final Command BREAK_COMMAND = new BreakCommand("");
    public static final Command SET_COMMAND = new SetCommand("");
    public static final Command GET_COMMAND = new GetCommand("");

    public static final Command[] all = {
            BREAK_COMMAND,
            SET_COMMAND,
            GET_COMMAND
    };
}
