package gbemu.core;

public class BitUtils {

    public static int signedByte(int data) {
        data &= 0xFF;
        if ((data & 0x80) == 0x80)
            return data - 0x100;
        else
            return data;
    }

    public static int lsb(int data) {
        return data & 0xFF;
    }

    public static int msb(int data) {
        return (data & 0xFF00) >> 8;
    }

    public static boolean inRange(int val, int lower, int upper) {
        return val <= upper && val >= lower;
    }
}
