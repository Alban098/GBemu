package console;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class represent a Command as inputted in the Console Window
 * holding the command and its arguments if present
 */
public class Command {

    final String command;
    final List<String> args;

    /**
     * Create a Command from its raw String representation
     * @param raw the raw String command
     */
    public Command(String raw) {
        String[] split = raw.split(" ");
        args = new ArrayList<>();
        command = split[0];
        args.addAll(Arrays.asList(split).subList(1, split.length));
    }
}