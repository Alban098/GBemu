package gbemu.core.cpu;

import java.util.function.Supplier;

import static gbemu.core.BitUtils.signedByte;

public class Instruction {

    private final Supplier<Integer> fct_operate;

    //Const variables
    private String name;
    private int opcode;
    private int length;
    private Type type;

    //Instruction instance variables
    private int[] parameters;
    private int addr;
    private final State cpu;

    private String addr_str;
    private String memory_str;
    private String disassembled;
    private String comment;

    public Instruction(int opcode, Type type, String name, int length, Supplier<Integer> fct_operate, State cpu) {
        String[] split = name.split(" ");
        this.name = String.format("%1$-5s", split[0]).toLowerCase() + (split.length == 2 ? split[1] : "");
        this.length = length;
        this.opcode = opcode;
        this.fct_operate = fct_operate;
        if (length > 1)
            parameters = new int[length - 1];
        else
            parameters = null;
        addr = 0x00;
        this.cpu = cpu;
        this.type = type;
        updateStrings();
    }

    int operate() {
        return fct_operate.get();
    }

    public int getParamByte() {
        if (length > 1 && parameters != null)
            return parameters[0];
        return 0x00;
    }

    public int getParamWord() {
        if (length > 2 && parameters != null)
            return (parameters[1] << 8) | parameters[0];
        return 0x0000;
    }

    public int getParamAddress() {
        if (opcode == 0xE0 || opcode == 0xF0)
            return 0xFF00 | parameters[0];
        if (opcode == 0xE2 || opcode == 0xF2)
            return 0xFF00 | cpu.getBc().getLow().read();
        if (length == 2 && parameters != null)
            return parameters[0];
        else if (length == 3 && parameters != null)
            return (parameters[1] << 8) | parameters[0];
        if (name.contains("(BC)")) return cpu.getBc().read();
        if (name.contains("(DE)")) return cpu.getDe().read();
        if (name.contains("(HL)")) return cpu.getHl().read();
        if (name.contains("(HL+)")) return cpu.getHl().read();
        if (name.contains("(HL-)")) return cpu.getHl().read();
        return 0x00;
    }

    public void setParam(int index, int param) {
        if (parameters != null && index < parameters.length)
            parameters[index] = param;
    }

    public void setParams(int... params) {
        if (length > 1 && params.length > 0 && parameters != null)
            parameters[0] = params[0];
        if (length > 2 && params.length > 1 && parameters != null)
            parameters[1] = params[1];
    }

    public void updateStrings() {
        this.comment = "";
        boolean db = name.equals("db   ");
        boolean param_8 = (name.contains("d8") || name.contains("a8") || (name.contains("r8") && !name.contains("jr"))) && parameters != null;
        boolean rel_8 = name.contains("r8") && name.contains("jr") && parameters != null;
        boolean param_16 = (name.contains("d16") || name.contains("a16")) && parameters != null;

        this.addr_str = String.format("%04X", addr);
        this.memory_str = "";

        if (db && parameters != null) {
            for (int i = 0; i < 2; i++)
                this.memory_str += String.format("%02X ", parameters[i]);
            this.memory_str += "...";
            this.disassembled = "db   ";
            for (int parameter : parameters)
                this.disassembled += String.format("%02X ", parameter);
        } else {
            this.memory_str = String.format("%02X ", opcode);
            if (param_8) {
                this.memory_str += String.format("%02X  ", parameters[0]);
                this.disassembled = name.replaceAll(".8", String.format("%02X", parameters[0]));
            } else if (param_16) {
                this.memory_str += String.format("%02X %02X", parameters[0], parameters[1]);
                this.disassembled = name.replaceAll(".16", String.format("%04X", parameters[0] | (parameters[1] << 8)));
            } else if (rel_8) {
                this.memory_str += String.format("%02X", parameters[0]);
                this.disassembled = name.replaceAll("r8", String.format("%04X", addr + length + signedByte(parameters[0])));
            } else {
                this.disassembled = name;
            }
        }
        if (disassembled.contains("(BC)")) comment = cpu.getBc().toString();
        else if (disassembled.contains("(DE)")) comment = cpu.getDe().toString();
        else if (disassembled.contains("(HL)")) comment = cpu.getHl().toString();
        else if (disassembled.contains("(HL+)")) comment = cpu.getHl().toString();
        else  if (disassembled.contains("(HL-)")) comment = cpu.getHl().toString();

        if (disassembled.contains("FF00")) comment = "controller";
        else if (disassembled.contains("FF01")) comment = "serial bus";
        else if (disassembled.contains("FF02")) comment = "serial control";
        else if (disassembled.contains("FF04")) comment = "div";
        else if (disassembled.contains("FF05")) comment = "tima";
        else if (disassembled.contains("FF06")) comment = "tma";
        else if (disassembled.contains("FF07")) comment = "tac";
        else if (disassembled.contains("FF0F")) comment = "interrupt flags";
        else if (disassembled.contains("FF40")) comment = "lcdc";
        else if (disassembled.contains("FF41")) comment = "stat";
        else if (disassembled.contains("FF42")) comment = "scroll y";
        else if (disassembled.contains("FF43")) comment = "scroll x";
        else if (disassembled.contains("FF44")) comment = "ly";
        else if (disassembled.contains("FF45")) comment = "lyc";
        else if (disassembled.contains("FF46")) comment = "dma";
        else if (disassembled.contains("FF47")) comment = "bgp";
        else if (disassembled.contains("FF48")) comment = "obp0";
        else if (disassembled.contains("FF49")) comment = "obp1";
        else if (disassembled.contains("FF4A")) comment = "win x";
        else if (disassembled.contains("FF4B")) comment = "win y";
        else if (disassembled.contains("FFFF")) comment = "interrupt enable";
    }

