package gui;

import debug.BreakPoint;
import debug.Debugger;
import debug.Logger;
import imgui.ImGui;
import imgui.type.ImString;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class ConsoleLayer extends AbstractDebugLayer {

    private final ImString consoleInput = new ImString();

    public ConsoleLayer(Debugger debugger) {
        super(debugger);
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
            interpretCommand(command);
            consoleInput.set("");
        }
        ImGui.end();
    }

    private void interpretCommand(Command command) {
        switch (command.command) {
            case "break" -> {
                switch (command.args.get(0)) {
                    case "-m" -> {
                        if ("-r".equals(command.args.get(1))) {
                            try {
                                debugger.removeBreakpoint(Integer.decode("0x" + command.args.get(2)));
                                Logger.log(Logger.Type.INFO, "Breakpoint removed");
                            } catch (Exception e) {
                                Logger.log(Logger.Type.ERROR, "Error removing breakpoint : " + e.getMessage());
                            }
                        } else {
                            try {
                                BreakPoint.Type type = BreakPoint.Type.WRITE;
                                if ("/r".equals(command.args.get(2)))
                                    type = BreakPoint.Type.READ;

                                debugger.addBreakpoint(Integer.decode("0x" + command.args.get(1)), type);
                                Logger.log(Logger.Type.INFO, "Breakpoint created");
                            } catch (Exception e) {
                                Logger.log(Logger.Type.ERROR, "Error creating breakpoint : " + e.getMessage());
                            }
                        }
                    }
                    case "-r" -> {
                        try {
                            debugger.removeBreakpoint(Integer.decode("0x" + command.args.get(1)));
                            Logger.log(Logger.Type.INFO,  "Breakpoint removed");
                        } catch (Exception e) {
                            Logger.log(Logger.Type.ERROR,"Error removing breakpoint : " + e.getMessage());
                        }
                    }
                    default -> {
                        try {
                            debugger.addBreakpoint(Integer.decode("0x" + command.args.get(0)), BreakPoint.Type.EXEC);
                            Logger.log(Logger.Type.INFO,  "Breakpoint created");
                        } catch (Exception e) {
                            Logger.log(Logger.Type.ERROR,"Error creating breakpoint : " + e.getMessage());
                        }
                    }
                }
            }
            case "help" -> {
                Logger.log(Logger.Type.INFO, "================= break =================");
                Logger.log(Logger.Type.INFO, " break (-r)/(-m)/(-m -r) addr [/r or /w if -m]");
                Logger.log(Logger.Type.INFO, " -r : remove breakpoint at addr");
                Logger.log(Logger.Type.INFO, " -m : add memory breakpoint at addr");
                Logger.log(Logger.Type.INFO, " -m -r : remove memory breakpoint at addr");
                Logger.log(Logger.Type.INFO, " /r : memory breakpoint on read to addr (only if -m)");
                Logger.log(Logger.Type.INFO, " /w : memory breakpoint on write to addr (only if -m)");
                Logger.log(Logger.Type.INFO, " addr : address in hex, ex:C5F6");
            }
            default -> Logger.log(Logger.Type.WARNING, "Unknown command !");
        }
    }

    public static class Command {
        final String command;
        final List<String> args;

        public Command(String raw) {
            String[] split = raw.split(" ");
            args = new ArrayList<>();
            command = split[0];
            args.addAll(Arrays.asList(split).subList(1, split.length));
        }
    }

}