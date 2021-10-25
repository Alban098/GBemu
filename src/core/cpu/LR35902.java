package core.cpu;

import core.Flags;
import core.GameBoy;
import core.memory.MMU;
import core.cpu.register.RegisterByte;
import core.cpu.register.RegisterWord;
import debug.Debugger;
import debug.DebuggerMode;

import java.util.*;

import static core.BitUtils.*;

public class LR35902 {

    public final List<Instruction> opcodes;
    public final List<Instruction> cb_opcodes;

    private final RegisterWord af;
    protected final RegisterWord bc;
    protected final RegisterWord de;
    protected final RegisterWord hl;
    private final RegisterByte a;
    private final RegisterByte f;
    private final RegisterByte b;
    protected final RegisterByte c;
    private final RegisterByte d;
    private final RegisterByte e;
    private final RegisterByte h;
    private final RegisterByte l;
    private final RegisterWord sp;
    private final RegisterWord pc;

    private final RegisterByte tmp_reg;
    private final State state;
    private final MMU memory;

    private int opcode_mcycle = 0;
    private boolean halted = false;
    private boolean ime = false;
    private Instruction next_instr;

    private final Debugger debugger;
    private boolean bootstrap_enabled = false;
    private final GameBoy gameboy;

    public LR35902(GameBoy gameboy) {
        this.gameboy = gameboy;
        af = new RegisterWord(0x01B0);
        a = af.getHigh();
        f = af.getLow();

        bc = new RegisterWord(0x0013);
        b = bc.getHigh();
        c = bc.getLow();

        de = new RegisterWord(0x00D8);
        d = de.getHigh();
        e = de.getLow();

        hl = new RegisterWord(0x014D);
        h = hl.getHigh();
        l = hl.getLow();

        sp = new RegisterWord(0xFFFE);
        pc = new RegisterWord(bootstrap_enabled ? 0 : 0x0100);

        tmp_reg = new RegisterByte(0x00);
        this.memory = gameboy.getMemory();
        state = new State();

        debugger = gameboy.getDebugger();
        debugger.link(state);

        opcodes = new ArrayList<>();
        opcodes.add(new Instruction(0x00, Instruction.Type.MISC, "NOP", 1, this::opcode_0x00_nop, state));
        opcodes.add(new Instruction(0x01, Instruction.Type.MISC, "LD BC,d16", 3, this::opcode_0x01_ld, state));
        opcodes.add(new Instruction(0x02, Instruction.Type.W, "LD (BC),A", 1, this::opcode_0x02_ld, state));
        opcodes.add(new Instruction(0x03, Instruction.Type.MISC, "INC BC", 1, this::opcode_0x03_inc, state));
        opcodes.add(new Instruction(0x04, Instruction.Type.MISC, "INC B", 1, this::opcode_0X04_inc, state));
        opcodes.add(new Instruction(0x05, Instruction.Type.MISC, "DEC B", 1, this::opcode_0x05_dec, state));
        opcodes.add(new Instruction(0x06, Instruction.Type.R, "LD B,d8", 2, this::opcode_0x06_ld, state));
        opcodes.add(new Instruction(0x07, Instruction.Type.MISC, "RLCA", 1, this::opcode_0x07_rlca, state));
        opcodes.add(new Instruction(0x08, Instruction.Type.W, "LD (d16),SP", 3, this::opcode_0x08_ld, state));
        opcodes.add(new Instruction(0x09, Instruction.Type.MISC, "ADD HL,BC", 1, this::opcode_0x09_add, state));
        opcodes.add(new Instruction(0x0A, Instruction.Type.R, "LD A,(BC)", 1, this::opcode_0x0A_ld, state));
        opcodes.add(new Instruction(0x0B, Instruction.Type.MISC, "DEC BC", 1, this::opcode_0x0B_dec, state));
        opcodes.add(new Instruction(0x0C, Instruction.Type.MISC, "INC C", 1, this::opcode_0x0C_inc, state));
        opcodes.add(new Instruction(0x0D, Instruction.Type.MISC, "DEC C", 1, this::opcode_0x0D_dec, state));
        opcodes.add(new Instruction(0x0E, Instruction.Type.MISC, "LD C,d8", 2, this::opcode_0x0E_ld, state));
        opcodes.add(new Instruction(0x0F, Instruction.Type.MISC, "RRCA", 1, this::opcode_0x0F_rrca, state));
        opcodes.add(new Instruction(0x10, Instruction.Type.MISC, "STOP", 2, this::opcode_0x10_stop, state));
        opcodes.add(new Instruction(0x11, Instruction.Type.MISC, "LD DE,d16", 3, this::opcode_0x11_ld, state));
        opcodes.add(new Instruction(0x12, Instruction.Type.W, "LD (DE),A", 1, this::opcode_0x12_ld, state));
        opcodes.add(new Instruction(0x13, Instruction.Type.MISC, "INC DE", 1, this::opcode_0x13_inc, state));
        opcodes.add(new Instruction(0x14, Instruction.Type.MISC, "INC D", 1, this::opcode_0x14_inc, state));
        opcodes.add(new Instruction(0x15, Instruction.Type.MISC, "DEC D", 1, this::opcode_0x15_dec, state));
        opcodes.add(new Instruction(0x16, Instruction.Type.MISC, "LD D,d8", 2, this::opcode_0x16_ld, state));
        opcodes.add(new Instruction(0x17, Instruction.Type.MISC, "RLA", 1, this::opcode_0x17_rla, state));
        opcodes.add(new Instruction(0x18, Instruction.Type.JUMP, "JR r8", 2, this::opcode_0x18_jr, state));
        opcodes.add(new Instruction(0x19, Instruction.Type.MISC, "ADD HL,DE", 1, this::opcode_0x19_add, state));
        opcodes.add(new Instruction(0x1A, Instruction.Type.R, "LD A,(DE)", 1, this::opcode_0x1A_ld, state));
        opcodes.add(new Instruction(0x1B, Instruction.Type.MISC, "DEC DE", 1, this::opcode_0x1B_dec, state));
        opcodes.add(new Instruction(0x1C, Instruction.Type.MISC, "INC E", 1, this::opcode_0x1C_inc, state));
        opcodes.add(new Instruction(0x1D, Instruction.Type.MISC, "DEC E", 1, this::opcode_0x1D_dec, state));
        opcodes.add(new Instruction(0x1E, Instruction.Type.MISC, "LD E,d8", 2, this::opcode_0x1E_ld, state));
        opcodes.add(new Instruction(0x1F, Instruction.Type.MISC, "RRA", 1, this::opcode_0x1F_rra, state));
        opcodes.add(new Instruction(0x20, Instruction.Type.JUMP, "JR NZ,r8", 2, this::opcode_0x20_jr, state));
        opcodes.add(new Instruction(0x21, Instruction.Type.MISC, "LD HL,d16", 3, this::opcode_0x21_ld, state));
        opcodes.add(new Instruction(0x22, Instruction.Type.W, "LD (HL+),A", 1, this::opcode_0x22_ld, state));
        opcodes.add(new Instruction(0x23, Instruction.Type.MISC, "INC HL", 1, this::opcode_0x23_inc, state));
        opcodes.add(new Instruction(0x24, Instruction.Type.MISC, "INC H", 1, this::opcode_0x24_inc, state));
        opcodes.add(new Instruction(0x25, Instruction.Type.MISC, "DEC H", 1, this::opcode_0x25_dec, state));
        opcodes.add(new Instruction(0x26, Instruction.Type.MISC, "LD H,d8", 2, this::opcode_0x26_ld, state));
        opcodes.add(new Instruction(0x27, Instruction.Type.MISC, "DAA", 1, this::opcode_0x27_daa, state));
        opcodes.add(new Instruction(0x28, Instruction.Type.JUMP, "JR Z,r8", 2, this::opcode_0x28_jr, state));
        opcodes.add(new Instruction(0x29, Instruction.Type.MISC, "ADD HL,HL", 1, this::opcode_0x29_add, state));
        opcodes.add(new Instruction(0x2A, Instruction.Type.R, "LD A,(HL+)", 1, this::opcode_0x2A_ld, state));
        opcodes.add(new Instruction(0x2B, Instruction.Type.MISC, "DEC HL", 1, this::opcode_0x2B_dec, state));
        opcodes.add(new Instruction(0x2C, Instruction.Type.MISC, "INC L", 1, this::opcode_0x2C_inc, state));
        opcodes.add(new Instruction(0x2D, Instruction.Type.MISC, "DEC L", 1, this::opcode_0x2D_dec, state));
        opcodes.add(new Instruction(0x2E, Instruction.Type.MISC, "LD L,d8", 2, this::opcode_0x2E_ld, state));
        opcodes.add(new Instruction(0x2F, Instruction.Type.MISC, "CPL", 1, this::opcode_0x2F_cpl, state));
        opcodes.add(new Instruction(0x30, Instruction.Type.JUMP, "JR NC,r8", 2, this::opcode_0x30_jr, state));
        opcodes.add(new Instruction(0x31, Instruction.Type.MISC, "LD SP,d16", 3, this::opcode_0x31_ld, state));
        opcodes.add(new Instruction(0x32, Instruction.Type.W, "LD (HL-),A", 1, this::opcode_0x32_ld, state));
        opcodes.add(new Instruction(0x33, Instruction.Type.MISC, "INC SP", 1, this::opcode_0x33_inc, state));
        opcodes.add(new Instruction(0x34, Instruction.Type.W, "INC (HL)", 1, this::opcode_0x34_inc, state));
        opcodes.add(new Instruction(0x35, Instruction.Type.W, "DEC (HL)", 1, this::opcode_0x35_dec, state));
        opcodes.add(new Instruction(0x36, Instruction.Type.W, "LD (HL),d8", 2, this::opcode_0x36_ld, state));
        opcodes.add(new Instruction(0x37, Instruction.Type.MISC, "SCF", 1, this::opcode_0x37_scf, state));
        opcodes.add(new Instruction(0x38, Instruction.Type.JUMP, "JR C,r8", 2, this::opcode_0x38_jr, state));
        opcodes.add(new Instruction(0x39, Instruction.Type.MISC, "ADD HL,SP", 1, this::opcode_0x39_add, state));
        opcodes.add(new Instruction(0x3A, Instruction.Type.R, "LD A,(HL-)", 1, this::opcode_0x3A_ld, state));
        opcodes.add(new Instruction(0x3B, Instruction.Type.MISC, "DEC SP", 1, this::opcode_0x3B_dec, state));
        opcodes.add(new Instruction(0x3C, Instruction.Type.MISC, "INC A", 1, this::opcode_0x3C_inc, state));
        opcodes.add(new Instruction(0x3D, Instruction.Type.MISC, "DEC A", 1, this::opcode_0x3D_dec, state));
        opcodes.add(new Instruction(0x3E, Instruction.Type.MISC, "LD A,d8", 2, this::opcode_0x3E_ld, state));
        opcodes.add(new Instruction(0x3F, Instruction.Type.MISC, "CCF", 1, this::opcode_0x3F_ccf, state));
        opcodes.add(new Instruction(0x40, Instruction.Type.MISC, "LD B,B", 1, this::opcode_0x40_ld, state));
        opcodes.add(new Instruction(0x41, Instruction.Type.MISC, "LD B,C", 1, this::opcode_0x41_ld, state));
        opcodes.add(new Instruction(0x42, Instruction.Type.MISC, "LD B,D", 1, this::opcode_0x42_ld, state));
        opcodes.add(new Instruction(0x43, Instruction.Type.MISC, "LD B,E", 1, this::opcode_0x43_ld, state));
        opcodes.add(new Instruction(0x44, Instruction.Type.MISC, "LD B,H", 1, this::opcode_0x44_ld, state));
        opcodes.add(new Instruction(0x45, Instruction.Type.MISC, "LD B,L", 1, this::opcode_0x45_ld, state));
        opcodes.add(new Instruction(0x46, Instruction.Type.R, "LD B,(HL)", 1, this::opcode_0x46_ld, state));
        opcodes.add(new Instruction(0x47, Instruction.Type.MISC, "LD B,A", 1, this::opcode_0x47_ld, state));
        opcodes.add(new Instruction(0x48, Instruction.Type.MISC, "LD C,B", 1, this::opcode_0x48_ld, state));
        opcodes.add(new Instruction(0x49, Instruction.Type.MISC, "LD C,C", 1, this::opcode_0x49_ld, state));
        opcodes.add(new Instruction(0x4A, Instruction.Type.MISC, "LD C,D", 1, this::opcode_0x4A_ld, state));
        opcodes.add(new Instruction(0x4B, Instruction.Type.MISC, "LD C,E", 1, this::opcode_0x4B_ld, state));
        opcodes.add(new Instruction(0x4C, Instruction.Type.MISC, "LD C,H", 1, this::opcode_0x4C_ld, state));
        opcodes.add(new Instruction(0x4D, Instruction.Type.MISC, "LD C,L", 1, this::opcode_0x4D_ld, state));
        opcodes.add(new Instruction(0x4E, Instruction.Type.R, "LD C,(HL)", 1, this::opcode_0x4E_ld, state));
        opcodes.add(new Instruction(0x4F, Instruction.Type.MISC, "LD C,A", 1, this::opcode_0x4F_ld, state));
        opcodes.add(new Instruction(0x50, Instruction.Type.MISC, "LD D,B", 1, this::opcode_0x50_ld, state));
        opcodes.add(new Instruction(0x51, Instruction.Type.MISC, "LD D,C", 1, this::opcode_0x51_ld, state));
        opcodes.add(new Instruction(0x52, Instruction.Type.MISC, "LD D,D", 1, this::opcode_0x52_ld, state));
        opcodes.add(new Instruction(0x53, Instruction.Type.MISC, "LD D,E", 1, this::opcode_0x53_ld, state));
        opcodes.add(new Instruction(0x54, Instruction.Type.MISC, "LD D,H", 1, this::opcode_0x54_ld, state));
        opcodes.add(new Instruction(0x55, Instruction.Type.MISC, "LD D,L", 1, this::opcode_0x55_ld, state));
        opcodes.add(new Instruction(0x56, Instruction.Type.R, "LD D,(HL)", 1, this::opcode_0x56_ld, state));
        opcodes.add(new Instruction(0x57, Instruction.Type.MISC, "LD D,A", 1, this::opcode_0x57_ld, state));
        opcodes.add(new Instruction(0x58, Instruction.Type.MISC, "LD E,B", 1, this::opcode_0x58_ld, state));
        opcodes.add(new Instruction(0x59, Instruction.Type.MISC, "LD E,C", 1, this::opcode_0x59_ld, state));
        opcodes.add(new Instruction(0x5A, Instruction.Type.MISC, "LD E,D", 1, this::opcode_0x5A_ld, state));
        opcodes.add(new Instruction(0x5B, Instruction.Type.MISC, "LD E,E", 1, this::opcode_0x5B_ld, state));
        opcodes.add(new Instruction(0x5C, Instruction.Type.MISC, "LD E,H", 1, this::opcode_0x5C_ld, state));
        opcodes.add(new Instruction(0x5D, Instruction.Type.MISC, "LD E,L", 1, this::opcode_0x5D_ld, state));
        opcodes.add(new Instruction(0x5E, Instruction.Type.R, "LD E,(HL)", 1, this::opcode_0x5E_ld, state));
        opcodes.add(new Instruction(0x5F, Instruction.Type.MISC, "LD E,A", 1, this::opcode_0x5F_ld, state));
        opcodes.add(new Instruction(0x60, Instruction.Type.MISC, "LD H,B", 1, this::opcode_0x60_ld, state));
        opcodes.add(new Instruction(0x61, Instruction.Type.MISC, "LD H,C", 1, this::opcode_0x61_ld, state));
        opcodes.add(new Instruction(0x62, Instruction.Type.MISC, "LD H,D", 1, this::opcode_0x62_ld, state));
        opcodes.add(new Instruction(0x63, Instruction.Type.MISC, "LD H,E", 1, this::opcode_0x63_ld, state));
        opcodes.add(new Instruction(0x64, Instruction.Type.MISC, "LD H,H", 1, this::opcode_0x64_ld, state));
        opcodes.add(new Instruction(0x65, Instruction.Type.MISC, "LD H,L", 1, this::opcode_0x65_ld, state));
        opcodes.add(new Instruction(0x66, Instruction.Type.R, "LD H,(HL)", 1, this::opcode_0x66_ld, state));
        opcodes.add(new Instruction(0x67, Instruction.Type.MISC, "LD H,A", 1, this::opcode_0x67_ld, state));
        opcodes.add(new Instruction(0x68, Instruction.Type.MISC, "LD L,B", 1, this::opcode_0x68_ld, state));
        opcodes.add(new Instruction(0x69, Instruction.Type.MISC, "LD L,C", 1, this::opcode_0x69_ld, state));
        opcodes.add(new Instruction(0x6A, Instruction.Type.MISC, "LD L,D", 1, this::opcode_0x6A_ld, state));
        opcodes.add(new Instruction(0x6B, Instruction.Type.MISC, "LD L,E", 1, this::opcode_0x6B_ld, state));
        opcodes.add(new Instruction(0x6C, Instruction.Type.MISC, "LD L,H", 1, this::opcode_0x6C_ld, state));
        opcodes.add(new Instruction(0x6D, Instruction.Type.MISC, "LD L,L", 1, this::opcode_0x6D_ld, state));
        opcodes.add(new Instruction(0x6E, Instruction.Type.R, "LD L,(HL)", 1, this::opcode_0x6E_ld, state));
        opcodes.add(new Instruction(0x6F, Instruction.Type.MISC, "LD L,A", 1, this::opcode_0x6F_ld, state));
        opcodes.add(new Instruction(0x70, Instruction.Type.W, "LD (HL),B", 1, this::opcode_0x70_ld, state));
        opcodes.add(new Instruction(0x71, Instruction.Type.W, "LD (HL),C", 1, this::opcode_0x71_ld, state));
        opcodes.add(new Instruction(0x72, Instruction.Type.W, "LD (HL),D", 1, this::opcode_0x72_ld, state));
        opcodes.add(new Instruction(0x73, Instruction.Type.W, "LD (HL),E", 1, this::opcode_0x73_ld, state));
        opcodes.add(new Instruction(0x74, Instruction.Type.W, "LD (HL),H", 1, this::opcode_0x74_ld, state));
        opcodes.add(new Instruction(0x75, Instruction.Type.W, "LD (HL),L", 1, this::opcode_0x75_ld, state));
        opcodes.add(new Instruction(0x76, Instruction.Type.MISC, "HALT", 1, this::opcode_0x76_halt, state));
        opcodes.add(new Instruction(0x77, Instruction.Type.W, "LD (HL),A", 1, this::opcode_0x77_ld, state));
        opcodes.add(new Instruction(0x78, Instruction.Type.MISC, "LD A,B", 1, this::opcode_0x78_ld, state));
        opcodes.add(new Instruction(0x79, Instruction.Type.MISC, "LD A,C", 1, this::opcode_0x79_ld, state));
        opcodes.add(new Instruction(0x7A, Instruction.Type.MISC, "LD A,D", 1, this::opcode_0x7A_ld, state));
        opcodes.add(new Instruction(0x7B, Instruction.Type.MISC, "LD A,E", 1, this::opcode_0x7B_ld, state));
        opcodes.add(new Instruction(0x7C, Instruction.Type.MISC, "LD A,H", 1, this::opcode_0x7C_ld, state));
        opcodes.add(new Instruction(0x7D, Instruction.Type.MISC, "LD A,L", 1, this::opcode_0x7D_ld, state));
        opcodes.add(new Instruction(0x7E, Instruction.Type.R, "LD A,(HL)", 1, this::opcode_0x7E_ld, state));
        opcodes.add(new Instruction(0x7F, Instruction.Type.MISC, "LD A,A", 1, this::opcode_0x7F_ld, state));
        opcodes.add(new Instruction(0x80, Instruction.Type.MISC, "ADD A,B", 1, this::opcode_0x80_add, state));
        opcodes.add(new Instruction(0x81, Instruction.Type.MISC, "ADD A,C", 1, this::opcode_0x81_add, state));
        opcodes.add(new Instruction(0x82, Instruction.Type.MISC, "ADD A,D", 1, this::opcode_0x82_add, state));
        opcodes.add(new Instruction(0x83, Instruction.Type.MISC, "ADD A,E", 1, this::opcode_0x83_add, state));
        opcodes.add(new Instruction(0x84, Instruction.Type.MISC, "ADD A,H", 1, this::opcode_0x84_add, state));
        opcodes.add(new Instruction(0x85, Instruction.Type.MISC, "ADD A,L", 1, this::opcode_0x85_add, state));
        opcodes.add(new Instruction(0x86, Instruction.Type.R, "ADD A,(HL)", 1, this::opcode_0x86_add, state));
        opcodes.add(new Instruction(0x87, Instruction.Type.MISC, "ADD A,A", 1, this::opcode_0x87_add, state));
        opcodes.add(new Instruction(0x88, Instruction.Type.MISC, "ADC A,B", 1, this::opcode_0x88_adc, state));
        opcodes.add(new Instruction(0x89, Instruction.Type.MISC, "ADC A,C", 1, this::opcode_0x89_adc, state));
        opcodes.add(new Instruction(0x8A, Instruction.Type.MISC, "ADC A,D", 1, this::opcode_0x8A_adc, state));
        opcodes.add(new Instruction(0x8B, Instruction.Type.MISC, "ADC A,E", 1, this::opcode_0x8B_adc, state));
        opcodes.add(new Instruction(0x8C, Instruction.Type.MISC, "ADC A,H", 1, this::opcode_0x8C_adc, state));
        opcodes.add(new Instruction(0x8D, Instruction.Type.MISC, "ADC A,L", 1, this::opcode_0x8D_adc, state));
        opcodes.add(new Instruction(0x8E, Instruction.Type.R, "ADC A,(HL)", 1, this::opcode_0x8E_adc, state));
        opcodes.add(new Instruction(0x8F, Instruction.Type.MISC, "ADC A,A", 1, this::opcode_0x8F_adc, state));
        opcodes.add(new Instruction(0x90, Instruction.Type.MISC, "SUB B", 1, this::opcode_0x90_sub, state));
        opcodes.add(new Instruction(0x91, Instruction.Type.MISC, "SUB C", 1, this::opcode_0x91_sub, state));
        opcodes.add(new Instruction(0x92, Instruction.Type.MISC, "SUB D", 1, this::opcode_0x92_sub, state));
        opcodes.add(new Instruction(0x93, Instruction.Type.MISC, "SUB E", 1, this::opcode_0x93_sub, state));
        opcodes.add(new Instruction(0x94, Instruction.Type.MISC, "SUB H", 1, this::opcode_0x94_sub, state));
        opcodes.add(new Instruction(0x95, Instruction.Type.MISC, "SUB L", 1, this::opcode_0x95_sub, state));
        opcodes.add(new Instruction(0x96, Instruction.Type.R, "SUB (HL)", 1, this::opcode_0x96_sub, state));
        opcodes.add(new Instruction(0x97, Instruction.Type.MISC, "SUB A", 1, this::opcode_0x97_sub, state));
        opcodes.add(new Instruction(0x98, Instruction.Type.MISC, "SBC A,B", 1, this::opcode_0x98_sbc, state));
        opcodes.add(new Instruction(0x99, Instruction.Type.MISC, "SBC A,C", 1, this::opcode_0x99_sbc, state));
        opcodes.add(new Instruction(0x9A, Instruction.Type.MISC, "SBC A,D", 1, this::opcode_0x9A_sbc, state));
        opcodes.add(new Instruction(0x9B, Instruction.Type.MISC, "SBC A,E", 1, this::opcode_0x9B_sbc, state));
        opcodes.add(new Instruction(0x9C, Instruction.Type.MISC, "SBC A,H", 1, this::opcode_0x9C_sbc, state));
        opcodes.add(new Instruction(0x9D, Instruction.Type.MISC, "SBC A,L", 1, this::opcode_0x9D_sbc, state));
        opcodes.add(new Instruction(0x9E, Instruction.Type.R, "SBC A,(HL)", 1, this::opcode_0x9E_sbc, state));
        opcodes.add(new Instruction(0x9F, Instruction.Type.MISC, "SBC A,A", 1, this::opcode_0x9F_sbc, state));
        opcodes.add(new Instruction(0xA0, Instruction.Type.MISC, "AND B", 1, this::opcode_0xA0_and, state));
        opcodes.add(new Instruction(0xA1, Instruction.Type.MISC, "AND C", 1, this::opcode_0xA1_and, state));
        opcodes.add(new Instruction(0xA2, Instruction.Type.MISC, "AND D", 1, this::opcode_0xA2_and, state));
        opcodes.add(new Instruction(0xA3, Instruction.Type.MISC, "AND E", 1, this::opcode_0xA3_and, state));
        opcodes.add(new Instruction(0xA4, Instruction.Type.MISC, "AND H", 1, this::opcode_0xA4_and, state));
        opcodes.add(new Instruction(0xA5, Instruction.Type.MISC, "AND L", 1, this::opcode_0xA5_and, state));
        opcodes.add(new Instruction(0xA6, Instruction.Type.R, "AND (HL)", 1, this::opcode_0xA6_and, state));
        opcodes.add(new Instruction(0xA7, Instruction.Type.MISC, "AND A", 1, this::opcode_0xA7_and, state));
        opcodes.add(new Instruction(0xA8, Instruction.Type.MISC, "XOR B", 1, this::opcode_0xA8_xor, state));
        opcodes.add(new Instruction(0xA9, Instruction.Type.MISC, "XOR C", 1, this::opcode_0xA9_xor, state));
        opcodes.add(new Instruction(0xAA, Instruction.Type.MISC, "XOR D", 1, this::opcode_0xAA_xor, state));
        opcodes.add(new Instruction(0xAB, Instruction.Type.MISC, "XOR E", 1, this::opcode_0xAB_xor, state));
        opcodes.add(new Instruction(0xAC, Instruction.Type.MISC, "XOR H", 1, this::opcode_0xAC_xor, state));
        opcodes.add(new Instruction(0xAD, Instruction.Type.MISC, "XOR L", 1, this::opcode_0xAD_xor, state));
        opcodes.add(new Instruction(0xAE, Instruction.Type.R, "XOR (HL)", 1, this::opcode_0xAE_xor, state));
        opcodes.add(new Instruction(0xAF, Instruction.Type.MISC, "XOR A", 1, this::opcode_0xAF_xor, state));
        opcodes.add(new Instruction(0xB0, Instruction.Type.MISC, "OR B", 1, this::opcode_0xB0_or, state));
        opcodes.add(new Instruction(0xB1, Instruction.Type.MISC, "OR C", 1, this::opcode_0xB1_or, state));
        opcodes.add(new Instruction(0xB2, Instruction.Type.MISC, "OR D", 1, this::opcode_0xB2_or, state));
        opcodes.add(new Instruction(0xB3, Instruction.Type.MISC, "OR E", 1, this::opcode_0xB3_or, state));
        opcodes.add(new Instruction(0xB4, Instruction.Type.MISC, "OR H", 1, this::opcode_0xB4_or, state));
        opcodes.add(new Instruction(0xB5, Instruction.Type.MISC, "OR L", 1, this::opcode_0xB5_or, state));
        opcodes.add(new Instruction(0xB6, Instruction.Type.R, "OR (HL)", 1, this::opcode_0xB6_or, state));
        opcodes.add(new Instruction(0xB7, Instruction.Type.MISC, "OR A", 1, this::opcode_0xB7_or, state));
        opcodes.add(new Instruction(0xB8, Instruction.Type.MISC, "CP B", 1, this::opcode_0xB8_cp, state));
        opcodes.add(new Instruction(0xB9, Instruction.Type.MISC, "CP C", 1, this::opcode_0xB9_cp, state));
        opcodes.add(new Instruction(0xBA, Instruction.Type.MISC, "CP D", 1, this::opcode_0xBA_cp, state));
        opcodes.add(new Instruction(0xBB, Instruction.Type.MISC, "CP E", 1, this::opcode_0xBB_cp, state));
        opcodes.add(new Instruction(0xBC, Instruction.Type.MISC, "CP H", 1, this::opcode_0xBC_cp, state));
        opcodes.add(new Instruction(0xBD, Instruction.Type.MISC, "CP L", 1, this::opcode_0xBD_cp, state));
        opcodes.add(new Instruction(0xBE, Instruction.Type.R, "CP (HL)", 1, this::opcode_0xBE_cp, state));
        opcodes.add(new Instruction(0xBF, Instruction.Type.MISC, "CP A", 1, this::opcode_0xBF_cp, state));
        opcodes.add(new Instruction(0xC0, Instruction.Type.JUMP, "RET NZ", 1, this::opcode_0xC0_ret, state));
        opcodes.add(new Instruction(0xC1, Instruction.Type.MISC, "POP BC", 1, this::opcode_0xC1_pop, state));
        opcodes.add(new Instruction(0xC2, Instruction.Type.JUMP, "JP NZ,a16", 3, this::opcode_0xC2_jp, state));
        opcodes.add(new Instruction(0xC3, Instruction.Type.JUMP, "JP a16", 3, this::opcode_0xC3_jp, state));
        opcodes.add(new Instruction(0xC4, Instruction.Type.JUMP, "CALL NZ,a16", 3, this::opcode_0xC4_call, state));
        opcodes.add(new Instruction(0xC5, Instruction.Type.MISC, "PUSH BC", 1, this::opcode_0xC5_push, state));
        opcodes.add(new Instruction(0xC6, Instruction.Type.MISC, "ADD A,d8", 2, this::opcode_0xC6_add, state));
        opcodes.add(new Instruction(0xC7, Instruction.Type.MISC, "RST 00H", 1, this::opcode_0xC7_rst, state));
        opcodes.add(new Instruction(0xC8, Instruction.Type.JUMP, "RET Z", 1, this::opcode_0xC8_ret, state));
        opcodes.add(new Instruction(0xC9, Instruction.Type.JUMP, "RET", 1, this::opcode_0xC9_ret, state));
        opcodes.add(new Instruction(0xCA, Instruction.Type.JUMP, "JP Z,a16", 3, this::opcode_0xCA_jp, state));
        opcodes.add(new Instruction(0xCB, Instruction.Type.MISC, "PREFIX CB", 1, this::prefix, state));
        opcodes.add(new Instruction(0xCC, Instruction.Type.JUMP, "CALL Z,a16", 3, this::opcode_0xCC_call, state));
        opcodes.add(new Instruction(0xCD, Instruction.Type.JUMP, "CALL a16", 3, this::opcode_0xCD_call, state));
        opcodes.add(new Instruction(0xCE, Instruction.Type.MISC, "ADC A,d8", 2, this::opcode_0xCE_adc, state));
        opcodes.add(new Instruction(0xCF, Instruction.Type.MISC, "RST 08H", 1, this::opcode_0xCF_rst, state));
        opcodes.add(new Instruction(0xD0, Instruction.Type.JUMP, "RET NC", 1, this::opcode_0xD0_ret, state));
        opcodes.add(new Instruction(0xD1, Instruction.Type.MISC, "POP DE", 1, this::opcode_0xD1_pop, state));
        opcodes.add(new Instruction(0xD2, Instruction.Type.JUMP, "JP NC,a16", 3, this::opcode_0xD2_jp, state));
        opcodes.add(new Instruction(0xD3, Instruction.Type.MISC, "", 1, this::opcode_0x00_nop, state));
        opcodes.add(new Instruction(0xD4, Instruction.Type.JUMP, "CALL NC,a16", 3, this::opcode_0xD4_call, state));
        opcodes.add(new Instruction(0xD5, Instruction.Type.MISC, "PUSH DE", 1, this::opcode_0xD5_push, state));
        opcodes.add(new Instruction(0xD6, Instruction.Type.MISC, "SUB d8", 2, this::opcode_0xD6_sub, state));
        opcodes.add(new Instruction(0xD7, Instruction.Type.MISC, "RST 10H", 1, this::opcode_0xD7_rst, state));
        opcodes.add(new Instruction(0xD8, Instruction.Type.JUMP, "RET C", 1, this::opcode_0xD8_ret, state));
        opcodes.add(new Instruction(0xD9, Instruction.Type.JUMP, "RETI", 1, this::opcode_0xD9_reti, state));
        opcodes.add(new Instruction(0xDA, Instruction.Type.JUMP, "JP C,a16", 3, this::opcode_0xDA_jp, state));
        opcodes.add(new Instruction(0xDB, Instruction.Type.MISC, "", 1, this::opcode_0x00_nop, state));
        opcodes.add(new Instruction(0xDC, Instruction.Type.JUMP, "CALL C,a16", 3, this::opcode_0xDC_call, state));
        opcodes.add(new Instruction(0xDD, Instruction.Type.MISC, "", 1, this::opcode_0x00_nop, state));
        opcodes.add(new Instruction(0xDE, Instruction.Type.MISC, "SBC A,d8", 2, this::opcode_0xDE_sbc, state));
        opcodes.add(new Instruction(0xDF, Instruction.Type.MISC, "RST 18H", 1, this::opcode_0xDF_rst, state));
        opcodes.add(new Instruction(0xE0, Instruction.Type.W, "LD (FFa8),A", 2, this::opcode_0xE0_ldh, state));
        opcodes.add(new Instruction(0xE1, Instruction.Type.MISC, "POP HL", 1, this::opcode_0xE1_pop, state));
        opcodes.add(new Instruction(0xE2, Instruction.Type.W, "LD (C),A", 1, this::opcode_0xE2_ld, state));
        opcodes.add(new Instruction(0xE3, Instruction.Type.MISC, "", 1, this::opcode_0x00_nop, state));
        opcodes.add(new Instruction(0xE4, Instruction.Type.MISC, "", 1, this::opcode_0x00_nop, state));
        opcodes.add(new Instruction(0xE5, Instruction.Type.MISC, "PUSH HL", 1, this::opcode_0xE5_push, state));
        opcodes.add(new Instruction(0xE6, Instruction.Type.MISC, "AND d8", 2, this::opcode_0xE6_and, state));
        opcodes.add(new Instruction(0xE7, Instruction.Type.MISC, "RST 20H", 1, this::opcode_0xE7_rst, state));
        opcodes.add(new Instruction(0xE8, Instruction.Type.MISC, "ADD SP,r8", 2, this::opcode_0xE8_add, state));
        opcodes.add(new Instruction(0xE9, Instruction.Type.JUMP, "JP (HL)", 1, this::opcode_0xE9_jp, state));
        opcodes.add(new Instruction(0xEA, Instruction.Type.W, "LD (a16),A", 3, this::opcode_0xEA_ld, state));
        opcodes.add(new Instruction(0xEB, Instruction.Type.MISC, "", 1, this::opcode_0x00_nop, state));
        opcodes.add(new Instruction(0xEC, Instruction.Type.MISC, "", 1, this::opcode_0x00_nop, state));
        opcodes.add(new Instruction(0xED, Instruction.Type.MISC, "", 1, this::opcode_0x00_nop, state));
        opcodes.add(new Instruction(0xEE, Instruction.Type.MISC, "XOR d8", 2, this::opcode_0xEE_xor, state));
        opcodes.add(new Instruction(0xEF, Instruction.Type.MISC, "RST 28H", 1, this::opcode_0xEF_rst, state));
        opcodes.add(new Instruction(0xF0, Instruction.Type.R, "LD A,(FFa8)", 2, this::opcode_0xF0_ldh, state));
        opcodes.add(new Instruction(0xF1, Instruction.Type.MISC, "POP AF", 1, this::opcode_0xF1_pop, state));
        opcodes.add(new Instruction(0xF2, Instruction.Type.R, "LD A,(C)", 1, this::opcode_0xF2_ld, state));
        opcodes.add(new Instruction(0xF3, Instruction.Type.MISC, "DI", 1, this::opcode_0xF3_di, state));
        opcodes.add(new Instruction(0xF4, Instruction.Type.MISC, "", 1, this::opcode_0x00_nop, state));
        opcodes.add(new Instruction(0xF5, Instruction.Type.MISC, "PUSH AF", 1, this::opcode_0xF5_push, state));
        opcodes.add(new Instruction(0xF6, Instruction.Type.MISC, "OR d8", 2, this::opcode_0xF6_or, state));
        opcodes.add(new Instruction(0xF7, Instruction.Type.MISC, "RST 30H", 1, this::opcode_0xF7_rst, state));
        opcodes.add(new Instruction(0xF8, Instruction.Type.MISC, "LD HL,SP+r8", 2, this::opcode_0xF8_ld, state));
        opcodes.add(new Instruction(0xF9, Instruction.Type.MISC, "LD SP,HL", 1, this::opcode_0xF9_ld, state));
        opcodes.add(new Instruction(0xFA, Instruction.Type.R, "LD A,(a16)", 3, this::opcode_0xFA_ld, state));
        opcodes.add(new Instruction(0xFB, Instruction.Type.MISC, "EI", 1, this::opcode_0xFB_ei, state));
        opcodes.add(new Instruction(0xFC, Instruction.Type.MISC, "", 1, this::opcode_0x00_nop, state));
        opcodes.add(new Instruction(0xFD, Instruction.Type.MISC, "", 1, this::opcode_0x00_nop, state));
        opcodes.add(new Instruction(0xFE, Instruction.Type.MISC, "CP d8", 2, this::opcode_0xFE_cp, state));
        opcodes.add(new Instruction(0xFF, Instruction.Type.MISC, "RST 38H", 1, this::opcode_0xFF_rst, state));

        cb_opcodes = new ArrayList<>();
        cb_opcodes.add(new Instruction(0x00, Instruction.Type.MISC, "RLC B", 1, this::opcode_0xCB00_rlc, state));
        cb_opcodes.add(new Instruction(0x01, Instruction.Type.MISC, "RLC C", 1, this::opcode_0xCB01_rlc, state));
        cb_opcodes.add(new Instruction(0x02, Instruction.Type.MISC, "RLC D", 1, this::opcode_0xCB02_rlc, state));
        cb_opcodes.add(new Instruction(0x03, Instruction.Type.MISC, "RLC E", 1, this::opcode_0xCB03_rlc, state));
        cb_opcodes.add(new Instruction(0x04, Instruction.Type.MISC, "RLC H", 1, this::opcode_0xCB04_rlc, state));
        cb_opcodes.add(new Instruction(0x05, Instruction.Type.MISC, "RLC L", 1, this::opcode_0xCB05_rlc, state));
        cb_opcodes.add(new Instruction(0x06, Instruction.Type.RW, "RLC (HL)", 1, this::opcode_0xCB06_rlc, state));
        cb_opcodes.add(new Instruction(0x07, Instruction.Type.MISC, "RLC A", 1, this::opcode_0xCB07_rlc, state));
        cb_opcodes.add(new Instruction(0x08, Instruction.Type.MISC, "RRC B", 1, this::opcode_0xCB08_rrc, state));
        cb_opcodes.add(new Instruction(0x09, Instruction.Type.MISC, "RRC C", 1, this::opcode_0xCB09_rrc, state));
        cb_opcodes.add(new Instruction(0x0A, Instruction.Type.MISC, "RRC D", 1, this::opcode_0xCB0A_rrc, state));
        cb_opcodes.add(new Instruction(0x0B, Instruction.Type.MISC, "RRC E", 1, this::opcode_0xCB0B_rrc, state));
        cb_opcodes.add(new Instruction(0x0C, Instruction.Type.MISC, "RRC H", 1, this::opcode_0xCB0C_rrc, state));
        cb_opcodes.add(new Instruction(0x0D, Instruction.Type.MISC, "RRC L", 1, this::opcode_0xCB0D_rrc, state));
        cb_opcodes.add(new Instruction(0x0E, Instruction.Type.RW, "RRC (HL)", 1, this::opcode_0xCB0E_rrc, state));
        cb_opcodes.add(new Instruction(0x0F, Instruction.Type.MISC, "RRC A", 1, this::opcode_0xCB0F_rrc, state));
        cb_opcodes.add(new Instruction(0x10, Instruction.Type.MISC, "RL B", 1, this::opcode_0xCB10_rl, state));
        cb_opcodes.add(new Instruction(0x11, Instruction.Type.MISC, "RL C", 1, this::opcode_0xCB11_rl, state));
        cb_opcodes.add(new Instruction(0x12, Instruction.Type.MISC, "RL D", 1, this::opcode_0xCB12_rl, state));
        cb_opcodes.add(new Instruction(0x13, Instruction.Type.MISC, "RL E", 1, this::opcode_0xCB13_rl, state));
        cb_opcodes.add(new Instruction(0x14, Instruction.Type.MISC, "RL H", 1, this::opcode_0xCB14_rl, state));
        cb_opcodes.add(new Instruction(0x15, Instruction.Type.MISC, "RL L", 1, this::opcode_0xCB15_rl, state));
        cb_opcodes.add(new Instruction(0x16, Instruction.Type.RW, "RL (HL)", 1, this::opcode_0xCB16_rl, state));
        cb_opcodes.add(new Instruction(0x17, Instruction.Type.MISC, "RL A", 1, this::opcode_0xCB17_rl, state));
        cb_opcodes.add(new Instruction(0x18, Instruction.Type.MISC, "RR B", 1, this::opcode_0xCB18_rr, state));
        cb_opcodes.add(new Instruction(0x19, Instruction.Type.MISC, "RR C", 1, this::opcode_0xCB19_rr, state));
        cb_opcodes.add(new Instruction(0x1A, Instruction.Type.MISC, "RR D", 1, this::opcode_0xCB1A_rr, state));
        cb_opcodes.add(new Instruction(0x1B, Instruction.Type.MISC, "RR E", 1, this::opcode_0xCB1B_rr, state));
        cb_opcodes.add(new Instruction(0x1C, Instruction.Type.MISC, "RR H", 1, this::opcode_0xCB1C_rr, state));
        cb_opcodes.add(new Instruction(0x1D, Instruction.Type.MISC, "RR L", 1, this::opcode_0xCB1D_rr, state));
        cb_opcodes.add(new Instruction(0x1E, Instruction.Type.RW, "RR (HL)", 1, this::opcode_0xCB1E_rr, state));
        cb_opcodes.add(new Instruction(0x1F, Instruction.Type.MISC, "RR A", 1, this::opcode_0xCB1F_rr, state));
        cb_opcodes.add(new Instruction(0x20, Instruction.Type.MISC, "SLA B", 1, this::opcode_0xCB20_sla, state));
        cb_opcodes.add(new Instruction(0x21, Instruction.Type.MISC, "SLA C", 1, this::opcode_0xCB21_sla, state));
        cb_opcodes.add(new Instruction(0x22, Instruction.Type.MISC, "SLA D", 1, this::opcode_0xCB22_sla, state));
        cb_opcodes.add(new Instruction(0x23, Instruction.Type.MISC, "SLA E", 1, this::opcode_0xCB23_sla, state));
        cb_opcodes.add(new Instruction(0x24, Instruction.Type.MISC, "SLA H", 1, this::opcode_0xCB24_sla, state));
        cb_opcodes.add(new Instruction(0x25, Instruction.Type.MISC, "SLA L", 1, this::opcode_0xCB25_sla, state));
        cb_opcodes.add(new Instruction(0x26, Instruction.Type.RW, "SLA (HL)", 1, this::opcode_0xCB26_sla, state));
        cb_opcodes.add(new Instruction(0x27, Instruction.Type.MISC, "SLA A", 1, this::opcode_0xCB27_sla, state));
        cb_opcodes.add(new Instruction(0x28, Instruction.Type.MISC, "SRA B", 1, this::opcode_0xCB28_sra, state));
        cb_opcodes.add(new Instruction(0x29, Instruction.Type.MISC, "SRA C", 1, this::opcode_0xCB29_sra, state));
        cb_opcodes.add(new Instruction(0x2A, Instruction.Type.MISC, "SRA D", 1, this::opcode_0xCB2A_sra, state));
        cb_opcodes.add(new Instruction(0x2B, Instruction.Type.MISC, "SRA E", 1, this::opcode_0xCB2B_sra, state));
        cb_opcodes.add(new Instruction(0x2C, Instruction.Type.MISC, "SRA H", 1, this::opcode_0xCB2C_sra, state));
        cb_opcodes.add(new Instruction(0x2D, Instruction.Type.MISC, "SRA L", 1, this::opcode_0xCB2D_sra, state));
        cb_opcodes.add(new Instruction(0x2E, Instruction.Type.RW, "SRA (HL)", 1, this::opcode_0xCB2E_sra, state));
        cb_opcodes.add(new Instruction(0x2F, Instruction.Type.MISC, "SRA A", 1, this::opcode_0xCB2F_sra, state));
        cb_opcodes.add(new Instruction(0x30, Instruction.Type.MISC, "SWAP B", 1, this::opcode_0xCB30_swap, state));
        cb_opcodes.add(new Instruction(0x31, Instruction.Type.MISC, "SWAP C", 1, this::opcode_0xCB31_swap, state));
        cb_opcodes.add(new Instruction(0x32, Instruction.Type.MISC, "SWAP D", 1, this::opcode_0xCB32_swap, state));
        cb_opcodes.add(new Instruction(0x33, Instruction.Type.MISC, "SWAP E", 1, this::opcode_0xCB33_swap, state));
        cb_opcodes.add(new Instruction(0x34, Instruction.Type.MISC, "SWAP H", 1, this::opcode_0xCB34_swap, state));
        cb_opcodes.add(new Instruction(0x35, Instruction.Type.MISC, "SWAP L", 1, this::opcode_0xCB35_swap, state));
        cb_opcodes.add(new Instruction(0x36, Instruction.Type.RW, "SWAP (HL)", 1, this::opcode_0xCB36_swap, state));
        cb_opcodes.add(new Instruction(0x37, Instruction.Type.MISC, "SWAP A", 1, this::opcode_0xCB37_swap, state));
        cb_opcodes.add(new Instruction(0x38, Instruction.Type.MISC, "SRL B", 1, this::opcode_0xCB38_srl, state));
        cb_opcodes.add(new Instruction(0x39, Instruction.Type.MISC, "SRL C", 1, this::opcode_0xCB39_srl, state));
        cb_opcodes.add(new Instruction(0x3A, Instruction.Type.MISC, "SRL D", 1, this::opcode_0xCB3A_srl, state));
        cb_opcodes.add(new Instruction(0x3B, Instruction.Type.MISC, "SRL E", 1, this::opcode_0xCB3B_srl, state));
        cb_opcodes.add(new Instruction(0x3C, Instruction.Type.MISC, "SRL H", 1, this::opcode_0xCB3C_srl, state));
        cb_opcodes.add(new Instruction(0x3D, Instruction.Type.MISC, "SRL L", 1, this::opcode_0xCB3D_srl, state));
        cb_opcodes.add(new Instruction(0x3E, Instruction.Type.RW, "SRL (HL)", 1, this::opcode_0xCB3E_srl, state));
        cb_opcodes.add(new Instruction(0x3F, Instruction.Type.MISC, "SRL A", 1, this::opcode_0xCB3F_srl, state));
        cb_opcodes.add(new Instruction(0x40, Instruction.Type.MISC, "BIT 0,B", 1, this::opcode_0xCB40_bit, state));
        cb_opcodes.add(new Instruction(0x41, Instruction.Type.MISC, "BIT 0,C", 1, this::opcode_0xCB41_bit, state));
        cb_opcodes.add(new Instruction(0x42, Instruction.Type.MISC, "BIT 0,D", 1, this::opcode_0xCB42_bit, state));
        cb_opcodes.add(new Instruction(0x43, Instruction.Type.MISC, "BIT 0,E", 1, this::opcode_0xCB43_bit, state));
        cb_opcodes.add(new Instruction(0x44, Instruction.Type.MISC, "BIT 0,H", 1, this::opcode_0xCB44_bit, state));
        cb_opcodes.add(new Instruction(0x45, Instruction.Type.MISC, "BIT 0,L", 1, this::opcode_0xCB45_bit, state));
        cb_opcodes.add(new Instruction(0x46, Instruction.Type.R, "BIT 0,(HL)", 1, this::opcode_0xCB46_bit, state));
        cb_opcodes.add(new Instruction(0x47, Instruction.Type.MISC, "BIT 0,A", 1, this::opcode_0xCB47_bit, state));
        cb_opcodes.add(new Instruction(0x48, Instruction.Type.MISC, "BIT 1,B", 1, this::opcode_0xCB48_bit, state));
        cb_opcodes.add(new Instruction(0x49, Instruction.Type.MISC, "BIT 1,C", 1, this::opcode_0xCB49_bit, state));
        cb_opcodes.add(new Instruction(0x4A, Instruction.Type.MISC, "BIT 1,D", 1, this::opcode_0xCB4A_bit, state));
        cb_opcodes.add(new Instruction(0x4B, Instruction.Type.MISC, "BIT 1,E", 1, this::opcode_0xCB4B_bit, state));
        cb_opcodes.add(new Instruction(0x4C, Instruction.Type.MISC, "BIT 1,H", 1, this::opcode_0xCB4C_bit, state));
        cb_opcodes.add(new Instruction(0x4D, Instruction.Type.MISC, "BIT 1,L", 1, this::opcode_0xCB4D_bit, state));
        cb_opcodes.add(new Instruction(0x4E, Instruction.Type.R, "BIT 1,(HL)", 1, this::opcode_0xCB4E_bit, state));
        cb_opcodes.add(new Instruction(0x4F, Instruction.Type.MISC, "BIT 1,A", 1, this::opcode_0xCB4F_bit, state));
        cb_opcodes.add(new Instruction(0x50, Instruction.Type.MISC, "BIT 2,B", 1, this::opcode_0xCB50_bit, state));
        cb_opcodes.add(new Instruction(0x51, Instruction.Type.MISC, "BIT 2,C", 1, this::opcode_0xCB51_bit, state));
        cb_opcodes.add(new Instruction(0x52, Instruction.Type.MISC, "BIT 2,D", 1, this::opcode_0xCB52_bit, state));
        cb_opcodes.add(new Instruction(0x53, Instruction.Type.MISC, "BIT 2,E", 1, this::opcode_0xCB53_bit, state));
        cb_opcodes.add(new Instruction(0x54, Instruction.Type.MISC, "BIT 2,H", 1, this::opcode_0xCB54_bit, state));
        cb_opcodes.add(new Instruction(0x55, Instruction.Type.MISC, "BIT 2,L", 1, this::opcode_0xCB55_bit, state));
        cb_opcodes.add(new Instruction(0x56, Instruction.Type.R, "BIT 2,(HL)", 1, this::opcode_0xCB56_bit, state));
        cb_opcodes.add(new Instruction(0x57, Instruction.Type.MISC, "BIT 2,A", 1, this::opcode_0xCB57_bit, state));
        cb_opcodes.add(new Instruction(0x58, Instruction.Type.MISC, "BIT 3,B", 1, this::opcode_0xCB58_bit, state));
        cb_opcodes.add(new Instruction(0x59, Instruction.Type.MISC, "BIT 3,C", 1, this::opcode_0xCB59_bit, state));
        cb_opcodes.add(new Instruction(0x5A, Instruction.Type.MISC, "BIT 3,D", 1, this::opcode_0xCB5A_bit, state));
        cb_opcodes.add(new Instruction(0x5B, Instruction.Type.MISC, "BIT 3,E", 1, this::opcode_0xCB5B_bit, state));
        cb_opcodes.add(new Instruction(0x5C, Instruction.Type.MISC, "BIT 3,H", 1, this::opcode_0xCB5C_bit, state));
        cb_opcodes.add(new Instruction(0x5D, Instruction.Type.MISC, "BIT 3,L", 1, this::opcode_0xCB5D_bit, state));
        cb_opcodes.add(new Instruction(0x5E, Instruction.Type.R, "BIT 3,(HL)", 1, this::opcode_0xCB5E_bit, state));
        cb_opcodes.add(new Instruction(0x5F, Instruction.Type.MISC, "BIT 3,A", 1, this::opcode_0xCB5F_bit, state));
        cb_opcodes.add(new Instruction(0x60, Instruction.Type.MISC, "BIT 4,B", 1, this::opcode_0xCB60_bit, state));
        cb_opcodes.add(new Instruction(0x61, Instruction.Type.MISC, "BIT 4,C", 1, this::opcode_0xCB61_bit, state));
        cb_opcodes.add(new Instruction(0x62, Instruction.Type.MISC, "BIT 4,D", 1, this::opcode_0xCB62_bit, state));
        cb_opcodes.add(new Instruction(0x63, Instruction.Type.MISC, "BIT 4,E", 1, this::opcode_0xCB63_bit, state));
        cb_opcodes.add(new Instruction(0x64, Instruction.Type.MISC, "BIT 4,H", 1, this::opcode_0xCB64_bit, state));
        cb_opcodes.add(new Instruction(0x65, Instruction.Type.MISC, "BIT 4,L", 1, this::opcode_0xCB65_bit, state));
        cb_opcodes.add(new Instruction(0x66, Instruction.Type.R, "BIT 4,(HL)", 1, this::opcode_0xCB66_bit, state));
        cb_opcodes.add(new Instruction(0x67, Instruction.Type.MISC, "BIT 4,A", 1, this::opcode_0xCB67_bit, state));
        cb_opcodes.add(new Instruction(0x68, Instruction.Type.MISC, "BIT 5,B", 1, this::opcode_0xCB68_bit, state));
        cb_opcodes.add(new Instruction(0x69, Instruction.Type.MISC, "BIT 5,C", 1, this::opcode_0xCB69_bit, state));
        cb_opcodes.add(new Instruction(0x6A, Instruction.Type.MISC, "BIT 5,D", 1, this::opcode_0xCB6A_bit, state));
        cb_opcodes.add(new Instruction(0x6B, Instruction.Type.MISC, "BIT 5,E", 1, this::opcode_0xCB6B_bit, state));
        cb_opcodes.add(new Instruction(0x6C, Instruction.Type.MISC, "BIT 5,H", 1, this::opcode_0xCB6C_bit, state));
        cb_opcodes.add(new Instruction(0x6D, Instruction.Type.MISC, "BIT 5,L", 1, this::opcode_0xCB6D_bit, state));
        cb_opcodes.add(new Instruction(0x6E, Instruction.Type.R, "BIT 5,(HL)", 1, this::opcode_0xCB6E_bit, state));
        cb_opcodes.add(new Instruction(0x6F, Instruction.Type.MISC, "BIT 5,A", 1, this::opcode_0xCB6F_bit, state));
        cb_opcodes.add(new Instruction(0x70, Instruction.Type.MISC, "BIT 6,B", 1, this::opcode_0xCB70_bit, state));
        cb_opcodes.add(new Instruction(0x71, Instruction.Type.MISC, "BIT 6,C", 1, this::opcode_0xCB71_bit, state));
        cb_opcodes.add(new Instruction(0x72, Instruction.Type.MISC, "BIT 6,D", 1, this::opcode_0xCB72_bit, state));
        cb_opcodes.add(new Instruction(0x73, Instruction.Type.MISC, "BIT 6,E", 1, this::opcode_0xCB73_bit, state));
        cb_opcodes.add(new Instruction(0x74, Instruction.Type.MISC, "BIT 6,H", 1, this::opcode_0xCB74_bit, state));
        cb_opcodes.add(new Instruction(0x75, Instruction.Type.MISC, "BIT 6,L", 1, this::opcode_0xCB75_bit, state));
        cb_opcodes.add(new Instruction(0x76, Instruction.Type.R, "BIT 6,(HL)", 1, this::opcode_0xCB76_bit, state));
        cb_opcodes.add(new Instruction(0x77, Instruction.Type.MISC, "BIT 6,A", 1, this::opcode_0xCB77_bit, state));
        cb_opcodes.add(new Instruction(0x78, Instruction.Type.MISC, "BIT 7,B", 1, this::opcode_0xCB78_bit, state));
        cb_opcodes.add(new Instruction(0x79, Instruction.Type.MISC, "BIT 7,C", 1, this::opcode_0xCB79_bit, state));
        cb_opcodes.add(new Instruction(0x7A, Instruction.Type.MISC, "BIT 7,D", 1, this::opcode_0xCB7A_bit, state));
        cb_opcodes.add(new Instruction(0x7B, Instruction.Type.MISC, "BIT 7,E", 1, this::opcode_0xCB7B_bit, state));
        cb_opcodes.add(new Instruction(0x7C, Instruction.Type.MISC, "BIT 7,H", 1, this::opcode_0xCB7C_bit, state));
        cb_opcodes.add(new Instruction(0x7D, Instruction.Type.MISC, "BIT 7,L", 1, this::opcode_0xCB7D_bit, state));
        cb_opcodes.add(new Instruction(0x7E, Instruction.Type.R, "BIT 7,(HL)", 1, this::opcode_0xCB7E_bit, state));
        cb_opcodes.add(new Instruction(0x7F, Instruction.Type.MISC, "BIT 7,A", 1, this::opcode_0xCB7F_bit, state));
        cb_opcodes.add(new Instruction(0x80, Instruction.Type.MISC, "RES 0,B", 1, this::opcode_0xCB80_res, state));
        cb_opcodes.add(new Instruction(0x81, Instruction.Type.MISC, "RES 0,C", 1, this::opcode_0xCB81_res, state));
        cb_opcodes.add(new Instruction(0x82, Instruction.Type.MISC, "RES 0,D", 1, this::opcode_0xCB82_res, state));
        cb_opcodes.add(new Instruction(0x83, Instruction.Type.MISC, "RES 0,E", 1, this::opcode_0xCB83_res, state));
        cb_opcodes.add(new Instruction(0x84, Instruction.Type.MISC, "RES 0,H", 1, this::opcode_0xCB84_res, state));
        cb_opcodes.add(new Instruction(0x85, Instruction.Type.MISC, "RES 0,L", 1, this::opcode_0xCB85_res, state));
        cb_opcodes.add(new Instruction(0x86, Instruction.Type.RW, "RES 0,(HL)", 1, this::opcode_0xCB86_res, state));
        cb_opcodes.add(new Instruction(0x87, Instruction.Type.MISC, "RES 0,A", 1, this::opcode_0xCB87_res, state));
        cb_opcodes.add(new Instruction(0x88, Instruction.Type.MISC, "RES 1,B", 1, this::opcode_0xCB88_res, state));
        cb_opcodes.add(new Instruction(0x89, Instruction.Type.MISC, "RES 1,C", 1, this::opcode_0xCB89_res, state));
        cb_opcodes.add(new Instruction(0x8A, Instruction.Type.MISC, "RES 1,D", 1, this::opcode_0xCB8A_res, state));
        cb_opcodes.add(new Instruction(0x8B, Instruction.Type.MISC, "RES 1,E", 1, this::opcode_0xCB8B_res, state));
        cb_opcodes.add(new Instruction(0x8C, Instruction.Type.MISC, "RES 1,H", 1, this::opcode_0xCB8C_res, state));
        cb_opcodes.add(new Instruction(0x8D, Instruction.Type.MISC, "RES 1,L", 1, this::opcode_0xCB8D_res, state));
        cb_opcodes.add(new Instruction(0x8E, Instruction.Type.RW, "RES 1,(HL)", 1, this::opcode_0xCB8E_res, state));
        cb_opcodes.add(new Instruction(0x8F, Instruction.Type.MISC, "RES 1,A", 1, this::opcode_0xCB8F_res, state));
        cb_opcodes.add(new Instruction(0x90, Instruction.Type.MISC, "RES 2,B", 1, this::opcode_0xCB90_res, state));
        cb_opcodes.add(new Instruction(0x91, Instruction.Type.MISC, "RES 2,C", 1, this::opcode_0xCB91_res, state));
        cb_opcodes.add(new Instruction(0x92, Instruction.Type.MISC, "RES 2,D", 1, this::opcode_0xCB92_res, state));
        cb_opcodes.add(new Instruction(0x93, Instruction.Type.MISC, "RES 2,E", 1, this::opcode_0xCB93_res, state));
        cb_opcodes.add(new Instruction(0x94, Instruction.Type.MISC, "RES 2,H", 1, this::opcode_0xCB94_res, state));
        cb_opcodes.add(new Instruction(0x95, Instruction.Type.MISC, "RES 2,L", 1, this::opcode_0xCB95_res, state));
        cb_opcodes.add(new Instruction(0x96, Instruction.Type.RW, "RES 2,(HL)", 1, this::opcode_0xCB96_res, state));
        cb_opcodes.add(new Instruction(0x97, Instruction.Type.MISC, "RES 2,A", 1, this::opcode_0xCB97_res, state));
        cb_opcodes.add(new Instruction(0x98, Instruction.Type.MISC, "RES 3,B", 1, this::opcode_0xCB98_res, state));
        cb_opcodes.add(new Instruction(0x99, Instruction.Type.MISC, "RES 3,C", 1, this::opcode_0xCB99_res, state));
        cb_opcodes.add(new Instruction(0x9A, Instruction.Type.MISC, "RES 3,D", 1, this::opcode_0xCB9A_res, state));
        cb_opcodes.add(new Instruction(0x9B, Instruction.Type.MISC, "RES 3,E", 1, this::opcode_0xCB9B_res, state));
        cb_opcodes.add(new Instruction(0x9C, Instruction.Type.MISC, "RES 3,H", 1, this::opcode_0xCB9C_res, state));
        cb_opcodes.add(new Instruction(0x9D, Instruction.Type.MISC, "RES 3,L", 1, this::opcode_0xCB9D_res, state));
        cb_opcodes.add(new Instruction(0x9E, Instruction.Type.RW, "RES 3,(HL)", 1, this::opcode_0xCB9E_res, state));
        cb_opcodes.add(new Instruction(0x9F, Instruction.Type.MISC, "RES 3,A", 1, this::opcode_0xCB9F_res, state));
        cb_opcodes.add(new Instruction(0xA0, Instruction.Type.MISC, "RES 4,B", 1, this::opcode_0xCBA0_res, state));
        cb_opcodes.add(new Instruction(0xA1, Instruction.Type.MISC, "RES 4,C", 1, this::opcode_0xCBA1_res, state));
        cb_opcodes.add(new Instruction(0xA2, Instruction.Type.MISC, "RES 4,D", 1, this::opcode_0xCBA2_res, state));
        cb_opcodes.add(new Instruction(0xA3, Instruction.Type.MISC, "RES 4,E", 1, this::opcode_0xCBA3_res, state));
        cb_opcodes.add(new Instruction(0xA4, Instruction.Type.MISC, "RES 4,H", 1, this::opcode_0xCBA4_res, state));
        cb_opcodes.add(new Instruction(0xA5, Instruction.Type.MISC, "RES 4,L", 1, this::opcode_0xCBA5_res, state));
        cb_opcodes.add(new Instruction(0xA6, Instruction.Type.RW, "RES 4,(HL)", 1, this::opcode_0xCBA6_res, state));
        cb_opcodes.add(new Instruction(0xA7, Instruction.Type.MISC, "RES 4,A", 1, this::opcode_0xCBA7_res, state));
        cb_opcodes.add(new Instruction(0xA8, Instruction.Type.MISC, "RES 5,B", 1, this::opcode_0xCBA8_res, state));
        cb_opcodes.add(new Instruction(0xA9, Instruction.Type.MISC, "RES 5,C", 1, this::opcode_0xCBA9_res, state));
        cb_opcodes.add(new Instruction(0xAA, Instruction.Type.MISC, "RES 5,D", 1, this::opcode_0xCBAA_res, state));
        cb_opcodes.add(new Instruction(0xAB, Instruction.Type.MISC, "RES 5,E", 1, this::opcode_0xCBAB_res, state));
        cb_opcodes.add(new Instruction(0xAC, Instruction.Type.MISC, "RES 5,H", 1, this::opcode_0xCBAC_res, state));
        cb_opcodes.add(new Instruction(0xAD, Instruction.Type.MISC, "RES 5,L", 1, this::opcode_0xCBAD_res, state));
        cb_opcodes.add(new Instruction(0xAE, Instruction.Type.RW, "RES 5,(HL)", 1, this::opcode_0xCBAE_res, state));
        cb_opcodes.add(new Instruction(0xAF, Instruction.Type.MISC, "RES 5,A", 1, this::opcode_0xCBAF_res, state));
        cb_opcodes.add(new Instruction(0xB0, Instruction.Type.MISC, "RES 6,B", 1, this::opcode_0xCBB0_res, state));
        cb_opcodes.add(new Instruction(0xB1, Instruction.Type.MISC, "RES 6,C", 1, this::opcode_0xCBB1_res, state));
        cb_opcodes.add(new Instruction(0xB2, Instruction.Type.MISC, "RES 6,D", 1, this::opcode_0xCBB2_res, state));
        cb_opcodes.add(new Instruction(0xB3, Instruction.Type.MISC, "RES 6,E", 1, this::opcode_0xCBB3_res, state));
        cb_opcodes.add(new Instruction(0xB4, Instruction.Type.MISC, "RES 6,H", 1, this::opcode_0xCBB4_res, state));
        cb_opcodes.add(new Instruction(0xB5, Instruction.Type.MISC, "RES 6,L", 1, this::opcode_0xCBB5_res, state));
        cb_opcodes.add(new Instruction(0xB6, Instruction.Type.RW, "RES 6,(HL)", 1, this::opcode_0xCBB6_res, state));
        cb_opcodes.add(new Instruction(0xB7, Instruction.Type.MISC, "RES 6,A", 1, this::opcode_0xCBB7_res, state));
        cb_opcodes.add(new Instruction(0xB8, Instruction.Type.MISC, "RES 7,B", 1, this::opcode_0xCBB8_res, state));
        cb_opcodes.add(new Instruction(0xB9, Instruction.Type.MISC, "RES 7,C", 1, this::opcode_0xCBB9_res, state));
        cb_opcodes.add(new Instruction(0xBA, Instruction.Type.MISC, "RES 7,D", 1, this::opcode_0xCBBA_res, state));
        cb_opcodes.add(new Instruction(0xBB, Instruction.Type.MISC, "RES 7,E", 1, this::opcode_0xCBBB_res, state));
        cb_opcodes.add(new Instruction(0xBC, Instruction.Type.MISC, "RES 7,H", 1, this::opcode_0xCBBC_res, state));
        cb_opcodes.add(new Instruction(0xBD, Instruction.Type.MISC, "RES 7,L", 1, this::opcode_0xCBBD_res, state));
        cb_opcodes.add(new Instruction(0xBE, Instruction.Type.RW, "RES 7,(HL)", 1, this::opcode_0xCBBE_res, state));
        cb_opcodes.add(new Instruction(0xBF, Instruction.Type.MISC, "RES 7,A", 1, this::opcode_0xCBBF_res, state));
        cb_opcodes.add(new Instruction(0xC0, Instruction.Type.MISC, "SET 0,B", 1, this::opcode_0xCBC0_set_set, state));
        cb_opcodes.add(new Instruction(0xC1, Instruction.Type.MISC, "SET 0,C", 1, this::opcode_0xCBC1_set, state));
        cb_opcodes.add(new Instruction(0xC2, Instruction.Type.MISC, "SET 0,D", 1, this::opcode_0xCBC2_set, state));
        cb_opcodes.add(new Instruction(0xC3, Instruction.Type.MISC, "SET 0,E", 1, this::opcode_0xCBC3_set, state));
        cb_opcodes.add(new Instruction(0xC4, Instruction.Type.MISC, "SET 0,H", 1, this::opcode_0xCBC4_set, state));
        cb_opcodes.add(new Instruction(0xC5, Instruction.Type.MISC, "SET 0,L", 1, this::opcode_0xCBC5_set, state));
        cb_opcodes.add(new Instruction(0xC6, Instruction.Type.RW, "SET 0,(HL)", 1, this::opcode_0xCBC6_set, state));
        cb_opcodes.add(new Instruction(0xC7, Instruction.Type.MISC, "SET 0,A", 1, this::opcode_0xCBC7_set, state));
        cb_opcodes.add(new Instruction(0xC8, Instruction.Type.MISC, "SET 1,B", 1, this::opcode_0xCBC8_set, state));
        cb_opcodes.add(new Instruction(0xC9, Instruction.Type.MISC, "SET 1,C", 1, this::opcode_0xCBC9_set, state));
        cb_opcodes.add(new Instruction(0xCA, Instruction.Type.MISC, "SET 1,D", 1, this::opcode_0xCBCA_set, state));
        cb_opcodes.add(new Instruction(0xCB, Instruction.Type.MISC, "SET 1,E", 1, this::opcode_0xCBCB_set, state));
        cb_opcodes.add(new Instruction(0xCC, Instruction.Type.MISC, "SET 1,H", 1, this::opcode_0xCBCC_set, state));
        cb_opcodes.add(new Instruction(0xCD, Instruction.Type.MISC, "SET 1,L", 1, this::opcode_0xCBCD_set, state));
        cb_opcodes.add(new Instruction(0xCE, Instruction.Type.RW, "SET 1,(HL)", 1, this::opcode_0xCBCE_set, state));
        cb_opcodes.add(new Instruction(0xCF, Instruction.Type.MISC, "SET 1,A", 1, this::opcode_0xCBCF_set, state));
        cb_opcodes.add(new Instruction(0xD0, Instruction.Type.MISC, "SET 2,B", 1, this::opcode_0xCBD0_set, state));
        cb_opcodes.add(new Instruction(0xD1, Instruction.Type.MISC, "SET 2,C", 1, this::opcode_0xCBD1_set, state));
        cb_opcodes.add(new Instruction(0xD2, Instruction.Type.MISC, "SET 2,D", 1, this::opcode_0xCBD2_set, state));
        cb_opcodes.add(new Instruction(0xD3, Instruction.Type.MISC, "SET 2,E", 1, this::opcode_0xCBD3_set, state));
        cb_opcodes.add(new Instruction(0xD4, Instruction.Type.MISC, "SET 2,H", 1, this::opcode_0xCBD4_set, state));
        cb_opcodes.add(new Instruction(0xD5, Instruction.Type.MISC, "SET 2,L", 1, this::opcode_0xCBD5_set, state));
        cb_opcodes.add(new Instruction(0xD6, Instruction.Type.RW, "SET 2,(HL)", 1, this::opcode_0xCBD6_set, state));
        cb_opcodes.add(new Instruction(0xD7, Instruction.Type.MISC, "SET 2,A", 1, this::opcode_0xCBD7_set, state));
        cb_opcodes.add(new Instruction(0xD8, Instruction.Type.MISC, "SET 3,B", 1, this::opcode_0xCBD8_set, state));
        cb_opcodes.add(new Instruction(0xD9, Instruction.Type.MISC, "SET 3,C", 1, this::opcode_0xCBD9_set, state));
        cb_opcodes.add(new Instruction(0xDA, Instruction.Type.MISC, "SET 3,D", 1, this::opcode_0xCBDA_set, state));
        cb_opcodes.add(new Instruction(0xDB, Instruction.Type.MISC, "SET 3,E", 1, this::opcode_0xCBDB_set, state));
        cb_opcodes.add(new Instruction(0xDC, Instruction.Type.MISC, "SET 3,H", 1, this::opcode_0xCBDC_set, state));
        cb_opcodes.add(new Instruction(0xDD, Instruction.Type.MISC, "SET 3,L", 1, this::opcode_0xCBDD_set, state));
        cb_opcodes.add(new Instruction(0xDE, Instruction.Type.RW, "SET 3,(HL)", 1, this::opcode_0xCBDE_set, state));
        cb_opcodes.add(new Instruction(0xDF, Instruction.Type.MISC, "SET 3,A", 1, this::opcode_0xCBDF_set, state));
        cb_opcodes.add(new Instruction(0xE0, Instruction.Type.MISC, "SET 4,B", 1, this::opcode_0xCBE0_set, state));
        cb_opcodes.add(new Instruction(0xE1, Instruction.Type.MISC, "SET 4,B", 1, this::opcode_0xCBE1_set, state));
        cb_opcodes.add(new Instruction(0xE2, Instruction.Type.MISC, "SET 4,D", 1, this::opcode_0xCBE2_set, state));
        cb_opcodes.add(new Instruction(0xE3, Instruction.Type.MISC, "SET 4,E", 1, this::opcode_0xCBE3_set, state));
        cb_opcodes.add(new Instruction(0xE4, Instruction.Type.MISC, "SET 4,H", 1, this::opcode_0xCBE4_set, state));
        cb_opcodes.add(new Instruction(0xE5, Instruction.Type.MISC, "SET 4,L", 1, this::opcode_0xCBE5_set, state));
        cb_opcodes.add(new Instruction(0xE6, Instruction.Type.RW, "SET 4,(HL)", 1, this::opcode_0xCBE6_set, state));
        cb_opcodes.add(new Instruction(0xE7, Instruction.Type.MISC, "SET 4,A", 1, this::opcode_0xCBE7_set, state));
        cb_opcodes.add(new Instruction(0xE8, Instruction.Type.MISC, "SET 5,B", 1, this::opcode_0xCBE8_set, state));
        cb_opcodes.add(new Instruction(0xE9, Instruction.Type.MISC, "SET 5,C", 1, this::opcode_0xCBE9_set, state));
        cb_opcodes.add(new Instruction(0xEA, Instruction.Type.MISC, "SET 5,D", 1, this::opcode_0xCBEA_set, state));
        cb_opcodes.add(new Instruction(0xEB, Instruction.Type.MISC, "SET 5,E", 1, this::opcode_0xCBEB_set, state));
        cb_opcodes.add(new Instruction(0xEC, Instruction.Type.MISC, "SET 5,H", 1, this::opcode_0xCBEC_set, state));
        cb_opcodes.add(new Instruction(0xED, Instruction.Type.MISC, "SET 5,L", 1, this::opcode_0xCBED_set, state));
        cb_opcodes.add(new Instruction(0xEE, Instruction.Type.RW, "SET 5,(HL)", 1, this::opcode_0xCBEE_set, state));
        cb_opcodes.add(new Instruction(0xEF, Instruction.Type.MISC, "SET 5,A", 1, this::opcode_0xCBEF_set, state));
        cb_opcodes.add(new Instruction(0xF0, Instruction.Type.MISC, "SET 6,B", 1, this::opcode_0xCBF0_set, state));
        cb_opcodes.add(new Instruction(0xF1, Instruction.Type.MISC, "SET 6,C", 1, this::opcode_0xCBF1_set, state));
        cb_opcodes.add(new Instruction(0xF2, Instruction.Type.MISC, "SET 6,D", 1, this::opcode_0xCBF2_set, state));
        cb_opcodes.add(new Instruction(0xF3, Instruction.Type.MISC, "SET 6,E", 1, this::opcode_0xCBF3_set, state));
        cb_opcodes.add(new Instruction(0xF4, Instruction.Type.MISC, "SET 6,H", 1, this::opcode_0xCBF4_set, state));
        cb_opcodes.add(new Instruction(0xF5, Instruction.Type.MISC, "SET 6,L", 1, this::opcode_0xCBF5_set, state));
        cb_opcodes.add(new Instruction(0xF6, Instruction.Type.RW, "SET 6,(HL)", 1, this::opcode_0xCBF6_set, state));
        cb_opcodes.add(new Instruction(0xF7, Instruction.Type.MISC, "SET 6,A", 1, this::opcode_0xCBF7_set, state));
        cb_opcodes.add(new Instruction(0xF8, Instruction.Type.MISC, "SET 7,B", 1, this::opcode_0xCBF8_set, state));
        cb_opcodes.add(new Instruction(0xF9, Instruction.Type.MISC, "SET 7,C", 1, this::opcode_0xCBF9_set, state));
        cb_opcodes.add(new Instruction(0xFA, Instruction.Type.MISC, "SET 7,D", 1, this::opcode_0xCBFA_set, state));
        cb_opcodes.add(new Instruction(0xFB, Instruction.Type.MISC, "SET 7,E", 1, this::opcode_0xCBFB_set, state));
        cb_opcodes.add(new Instruction(0xFC, Instruction.Type.MISC, "SET 7,H", 1, this::opcode_0xCBFC_set, state));
        cb_opcodes.add(new Instruction(0xFD, Instruction.Type.MISC, "SET 7,L", 1, this::opcode_0xCBFD_set, state));
        cb_opcodes.add(new Instruction(0xFE, Instruction.Type.RW, "SET 7,(HL)", 1, this::opcode_0xCBFE_set, state));
        cb_opcodes.add(new Instruction(0xFF, Instruction.Type.MISC, "SET 7,A", 1, this::opcode_0xCBFF_set, state));
    }

