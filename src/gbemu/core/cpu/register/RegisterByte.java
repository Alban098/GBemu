package gbemu.core.cpu.register;

public class RegisterByte {

    private int data;

    public RegisterByte(int data) {
        this.data = data & 0xFF;
    }

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
        return "$" + String.format("%02X", data);
    }
}
