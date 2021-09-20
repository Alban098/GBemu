package core;

public class BitUtils {

    public static int signedByte(int data) {
        if ((data & 0x80) == 0x80)
            return data - 0x100;
        else
            return data;
    }

    public static int signedWord(int data) {
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

    public static byte[] hexStringToAscii(String hex) {
        int l = hex.length();
        byte[] data = new byte[l / 2];
        for (int i = 0; i < l; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