    public void init() {
        reset();
        fetchInstruction();
        state.set(af, bc, de, hl, sp, pc, ime, next_instr);
        if (debugger.isHooked(DebuggerMode.CPU))
            debugger.clock();
    }

    public int execute() {
        if (!halted) {
            //Wait until opcode has finished execution
            if (opcode_mcycle-- < 1) {
                opcode_mcycle = next_instr.operate();
                handleInterrupts();
                fetchInstruction();
                state.set(af, bc, de, hl, sp, pc, ime, next_instr);
                if (debugger.isHooked(DebuggerMode.CPU))
                    debugger.breakpointCheck();
            }
        } else if (handleInterrupts()) {
            fetchInstruction();
            state.set(af, bc, de, hl, sp, pc, ime, next_instr);
            if (debugger.isHooked(DebuggerMode.CPU))
                debugger.breakpointCheck();
        }
        return opcode_mcycle;
    }

    public boolean handleInterrupts() {
        if (ime || halted) {
            int interruptRequest = readByte(MMU.IF) & readByte(MMU.IE) & 0x1F;
            if (interruptRequest != 0) {
                if (ime && !halted) {
                    ime = false;
                    if ((interruptRequest & Flags.IF_VBLANK_IRQ) == Flags.IF_VBLANK_IRQ) {
                        memory.writeIORegisterBit(MMU.IF, Flags.IF_VBLANK_IRQ, false);
                        pushStack(pc.read());
                        pc.write(MMU.IRQ_V_BLANK_VECTOR);
                        opcode_mcycle = 8;
                        return true;
                    } else if ((interruptRequest & Flags.IF_LCD_STAT_IRQ) == Flags.IF_LCD_STAT_IRQ) {
                        memory.writeIORegisterBit(MMU.IF, Flags.IF_LCD_STAT_IRQ, false);
                        pushStack(pc.read());
                        pc.write(MMU.IRQ_LCD_VECTOR);
                        opcode_mcycle = 8;
                        return true;
                    } else if ((interruptRequest & Flags.IF_TIMER_IRQ) == Flags.IF_TIMER_IRQ) {
                        memory.writeIORegisterBit(MMU.IF, Flags.IF_TIMER_IRQ, false);
                        pushStack(pc.read());
                        pc.write(MMU.IRQ_TIMER_VECTOR);
                        opcode_mcycle = 8;
                        return true;
                    } else if ((interruptRequest & Flags.IF_SERIAL_IRQ) == Flags.IF_SERIAL_IRQ) {
                        memory.writeIORegisterBit(MMU.IF, Flags.IF_SERIAL_IRQ, false);
                        pushStack(pc.read());
                        pc.write(MMU.IRQ_SERIAL_VECTOR);
                        opcode_mcycle = 8;
                        return true;
                    } else if ((interruptRequest & Flags.IF_JOYPAD_IRQ) == Flags.IF_JOYPAD_IRQ) {
                        memory.writeIORegisterBit(MMU.IF, Flags.IF_JOYPAD_IRQ, false);
                        pushStack(pc.read());
                        pc.write(MMU.IRQ_INPUT_VECTOR);
                        opcode_mcycle = 8;
                        return true;
                    }
                }
                halted = false;
            }
        }
        return false;
    }

