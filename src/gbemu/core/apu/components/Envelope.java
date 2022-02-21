package gbemu.core.apu.components;

public class Envelope {

    private int elapsed_ticks = 0;
    private int ticks = 0;
    private int volume = 0;
    private boolean increasing = false;

    public void setEnvelope(int ticks, int volume, boolean increase) {
        this.ticks = ticks;
        this.volume = volume;
        this.increasing = increase;

        elapsed_ticks = 0;
    }

    public void clock() {
        if (ticks == 0)
            return;
        elapsed_ticks++;
        elapsed_ticks %= ticks;
        if (elapsed_ticks == 0) {
            if (increasing && volume < 15)
                volume++;
            else if (!increasing && volume > 0)
                volume--;
        }
    }

    public int getVolume() {
        return volume;
    }

    public void reset() {
        elapsed_ticks = 0;
        ticks = 0;
        volume = 0;
        increasing = false;
    }
}
