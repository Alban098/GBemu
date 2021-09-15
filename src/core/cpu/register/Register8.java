package core.cpu.register;

public class Register8 {

    private int data = 0x00;

    public Register8(int data) {
        this.data = data & 0xFF;
    }

    public Register8() {}

    public void write(int data) {
        this.data = data & 0xFF;
    }

    public int read() {
        return data;
    }

    public void inc() {
        data = (data + 1) & 0xFF;
    }

    public void dec() {
        data = (data - 1) & 0xFF;
    }

    @Override
    public String toString() {
        return "$" + Integer.toHexString(data);
    }
}