    public void fetchInstruction() {
        int opcode = readByte(pc.readInc());
        if (opcode == 0xCB) {
            next_instr = cb_opcodes.get(readByte(pc.readInc()));
            next_instr.setAddr(pc.read() - 2);
        } else {
            next_instr = opcodes.get(opcode);
            next_instr.setAddr(pc.read() - 1);
        }

        if (next_instr.getLength() == 2)
            next_instr.setParams(readByte(pc.readInc()));

        if (next_instr.getLength() == 3)
            next_instr.setParams(readByte(pc.readInc()), readByte(pc.readInc()));
    }

    public void reset() {
        switch (gameboy.mode) {
            case DMG -> {
                af.write(0x0100);
                setFlag(Flags.Z, true);
                bc.write(0x0013);
                de.write(0x00D8);
                hl.write(0x014D);
            }
            case CGB -> {
                af.write(0x1100);
                setFlag(Flags.Z, true);
                bc.write(0x0000);
                de.write(0xFF56);
                hl.write(0x000D);
            }
        }
        pc.write(bootstrap_enabled ? 0 : 0x0100);
        memory.writeRaw(MMU.BOOTSTRAP_CONTROL, bootstrap_enabled ? 0 : 1);
        sp.write(0xFFFE);
        opcode_mcycle = 0;
        ime = false;
    }

