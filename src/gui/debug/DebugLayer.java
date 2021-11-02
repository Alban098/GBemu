package gui.debug;

import gbemu.extension.debug.Debugger;
import gui.std.Layer;

public abstract class DebugLayer extends Layer {

    protected final Debugger debugger;

    public DebugLayer(Debugger debugger) {
        super();
        this.debugger = debugger;
    }

}
