package gbemu.core;

/**
 * This class contains some utility function to simplify 8-bit and 16-bit data treatment
 */
public class BitUtils {

    /**
     * Treats an int binary representation as an 8-bit signed value, used for relative addressing that uses 8-bit 2's complemented values
     * example : (1111 1111) as 8-bit is -1 but is treated as 255 as an int
     * so int will be converted to (1111 1111 1111 1111 1111 1111 1111 1111) which is the right representation of -1 as a 32-bit integer
     * @param data the data to convert
     * @return the converted data
     */
    public static int signedByte(int data) {
        data &= 0xFF;
        if ((data & 0x80) == 0x80)
            return data - 0x100;
        else
            return data;
    }

    /**
     * Return the 8 least significant bits of a 16-bit values
     * @param data the data to extract lsb from
     * @return the 8 least significant bits of the 16-bit values
     */
    public static int lsb(int data) {
        return data & 0xFF;
    }

    /**
     * Return the 8 most significant bits of a 16-bit values
     * @param data the data to extract lsb from
     * @return the 8 most significant bits of the 16-bit values
     */
    public static int msb(int data) {
        return (data & 0xFF00) >> 8;
    }

    /**
     * Return whether a value is in a specified range or not
     * @param val the value to test
     * @param lower the lower bound of the interval (included)
     * @param upper the upper bound of the interval (included)
     * @return is the value in the range
     */
    public static boolean inRange(int val, int lower, int upper) {
        return val <= upper && val >= lower;
    }
}
