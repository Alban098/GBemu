package gbemu.core.apu.components;

public class LengthCounter {

    private boolean stop_loop = false;
    private int length = 0;

    public void setLength(int value, boolean stop_loop) {
        length = length == 0 ? value : length;
        this.stop_loop = stop_loop;
    }

    public boolean clock() {
        length -= length != 0 ? 1 : 0;
        return !stop_loop || length != 0;
    }

    public void reset() {
        stop_loop = false;
        length = 0;
    }
}
