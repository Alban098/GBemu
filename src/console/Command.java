package console;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Command {
    final String command;
    final List<String> args;

    public Command(String raw) {
        String[] split = raw.split(" ");
        args = new ArrayList<>();
        command = split[0];
        args.addAll(Arrays.asList(split).subList(1, split.length));
    }
}