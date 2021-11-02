package gbemu.core.apu.components;

public class LengthCounter {

    private boolean stopLoop = false;
    private int length = 0;

    public void setLength(int value, boolean stopLoop) {
        length = length == 0 ? value : length;
        this.stopLoop = stopLoop;
    }

    public boolean clock() {
        length -= length != 0 ? 1 : 0;
        return !(length == 0 || stopLoop);
    }

    public void reset() {
        stopLoop = false;
        length = 0;
    }
}
