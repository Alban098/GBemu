package gui;

import debug.Debugger;

public abstract class AbstractDebugLayer {

    private boolean visible = false;

    protected final Debugger debugger;

    public AbstractDebugLayer(Debugger debugger) {
        this.debugger = debugger;
    }

    public abstract void render();

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