    public void setAddr(int addr) {
        this.addr = addr & 0xFFFF;
    }

    @Override
    public String toString() {
        boolean db = name.equals("db   ");
        boolean param_8 = (name.contains("d8") || name.contains("a8") || (name.contains("r8") && !name.contains("jr"))) && parameters != null;
        boolean rel_8 = name.contains("r8") && name.contains("jr") && parameters != null;
        boolean param_16 = (name.contains("d16") || name.contains("a16")) && parameters != null;
        StringBuilder op;
        if (db && parameters != null) {
            op = new StringBuilder(String.format("%04X", addr) + ": ");
            for (int i = 0; i < 3; i++)
                op.append(String.format("%02X ", parameters[i]));
            op.append("+ | ").append(name);
            for (int parameter : parameters)
                op.append(String.format("%02X ", parameter));
        } else {
            op = new StringBuilder(String.format("$%04X", addr) + " : " + String.format("%02X ", opcode));
            if (param_8 && parameters != null)
                op.append(String.format("%02X   ", parameters[0])).append("   | ").append(name.replaceAll(".8", String.format("%02X", parameters[0])));
            else if (param_16 && parameters != null)
                op.append(String.format("%02X ", parameters[0])).append(String.format("%02X", parameters[1])).append("   | ").append(name.replaceAll(".16", String.format("%04X", parameters[0] | (parameters[1] << 8))));
            else if (rel_8 && parameters != null)
                op.append(String.format("%02X   ", parameters[0])).append("   | ").append(name.replaceAll("r8", String.format("%04X", addr + length + signedByte(parameters[0]))));
            else
                op.append("        | ").append(name);
        }
        while(op.length() <= 35)
            op.append(" ");
        if (op.toString().contains("(BC)")) op.append(cpu.getBc().toString());
        else if (op.toString().contains("(DE)")) op.append(cpu.getDe().toString());
        else if (op.toString().contains("(HL)")) op.append(cpu.getHl().toString());
        else if (op.toString().contains("(HL+)")) op.append(cpu.getHl().toString());
        else if (op.toString().contains("(HL-)")) op.append(cpu.getHl().toString());

        if (op.toString().contains("FF00")) op.append(" controller");
        else if (op.toString().contains("FF01")) op.append(" serial bus");
        else if (op.toString().contains("FF02")) op.append(" serial control");
        else if (op.toString().contains("FF04")) op.append(" div");
        else if (op.toString().contains("FF05")) op.append(" tima");
        else if (op.toString().contains("FF06")) op.append(" tma");
        else if (op.toString().contains("FF07")) op.append(" tac");
        else if (op.toString().contains("FF0F")) op.append(" interrupt flags");
        else if (op.toString().contains("FF40")) op.append(" lcdc");
        else if (op.toString().contains("FF41")) op.append(" stat");
        else if (op.toString().contains("FF42")) op.append(" scroll y");
        else if (op.toString().contains("FF43")) op.append(" scroll x");
        else if (op.toString().contains("FF44")) op.append(" ly");
        else if (op.toString().contains("FF45")) op.append(" lyc");
        else if (op.toString().contains("FF46")) op.append(" dma");
        else if (op.toString().contains("FF47")) op.append(" bgp");
        else if (op.toString().contains("FF48")) op.append(" obp0");
        else if (op.toString().contains("FF49")) op.append(" obp1");
        else if (op.toString().contains("FF4A")) op.append(" win x");
        else if (op.toString().contains("FF4B")) op.append(" win y");
        else if (op.toString().contains("FFFF")) op.append(" interrupt enable");
        return op.toString().toUpperCase();
    }

    public int getLength() {
        return length;
    }

    public void copyMeta(Instruction instruction) {
        addr = instruction.addr;
        name = instruction.name;
        type = instruction.type;
        opcode = instruction.opcode;
        length = instruction.length;
        if (length == 1 && parameters != null)
            parameters = null;
        else if (length == 2 && (parameters == null || parameters.length != 1))
            parameters = new int[1];
        else if (length == 3 && (parameters == null || parameters.length != 2))
            parameters = new int[2];
        if (parameters != null)
            System.arraycopy(instruction.parameters, 0, parameters, 0, parameters.length);
    }

    public void setLength(int length) {
        this.length = length;
        if (length > 1)
            parameters = new int[length - 1];
        else
            parameters = null;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setOpcode(int opcode) {
        this.opcode = opcode;
    }

    public int getAddr() {
        return addr;
    }

    public Type getType() {
        return type;
    }

    public String getAddrStr() {
        return addr_str;
    }

    public String getMemoryStr() {
        return memory_str;
    }

    public String getDisassembled() {
        return disassembled;
    }

    public String getComment() {
        return comment;
    }

    public enum Type {
        R,
        W,
        RW,
        JUMP,
        MISC
    }
}
