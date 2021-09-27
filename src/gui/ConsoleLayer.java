package gui;

import core.GameBoy;
import debug.Logger;
import imgui.ImGui;
import imgui.type.ImString;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class ConsoleLayer extends AbstractDebugLayer {

    private final ImString consoleInput = new ImString();

    public ConsoleLayer(GameBoy gameboy) {
        super(gameboy);
    }


    public void render() {
        ImGui.begin("Console");
        ImGui.beginChild("Scrolling", ImGui.getWindowWidth() - 15, ImGui.getWindowHeight() - 58, true);
        ImGui.setScrollY(ImGui.getScrollMaxY());
        for (Logger.Line line : Logger.getLines())
            ImGui.textColored(line.getColor().getRed(), line.getColor().getGreen(), line.getColor().getBlue(), 255, Utils.getPrettifiedOutput(line.getContent(), (int) (ImGui.getWindowWidth() - 15 / ImGui.getFontSize())));
        ImGui.endChild();
        ImGui.pushItemWidth(ImGui.getWindowWidth() - 66);
        ImGui.inputText("", consoleInput);
        ImGui.sameLine();
        if (ImGui.button("Enter")) {
            Logger.log(Logger.Type.INPUT, "> " + consoleInput.get());
            Command command = new Command(consoleInput.toString());
            interpretCommand(command, this.gameboy);
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

    public static class Command {
        String command;
        List<String> args;

        public Command(String raw) {
            String[] split = raw.split(" ");
            args = new ArrayList<>();
            command = split[0];
            args.addAll(Arrays.asList(split).subList(1, split.length));
        }
    }

}