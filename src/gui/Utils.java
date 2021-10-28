package gui;

public class Utils {

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
