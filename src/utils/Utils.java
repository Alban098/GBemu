package utils;

/**
 * This class regroups useful methods used all over the project
 */
public class Utils {

    /**
     * Convert a RAW string to a multiline one
     * @param input the RAW input
     * @param lineLength number of character in each line
     * @return the prettified output
     */
    public static String getPrettifiedOutput(String input, int lineLength) {
        StringBuilder sb = new StringBuilder();
        for (String line : input.lines().toList()) {
            if (line.length() > lineLength) {
                for (int i = 1; i <= line.length(); i++) {
                    sb.append(line.charAt(i-1));
                    if (i % lineLength == 0)
                        sb.append("\n");
                }
                sb.append("\n");
            } else {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * Convert from an N bits unsigned Integer to its binary representation
     * @param value the RAW value to convert
     * @param nbBits how many bits to consider for conversion
     * @return the N bits String representation
     */
    public static String binaryString(int value, int nbBits) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < nbBits; i++) {
            if (i != 0 && i % 4 == 0)
                s.append(" ");
            s.append(((value & 0x80) != 0) ? "1" : "0");
            value <<= 1;
        }
        return s.toString();
    }
}
