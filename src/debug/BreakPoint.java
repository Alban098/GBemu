package debug;

public record BreakPoint(int address, debug.BreakPoint.Type type) {

    public enum Type {
        EXEC,
        WRITE,
        READ,
        RW,
        ALL;

        private static final String[] array = {EXEC.name(), WRITE.name(), READ.name(), RW.name(), ALL.name()};

        public static String[] stringArray() {
            return array;
        }
    }
}
