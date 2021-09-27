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
}
