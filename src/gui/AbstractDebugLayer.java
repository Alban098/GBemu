package gui;

import core.GameBoy;

public abstract class AbstractDebugLayer {

    private boolean visible = false;

    protected final GameBoy gameboy;

    public AbstractDebugLayer(GameBoy gameboy) {
        this.gameboy = gameboy;
    }

    public abstract void render();

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