    private int readByte(int addr) {
        return memory.readByte(addr & 0xFFFF);
    }

    private void writeByte(int addr, int data) {
        memory.writeByte(addr & 0xFFFF, data & 0xFF);
    }

    private void writeWord(int addr, int data) {
        addr &= 0xFFFF;
        writeByte(addr, lsb(data));
        writeByte(addr + 1, msb(data));
    }

    private void pushStack(int data) {
        sp.dec();
        writeByte(sp.read(), msb(data));
        sp.dec();
        writeByte(sp.read(), lsb(data));
    }

    private int popStack() {
        int data = readByte(sp.read()) | (readByte(sp.read() + 1) << 8);
        sp.inc();
        sp.inc();
        return data;
    }

    private void setFlag(int flag, boolean state) {
        if (state)
            f.write(f.read() | flag);
        else
            f.write(f.read() & ~flag);
    }

    public boolean hasFlag(int flag) {
        return (f.read() & flag) == flag;
    }

    private int prefix() {
        int opcode = readByte(pc.read());
        pc.inc();
        return 4 + cb_opcodes.get(opcode).operate();
    }

    //===============REG UTILS===============//

    public void inc_regByte(RegisterByte reg) {
        reg.inc();
        setFlag(Flags.Z, reg.read() == 0x00);
        setFlag(Flags.N, false);
        setFlag(Flags.H, (reg.read() & 0x0F) == 0x00);
    }

