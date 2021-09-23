package debug;

import core.GameBoy;

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
        if (GameBoy.DEBUG) {
            switch (type) {
                case ERROR -> getInstance().lines.offer(new Line(Color.RED, string));
                case WARNING -> getInstance().lines.offer(new Line(Color.ORANGE, string));
                case INFO -> getInstance().lines.offer(new Line(Color.GREEN, string));
                case INPUT -> getInstance().lines.offer(new Line(Color.WHITE, string));
            }
            if (getInstance().lines.size() > MAX_LINES)
                getInstance().lines.poll();
        }
    }

    public static Queue<Line> getLines() {
        return getInstance().lines;
    }

    public Logger() {
        lines = new ConcurrentLinkedQueue<>();
    }

    public static class Line {
        private final Color color;
        private final String content;

        public Line(Color color, String content) {
            this.color = color;
            this.content = content;
        }

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
