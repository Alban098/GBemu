package debug;

import java.awt.*;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Logger {

    public static final int MAX_LINES = 100;
    private static Logger instance;

    private final Queue<Line> lines;

    public static Logger getInstance() {
        if (instance == null)
            instance = new Logger();
        return instance;
    }

    public static void log(Type type, String string) {
        switch (type) {
            case ERROR -> getInstance().lines.offer(new Line(Color.RED, string));
            case WARNING -> getInstance().lines.offer(new Line(Color.ORANGE, string));
            case INFO -> getInstance().lines.offer(new Line(Color.GREEN, string));
            case INPUT -> getInstance().lines.offer(new Line(Color.WHITE, string));
        }
        if (getInstance().lines.size() > MAX_LINES)
            getInstance().lines.poll();
    }

    public static Queue<Line> getLines() {
        return getInstance().lines;
    }

    public Logger() {
        lines = new ConcurrentLinkedQueue<>();
    }

    public record Line(Color color, String content) {

        public Color getColor() {
            return color;
        }

        public String getContent() {
            return content;
        }
    }

    public enum Type {
        ERROR,
        WARNING,
        INFO,
        INPUT
    }
}