    public void dec_regByte(RegisterByte reg) {
        setFlag(Flags.Z, reg.read() == 0x01);
        setFlag(Flags.N, true);
        setFlag(Flags.H, (reg.read() & 0x0F) == 0x00);
        reg.dec();
    }

    public void add_regWord(RegisterWord reg, int data) {
        data &= 0xFFFF;
        int result = reg.read() + data;

        setFlag(Flags.H, ((reg.read() ^ data ^ (result & 0xFFFF)) & 0x1000) == 0x1000);
        setFlag(Flags.C, (result & 0x10000) == 0x10000);
        setFlag(Flags.N, false);

        reg.write(result & 0xFFFF);
    }

    public void add_regByte(RegisterByte reg, int data) {
        data &= 0xFF;
        int result = reg.read() + data;

        setFlag(Flags.H, ((reg.read() & 0xF) + (data & 0xF)) > 0xF);
        setFlag(Flags.C, (result & 0x100) == 0x100);
        setFlag(Flags.N, false);
        setFlag(Flags.Z, (result & 0xFF) == 0x00);
        reg.write(result & 0xFF);
    }

    public void adc_regByte(RegisterByte reg, int data) {
        data &= 0xFF;
        int result = reg.read() + data + (hasFlag(Flags.C) ? 1 : 0);

        setFlag(Flags.H, ((reg.read() & 0xF) + (data & 0xF) + (hasFlag(Flags.C) ? 1 : 0)) > 0xF);
        setFlag(Flags.C, (result & 0x100) == 0x100);
        setFlag(Flags.N, false);
        setFlag(Flags.Z, (result & 0xFF) == 0x00);
        reg.write(result & 0xFF);
    }

    public void sub_regByte(RegisterByte reg, int data) {
        data &= 0xFF;
        int result = (reg.read() - data) & 0xFF;

        setFlag(Flags.H, (reg.read() & 0xF) - (data & 0xF) < 0);
        setFlag(Flags.C, reg.read() < data);
        setFlag(Flags.N, true);
        setFlag(Flags.Z, (result & 0xFF) == 0x0);

        reg.write(result);
    }

    public void sbc_regByte(RegisterByte reg, int data) {
        data &= 0xFF;
        int result = (reg.read() - data - (hasFlag(Flags.C) ? 1 : 0));

        setFlag(Flags.Z, (result & 0xFF) == 0x0);
        setFlag(Flags.N, true);
        setFlag(Flags.H, ((reg.read() & 0xF) - (data & 0xF) - (hasFlag(Flags.C) ? 1 : 0) < 0));
        setFlag(Flags.C, result < 0);

        reg.write(result & 0xFF);
    }

    public void and_regByte(RegisterByte reg, int data) {
        data &= 0xFF;
        reg.write(a.read() & data);

        setFlag(Flags.Z, reg.read() == 0x00);
        setFlag(Flags.N, false);
        setFlag(Flags.H, true);
        setFlag(Flags.C, false);
    }

    public void xor_regByte(RegisterByte reg, int data) {
        data &= 0xFF;
        reg.write(a.read() ^ data);

        setFlag(Flags.Z, reg.read() == 0x00);
        setFlag(Flags.N, false);
        setFlag(Flags.H, false);
        setFlag(Flags.C, false);
    }

    public void or_regByte(RegisterByte reg, int data) {
        data &= 0xFF;
        reg.write(a.read() | data);

        setFlag(Flags.Z, reg.read() == 0x00);
        setFlag(Flags.N, false);
        setFlag(Flags.H, false);
        setFlag(Flags.C, false);
    }

    public void cp_regByte(RegisterByte reg, int data) {
        data &= 0xFF;
        setFlag(Flags.Z, reg.read() == data);
        setFlag(Flags.N, true);
        setFlag(Flags.H, (reg.read() & 0xF) - (data & 0xF) < 0);
        setFlag(Flags.C, reg.read() < data);
    }

    public void rlc_regByte(RegisterByte reg) {
        int result = (reg.read() << 1) | ((reg.read() >> 7) & 0x01);
        result &= 0xFF;

        setFlag(Flags.Z, result == 0x00);
        setFlag(Flags.N, false);
        setFlag(Flags.H, false);
        setFlag(Flags.C, ((reg.read() >> 7) & 0x01) != 0x0);

        reg.write(result);
    }

    public void rrc_regByte(RegisterByte reg) {
        int result = ((reg.read() & 0x01) << 7) | (reg.read() >> 1);
        result &= 0xFF;

        setFlag(Flags.Z, result == 0x00);
        setFlag(Flags.N, false);
        setFlag(Flags.H, false);
        setFlag(Flags.C, (reg.read() & 0x01) != 0x0);

        reg.write(result);
    }

    public void rl_regByte(RegisterByte reg) {
        int result = (reg.read() << 1) | (hasFlag(Flags.C) ? 1 : 0);
        result &= 0xFF;

        setFlag(Flags.Z, result == 0x00);
        setFlag(Flags.N, false);
        setFlag(Flags.H, false);
        setFlag(Flags.C, ((reg.read() >> 7) & 0x01) != 0x0);

        reg.write(result);
    }

    public void rr_regByte(RegisterByte reg) {
        int result = (hasFlag(Flags.C) ? 0x80 : 0x00) | (reg.read() >> 1);
        result &= 0xFF;

        setFlag(Flags.Z, result == 0x00);
        setFlag(Flags.N, false);
        setFlag(Flags.H, false);
        setFlag(Flags.C, (reg.read() & 0x01) != 0x0);

        reg.write(result);
    }

    public void sla_regByte(RegisterByte reg) {
        int result = reg.read() << 1;
        result &= 0xFF;

        setFlag(Flags.Z, result == 0x00);
        setFlag(Flags.N, false);
        setFlag(Flags.H, false);
        setFlag(Flags.C, ((reg.read() >> 7) & 0x01) != 0x0);

        reg.write(result);
    }

    public void sra_regByte(RegisterByte reg) {
        int result = (reg.read() & 0x80) | (reg.read() >> 1);
        result &= 0xFF;

        setFlag(Flags.Z, result == 0x00);
        setFlag(Flags.N, false);
        setFlag(Flags.H, false);
        setFlag(Flags.C, (reg.read() & 0x01) != 0x0);

        reg.write(result);
    }

    public void srl_regByte(RegisterByte reg) {
        int result = reg.read() >> 1;
        result &= 0xFF;

        setFlag(Flags.Z, result == 0x00);
        setFlag(Flags.N, false);
        setFlag(Flags.H, false);
        setFlag(Flags.C, (reg.read() & 0x01) != 0x0);

        reg.write(result);
    }

    public void swap_regByte(RegisterByte reg) {
        reg.write(((reg.read() & 0x0F) << 4) | ((reg.read() & 0xF0) >> 4));

        setFlag(Flags.Z, reg.read() == 0x00);
        setFlag(Flags.N, false);
        setFlag(Flags.H, false);
        setFlag(Flags.C, false);
    }

    public void set_regByte(RegisterByte reg, int bit) {
        reg.write(reg.read() | ((0x01) << (bit & 0x07)));
    }

    public void res_regByte(RegisterByte reg, int bit) {
        reg.write(reg.read() & ~((0x01) << (bit & 0x07)));
    }

    public void bit_regByte(int bit, RegisterByte reg) {
        setFlag(Flags.Z, ((reg.read() >> bit) & 0x01) == 0x00);
        setFlag(Flags.N, false);
        setFlag(Flags.H, true);
    }

    //=================OPCODE==================//
    public int opcode_0x03_inc() {
        //INC BC
        bc.inc();
        return 8;
    }

    public int opcode_0X04_inc() {
        //INC B
        inc_regByte(b);
        return 4;
    }

    public int opcode_0x0C_inc() {
        //INC C
        inc_regByte(c);
        return 4;
    }

    public int opcode_0x13_inc() {
        //INC DE
        de.inc();
        return 8;
    }

    public int opcode_0x14_inc() {
        //INC D
        inc_regByte(d);
        return 4;
    }

    public int opcode_0x1C_inc() {
        //INC E
        inc_regByte(e);
        return 4;
    }

    public int opcode_0x23_inc() {
        //INC HL
        hl.inc();
        return 8;
    }

    public int opcode_0x24_inc() {
        //INC H
        inc_regByte(h);
        return 4;
    }

    public int opcode_0x2C_inc() {
        //INC L
        inc_regByte(l);
        return 4;
    }

    public int opcode_0x33_inc() {
        //INC SP
        sp.inc();
        return 8;
    }

    public int opcode_0x34_inc() {
        //INC (HL)
        tmp_reg.write(readByte(hl.read()));
        inc_regByte(tmp_reg);
        writeByte(hl.read(), tmp_reg.read());
        return 12;
    }

    public int opcode_0x3C_inc() {
        //INC A
        inc_regByte(a);
        return 4;
    }

    public int opcode_0x05_dec() {
        //DEC B
        dec_regByte(b);
        return 4;
    }

    public int opcode_0x0B_dec() {
        //DEC BC
        bc.dec();
        return 8;
    }

    public int opcode_0x0D_dec() {
        //DEC C
        dec_regByte(c);
        return 4;
    }

    public int opcode_0x15_dec() {
        //DEC D
        dec_regByte(d);
        return 4;
    }

    public int opcode_0x1B_dec() {
        //DEC DE
        de.dec();
        return 8;
    }

    public int opcode_0x1D_dec() {
        //DEC E
        dec_regByte(e);
        return 4;
    }

    public int opcode_0x25_dec() {
        //DEC H
        dec_regByte(h);
        return 4;
    }

    public int opcode_0x2B_dec() {
        //DEC HL
        hl.dec();
        return 8;
    }

    public int opcode_0x2D_dec() {
        //DEC L
        dec_regByte(l);
        return 4;
    }

    public int opcode_0x35_dec() {
        //DEC (HL)
        tmp_reg.write(readByte(hl.read()));
        dec_regByte(tmp_reg);
        writeByte(hl.read(), tmp_reg.read());
        return 12;
    }

    public int opcode_0x3B_dec() {
        //DEC SP
        sp.dec();
        return 8;
    }

    public int opcode_0x3D_dec() {
        //DEC A
        dec_regByte(a);
        return 4;
    }

    public int opcode_0x09_add() {
        //ADD HL, BC
        add_regWord(hl, bc.read());
        return 8;
    }

    public int opcode_0x19_add() {
        //ADD HL, DE
        add_regWord(hl, de.read());
        return 8;
    }

    public int opcode_0x29_add() {
        //ADD HL, HL
        add_regWord(hl, hl.read());
        return 8;
    }

    public int opcode_0x39_add() {
        //ADD HL, SP
        add_regWord(hl, sp.read());
        return 8;
    }

    public int opcode_0x80_add() {
        //ADD A, B
        add_regByte(a, b.read());
        return 4;
    }

    public int opcode_0x81_add() {
        //ADD A, C
        add_regByte(a, c.read());
        return 4;
    }

    public int opcode_0x82_add() {
        //ADD A, D
        add_regByte(a, d.read());
        return 4;
    }

    public int opcode_0x83_add() {
        //ADD A, E
        add_regByte(a, e.read());
        return 4;
    }

    public int opcode_0x84_add() {
        //ADD A, H
        add_regByte(a, h.read());
        return 4;
    }

    public int opcode_0x85_add() {
        //ADD A, L
        add_regByte(a, l.read());
        return 4;
    }

    public int opcode_0x86_add() {
        //ADD A, (HL)
        add_regByte(a, readByte(hl.read()));
        return 8;
    }

    public int opcode_0x87_add() {
        //ADD A, A
        add_regByte(a, a.read());
        return 4;
    }

    public int opcode_0xC6_add() {
        //ADD A, d8
        add_regByte(a, next_instr.getParamByte());
        return 8;
    }

    public int opcode_0xE8_add() {
        //ADD SP, r8
        int data = signedByte(next_instr.getParamByte());
        int result = sp.read() + data;

        setFlag(Flags.Z, false);
        setFlag(Flags.N, false);
        setFlag(Flags.H, ((sp.read() ^ data ^ (result & 0xFFFF)) & 0x10) == 0x10);
        setFlag(Flags.C, ((sp.read() ^ data ^ (result & 0xFFFF)) & 0x100) == 0x100);

        sp.write(result & 0xFFFF);
        return 16;
    }

    public int opcode_0x27_daa() {
        //DAA
        int result = 0;
        if (hasFlag(Flags.H) || (!hasFlag(Flags.N) && (a.read() & 0xF) > 0x9))
            result = 6;

        if (hasFlag(Flags.C) || (!hasFlag(Flags.N) && a.read() > 0x99)) {
            result |= 0x60;
            setFlag(Flags.C, true);
        }

        a.write(a.read() + (hasFlag(Flags.N) ? -result : result));
        setFlag(Flags.Z, a.read() == 0);
        setFlag(Flags.H, false);

        return 4;
    }

    public int opcode_0x2F_cpl() {
        //CPL
        a.write(~a.read());
        setFlag(Flags.N, true);
        setFlag(Flags.H, true);
        return 4;
    }

    public int opcode_0x37_scf() {
        //SCF
        setFlag(Flags.N, false);
        setFlag(Flags.H, false);
        setFlag(Flags.C, true);
        return 4;
    }

    public int opcode_0x3F_ccf() {
        //CCF
        setFlag(Flags.N, false);
        setFlag(Flags.H, false);
        setFlag(Flags.C, !hasFlag(Flags.C));
        return 4;
    }

    public int opcode_0x88_adc() {
        //ADC A, B
        adc_regByte(a, b.read());
        return 4;
    }

    public int opcode_0x89_adc() {
        //ADC A, C
        adc_regByte(a, c.read());
        return 4;
    }

    public int opcode_0x8A_adc() {
        //ADC A, D
        adc_regByte(a, d.read());
        return 4;
    }

    public int opcode_0x8B_adc() {
        //ADC A, E
        adc_regByte(a, e.read());
        return 4;
    }

    public int opcode_0x8C_adc() {
        //ADC A, H
        adc_regByte(a, h.read());
        return 4;
    }

    public int opcode_0x8D_adc() {
        //ADC A, L
        adc_regByte(a, l.read());
        return 4;
    }

    public int opcode_0x8E_adc() {
        //ADD A, (HL)
        adc_regByte(a, readByte(hl.read()));
        return 8;
    }

    public int opcode_0x8F_adc() {
        //ADD A, A
        adc_regByte(a, a.read());
        return 4;
    }

    public int opcode_0xCE_adc() {
        //ADC A, d8
        adc_regByte(a, next_instr.getParamByte());
        return 8;
    }

    public int opcode_0x90_sub() {
        //SUB B
        sub_regByte(a, b.read());
        return 4;
    }

    public int opcode_0x91_sub() {
        //SUB C
        sub_regByte(a, c.read());
        return 4;
    }

    public int opcode_0x92_sub() {
        //SUB D
        sub_regByte(a, d.read());
        return 4;
    }

    public int opcode_0x93_sub() {
        //SUB E
        sub_regByte(a, e.read());
        return 4;
    }

