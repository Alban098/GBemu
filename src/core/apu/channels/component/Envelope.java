package core.apu.channels.component;

public class Envelope {

    private int elapsedTicks = 0;
    private int ticks = 0;
    private int volume = 0;
    private boolean increasing = false;

    public void setEnvelope(int ticks, int volume, boolean increase) {
        this.ticks = ticks;
        this.volume = volume;
        this.increasing = increase;

        elapsedTicks = 0;
    }

    public void clock() {
        if (ticks == 0)
            return;
        elapsedTicks++;
        elapsedTicks %= ticks;
        if (elapsedTicks == 0) {
            if (increasing && volume < 15)
                volume++;
            else if (!increasing && volume > 0)
                volume--;
        }
    }

    public int getVolume() {
        return volume;
    }
}
