package gui;

import core.GameBoy;
import debug.Logger;
import imgui.ImGui;
import imgui.type.ImString;

import java.util.ArrayList;
import java.util.List;


public class ConsoleLayer {

    private final ImString consoleInput = new ImString();

    public void imgui(GameBoy gameBoy) {
        ImGui.begin("Console");
        ImGui.beginChild("Scrolling", ImGui.getWindowWidth() - 15, ImGui.getWindowHeight() - 58, true);
        ImGui.setScrollY(ImGui.getScrollMaxY());
        for (Logger.Line line : Logger.getLines())
            ImGui.textColored(line.getColor().getRed(), line.getColor().getGreen(), line.getColor().getBlue(), 255, getPrettifiedOutput(line.getContent(), (int) (ImGui.getWindowWidth() - 15 / ImGui.getFontSize())));
        ImGui.endChild();
        ImGui.pushItemWidth(ImGui.getWindowWidth() - 66);
        ImGui.inputText("", consoleInput);
        ImGui.sameLine();
        if (ImGui.button("Enter")) {
            Logger.log(Logger.Type.INPUT, "> " + consoleInput.get());
            Command command = new Command(consoleInput.toString());
            interpretCommand(command, gameBoy);
            consoleInput.set("");
        }
        ImGui.end();
    }

    private void interpretCommand(Command command, GameBoy gameBoy) {
        switch (command.command) {
            case "break" -> {
                try {
                    gameBoy.addBreakpoint(Integer.decode("0x" + command.args.get(0)));
                    Logger.log(Logger.Type.INFO,  "Breakpoint created");
                } catch (Exception e) {
                    Logger.log(Logger.Type.ERROR,"Error creating breakpoint : " + e.getMessage());
                }
            }
            case "break-m" -> {
                try {
                    gameBoy.addMemoryBreakpoint(Integer.decode("0x" + command.args.get(0)));
                    Logger.log(Logger.Type.INFO,  "Breakpoint created");
                } catch (Exception e) {
                    Logger.log(Logger.Type.ERROR,"Error creating breakpoint : " + e.getMessage());
                }
            }
            default -> Logger.log(Logger.Type.WARNING, "Unknown command !");
        }
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