    public int opcode_0x94_sub() {
        //SUB H
        sub_regByte(a, h.read());
        return 4;
    }

    public int opcode_0x95_sub() {
        //SUB L
        sub_regByte(a, l.read());
        return 4;
    }

    public int opcode_0x96_sub() {
        //SUB (HL)
        sub_regByte(a, readByte(hl.read()));
        return 8;
    }

    public int opcode_0x97_sub() {
        //SUB A
        sub_regByte(a, a.read());
        return 4;
    }

    public int opcode_0xD6_sub() {
        //SUB d8
        sub_regByte(a, next_instr.getParamByte());
        return 8;
    }

    public int opcode_0x98_sbc() {
        //SBC A, B
        sbc_regByte(a, b.read());
        return 4;
    }

    public int opcode_0x99_sbc() {
        //SBC A,C
        sbc_regByte(a, c.read());
        return 4;
    }

    public int opcode_0x9A_sbc() {
        //SBC A, D
        sbc_regByte(a, d.read());
        return 4;
    }

    public int opcode_0x9B_sbc() {
        //SBC A, E
        sbc_regByte(a, e.read());
        return 4;
    }

    public int opcode_0x9C_sbc() {
        //SBC A, H
        sbc_regByte(a, h.read());
        return 4;
    }

    public int opcode_0x9D_sbc() {
        //SBC A, L
        sbc_regByte(a, l.read());
        return 4;
    }

    public int opcode_0x9E_sbc() {
        //SBC A, (HL)
        sbc_regByte(a, readByte(hl.read()));
        return 8;
    }

    public int opcode_0x9F_sbc() {
        //SBC A, A
        sbc_regByte(a, a.read());
        return 4;
    }

    public int opcode_0xDE_sbc() {
        //SBC A, d8
        sbc_regByte(a, next_instr.getParamByte());
        return 8;
    }

    public int opcode_0xA0_and() {
        //AND B
        and_regByte(a, b.read());
        return 4;
    }

    public int opcode_0xA1_and() {
        //AND C
        and_regByte(a, c.read());
        return 4;
    }

    public int opcode_0xA2_and() {
        //AND D
        and_regByte(a, d.read());
        return 4;
    }

    public int opcode_0xA3_and() {
        //AND E
        and_regByte(a, e.read());
        return 4;
    }

    public int opcode_0xA4_and() {
        //AND H
        and_regByte(a, h.read());
        return 4;
    }

    public int opcode_0xA5_and() {
        //AND L
        and_regByte(a, l.read());
        return 4;
    }

    public int opcode_0xA6_and() {
        //AND (HL)
        and_regByte(a, readByte(hl.read()));
        return 8;
    }

    public int opcode_0xA7_and() {
        //AND A
        and_regByte(a, a.read());
        return 4;
    }

    public int opcode_0xE6_and() {
        //AND d8
        and_regByte(a, next_instr.getParamByte());
        return 8;
    }

    public int opcode_0xA8_xor() {
        //XOR B
        xor_regByte(a, b.read());
        return 4;
    }

    public int opcode_0xA9_xor() {
        //XOR C
        xor_regByte(a, c.read());
        return 4;
    }

    public int opcode_0xAA_xor() {
        //XOR D
        xor_regByte(a, d.read());
        return 4;
    }

    public int opcode_0xAB_xor() {
        //XOR E
        xor_regByte(a, e.read());
        return 4;
    }

    public int opcode_0xAC_xor() {
        //XOR H
        xor_regByte(a, h.read());
        return 4;
    }

    public int opcode_0xAD_xor() {
        //XOR L
        xor_regByte(a, l.read());
        return 4;
    }

    public int opcode_0xAE_xor() {
        //XOR (HL)
        xor_regByte(a, readByte(hl.read()));
        return 8;
    }

    public int opcode_0xAF_xor() {
        //XOR A
        xor_regByte(a, a.read());
        return 4;
    }

    public int opcode_0xEE_xor() {
        //XOR d8
        xor_regByte(a, next_instr.getParamByte());
        return 8;
    }

    public int opcode_0xB0_or() {
        //OR B
        or_regByte(a, b.read());
        return 4;
    }

    public int opcode_0xB1_or() {
        //OR C
        or_regByte(a, c.read());
        return 4;
    }

    public int opcode_0xB2_or() {
        //OR D
        or_regByte(a, d.read());
        return 4;
    }

    public int opcode_0xB3_or() {
        //OR E
        or_regByte(a, e.read());
        return 4;
    }

    public int opcode_0xB4_or() {
        //OR H
        or_regByte(a, h.read());
        return 4;
    }

    public int opcode_0xB5_or() {
        //OR L
        or_regByte(a, l.read());
        return 4;
    }

    public int opcode_0xB6_or() {
        //OR (HL)
        or_regByte(a, readByte(hl.read()));
        return 8;
    }

    public int opcode_0xB7_or() {
        //OR A
        or_regByte(a, a.read());
        return 4;
    }

    public int opcode_0xF6_or() {
        //OR d8
        or_regByte(a, next_instr.getParamByte());
        return 8;
    }

    public int opcode_0xB8_cp() {
        // CP B
        cp_regByte(a, b.read());
        return 4;
    }

    public int opcode_0xB9_cp() {
        // CP C
        cp_regByte(a, c.read());
        return 4;
    }

    public int opcode_0xBA_cp() {
        // CP D
        cp_regByte(a, d.read());
        return 4;
    }

    public int opcode_0xBB_cp() {
        // CP E
        cp_regByte(a, e.read());
        return 4;
    }

    public int opcode_0xBC_cp() {
        // CP H
        cp_regByte(a, h.read());
        return 4;
    }

    public int opcode_0xBD_cp() {
        // CP L
        cp_regByte(a, l.read());
        return 4;
    }

    public int opcode_0xBE_cp() {
        // CP (HL)
        cp_regByte(a, readByte(hl.read()));
        return 8;
    }

    public int opcode_0xBF_cp() {
        // CP A
        cp_regByte(a, a.read());
        return 4;
    }

    public int opcode_0xFE_cp() {
        // CP d8
        cp_regByte(a, next_instr.getParamByte());
        return 8;
    }

    public int opcode_0x18_jr() {
        //JR r8
        pc.write(pc.read() + signedByte(next_instr.getParamByte()));
        return 12;
    }

    public int opcode_0x20_jr() {
        //JR NZ r8
        if (!hasFlag(Flags.Z)) {
            pc.write(pc.read() + signedByte(next_instr.getParamByte()));
            return 12;
        }
        return 8;
    }

    public int opcode_0x28_jr() {
        //JR Z r8
        if (hasFlag(Flags.Z)) {
            pc.write(pc.read() + signedByte(next_instr.getParamByte()));
            return 12;
        }
        return 8;
    }

    public int opcode_0x30_jr() {
        //JR NC r8
        if (!hasFlag(Flags.C)) {
            pc.write(pc.read() + signedByte(next_instr.getParamByte()));
            return 12;
        }
        return 8;
    }

    public int opcode_0x38_jr() {
        //JR C r8
        if (hasFlag(Flags.C)) {
            pc.write(pc.read() + signedByte(next_instr.getParamByte()));
            return 12;
        }
        return 8;
    }

    public int opcode_0xC0_ret() {
        //RET NZ
        if (!hasFlag(Flags.Z)) {
            pc.write(popStack());
            return 20;
        }
        return 8;
    }

    public int opcode_0xC8_ret() {
        //RET Z
        if (hasFlag(Flags.Z)) {
            pc.write(popStack());
            return 20;
        }
        return 8;
    }

    public int opcode_0xC9_ret() {
        //RET
        pc.write(popStack());
        return 16;
    }

    public int opcode_0xD0_ret() {
        //RET NC
        if (!hasFlag(Flags.C)) {
            pc.write(popStack());
            return 20;
        }
        return 8;
    }

    public int opcode_0xD8_ret() {
        //RET C
        if (hasFlag(Flags.C)) {
            pc.write(popStack());
            return 20;
        }
        return 8;
    }

    public int opcode_0xC2_jp() {
        //JP NZ a16
        if (!hasFlag(Flags.Z)) {
            pc.write(next_instr.getParamWord());
            return 16;
        }
        return 12;
    }

    public int opcode_0xC3_jp() {
        //JP a16
        pc.write(next_instr.getParamWord());
        return 16;
    }

    public int opcode_0xCA_jp() {
        //JP Z a16
        if (hasFlag(Flags.Z)) {
            pc.write(next_instr.getParamWord());
            return 16;
        }
        return 12;
    }

    public int opcode_0xD2_jp() {
        //JP NC a16
        if (!hasFlag(Flags.C)) {
            pc.write(next_instr.getParamWord());
            return 16;
        }
        return 12;
    }

    public int opcode_0xDA_jp() {
        //JP C a16
        if (hasFlag(Flags.C)) {
            pc.write(next_instr.getParamWord());
            return 16;
        }
        return 12;
    }

    public int opcode_0xE9_jp() {
        //JP (HL)
        pc.write(hl.read());
        return 4;
    }

    public int opcode_0xC4_call() {
        //CALL NZ a16
        if (!hasFlag(Flags.Z)) {
            pushStack(pc.read());
            pc.write(next_instr.getParamWord());
            return 24;
        }
        return 12;
    }

    public int opcode_0xCC_call() {
        //CALL Z a16
        if (hasFlag(Flags.Z)) {
            pushStack(pc.read());
            pc.write(next_instr.getParamWord());
            return 24;
        }
        return 12;
    }

    public int opcode_0xCD_call() {
        //CALL a16
        pushStack(pc.read());
        pc.write(next_instr.getParamWord());
        return 24;
    }

    public int opcode_0xD4_call() {
        //CALL NC a16
        if (!hasFlag(Flags.C)) {
            pushStack(pc.read());
            pc.write(next_instr.getParamWord());
            return 24;
        }
        return 12;
    }

    public int opcode_0xDC_call() {
        //CALL C a16
        if (hasFlag(Flags.C)) {
            pushStack(pc.read());
            pc.write(next_instr.getParamWord());
            return 24;
        }
        return 12;
    }

    public int opcode_0xC7_rst() {
        //RST 00H
        pushStack(pc.read());
        pc.write(0x0000);
        return 16;
    }

    public int opcode_0xCF_rst() {
        //RST 08H
        pushStack(pc.read());
        pc.write(0x0008);
        return 16;
    }

    public int opcode_0xD7_rst() {
        //RST 10H
        pushStack(pc.read());
        pc.write(0x0010);
        return 16;
    }

    public int opcode_0xDF_rst() {
        //RST 18H
        pushStack(pc.read());
        pc.write(0x0018);
        return 16;
    }

    public int opcode_0xE7_rst() {
        //RST 20H
        pushStack(pc.read());
        pc.write(0x0020);
        return 16;
    }

    public int opcode_0xEF_rst() {
        //RST 28H
        pushStack(pc.read());
        pc.write(0x0028);
        return 16;
    }

    public int opcode_0xF7_rst() {
        //RST 30H
        pushStack(pc.read());
        pc.write(0x0030);
        return 16;
    }

    public int opcode_0xFF_rst() {
        //RST 38H
        pushStack(pc.read());
        pc.write(0x0038);
        return 16;
    }

    public int opcode_0xD9_reti() {
        //RETI
        pc.write(popStack());
        ime = true;
        return 16;
    }

    public int opcode_0x00_nop() {
        //NOP
        return 4;
    }

    public int opcode_0x07_rlca() {
        //RLCA
        rlc_regByte(a);
        setFlag(Flags.Z, false);
        return 4;
    }

    public int opcode_0x0F_rrca() {
        //RRCA
        rrc_regByte(a);
        setFlag(Flags.Z, false);
        return 4;
    }

    public int opcode_0x10_stop() {
        //STOP 0
        return 4;
    }

    public int opcode_0x17_rla() {
        //RLA
        rl_regByte(a);
        setFlag(Flags.Z, false);
        return 4;
    }

    public int opcode_0x1F_rra() {
        //RRA
        rr_regByte(a);
        setFlag(Flags.Z, false);
        return 4;
    }

    public int opcode_0x76_halt() {
        //HALT
        halted = true;
        return 4;
    }

    public int opcode_0xF3_di() {
        //DI
        ime = false;
        return 4;
    }

    public int opcode_0xFB_ei() {
        //EI
        ime = true;
        return 4;
    }

    public int opcode_0x01_ld() {
        //LD BC,d16
        bc.write(next_instr.getParamWord());
        return 12;
    }

    public int opcode_0x02_ld() {
        //LD (BC),A
        writeByte(bc.read(), a.read());
        return 8;
    }

    public int opcode_0x06_ld() {
        //LD B,d8
        b.write(next_instr.getParamByte());
        return 8;
    }

    public int opcode_0x08_ld() {
        //LD (a16),SP
        writeWord(next_instr.getParamWord(), sp.read());
        return 20;
    }

    public int opcode_0x0A_ld() {
        //LD A,(BC)
        a.write(readByte(bc.read()));
        return 8;
    }

    public int opcode_0x0E_ld() {
        //LD C,d8
        c.write(next_instr.getParamByte());
        return 8;
    }

    public int opcode_0x11_ld() {
        //LD DE,d16
        de.write(next_instr.getParamWord());
        return 12;
    }

    public int opcode_0x12_ld() {
        //LD (DE),A
        writeByte(de.read(), a.read());
        return 8;
    }

    public int opcode_0x16_ld() {
        //LD D,d8
        d.write(next_instr.getParamByte());
        return 8;
    }

    public int opcode_0x1A_ld() {
        //LD A,(DE)
        a.write(readByte(de.read()));
        return 8;
    }

    public int opcode_0x1E_ld() {
        //LD E,d8
        e.write(next_instr.getParamByte());
        return 8;
    }

    public int opcode_0x21_ld() {
        //LD HL,d16
        hl.write(next_instr.getParamWord());
        return 12;
    }

    public int opcode_0x22_ld() {
        //LD (HL+),A
        writeByte(hl.read(), a.read());
        hl.inc();
        return 8;
    }

    public int opcode_0x26_ld() {
        //LD H,d8
        h.write(next_instr.getParamByte());
        return 8;
    }

    public int opcode_0x2A_ld() {
        //LD A,(HL+)
        a.write(readByte(hl.read()));
        hl.inc();
        return 8;
    }

    public int opcode_0x2E_ld() {
        //LD L,d8
        l.write(next_instr.getParamByte());
        return 8;
    }

    public int opcode_0x31_ld() {
        //LD SP,d16
        sp.write(next_instr.getParamWord());
        return 12;
    }

    public int opcode_0x32_ld() {
        //LD (HL-),A
        writeByte(hl.read(), a.read());
        hl.dec();
        return 8;
    }

    public int opcode_0x36_ld() {
        //LD (HL),d8
        writeByte(hl.read(), next_instr.getParamByte());
        return 12;
    }

    public int opcode_0x3A_ld() {
        //LD A,(HL-)
        a.write(readByte(hl.read()));
        hl.dec();
        return 8;
    }

    public int opcode_0x3E_ld() {
        //LD A,d8
        a.write(next_instr.getParamByte());
        return 8;
    }

    public int opcode_0x40_ld() {
        //LD B,B
        b.write(b.read());
        return 4;
    }

    public int opcode_0x41_ld() {
        //LD B,C
        b.write(c.read());
        return 4;
    }

    public int opcode_0x42_ld() {
        //LD B,D
        b.write(d.read());
        return 4;
    }

    public int opcode_0x43_ld() {
        //LD B,E
        b.write(e.read());
        return 4;
    }

    public int opcode_0x44_ld() {
        //LD B,H
        b.write(h.read());
        return 4;
    }

    public int opcode_0x45_ld() {
        //LD B,L
        b.write(l.read());
        return 4;
    }

    public int opcode_0x46_ld() {
        //LD B,(HL)
        b.write(readByte(hl.read()));
        return 8;
    }

    public int opcode_0x47_ld() {
        //LD B,A
        b.write(a.read());
        return 4;
    }

    public int opcode_0x48_ld() {
        //LD C,B
        c.write(b.read());
        return 4;
    }

    public int opcode_0x49_ld() {
        //LD C,C
        c.write(c.read());
        return 4;
    }

    public int opcode_0x4A_ld() {
        //LD C,D
        c.write(d.read());
        return 4;
    }

    public int opcode_0x4B_ld() {
        //LD C,E
        c.write(e.read());
        return 4;
    }

    public int opcode_0x4C_ld() {
        //LD C,H
        c.write(h.read());
        return 4;
    }

    public int opcode_0x4D_ld() {
        //LD C,L
        c.write(l.read());
        return 4;
    }

    public int opcode_0x4E_ld() {
        //LD C,(HL)
        c.write(readByte(hl.read()));
        return 8;
    }

    public int opcode_0x4F_ld() {
        //LD C,A
        c.write(a.read());
        return 4;
    }

    public int opcode_0x50_ld() {
        //LD D,B
        d.write(b.read());
        return 4;
    }

    public int opcode_0x51_ld() {
        //LD D,C
        d.write(c.read());
        return 4;
    }

    public int opcode_0x52_ld() {
        //LD D,D
        d.write(d.read());
        return 4;
    }

    public int opcode_0x53_ld() {
        //LD D,E
        d.write(e.read());
        return 4;
    }

    public int opcode_0x54_ld() {
        //LD D,H
        d.write(h.read());
        return 4;
    }

    public int opcode_0x55_ld() {
        //LD D,L
        d.write(l.read());
        return 4;
    }

    public int opcode_0x56_ld() {
        //LD D,(HL)
        d.write(readByte(hl.read()));
        return 8;
    }

    public int opcode_0x57_ld() {
        //LD D,A
        d.write(a.read());
        return 4;
    }

    public int opcode_0x58_ld() {
        //LD E,B
        e.write(b.read());
        return 4;
    }

    public int opcode_0x59_ld() {
        //LD E,C
        e.write(c.read());
        return 4;
    }

    public int opcode_0x5A_ld() {
        //LD E,D
        e.write(d.read());
        return 4;
    }

    public int opcode_0x5B_ld() {
        //LD E,E
        e.write(e.read());
        return 4;
    }

    public int opcode_0x5C_ld() {
        //LD E,H
        e.write(h.read());
        return 4;
    }

    public int opcode_0x5D_ld() {
        //LD E,L
        e.write(l.read());
        return 4;
    }

    public int opcode_0x5E_ld() {
        //LD E,(HL)
        e.write(readByte(hl.read()));
        return 8;
    }

    public int opcode_0x5F_ld() {
        //LD E,A
        e.write(a.read());
        return 4;
    }

    public int opcode_0x60_ld() {
        //LD H,B
        h.write(b.read());
        return 4;
    }

    public int opcode_0x61_ld() {
        //LD H,C
        h.write(c.read());
        return 4;
    }

    public int opcode_0x62_ld() {
        //LD H,D
        h.write(d.read());
        return 4;
    }

    public int opcode_0x63_ld() {
        //LD H,E
        h.write(e.read());
        return 4;
    }

    public int opcode_0x64_ld() {
        //LD H,H
        h.write(h.read());
        return 4;
    }

    public int opcode_0x65_ld() {
        //LD H,L
        h.write(l.read());
        return 4;
    }

    public int opcode_0x66_ld() {
        //LD H,(HL)
        h.write(readByte(hl.read()));
        return 8;
    }

    public int opcode_0x67_ld() {
        //LD H,A
        h.write(a.read());
        return 4;
    }

    public int opcode_0x68_ld() {
        //LD L,B
        l.write(b.read());
        return 4;
    }

