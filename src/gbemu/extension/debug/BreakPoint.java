package gbemu.extension.debug;

/**
 * A record representing a breakpoint containing its address and its access type
 */
public record BreakPoint(int address, gbemu.extension.debug.BreakPoint.Type type) {

    public enum Type {
        EXEC,
        WRITE,
        READ,
        RW,
        ALL;

        //An array containing all the types, used for UI purposes
        private static final String[] array = {EXEC.name(), WRITE.name(), READ.name(), RW.name(), ALL.name()};

        /**
         * Return the array of all possible Type as Strings
         * @return an array of all possible Type as Strings
         */
        public static String[] stringArray() {
            return array;
        }
    }
}
