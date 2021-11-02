package gui.debug;

import console.Command;
import console.Console;
import console.Type;
import gbemu.extension.debug.Debugger;
import imgui.ImGui;
import imgui.type.ImString;
import threading.ConsoleThread;
import utils.Utils;


public class ConsoleLayer extends DebugLayer {

    private final ImString consoleInput = new ImString();
    private ConsoleThread consoleThread;

    public ConsoleLayer(Debugger debugger) {
        super(debugger);
    }

    public void hookThread(ConsoleThread consoleThread) {
        this.consoleThread = consoleThread;
    }

    public void render() {
        ImGui.begin("Console");
        ImGui.beginChild("Scrolling", ImGui.getWindowWidth() - 15, ImGui.getWindowHeight() - 58, true);
        ImGui.setScrollY(ImGui.getScrollMaxY());
        for (Console.Line line : Console.getInstance().getLines())
            ImGui.textColored(line.getColor().getRed(), line.getColor().getGreen(), line.getColor().getBlue(), 255, Utils.getPrettifiedOutput(line.getContent(), (int) (ImGui.getWindowWidth() - 15 / ImGui.getFontSize())));
        ImGui.endChild();
        ImGui.pushItemWidth(ImGui.getWindowWidth() - 66);
        ImGui.inputText("", consoleInput);
        ImGui.sameLine();
        if (ImGui.button("Enter")) {
            Console.getInstance().log(Type.INPUT, "> " + consoleInput.get());

            consoleThread.offerCommand(new Command(consoleInput.toString()));
            synchronized (consoleThread) {
                consoleThread.notify();
            }

            consoleInput.set("");
        }
        ImGui.end();
    }
}