    public int opcode_0x69_ld() {
        //LD L,C
        l.write(c.read());
        return 4;
    }

    public int opcode_0x6A_ld() {
        //LD L,D
        l.write(d.read());
        return 4;
    }

    public int opcode_0x6B_ld() {
        //LD L,E
        l.write(e.read());
        return 4;
    }

    public int opcode_0x6C_ld() {
        //LD L,H
        l.write(h.read());
        return 4;
    }

    public int opcode_0x6D_ld() {
        //LD L,L
        l.write(l.read());
        return 4;
    }

    public int opcode_0x6E_ld() {
        //LD L,(HL)
        l.write(readByte(hl.read()));
        return 8;
    }

    public int opcode_0x6F_ld() {
        //LD L,A
        l.write(a.read());
        return 4;
    }

    public int opcode_0x70_ld() {
        //LD (HL),B
        writeByte(hl.read(), b.read());
        return 8;
    }

    public int opcode_0x71_ld() {
        //LD (HL),C
        writeByte(hl.read(), c.read());
        return 8;
    }

    public int opcode_0x72_ld() {
        //LD (HL),D
        writeByte(hl.read(), d.read());
        return 8;
    }

    public int opcode_0x73_ld() {
        //LD (HL),E
        writeByte(hl.read(), e.read());
        return 8;
    }

    public int opcode_0x74_ld() {
        //LD (HL),H
        writeByte(hl.read(), h.read());
        return 8;
    }

    public int opcode_0x75_ld() {
        //LD (HL),L
        writeByte(hl.read(), l.read());
        return 8;
    }

    public int opcode_0x77_ld() {
        //LD (HL),A
        writeByte(hl.read(), a.read());
        return 8;
    }

    public int opcode_0x78_ld() {
        //LD A,B
        a.write(b.read());
        return 4;
    }

    public int opcode_0x79_ld() {
        //LD A,C
        a.write(c.read());
        return 4;
    }

    public int opcode_0x7A_ld() {
        //LD A,D
        a.write(d.read());
        return 4;
    }

    public int opcode_0x7B_ld() {
        //LD A,E
        a.write(e.read());
        return 4;
    }

    public int opcode_0x7C_ld() {
        //LD A,H
        a.write(h.read());
        return 4;
    }

    public int opcode_0x7D_ld() {
        //LD A,L
        a.write(l.read());
        return 4;
    }

    public int opcode_0x7E_ld() {
        //LD A,(HL)
        a.write(readByte(hl.read()));
        return 8;
    }

    public int opcode_0x7F_ld() {
        //LD A,A
        a.write(a.read());
        return 4;
    }

    public int opcode_0xE2_ld() {
        //LD (C),A
        writeByte(0xFF00 + c.read(), a.read());
        return 8;
    }

    public int opcode_0xEA_ld() {
        //LD (a16),A
        writeByte(next_instr.getParamWord(), a.read());
        return 16;
    }

    public int opcode_0xF2_ld() {
        //LD A,(C)
        a.write(readByte(0xFF00 + c.read()));
        return 8;
    }

    public int opcode_0xF8_ld() {
        //LD HL,SP+r8
        int signedValue = signedByte(next_instr.getParamByte());
        int result = sp.read() + signedValue;

        setFlag(Flags.Z, false);
        setFlag(Flags.N, false);
        setFlag(Flags.H, ((sp.read() ^ signedValue ^ (result & 0xFFFF)) & 0x10) == 0x10);
        setFlag(Flags.C, ((sp.read() ^ signedValue ^ (result & 0xFFFF)) & 0x100) == 0x100);

        hl.write(result & 0xFFFF);
        return 12;
    }

    public int opcode_0xF9_ld() {
        //LD SP,HL
        sp.write(hl.read());
        return 8;
    }

    public int opcode_0xFA_ld() {
        //LD A,(a16)
        a.write(readByte(next_instr.getParamWord()));
        return 16;
    }

    public int opcode_0xC1_pop() {
        //POP BC
        bc.write(popStack());
        return 12;
    }

    public int opcode_0xD1_pop() {
        //POP DE
        de.write(popStack());
        return 12;
    }

    public int opcode_0xE1_pop() {
        //POP HL
        hl.write(popStack());
        return 12;
    }

    public int opcode_0xF1_pop() {
        //POP AF
        // On Pop AF the Bits 0-3 are ignored
        af.write(popStack() & 0xFFF0);
        return 12;
    }

    public int opcode_0xC5_push() {
        //PUSH BC
        pushStack(bc.read());
        return 16;
    }

    public int opcode_0xD5_push() {
        //PUSH DE
        pushStack(de.read());
        return 16;
    }

    public int opcode_0xE5_push() {
        //PUSH HL
        pushStack(hl.read());
        return 16;
    }

    public int opcode_0xF5_push() {
        //PUSH AF
        pushStack(af.read());
        return 16;
    }

    public int opcode_0xE0_ldh() {
        //LDH (a8) A
        writeByte(0xFF00 + next_instr.getParamByte(), a.read());
        return 12;
    }

    public int opcode_0xF0_ldh() {
        //LDH A,(a8)
        a.write(readByte(0xFF00 + next_instr.getParamByte()));
        return 12;
    }

    public int opcode_0xCB00_rlc() {
        //RLC B
        rlc_regByte(b);
        return 8;
    }

    public int opcode_0xCB01_rlc() {
        //RLC C
        rlc_regByte(c);
        return 8;
    }

    public int opcode_0xCB02_rlc() {
        //RLC D
        rlc_regByte(d);
        return 8;
    }

    public int opcode_0xCB03_rlc() {
        //RLC E
        rlc_regByte(e);
        return 8;
    }

    public int opcode_0xCB04_rlc() {
        //RLC H
        rlc_regByte(h);
        return 8;
    }

    public int opcode_0xCB05_rlc() {
        //RLC L
        rlc_regByte(l);
        return 8;
    }

    public int opcode_0xCB06_rlc() {
        //RLC (HL)
        tmp_reg.write(readByte(hl.read()));
        rlc_regByte(tmp_reg);
        writeByte(hl.read(), tmp_reg.read());
        return 16;
    }

    public int opcode_0xCB07_rlc() {
        //RLC A
        rlc_regByte(a);
        return 8;
    }

    public int opcode_0xCB08_rrc() {
        //RRC B
        rrc_regByte(b);
        return 8;
    }

    public int opcode_0xCB09_rrc() {
        //RRC C
        rrc_regByte(c);
        return 8;
    }

    public int opcode_0xCB0A_rrc() {
        //RRC D
        rrc_regByte(d);
        return 8;
    }

    public int opcode_0xCB0B_rrc() {
        //RRC E
        rrc_regByte(e);
        return 8;
    }

    public int opcode_0xCB0C_rrc() {
        //RRC H
        rrc_regByte(h);
        return 8;
    }

    public int opcode_0xCB0D_rrc() {
        //RRC L
        rrc_regByte(l);
        return 8;
    }

    public int opcode_0xCB0E_rrc() {
        //RRC (HL)
        tmp_reg.write(readByte(hl.read()));
        rrc_regByte(tmp_reg);
        writeByte(hl.read(), tmp_reg.read());
        return 16;
    }

    public int opcode_0xCB0F_rrc() {
        //RRC A
        rrc_regByte(a);
        return 8;
    }

    public int opcode_0xCB10_rl() {
        //RL B
        rl_regByte(b);
        return 8;
    }

    public int opcode_0xCB11_rl() {
        //RL C
        rl_regByte(c);
        return 8;
    }

    public int opcode_0xCB12_rl() {
        //RL D
        rl_regByte(d);
        return 8;
    }

    public int opcode_0xCB13_rl() {
        //RL E
        rl_regByte(e);
        return 8;
    }

    public int opcode_0xCB14_rl() {
        //RL H
        rl_regByte(h);
        return 8;
    }

    public int opcode_0xCB15_rl() {
        //RL L
        rl_regByte(l);
        return 8;
    }

    public int opcode_0xCB16_rl() {
        //RL (HL)
        tmp_reg.write(readByte(hl.read()));
        rl_regByte(tmp_reg);
        writeByte(hl.read(), tmp_reg.read());
        return 16;
    }

    public int opcode_0xCB17_rl() {
        //RL A
        rl_regByte(a);
        return 8;
    }

    public int opcode_0xCB18_rr() {
        //RR B
        rr_regByte(b);
        return 8;
    }

    public int opcode_0xCB19_rr() {
        //RR C
        rr_regByte(c);
        return 8;
    }

    public int opcode_0xCB1A_rr() {
        //RR D
        rr_regByte(d);
        return 8;
    }

    public int opcode_0xCB1B_rr() {
        //RR E
        rr_regByte(e);
        return 8;
    }

    public int opcode_0xCB1C_rr() {
        //RR H
        rr_regByte(h);
        return 8;
    }

    public int opcode_0xCB1D_rr() {
        //RR L
        rr_regByte(l);
        return 8;
    }

    public int opcode_0xCB1E_rr() {
        //RR (HL)
        tmp_reg.write(readByte(hl.read()));
        rr_regByte(tmp_reg);
        writeByte(hl.read(), tmp_reg.read());
        return 16;
    }

    public int opcode_0xCB1F_rr() {
        //RR A
        rr_regByte(a);
        return 8;
    }

    public int opcode_0xCB20_sla() {
        //SLA B
        sla_regByte(b);
        return 8;
    }

    public int opcode_0xCB21_sla() {
        //SLA C
        sla_regByte(c);
        return 8;
    }

    public int opcode_0xCB22_sla() {
        //SLA D
        sla_regByte(d);
        return 8;
    }

    public int opcode_0xCB23_sla() {
        //SLA E
        sla_regByte(e);
        return 8;
    }

    public int opcode_0xCB24_sla() {
        //SLA H
        sla_regByte(h);
        return 8;
    }

    public int opcode_0xCB25_sla() {
        //SLA L
        sla_regByte(l);
        return 8;
    }

    public int opcode_0xCB26_sla() {
        //SLA (HL)
        tmp_reg.write(readByte(hl.read()));
        sla_regByte(tmp_reg);
        writeByte(hl.read(), tmp_reg.read());
        return 16;
    }

    public int opcode_0xCB27_sla() {
        //SLA A
        sla_regByte(a);
        return 8;
    }

    public int opcode_0xCB28_sra() {
        //SRA B
        sra_regByte(b);
        return 8;
    }

    public int opcode_0xCB29_sra() {
        //SRA C
        sra_regByte(c);
        return 8;
    }

    public int opcode_0xCB2A_sra() {
        //SRA D
        sra_regByte(d);
        return 8;
    }

    public int opcode_0xCB2B_sra() {
        //SRA E
        sra_regByte(e);
        return 8;
    }

    public int opcode_0xCB2C_sra() {
        //SRA H
        sra_regByte(h);
        return 8;
    }

    public int opcode_0xCB2D_sra() {
        //SRA L
        sra_regByte(l);
        return 8;
    }

    public int opcode_0xCB2E_sra() {
        //SRA (HL)
        tmp_reg.write(readByte(hl.read()));
        sra_regByte(tmp_reg);
        writeByte(hl.read(), tmp_reg.read());
        return 16;
    }

    public int opcode_0xCB2F_sra() {
        //SRA A
        sra_regByte(a);
        return 8;
    }

    public int opcode_0xCB30_swap() {
        //SWAP B
        swap_regByte(b);
        return 8;
    }

    public int opcode_0xCB31_swap() {
        //SWAP C
        swap_regByte(c);
        return 8;
    }

    public int opcode_0xCB32_swap() {
        //SWAP D
        swap_regByte(d);
        return 8;
    }

    public int opcode_0xCB33_swap() {
        //SWAP E
        swap_regByte(e);
        return 8;
    }

    public int opcode_0xCB34_swap() {
        //SWAP H
        swap_regByte(h);
        return 8;
    }

    public int opcode_0xCB35_swap() {
        //SWAP L
        swap_regByte(l);
        return 8;
    }

    public int opcode_0xCB36_swap() {
        //SWAP (HL)
        tmp_reg.write(readByte(hl.read()));
        swap_regByte(tmp_reg);
        writeByte(hl.read(), tmp_reg.read());
        return 16;
    }

    public int opcode_0xCB37_swap() {
        //SWAP A
        swap_regByte(a);
        return 8;
    }

    public int opcode_0xCB38_srl() {
        //SRL B
        srl_regByte(b);
        return 8;
    }

    public int opcode_0xCB39_srl() {
        //SRL C
        srl_regByte(c);
        return 8;
    }

    public int opcode_0xCB3A_srl() {
        //SRL D
        srl_regByte(d);
        return 8;
    }

    public int opcode_0xCB3B_srl() {
        //SRL E
        srl_regByte(e);
        return 8;
    }

    public int opcode_0xCB3C_srl() {
        //SRL H
        srl_regByte(h);
        return 8;
    }

    public int opcode_0xCB3D_srl() {
        //SRL L
        srl_regByte(l);
        return 8;
    }

    public int opcode_0xCB3E_srl() {
        //SRL (HL)
        tmp_reg.write(readByte(hl.read()));
        srl_regByte(tmp_reg);
        writeByte(hl.read(), tmp_reg.read());
        return 16;
    }

    public int opcode_0xCB3F_srl() {
        //SRL A
        srl_regByte(a);
        return 8;
    }

    public int opcode_0xCB40_bit() {
        //BIT 0,B
        bit_regByte(0, b);
        return 8;
    }

    public int opcode_0xCB41_bit() {
        //BIT 0,C
        bit_regByte(0, c);
        return 8;
    }

    public int opcode_0xCB42_bit() {
        //BIT 0,D
        bit_regByte(0, d);
        return 8;
    }

    public int opcode_0xCB43_bit() {
        //BIT 0,E
        bit_regByte(0, e);
        return 8;
    }

    public int opcode_0xCB44_bit() {
        //BIT 0,H
        bit_regByte(0, h);
        return 8;
    }

    public int opcode_0xCB45_bit() {
        //BIT 0,L
        bit_regByte(0, l);
        return 8;
    }

    public int opcode_0xCB46_bit() {
        //BIT 0,(HL)
        tmp_reg.write(readByte(hl.read()));
        bit_regByte(0, tmp_reg);
        return 12;
    }

    public int opcode_0xCB47_bit() {
        //BIT 0,A
        bit_regByte(0, a);
        return 8;
    }

    public int opcode_0xCB48_bit() {
        //BIT 1,B
        bit_regByte(1, b);
        return 8;
    }

    public int opcode_0xCB49_bit() {
        //BIT 1,C
        bit_regByte(1, c);
        return 8;
    }

    public int opcode_0xCB4A_bit() {
        //BIT 1,D
        bit_regByte(1, d);
        return 8;
    }

    public int opcode_0xCB4B_bit() {
        //BIT 1,E
        bit_regByte(1, e);
        return 8;
    }

    public int opcode_0xCB4C_bit() {
        //BIT 1,H
        bit_regByte(1, h);
        return 8;
    }

    public int opcode_0xCB4D_bit() {
        //BIT 1,L
        bit_regByte(1, l);
        return 8;
    }

    public int opcode_0xCB4E_bit() {
        //BIT 1,(HL)
        tmp_reg.write(readByte(hl.read()));
        bit_regByte(1, tmp_reg);
        return 12;
    }

    public int opcode_0xCB4F_bit() {
        //BIT 1,A
        bit_regByte(1, a);
        return 8;
    }

    public int opcode_0xCB50_bit() {
        //BIT 2,B
        bit_regByte(2, b);
        return 8;
    }

    public int opcode_0xCB51_bit() {
        //BIT 2,C
        bit_regByte(2, c);
        return 8;
    }

    public int opcode_0xCB52_bit() {
        //BIT 2,D
        bit_regByte(2, d);
        return 8;
    }

    public int opcode_0xCB53_bit() {
        //BIT 2,E
        bit_regByte(2, e);
        return 8;
    }

    public int opcode_0xCB54_bit() {
        //BIT 2,H
        bit_regByte(2, h);
        return 8;
    }

    public int opcode_0xCB55_bit() {
        //BIT 2,L
        bit_regByte(2, l);
        return 8;
    }

    public int opcode_0xCB56_bit() {
        //BIT 2,(HL)
        tmp_reg.write(readByte(hl.read()));
        bit_regByte(2, tmp_reg);
        return 12;
    }

    public int opcode_0xCB57_bit() {
        //BIT 2,A
        bit_regByte(2, a);
        return 8;
    }

    public int opcode_0xCB58_bit() {
        //BIT 3,B
        bit_regByte(3, b);
        return 8;
    }

    public int opcode_0xCB59_bit() {
        //BIT 3,C
        bit_regByte(3, c);
        return 8;
    }

    public int opcode_0xCB5A_bit() {
        //BIT 3,D
        bit_regByte(3, d);
        return 8;
    }

    public int opcode_0xCB5B_bit() {
        //BIT 3,E
        bit_regByte(3, e);
        return 8;
    }

    public int opcode_0xCB5C_bit() {
        //BIT 3,H
        bit_regByte(3, h);
        return 8;
    }

    public int opcode_0xCB5D_bit() {
        //BIT 3,L
        bit_regByte(3, l);
        return 8;
    }

    public int opcode_0xCB5E_bit() {
        //BIT 3,(HL)
        tmp_reg.write(readByte(hl.read()));
        bit_regByte(3, tmp_reg);
        return 12;
    }

    public int opcode_0xCB5F_bit() {
        //BIT 3,A
        bit_regByte(3, a);
        return 8;
    }

    public int opcode_0xCB60_bit() {
        //BIT 4,B
        bit_regByte(4, b);
        return 8;
    }

    public int opcode_0xCB61_bit() {
        //BIT 4,C
        bit_regByte(4, c);
        return 8;
    }

    public int opcode_0xCB62_bit() {
        //BIT 4,D
        bit_regByte(4, d);
        return 8;
    }

    public int opcode_0xCB63_bit() {
        //BIT 4,E
        bit_regByte(4, e);
        return 8;
    }

    public int opcode_0xCB64_bit() {
        //BIT 4,H
        bit_regByte(4, h);
        return 8;
    }

    public int opcode_0xCB65_bit() {
        //BIT 4,L
        bit_regByte(4, l);
        return 8;
    }

    public int opcode_0xCB66_bit() {
        //BIT 4,(HL)
        tmp_reg.write(readByte(hl.read()));
        bit_regByte(4, tmp_reg);
        return 12;
    }

    public int opcode_0xCB67_bit() {
        //BIT 4,A
        bit_regByte(4, a);
        return 8;
    }

    public int opcode_0xCB68_bit() {
        //BIT 5,B
        bit_regByte(5, b);
        return 8;
    }

    public int opcode_0xCB69_bit() {
        //BIT 5,C
        bit_regByte(5, c);
        return 8;
    }

    public int opcode_0xCB6A_bit() {
        //BIT 5,D
        bit_regByte(5, d);
        return 8;
    }

    public int opcode_0xCB6B_bit() {
        //BIT 5,E
        bit_regByte(5, e);
        return 8;
    }

    public int opcode_0xCB6C_bit() {
        //BIT 5,H
        bit_regByte(5, h);
        return 8;
    }

    public int opcode_0xCB6D_bit() {
        //BIT 5,L
        bit_regByte(5, l);
        return 8;
    }

    public int opcode_0xCB6E_bit() {
        //BIT 5,(HL)
        tmp_reg.write(readByte(hl.read()));
        bit_regByte(5, tmp_reg);
        return 12;
    }

    public int opcode_0xCB6F_bit() {
        //BIT 5,A
        bit_regByte(5, a);
        return 8;
    }

