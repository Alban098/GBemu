package core.apu.channels.component;

public class LengthCounter {

    private boolean stopLoop = false;
    private int length = 0;

    public void setLength(int value, boolean stopLoop) {
        length = length == 0 ? value : length;
        this.stopLoop = stopLoop;
    }

    public boolean clock() {
        length -= length != 0 ? 1 : 0;
        return !stopLoop || length != 0;
    }
}
