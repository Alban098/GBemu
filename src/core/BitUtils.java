package core;

public class BitUtils {

    public static int signed8(int data) {
        if ((data & 0x80) == 0x80)
            return data - 0x100;
        else
            return data;
    }

    public static int signed16(int data) {
        if ((data & 0x8000) == 0x8000)
            return data - 0x10000;
        else
            return data;
    }

    public static int lsb(int data) {
        return data & 0xFF;
    }

    public static int msb(int data) {
        return (data & 0xFF00) >> 8;
    }

    public static void main(String[] args) {
        System.out.println(0xFF);
    }
}
