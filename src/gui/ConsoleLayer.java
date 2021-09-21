package gui;

import core.GameBoy;
import imgui.ImGui;
import imgui.type.ImString;

import java.util.ArrayList;
import java.util.List;


public class ConsoleLayer {

    private final ImString consoleInput = new ImString();
    private final StringBuilder console = new StringBuilder();

    public void imgui(GameBoy gameBoy) {
        ImGui.begin("Console");
        ImGui.setWindowSize(515, 192);
        ImGui.beginChild("Scrolling", 500, 130);
        ImGui.textColored(0, 255, 0, 255, getPrettifiedOutput(console.toString(), 69));
        ImGui.endChild();
        ImGui.inputText("", consoleInput);
        ImGui.sameLine();
        if (ImGui.button("Enter")) {
            console.append("\n--> ").append(consoleInput);
            Command command = new Command(consoleInput.toString());

            switch (command.command) {
                case "break" -> {
                    try {
                        gameBoy.addBreakpoint(Integer.decode("0x" + command.args.get(0)));
                        console.append("\nBreakpoint created");
                    } catch (Exception e) {
                        console.append("\nError creating breakpoint : ").append(e.getMessage());
                    }
                }
                case "break-m" -> {
                    try {
                        gameBoy.addMemoryBreakpoint(Integer.decode("0x" + command.args.get(0)));
                        console.append("\nBreakpoint created");
                    } catch (Exception e) {
                        console.append("\nError creating breakpoint : ").append(e.getMessage());
                    }
                }
                default -> console.append("\nUnknown command !");
            }

            consoleInput.set("");

        }
        ImGui.end();
    }

    private static String getPrettifiedOutput(String input, int lineLength) {
        StringBuilder sb = new StringBuilder();
        for (String line : input.lines().toList()) {
            if (line.length() > lineLength) {
                for (int i = 1; i <= line.length(); i++) {
                    sb.append(line.charAt(i-1));
                    if (i % lineLength == 0)
                        sb.append("\n");
                }
                sb.append("\n");
            } else {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    public static class Command {
        String command;
        List<String> args;

        public Command(String raw) {
            String[] split = raw.split(" ");
            args = new ArrayList<>();
            command = split[0];
            for (int i = 1; i < split.length; i++) {
                args.add(split[i]);
            }
        }
    }

}