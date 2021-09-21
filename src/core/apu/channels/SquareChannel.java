package core.apu.channels;

import core.MMU;

public class SquareChannel {

    private final MMU memory;
    public int sample;

    public SquareChannel(MMU memory) {
        this.memory = memory;
    }

    public void clock() {
    }

    public void restart() {

    }

    public void tickLength() {
    }

    public void tickEnvelope() {
    }

    public void tickSweep() {
    }

    public void updateSample() {
    }
}
