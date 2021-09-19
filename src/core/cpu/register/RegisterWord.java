package core.cpu.register;

public class RegisterWord {

    private final RegisterByte high;
    private final RegisterByte low;

    public RegisterWord(int data) {
        high = new RegisterByte((data & 0xFF00) >> 8);
        low = new RegisterByte(data & 0xFF);
    }

    public void write(int data) {
        high.write((data & 0xFF00) >> 8);
        low.write(data & 0xFF);
    }

    public int read() {
        return (high.read() << 8) | low.read();
    }

    public int read(boolean increment) {
        int r = (high.read() << 8) | low.read();
        inc();
        return r;
    }

    public RegisterByte getHigh() {
        return high;
    }

    public RegisterByte getLow() {
        return low;
    }

    public void inc() {
        low.inc();
        if (low.read() == 0x00)
            high.inc();
    }

    public void dec() {
        low.dec();
        if (low.read() == 0xFF)
            high.dec();
    }

    @Override
    public String toString() {
        return "$" + String.format("%04X", low.read() | (high.read() << 8));
    }
}
