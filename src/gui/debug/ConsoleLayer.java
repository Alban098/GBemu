package gui.debug;

import console.LogLevel;
import console.commands.Command;
import console.Console;
import gbemu.extension.debug.Debugger;
import imgui.ImGui;
import imgui.type.ImString;
import threading.ConsoleThread;
import utils.Utils;

/**
 * This class represent the DebugLayer in charge of handling command inputs and outputs
 * alongside error reporting
 */
public class ConsoleLayer extends DebugLayer {

    private final ImString console_input = new ImString();
    private ConsoleThread console_thread;

    /**
     * Create a new instance of ConsoleLayer
     * @param debugger the debugger to link to
     */
    public ConsoleLayer(Debugger debugger) {
        super(debugger);
    }

    public void hookThread(ConsoleThread consoleThread) {
        this.console_thread = consoleThread;
    }

    /**
     * Render the layer to the screen
     * and propagate command to the console thread if needed
     */
    public void render() {
        ImGui.begin("Console");
        ImGui.beginChild("Scrolling", ImGui.getWindowWidth() - 15, ImGui.getWindowHeight() - 58, true);
        ImGui.setScrollY(ImGui.getScrollMaxY());
        for (Console.Line line : Console.getInstance().getLines())
            ImGui.textColored(line.getColor().getRed(), line.getColor().getGreen(), line.getColor().getBlue(), 255, Utils.getPrettifiedOutput(line.getContent(), (int) (ImGui.getWindowWidth() - 15 / ImGui.getFontSize())));
        ImGui.endChild();
        ImGui.pushItemWidth(ImGui.getWindowWidth() - 66);
        ImGui.inputText("##", console_input);
        ImGui.sameLine();
        if (ImGui.button("Enter")) {
            Console.getInstance().log(LogLevel.INPUT, "> " + console_input.get());

            console_thread.offerCommand(Command.build(console_input.toString()));
            synchronized (console_thread) {
                console_thread.notify();
            }

            console_input.set("");
        }
        ImGui.end();
    }
}