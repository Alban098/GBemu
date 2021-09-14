package core.cpu.register;

public class Register16 {

    private final Register8 high;
    private final Register8 low;

    public Register16(int data) {
        high = new Register8((data & 0xFF00) << 8);
        low = new Register8(data & 0xFF);
    }

    public void write(int data) {
        high.write((data & 0xFF00) >> 8);
        low.write(data & 0xFF);
    }

    public int read() {
        return (high.read() << 8) | low.read();
    }

    public void writeHigh(int data) {
        high.write(data);
    }

    public void writeLow(int data) {
        low.write(data);
    }

    public int msb() {
        return high.read();
    }

    public int lsb() {
        return low.read();
    }

    public Register8 getHigh() {
        return high;
    }

    public Register8 getLow() {
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
}