    public int opcode_0xCB70_bit() {
        //BIT 6,B
        bit_regByte(6, b);
        return 8;
    }

    public int opcode_0xCB71_bit() {
        //BIT 6,C
        bit_regByte(6, c);
        return 8;
    }

    public int opcode_0xCB72_bit() {
        //BIT 6,D
        bit_regByte(6, d);
        return 8;
    }

    public int opcode_0xCB73_bit() {
        //BIT 6,E
        bit_regByte(6, e);
        return 8;
    }

    public int opcode_0xCB74_bit() {
        //BIT 6,H
        bit_regByte(6, h);
        return 8;
    }

    public int opcode_0xCB75_bit() {
        //BIT 6,L
        bit_regByte(6, l);
        return 8;
    }

    public int opcode_0xCB76_bit() {
        //BIT 6,(HL)
        tmp_reg.write(readByte(hl.read()));
        bit_regByte(6, tmp_reg);
        return 12;
    }

    public int opcode_0xCB77_bit() {
        //BIT 6,A
        bit_regByte(6, a);
        return 8;
    }

    public int opcode_0xCB78_bit() {
        //BIT 7,B
        bit_regByte(7, b);
        return 8;
    }

    public int opcode_0xCB79_bit() {
        //BIT 7,C
        bit_regByte(7, c);
        return 8;
    }

    public int opcode_0xCB7A_bit() {
        //BIT 7,D
        bit_regByte(7, d);
        return 8;
    }

    public int opcode_0xCB7B_bit() {
        //BIT 7,E
        bit_regByte(7, e);
        return 8;
    }

    public int opcode_0xCB7C_bit() {
        //BIT 7,H
        bit_regByte(7, h);
        return 8;
    }

    public int opcode_0xCB7D_bit() {
        //BIT 7,L
        bit_regByte(7, l);
        return 8;
    }

    public int opcode_0xCB7E_bit() {
        //BIT 7,(HL)
        tmp_reg.write(readByte(hl.read()));
        bit_regByte(7, tmp_reg);
        return 12;
    }

    public int opcode_0xCB7F_bit() {
        //BIT 7,A
        bit_regByte(7, a);
        return 8;
    }

    public int opcode_0xCB80_res() {
        //RES 0,B
        res_regByte(b, 0);
        return 8;
    }

    public int opcode_0xCB81_res() {
        //RES 0,C
        res_regByte(c, 0);
        return 8;
    }

    public int opcode_0xCB82_res() {
        //RES 0,D
        res_regByte(d, 0);
        return 8;
    }

    public int opcode_0xCB83_res() {
        //RES 0,E
        res_regByte(e, 0);
        return 8;
    }

    public int opcode_0xCB84_res() {
        //RES 0,H
        res_regByte(h, 0);
        return 8;
    }

    public int opcode_0xCB85_res() {
        //RES 0,L
        res_regByte(l, 0);
        return 8;
    }

    public int opcode_0xCB86_res() {
        //RES 0,(HL)
        tmp_reg.write(readByte(hl.read()));
        res_regByte(tmp_reg, 0);
        writeByte(hl.read(), tmp_reg.read());
        return 16;
    }

    public int opcode_0xCB87_res() {
        //RES 0,A
        res_regByte(a, 0);
        return 8;
    }

    public int opcode_0xCB88_res() {
        //RES 1,B
        res_regByte(b, 1);
        return 8;
    }

    public int opcode_0xCB89_res() {
        //RES 1,C
        res_regByte(c, 1);
        return 8;
    }

    public int opcode_0xCB8A_res() {
        //RES 1,D
        res_regByte(d, 1);
        return 8;
    }

    public int opcode_0xCB8B_res() {
        //RES 1,E
        res_regByte(e, 1);
        return 8;
    }

    public int opcode_0xCB8C_res() {
        //RES 1,H
        res_regByte(h, 1);
        return 8;
    }

    public int opcode_0xCB8D_res() {
        //RES 1,L
        res_regByte(l, 1);
        return 8;
    }

    public int opcode_0xCB8E_res() {
        //RES 1,(HL)
        tmp_reg.write(readByte(hl.read()));
        res_regByte(tmp_reg, 1);
        writeByte(hl.read(), tmp_reg.read());
        return 16;
    }

    public int opcode_0xCB8F_res() {
        //RES 1,A
        res_regByte(a, 1);
        return 8;
    }

    public int opcode_0xCB90_res() {
        //RES 2,B
        res_regByte(b, 2);
        return 8;
    }

    public int opcode_0xCB91_res() {
        //RES 2,C
        res_regByte(c, 2);
        return 8;
    }

    public int opcode_0xCB92_res() {
        //RES 2,D
        res_regByte(d, 2);
        return 8;
    }

    public int opcode_0xCB93_res() {
        //RES 2,E
        res_regByte(e, 2);
        return 8;
    }

    public int opcode_0xCB94_res() {
        //RES 2,H
        res_regByte(h, 2);
        return 8;
    }

    public int opcode_0xCB95_res() {
        //RES 2,L
        res_regByte(l, 2);
        return 8;
    }

    public int opcode_0xCB96_res() {
        //RES 2,(HL)
        tmp_reg.write(readByte(hl.read()));
        res_regByte(tmp_reg, 2);
        writeByte(hl.read(), tmp_reg.read());
        return 16;
    }

    public int opcode_0xCB97_res() {
        //RES 2,A
        res_regByte(a, 2);
        return 8;
    }

    public int opcode_0xCB98_res() {
        //RES 3,B
        res_regByte(b, 3);
        return 8;
    }

    public int opcode_0xCB99_res() {
        //RES 3,C
        res_regByte(c, 3);
        return 8;
    }

    public int opcode_0xCB9A_res() {
        //RES 3,D
        res_regByte(d, 3);
        return 8;
    }

    public int opcode_0xCB9B_res() {
        //RES 3,E
        res_regByte(e, 3);
        return 8;
    }

    public int opcode_0xCB9C_res() {
        //RES 3,H
        res_regByte(h, 3);
        return 8;
    }

    public int opcode_0xCB9D_res() {
        //RES 3,L
        res_regByte(l, 3);
        return 8;
    }

    public int opcode_0xCB9E_res() {
        //RES 3,(HL)
        tmp_reg.write(readByte(hl.read()));
        res_regByte(tmp_reg, 3);
        writeByte(hl.read(), tmp_reg.read());
        return 16;
    }

    public int opcode_0xCB9F_res() {
        //RES 3,A
        res_regByte(a, 3);
        return 8;
    }

    public int opcode_0xCBA0_res() {
        //RES 4,B
        res_regByte(b, 4);
        return 8;
    }

    public int opcode_0xCBA1_res() {
        //RES 4,C
        res_regByte(c, 4);
        return 8;
    }

    public int opcode_0xCBA2_res() {
        //RES 4,D
        res_regByte(d, 4);
        return 8;
    }

    public int opcode_0xCBA3_res() {
        //RES 4,E
        res_regByte(e, 4);
        return 8;
    }

    public int opcode_0xCBA4_res() {
        //RES 4,H
        res_regByte(h, 4);
        return 8;
    }

    public int opcode_0xCBA5_res() {
        //RES 4,L
        res_regByte(l, 4);
        return 8;
    }

    public int opcode_0xCBA6_res() {
        //RES 4,(HL)
        tmp_reg.write(readByte(hl.read()));
        res_regByte(tmp_reg, 4);
        writeByte(hl.read(), tmp_reg.read());
        return 16;
    }

    public int opcode_0xCBA7_res() {
        //RES 4,A
        res_regByte(a, 4);
        return 8;
    }

    public int opcode_0xCBA8_res() {
        //RES 5,B
        res_regByte(b, 5);
        return 8;
    }

    public int opcode_0xCBA9_res() {
        //RES 5,C
        res_regByte(c, 5);
        return 8;
    }

    public int opcode_0xCBAA_res() {
        //RES 5,D
        res_regByte(d, 5);
        return 8;
    }

    public int opcode_0xCBAB_res() {
        //RES 5,E
        res_regByte(e, 5);
        return 8;
    }

    public int opcode_0xCBAC_res() {
        //RES 5,H
        res_regByte(h, 5);
        return 8;
    }

    public int opcode_0xCBAD_res() {
        //RES 5,L
        res_regByte(l, 5);
        return 8;
    }

    public int opcode_0xCBAE_res() {
        //RES 5,(HL)
        tmp_reg.write(readByte(hl.read()));
        res_regByte(tmp_reg, 5);
        writeByte(hl.read(), tmp_reg.read());
        return 16;
    }

    public int opcode_0xCBAF_res() {
        //RES 5,A
        res_regByte(a, 5);
        return 8;
    }

    public int opcode_0xCBB0_res() {
        //RES 6,B
        res_regByte(b, 6);
        return 8;
    }

    public int opcode_0xCBB1_res() {
        //RES 6,C
        res_regByte(c, 6);
        return 8;
    }

    public int opcode_0xCBB2_res() {
        //RES 6,D
        res_regByte(d, 6);
        return 8;
    }

    public int opcode_0xCBB3_res() {
        //RES 6,E
        res_regByte(e, 6);
        return 8;
    }

    public int opcode_0xCBB4_res() {
        //RES 6,H
        res_regByte(h, 6);
        return 8;
    }

    public int opcode_0xCBB5_res() {
        //RES 6,L
        res_regByte(l, 6);
        return 8;
    }

    public int opcode_0xCBB6_res() {
        //RES 6,(HL)
        tmp_reg.write(readByte(hl.read()));
        res_regByte(tmp_reg, 6);
        writeByte(hl.read(), tmp_reg.read());
        return 16;
    }

    public int opcode_0xCBB7_res() {
        //RES 6,A
        res_regByte(a, 6);
        return 8;
    }

    public int opcode_0xCBB8_res() {
        //RES 7,B
        res_regByte(b, 7);
        return 8;
    }

    public int opcode_0xCBB9_res() {
        //RES 7,C
        res_regByte(c, 7);
        return 8;
    }

    public int opcode_0xCBBA_res() {
        //RES 7,D
        res_regByte(d, 7);
        return 8;
    }

    public int opcode_0xCBBB_res() {
        //RES 7,E
        res_regByte(e, 7);
        return 8;
    }

    public int opcode_0xCBBC_res() {
        //RES 7,H
        res_regByte(h, 7);
        return 8;
    }

    public int opcode_0xCBBD_res() {
        //RES 7,L
        res_regByte(l, 7);
        return 8;
    }

    public int opcode_0xCBBE_res() {
        //RES 7,(HL)
        tmp_reg.write(readByte(hl.read()));
        res_regByte(tmp_reg, 7);
        writeByte(hl.read(), tmp_reg.read());
        return 16;
    }

    public int opcode_0xCBBF_res() {
        //RES 7,A
        res_regByte(a, 7);
        return 8;
    }

    public int opcode_0xCBC0_set_set() {
        //SET 0,B
        set_regByte(b, 0);
        return 8;
    }

    public int opcode_0xCBC1_set() {
        //SET 0,C
        set_regByte(c, 0);
        return 8;
    }

    public int opcode_0xCBC2_set() {
        //SET 0,D
        set_regByte(d, 0);
        return 8;
    }

    public int opcode_0xCBC3_set() {
        //SET 0,E
        set_regByte(e, 0);
        return 8;
    }

    public int opcode_0xCBC4_set() {
        //SET 0,H
        set_regByte(h, 0);
        return 8;
    }

    public int opcode_0xCBC5_set() {
        //SET 0,L
        set_regByte(l, 0);
        return 8;
    }

    public int opcode_0xCBC6_set() {
        //SET 0,(HL)
        tmp_reg.write(readByte(hl.read()));
        set_regByte(tmp_reg, 0);
        writeByte(hl.read(), tmp_reg.read());
        return 16;
    }

    public int opcode_0xCBC7_set() {
        //SET 0,A
        set_regByte(a, 0);
        return 8;
    }

    public int opcode_0xCBC8_set() {
        //SET 1,B
        set_regByte(b, 1);
        return 8;
    }

    public int opcode_0xCBC9_set() {
        //SET 1,C
        set_regByte(c, 1);
        return 8;
    }

    public int opcode_0xCBCA_set() {
        //SET 1,D
        set_regByte(d, 1);
        return 8;
    }

    public int opcode_0xCBCB_set() {
        //SET 1,E
        set_regByte(e, 1);
        return 8;
    }

    public int opcode_0xCBCC_set() {
        //SET 1,H
        set_regByte(h, 1);
        return 8;
    }

    public int opcode_0xCBCD_set() {
        //SET 1,L
        set_regByte(l, 1);
        return 8;
    }

    public int opcode_0xCBCE_set() {
        //SET 1,(HL)
        tmp_reg.write(readByte(hl.read()));
        set_regByte(tmp_reg, 1);
        writeByte(hl.read(), tmp_reg.read());
        return 16;
    }

    public int opcode_0xCBCF_set() {
        //SET 1,A
        set_regByte(a, 1);
        return 8;
    }

    public int opcode_0xCBD0_set() {
        //SET 2,B
        set_regByte(b, 2);
        return 8;
    }

    public int opcode_0xCBD1_set() {
        //SET 2,C
        set_regByte(c, 2);
        return 8;
    }

    public int opcode_0xCBD2_set() {
        //SET 2,D
        set_regByte(d, 2);
        return 8;
    }

    public int opcode_0xCBD3_set() {
        //SET 2,E
        set_regByte(e, 2);
        return 8;
    }

    public int opcode_0xCBD4_set() {
        //SET 2,H
        set_regByte(h, 2);
        return 8;
    }

    public int opcode_0xCBD5_set() {
        //SET 2,L
        set_regByte(l, 2);
        return 8;
    }

    public int opcode_0xCBD6_set() {
        //SET 2,(HL)
        tmp_reg.write(readByte(hl.read()));
        set_regByte(tmp_reg, 2);
        writeByte(hl.read(), tmp_reg.read());
        return 16;
    }

    public int opcode_0xCBD7_set() {
        //SET 2,A
        set_regByte(a, 2);
        return 8;
    }

    public int opcode_0xCBD8_set() {
        //SET 3,B
        set_regByte(b, 3);
        return 8;
    }

    public int opcode_0xCBD9_set() {
        //SET 3,C
        set_regByte(c, 3);
        return 8;
    }

    public int opcode_0xCBDA_set() {
        //SET 3,D
        set_regByte(d, 3);
        return 8;
    }

    public int opcode_0xCBDB_set() {
        //SET 3,E
        set_regByte(e, 3);
        return 8;
    }

    public int opcode_0xCBDC_set() {
        //SET 3,H
        set_regByte(h, 3);
        return 8;
    }

    public int opcode_0xCBDD_set() {
        //SET 3,L
        set_regByte(l, 3);
        return 8;
    }

    public int opcode_0xCBDE_set() {
        //SET 3,(HL)
        tmp_reg.write(readByte(hl.read()));
        set_regByte(tmp_reg, 3);
        writeByte(hl.read(), tmp_reg.read());
        return 16;
    }

    public int opcode_0xCBDF_set() {
        //SET 3,A
        set_regByte(a, 3);
        return 8;
    }

    public int opcode_0xCBE0_set() {
        //SET 4,B
        set_regByte(b, 4);
        return 8;
    }

    public int opcode_0xCBE1_set() {
        //SET 4,C
        set_regByte(c, 4);
        return 8;
    }

    public int opcode_0xCBE2_set() {
        //SET 4,D
        set_regByte(d, 4);
        return 8;
    }

    public int opcode_0xCBE3_set() {
        //SET 4,E
        set_regByte(e, 4);
        return 8;
    }

    public int opcode_0xCBE4_set() {
        //SET 4,H
        set_regByte(h, 4);
        return 8;
    }

    public int opcode_0xCBE5_set() {
        //SET 4,L
        set_regByte(l, 4);
        return 8;
    }

    public int opcode_0xCBE6_set() {
        //SET 4,(HL)
        tmp_reg.write(readByte(hl.read()));
        set_regByte(tmp_reg, 4);
        writeByte(hl.read(), tmp_reg.read());
        return 16;
    }

    public int opcode_0xCBE7_set() {
        //SET 4,A
        set_regByte(a, 4);
        return 8;
    }

    public int opcode_0xCBE8_set() {
        //SET 5,B
        set_regByte(b, 5);
        return 8;
    }

    public int opcode_0xCBE9_set() {
        //SET 5,C
        set_regByte(c, 5);
        return 8;
    }

    public int opcode_0xCBEA_set() {
        //SET 5,D
        set_regByte(d, 5);
        return 8;
    }

    public int opcode_0xCBEB_set() {
        //SET 5,E
        set_regByte(e, 5);
        return 8;
    }

    public int opcode_0xCBEC_set() {
        //SET 5,H
        set_regByte(h, 5);
        return 8;
    }

    public int opcode_0xCBED_set() {
        //SET 5,L
        set_regByte(l, 5);
        return 8;
    }

    public int opcode_0xCBEE_set() {
        //SET 5,(HL)
        tmp_reg.write(readByte(hl.read()));
        set_regByte(tmp_reg, 5);
        writeByte(hl.read(), tmp_reg.read());
        return 16;
    }

    public int opcode_0xCBEF_set() {
        //SET 5,A
        set_regByte(a, 5);
        return 8;
    }

    public int opcode_0xCBF0_set() {
        //SET 6,B
        set_regByte(b, 6);
        return 8;
    }

    public int opcode_0xCBF1_set() {
        //SET 6,C
        set_regByte(c, 6);
        return 8;
    }

    public int opcode_0xCBF2_set() {
        //SET 6,D
        set_regByte(d, 6);
        return 8;
    }

    public int opcode_0xCBF3_set() {
        //SET 6,E
        set_regByte(e, 6);
        return 8;
    }

    public int opcode_0xCBF4_set() {
        //SET 6,H
        set_regByte(h, 6);
        return 8;
    }

    public int opcode_0xCBF5_set() {
        //SET 6,L
        set_regByte(l, 6);
        return 8;
    }

    public int opcode_0xCBF6_set() {
        //SET 6,(HL)
        tmp_reg.write(readByte(hl.read()));
        set_regByte(tmp_reg, 6);
        writeByte(hl.read(), tmp_reg.read());
        return 16;
    }

    public int opcode_0xCBF7_set() {
        //SET 6,A
        set_regByte(a, 6);
        return 8;
    }

    public int opcode_0xCBF8_set() {
        //SET 7,B
        set_regByte(b, 7);
        return 8;
    }

    public int opcode_0xCBF9_set() {
        //SET 7,C
        set_regByte(c, 7);
        return 8;
    }

    public int opcode_0xCBFA_set() {
        //SET 7,D
        set_regByte(d, 7);
        return 8;
    }

    public int opcode_0xCBFB_set() {
        //SET 7,E
        set_regByte(e, 7);
        return 8;
    }

    public int opcode_0xCBFC_set() {
        //SET 7,H
        set_regByte(h, 7);
        return 8;
    }

    public int opcode_0xCBFD_set() {
        //SET 7,L
        set_regByte(l, 7);
        return 8;
    }

    public int opcode_0xCBFE_set() {
        //SET 7,(HL)
        tmp_reg.write(readByte(hl.read()));
        set_regByte(tmp_reg, 7);
        writeByte(hl.read(), tmp_reg.read());
        return 16;
    }

    public int opcode_0xCBFF_set() {
        //SET 7,A
        set_regByte(a, 7);
        return 8;
    }

    public void enableBootstrap(boolean enabled) {
        this.bootstrap_enabled = enabled;
    }
}
