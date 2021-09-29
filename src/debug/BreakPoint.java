package debug;

public record BreakPoint(int address, debug.BreakPoint.Type type) {

    public enum Type {
        EXEC,
        WRITE,
        READ
    }
}
