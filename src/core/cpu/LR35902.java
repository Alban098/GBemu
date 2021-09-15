package core.cpu;

import core.Memory;
import core.cpu.register.Register16;
import core.cpu.register.Register8;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static core.BitUtils.lsb;
import static core.BitUtils.msb;
import static core.BitUtils.signed8;

public class LR35902 {


    private final List<Instruction> opcodes;
    private final List<Instruction> cb_opcodes;

    private final Register16 af;
    private final Register16 bc;
    private final Register16 de;
    private final Register16 hl;
    private final Register8 a;
    private final Register8 f;
    private final Register8 b;
    private final Register8 c;
    private final Register8 d;
    private final Register8 e;
    private final Register8 h;
    private final Register8 l;
    private final Register16 sp;
    private final Register16 pc;

    private final Memory memory;

    private long cycle = 0;
    private int remaining_cycle_until_op = 0;
    private int disable_interrupt_in_opcode = -1;
    private int enable_interrupt_in_opcode = -1;

    private boolean halted = false;
    private boolean IME = true;

    public LR35902(Memory memory) {
        af = new Register16(0x01B0);
        a = af.getHigh();
        f = af.getLow();

        bc = new Register16(0x0013);
        b = bc.getHigh();
        c = bc.getLow();

        de = new Register16(0x00D8);
        d = de.getHigh();
        e = de.getLow();

        hl = new Register16(0x014D);
        h = hl.getHigh();
        l = hl.getLow();

        sp = new Register16(0xFFFE);
        pc = new Register16(0x0100);

        this.memory = memory;

        opcodes = new ArrayList<>();
        opcodes.add(new Instruction(0x00, "NOP", this::opcode_NOP));
        opcodes.add(new Instruction(0x01, "LD BC,#16", () -> opcode_LD(bc)));
        opcodes.add(new Instruction(0x02, "LD (BC),A", () -> opcode_LD_to_addr_from_A(bc)));
        opcodes.add(new Instruction(0x03, "INC BC", () -> opcode_INC(bc)));
        opcodes.add(new Instruction(0x04, "INC B", () -> opcode_INC(b)));
        opcodes.add(new Instruction(0x05, "DEC B", () -> opcode_DEC(b)));
        opcodes.add(new Instruction(0x06, "LD B,#8", () -> opcode_LD(b)));
        opcodes.add(new Instruction(0x07, "RLCA", this::opcode_RLCA));
        opcodes.add(new Instruction(0x08, "LD (#16),SP", this::opcode_LD_from_SP));
        opcodes.add(new Instruction(0x09, "ADD HL,BC", () -> opcode_ADD_HL(bc)));
        opcodes.add(new Instruction(0x0A, "LD A,(BC)", () -> opcode_LD_to_A_from_addr(bc)));
        opcodes.add(new Instruction(0x0B, "DEC BC", () -> opcode_DEC(bc)));
        opcodes.add(new Instruction(0x0C, "INC C", () -> opcode_INC(c)));
        opcodes.add(new Instruction(0x0D, "DEC C", () -> opcode_DEC(c)));
        opcodes.add(new Instruction(0x0E, "LD C,#8", () -> opcode_LD(c)));
        opcodes.add(new Instruction(0x0F, "RRCA", this::opcode_RRCA));
        opcodes.add(new Instruction(0x10, "STOP", this::opcode_STOP));
        opcodes.add(new Instruction(0x11, "LD DE,d16", () -> opcode_LD(de)));
        opcodes.add(new Instruction(0x12, "LD (DE),A", () -> opcode_LD_to_addr_from_A(de)));
        opcodes.add(new Instruction(0x13, "INC DE", () -> opcode_INC(de)));
        opcodes.add(new Instruction(0x14, "INC D", () -> opcode_INC(d)));
        opcodes.add(new Instruction(0x15, "DEC D", () -> opcode_DEC(d)));
        opcodes.add(new Instruction(0x16, "LD D,d8", () -> opcode_LD(d)));
        opcodes.add(new Instruction(0x17, "RLA", this::opcode_RLA));
        opcodes.add(new Instruction(0x18, "JR r8", this::opcode_JR));
        opcodes.add(new Instruction(0x19, "ADD HL,DE", () -> opcode_ADD_HL(de)));
        opcodes.add(new Instruction(0x1A, "LD A,(DE)", () -> opcode_LD_to_A_from_addr(de)));
        opcodes.add(new Instruction(0x1B, "DEC DE", () -> opcode_DEC(de)));
        opcodes.add(new Instruction(0x1C, "INC E", () -> opcode_INC(e)));
        opcodes.add(new Instruction(0x1D, "DEC E", () -> opcode_DEC(e)));
        opcodes.add(new Instruction(0x1E, "LD E,d8", () -> opcode_LD(e)));
        opcodes.add(new Instruction(0x1F, "RRA", this::opcode_RRA));
        opcodes.add(new Instruction(0x20, "JR NZ,r8", this::opcode_JR_NZ));
        opcodes.add(new Instruction(0x21, "LD HL,d16", () -> opcode_LD(hl)));
        opcodes.add(new Instruction(0x22, "LD (HL+),A", this::opcode_LD_HLI_A));
        opcodes.add(new Instruction(0x23, "INC HL", () -> opcode_INC(hl)));
        opcodes.add(new Instruction(0x24, "INC H", () -> opcode_INC(h)));
        opcodes.add(new Instruction(0x25, "DEC H", () -> opcode_DEC(h)));
        opcodes.add(new Instruction(0x26, "LD H,d8", () -> opcode_LD(h)));
        opcodes.add(new Instruction(0x27, "DAA", this::opcode_DAA));
        opcodes.add(new Instruction(0x28, "JR Z,r8", this::opcode_JR_Z));
        opcodes.add(new Instruction(0x29, "ADD HL,HL", () -> opcode_ADD_HL(hl)));
        opcodes.add(new Instruction(0x2A, "LD A,(HL+)", this::opcode_LD_A_HLI));
        opcodes.add(new Instruction(0x2B, "DEC HL", () -> opcode_DEC(hl)));
        opcodes.add(new Instruction(0x2C, "INC L", () -> opcode_INC(l)));
        opcodes.add(new Instruction(0x2D, "DEC L", () -> opcode_DEC(l)));
        opcodes.add(new Instruction(0x2E, "LD L,d8", () -> opcode_LD(l)));
        opcodes.add(new Instruction(0x2F, "CPL", this::opcode_CPL));
        opcodes.add(new Instruction(0x30, "JR NC,r8", this::opcode_JR_NC));
        opcodes.add(new Instruction(0x31, "LD SP,d16", () -> opcode_LD(sp)));
        opcodes.add(new Instruction(0x32, "LD (HL-),A", this::opcode_LD_HLD_A));
        opcodes.add(new Instruction(0x33, "INC SP", () -> opcode_INC(sp)));
        opcodes.add(new Instruction(0x34, "INC (HL)", this::opcode_INC_HL));
        opcodes.add(new Instruction(0x35, "DEC (HL)", this::opcode_DEC_HL));
        opcodes.add(new Instruction(0x36, "LD (HL),d8", this::opcode_LD_to_HL_addr));
        opcodes.add(new Instruction(0x37, "SCF", this::opcode_SCF));
        opcodes.add(new Instruction(0x38, "JR C,r8", this::opcode_JR_C));
        opcodes.add(new Instruction(0x39, "ADD HL,SP", () -> opcode_ADD_HL(sp)));
        opcodes.add(new Instruction(0x3A, "LD A,(HL-)", this::opcode_LD_A_HLD));
        opcodes.add(new Instruction(0x3B, "DEC SP", () -> opcode_DEC(sp)));
        opcodes.add(new Instruction(0x3C, "INC A", () -> opcode_INC(a)));
        opcodes.add(new Instruction(0x3D, "DEC A", () -> opcode_DEC(a)));
        opcodes.add(new Instruction(0x3E, "LD A,d8", () -> opcode_LD(a)));
        opcodes.add(new Instruction(0x3F, "CCF", this::opcode_CCF));
        opcodes.add(new Instruction(0x40, "LD B,B", () -> opcode_LD(b, b)));
        opcodes.add(new Instruction(0x41, "LD B,C", () -> opcode_LD(b, c)));
        opcodes.add(new Instruction(0x42, "LD B,D", () -> opcode_LD(b, d)));
        opcodes.add(new Instruction(0x43, "LD B,E", () -> opcode_LD(b, e)));
        opcodes.add(new Instruction(0x44, "LD B,H", () -> opcode_LD(b, h)));
        opcodes.add(new Instruction(0x45, "LD B,L", () -> opcode_LD(b, l)));
        opcodes.add(new Instruction(0x46, "LD B,(HL)", () -> opcode_LD_from_HL_addr(b)));
        opcodes.add(new Instruction(0x47, "LD B,A", () -> opcode_LD(b, a)));
        opcodes.add(new Instruction(0x48, "LD C,B", () -> opcode_LD(c, b)));
        opcodes.add(new Instruction(0x49, "LD C,C", () -> opcode_LD(c, c)));
        opcodes.add(new Instruction(0x4A, "LD C,D", () -> opcode_LD(c, d)));
        opcodes.add(new Instruction(0x4B, "LD C,E", () -> opcode_LD(c, e)));
        opcodes.add(new Instruction(0x4C, "LD C,H", () -> opcode_LD(c, h)));
        opcodes.add(new Instruction(0x4D, "LD C,L", () -> opcode_LD(c, l)));
        opcodes.add(new Instruction(0x4E, "LD C,(HL)", () -> opcode_LD_from_HL_addr(c)));
        opcodes.add(new Instruction(0x4F, "LD C,A", () -> opcode_LD(c, a)));
        opcodes.add(new Instruction(0x50, "LD D,B", () -> opcode_LD(d, b)));
        opcodes.add(new Instruction(0x51, "LD D,C", () -> opcode_LD(d, c)));
        opcodes.add(new Instruction(0x52, "LD D,D", () -> opcode_LD(d, d)));
        opcodes.add(new Instruction(0x53, "LD D,E", () -> opcode_LD(d, e)));
        opcodes.add(new Instruction(0x54, "LD D,H", () -> opcode_LD(d, h)));
        opcodes.add(new Instruction(0x55, "LD D,L", () -> opcode_LD(d, l)));
        opcodes.add(new Instruction(0x56, "LD D,(HL)", () -> opcode_LD_from_HL_addr(d)));
        opcodes.add(new Instruction(0x57, "LD D,A", () -> opcode_LD(d, a)));
        opcodes.add(new Instruction(0x58, "LD E,B", () -> opcode_LD(e, b)));
        opcodes.add(new Instruction(0x59, "LD E,C", () -> opcode_LD(e, c)));
        opcodes.add(new Instruction(0x5A, "LD E,D", () -> opcode_LD(e, d)));
        opcodes.add(new Instruction(0x5B, "LD E,E", () -> opcode_LD(e, e)));
        opcodes.add(new Instruction(0x5C, "LD E,H", () -> opcode_LD(e, h)));
        opcodes.add(new Instruction(0x5D, "LD E,L", () -> opcode_LD(e, l)));
        opcodes.add(new Instruction(0x5E, "LD E,(HL)", () -> opcode_LD_from_HL_addr(e)));
        opcodes.add(new Instruction(0x5F, "LD E,A", () -> opcode_LD(e, a)));
        opcodes.add(new Instruction(0x60, "LD H,B", () -> opcode_LD(h, b)));
        opcodes.add(new Instruction(0x61, "LD H,C", () -> opcode_LD(h, c)));
        opcodes.add(new Instruction(0x62, "LD H,D", () -> opcode_LD(h, d)));
        opcodes.add(new Instruction(0x63, "LD H,E", () -> opcode_LD(h, e)));
        opcodes.add(new Instruction(0x64, "LD H,H", () -> opcode_LD(h, h)));
        opcodes.add(new Instruction(0x65, "LD H,L", () -> opcode_LD(h, l)));
        opcodes.add(new Instruction(0x66, "LD H,(HL)", () -> opcode_LD_from_HL_addr(h)));
        opcodes.add(new Instruction(0x67, "LD H,A", () -> opcode_LD(h, a)));
        opcodes.add(new Instruction(0x68, "LD L,B", () -> opcode_LD(l, b)));
        opcodes.add(new Instruction(0x69, "LD L,C", () -> opcode_LD(l, c)));
        opcodes.add(new Instruction(0x6A, "LD L,D", () -> opcode_LD(l, d)));
        opcodes.add(new Instruction(0x6B, "LD L,E", () -> opcode_LD(l, e)));
        opcodes.add(new Instruction(0x6C, "LD L,H", () -> opcode_LD(l, h)));
        opcodes.add(new Instruction(0x6D, "LD L,L", () -> opcode_LD(l, l)));
        opcodes.add(new Instruction(0x6E, "LD L,(HL)", () -> opcode_LD_from_HL_addr(l)));
        opcodes.add(new Instruction(0x6F, "LD L,A", () -> opcode_LD(l, a)));
        opcodes.add(new Instruction(0x70, "LD (HL),B", () -> opcode_LD_to_HL_addr(b)));
        opcodes.add(new Instruction(0x71, "LD (HL),C", () -> opcode_LD_to_HL_addr(c)));
        opcodes.add(new Instruction(0x72, "LD (HL),D", () -> opcode_LD_to_HL_addr(d)));
        opcodes.add(new Instruction(0x73, "LD (HL),E", () -> opcode_LD_to_HL_addr(e)));
        opcodes.add(new Instruction(0x74, "LD (HL),H", () -> opcode_LD_to_HL_addr(h)));
        opcodes.add(new Instruction(0x75, "LD (HL),L", () -> opcode_LD_to_HL_addr(l)));
        opcodes.add(new Instruction(0x76, "HALT", this::opcode_HALT));
        opcodes.add(new Instruction(0x77, "LD (HL),A", () -> opcode_LD_to_HL_addr(a)));
        opcodes.add(new Instruction(0x78, "LD A,B", () -> opcode_LD(a, b)));
        opcodes.add(new Instruction(0x79, "LD A,C", () -> opcode_LD(a, c)));
        opcodes.add(new Instruction(0x7A, "LD A,D", () -> opcode_LD(a, d)));
        opcodes.add(new Instruction(0x7B, "LD A,E", () -> opcode_LD(a, e)));
        opcodes.add(new Instruction(0x7C, "LD A,H", () -> opcode_LD(a, h)));
        opcodes.add(new Instruction(0x7D, "LD A,L", () -> opcode_LD(a, l)));
        opcodes.add(new Instruction(0x7E, "LD A,(HL)", () -> opcode_LD_from_HL_addr(a)));
        opcodes.add(new Instruction(0x7F, "LD A,A", () -> opcode_LD(a, a)));
        opcodes.add(new Instruction(0x80, "ADD A,B", () -> opcode_ADD(b)));
        opcodes.add(new Instruction(0x81, "ADD A,C", () -> opcode_ADD(c)));
        opcodes.add(new Instruction(0x82, "ADD A,D", () -> opcode_ADD(d)));
        opcodes.add(new Instruction(0x83, "ADD A,E", () -> opcode_ADD(e)));
        opcodes.add(new Instruction(0x84, "ADD A,H", () -> opcode_ADD(h)));
        opcodes.add(new Instruction(0x85, "ADD A,L", () -> opcode_ADD(l)));
        opcodes.add(new Instruction(0x86, "ADD A,(HL)", this::opcode_ADD_HL));
        opcodes.add(new Instruction(0x87, "ADD A,A", () -> opcode_ADD(a)));
        opcodes.add(new Instruction(0x88, "ADC A,B", () -> opcode_ADC(b)));
        opcodes.add(new Instruction(0x89, "ADC A,C", () -> opcode_ADC(c)));
        opcodes.add(new Instruction(0x8A, "ADC A,D", () -> opcode_ADC(d)));
        opcodes.add(new Instruction(0x8B, "ADC A,E", () -> opcode_ADC(e)));
        opcodes.add(new Instruction(0x8C, "ADC A,H", () -> opcode_ADC(h)));
        opcodes.add(new Instruction(0x8D, "ADC A,L", () -> opcode_ADC(l)));
        opcodes.add(new Instruction(0x8E, "ADC A,(HL)", this::opcode_ADC_HL));
        opcodes.add(new Instruction(0x8F, "ADC A,A", () -> opcode_ADC(a)));
        opcodes.add(new Instruction(0x90, "SUB B", () -> opcode_SUB(b)));
        opcodes.add(new Instruction(0x91, "SUB C", () -> opcode_SUB(c)));
        opcodes.add(new Instruction(0x92, "SUB D", () -> opcode_SUB(d)));
        opcodes.add(new Instruction(0x93, "SUB E", () -> opcode_SUB(e)));
        opcodes.add(new Instruction(0x94, "SUB H", () -> opcode_SUB(h)));
        opcodes.add(new Instruction(0x95, "SUB L", () -> opcode_SUB(l)));
        opcodes.add(new Instruction(0x96, "SUB (HL)", this::opcode_SUB_HL));
        opcodes.add(new Instruction(0x97, "SUB A", () -> opcode_SUB(a)));
        opcodes.add(new Instruction(0x98, "SBC A,B", () -> opcode_SBC(b)));
        opcodes.add(new Instruction(0x99, "SBC A,C", () -> opcode_SBC(c)));
        opcodes.add(new Instruction(0x9A, "SBC A,D", () -> opcode_SBC(d)));
        opcodes.add(new Instruction(0x9B, "SBC A,E", () -> opcode_SBC(e)));
        opcodes.add(new Instruction(0x9C, "SBC A,H", () -> opcode_SBC(h)));
        opcodes.add(new Instruction(0x9D, "SBC A,L", () -> opcode_SBC(l)));
        opcodes.add(new Instruction(0x9E, "SBC A,(HL)", this::opcode_SBC_HL));
        opcodes.add(new Instruction(0x9F, "SBC A,A", () -> opcode_SBC(a)));
        opcodes.add(new Instruction(0xA0, "AND B", () -> opcode_AND(b)));
        opcodes.add(new Instruction(0xA1, "AND C", () -> opcode_AND(c)));
        opcodes.add(new Instruction(0xA2, "AND D", () -> opcode_AND(d)));
        opcodes.add(new Instruction(0xA3, "AND E", () -> opcode_AND(e)));
        opcodes.add(new Instruction(0xA4, "AND H", () -> opcode_AND(h)));
        opcodes.add(new Instruction(0xA5, "AND L", () -> opcode_AND(l)));
        opcodes.add(new Instruction(0xA6, "AND (HL)", this::opcode_AND_HL));
        opcodes.add(new Instruction(0xA7, "AND A", () -> opcode_AND(a)));
        opcodes.add(new Instruction(0xA8, "XOR B", () -> opcode_XOR(b)));
        opcodes.add(new Instruction(0xA9, "XOR C", () -> opcode_XOR(c)));
        opcodes.add(new Instruction(0xAA, "XOR D", () -> opcode_XOR(d)));
        opcodes.add(new Instruction(0xAB, "XOR E", () -> opcode_XOR(e)));
        opcodes.add(new Instruction(0xAC, "XOR H", () -> opcode_XOR(h)));
        opcodes.add(new Instruction(0xAD, "XOR L", () -> opcode_XOR(l)));
        opcodes.add(new Instruction(0xAE, "XOR (HL)", this::opcode_XOR_HL));
        opcodes.add(new Instruction(0xAF, "XOR A", () -> opcode_XOR(a)));
        opcodes.add(new Instruction(0xB0, "OR B", () -> opcode_OR(b)));
        opcodes.add(new Instruction(0xB1, "OR C", () -> opcode_OR(c)));
        opcodes.add(new Instruction(0xB2, "OR D", () -> opcode_OR(d)));
        opcodes.add(new Instruction(0xB3, "OR E", () -> opcode_OR(e)));
        opcodes.add(new Instruction(0xB4, "OR H", () -> opcode_OR(h)));
        opcodes.add(new Instruction(0xB5, "OR L", () -> opcode_OR(l)));
        opcodes.add(new Instruction(0xB6, "OR (HL)", this::opcode_OR_HL));
        opcodes.add(new Instruction(0xB7, "OR A", () -> opcode_OR(a)));
        opcodes.add(new Instruction(0xB8, "CP B", () -> opcode_CP(b)));
        opcodes.add(new Instruction(0xB9, "CP C", () -> opcode_CP(c)));
        opcodes.add(new Instruction(0xBA, "CP D", () -> opcode_CP(d)));
        opcodes.add(new Instruction(0xBB, "CP E", () -> opcode_CP(e)));
        opcodes.add(new Instruction(0xBC, "CP H", () -> opcode_CP(h)));
        opcodes.add(new Instruction(0xBD, "CP L", () -> opcode_CP(l)));
        opcodes.add(new Instruction(0xBE, "CP (HL)", this::opcode_CP_HL));
        opcodes.add(new Instruction(0xBF, "CP A", () -> opcode_CP(a)));
        opcodes.add(new Instruction(0xC0, "RET NZ", this::opcode_RET_NZ));
        opcodes.add(new Instruction(0xC1, "POP BC", () -> opcode_POP(bc)));
        opcodes.add(new Instruction(0xC2, "JP NZ,a16", this::opcode_JP_NZ));
        opcodes.add(new Instruction(0xC3, "JP a16", this::opcode_JP));
        opcodes.add(new Instruction(0xC4, "CALL NZ,a16", this::opcode_CALL_NZ));
        opcodes.add(new Instruction(0xC5, "PUSH BC", () -> opcode_PUSH(bc)));
        opcodes.add(new Instruction(0xC6, "ADD A,d8", this::opcode_ADD));
        opcodes.add(new Instruction(0xC7, "RST 00H", () -> opcode_RST(0x00)));
        opcodes.add(new Instruction(0xC8, "RET Z", this::opcode_RET_Z));
        opcodes.add(new Instruction(0xC9, "RET", this::opcode_RET));
        opcodes.add(new Instruction(0xCA, "JP Z,a16", this::opcode_JP_Z));
        opcodes.add(new Instruction(0xCB, "PREFIX CB", this::prefix));
        opcodes.add(new Instruction(0xCC, "CALL Z,a16", this::opcode_CALL_Z));
        opcodes.add(new Instruction(0xCD, "CALL a16", this::opcode_CALL));
        opcodes.add(new Instruction(0xCE, "ADC A,d8", this::opcode_ADC));
        opcodes.add(new Instruction(0xCF, "RST 08H", () -> opcode_RST(0x08)));
        opcodes.add(new Instruction(0xD0, "RET NC", this::opcode_RET_NC));
        opcodes.add(new Instruction(0xD1, "POP DE", () -> opcode_POP(de)));
        opcodes.add(new Instruction(0xD2, "JP NC,a16", this::opcode_JP_NC));
        opcodes.add(new Instruction(0xD3, "", this::opcode_NOP));
        opcodes.add(new Instruction(0xD4, "CALL NC,a16", this::opcode_CALL_NC));
        opcodes.add(new Instruction(0xD5, "PUSH DE", () -> opcode_PUSH(de)));
        opcodes.add(new Instruction(0xD6, "SUB d8", this::opcode_SUB));
        opcodes.add(new Instruction(0xD7, "RST 10H", () -> opcode_RST(0x10)));
        opcodes.add(new Instruction(0xD8, "RET C", this::opcode_RET_C));
        opcodes.add(new Instruction(0xD9, "RETI", this::opcode_RETI));
        opcodes.add(new Instruction(0xDA, "JP C,a16", this::opcode_JP_C));
        opcodes.add(new Instruction(0xDB, "", this::opcode_NOP));
        opcodes.add(new Instruction(0xDC, "CALL C,a16", this::opcode_CALL_C));
        opcodes.add(new Instruction(0xDD, "", this::opcode_NOP));
        opcodes.add(new Instruction(0xDE, "SBC A,d8", this::opcode_SBC));
        opcodes.add(new Instruction(0xDF, "RST 18H", () -> opcode_RST(0x18)));
        opcodes.add(new Instruction(0xE0, "LDH (a8),A", this::opcode_LD_from_A_off));
        opcodes.add(new Instruction(0xE1, "POP HL", () -> opcode_POP(hl)));
        opcodes.add(new Instruction(0xE2, "LD (C),A", this::opcode_LD_C_A));
        opcodes.add(new Instruction(0xE3, "", this::opcode_NOP));
        opcodes.add(new Instruction(0xE4, "", this::opcode_NOP));
        opcodes.add(new Instruction(0xE5, "PUSH HL", () -> opcode_PUSH(hl)));
        opcodes.add(new Instruction(0xE6, "AND d8", this::opcode_AND));
        opcodes.add(new Instruction(0xE7, "RST 20H", () -> opcode_RST(0x20)));
        opcodes.add(new Instruction(0xE8, "ADD SP,r8", this::opcode_ADD_SP));
        opcodes.add(new Instruction(0xE9, "JP (HL)", this::opcode_JP_HL));
        opcodes.add(new Instruction(0xEA, "LD (a16),A", this::opcode_LD_to_addr_from_A));
        opcodes.add(new Instruction(0xEB, "", this::opcode_NOP));
        opcodes.add(new Instruction(0xEC, "", this::opcode_NOP));
        opcodes.add(new Instruction(0xED, "", this::opcode_NOP));
        opcodes.add(new Instruction(0xEE, "XOR d8", this::opcode_XOR));
        opcodes.add(new Instruction(0xEF, "RST 28H", () -> opcode_RST(0x28)));
        opcodes.add(new Instruction(0xF0, "LDH A,(a8)", this::opcode_LD_from_off_A));
        opcodes.add(new Instruction(0xF1, "POP AF", () -> opcode_POP(af)));
        opcodes.add(new Instruction(0xF2, "LD A,(C)", this::opcode_LD_A_C));
        opcodes.add(new Instruction(0xF3, "DI", this::opcode_DI));
        opcodes.add(new Instruction(0xF4, "", this::opcode_NOP));
        opcodes.add(new Instruction(0xF5, "PUSH AF", () -> opcode_PUSH(af)));
        opcodes.add(new Instruction(0xF6, "OR d8", this::opcode_OR));
        opcodes.add(new Instruction(0xF7, "RST 30H", () -> opcode_RST(0x30)));
        opcodes.add(new Instruction(0xF8, "LD HL,SP+r8", this::opcode_LD_HL_SP_off));
        opcodes.add(new Instruction(0xF9, "LD SP,HL", this::opcode_LD_SP_HL));
        opcodes.add(new Instruction(0xFA, "LD A,(a16)", this::opcode_LD_to_A_REL));
        opcodes.add(new Instruction(0xFB, "EI", this::opcode_EI));
        opcodes.add(new Instruction(0xFC, "", this::opcode_NOP));
        opcodes.add(new Instruction(0xFD, "", this::opcode_NOP));
        opcodes.add(new Instruction(0xFE, "CP d8", this::opcode_CP));
        opcodes.add(new Instruction(0xFF, "RST 38H", () -> opcode_RST(0x38)));

        cb_opcodes = new ArrayList<>();
        cb_opcodes.add(new Instruction(0x00, "RLC B", () -> opcode_RLC(b)));
        cb_opcodes.add(new Instruction(0x01, "RLC C", () -> opcode_RLC(c)));
        cb_opcodes.add(new Instruction(0x02, "RLC D", () -> opcode_RLC(d)));
        cb_opcodes.add(new Instruction(0x03, "RLC E", () -> opcode_RLC(e)));
        cb_opcodes.add(new Instruction(0x04, "RLC H", () -> opcode_RLC(h)));
        cb_opcodes.add(new Instruction(0x05, "RLC L", () -> opcode_RLC(l)));
        cb_opcodes.add(new Instruction(0x06, "RLC (HL)", this::opcode_RLC_HL));
        cb_opcodes.add(new Instruction(0x07, "RLC A", () -> opcode_RLC(a)));
        cb_opcodes.add(new Instruction(0x08, "RRC B", () -> opcode_RRC(b)));
        cb_opcodes.add(new Instruction(0x09, "RRC C", () -> opcode_RRC(c)));
        cb_opcodes.add(new Instruction(0x0A, "RRC D", () -> opcode_RRC(d)));
        cb_opcodes.add(new Instruction(0x0B, "RRC E", () -> opcode_RRC(e)));
        cb_opcodes.add(new Instruction(0x0C, "RRC H", () -> opcode_RRC(h)));
        cb_opcodes.add(new Instruction(0x0D, "RRC L", () -> opcode_RRC(l)));
        cb_opcodes.add(new Instruction(0x0E, "RRC (HL)", this::opcode_RRC_HL));
        cb_opcodes.add(new Instruction(0x0F, "RRC A", () -> opcode_RRC(a)));
        cb_opcodes.add(new Instruction(0x10, "RL B", () -> opcode_RL(b)));
        cb_opcodes.add(new Instruction(0x11, "RL C", () -> opcode_RL(c)));
        cb_opcodes.add(new Instruction(0x12, "RL D", () -> opcode_RL(d)));
        cb_opcodes.add(new Instruction(0x13, "RL E", () -> opcode_RL(e)));
        cb_opcodes.add(new Instruction(0x14, "RL H", () -> opcode_RL(h)));
        cb_opcodes.add(new Instruction(0x15, "RL L", () -> opcode_RL(l)));
        cb_opcodes.add(new Instruction(0x16, "RL (HL)", this::opcode_RL_HL));
        cb_opcodes.add(new Instruction(0x17, "RL A", () -> opcode_RL(a)));
        cb_opcodes.add(new Instruction(0x18, "RR B", () -> opcode_RR(b)));
        cb_opcodes.add(new Instruction(0x19, "RR C", () -> opcode_RR(c)));
        cb_opcodes.add(new Instruction(0x1A, "RR D", () -> opcode_RR(d)));
        cb_opcodes.add(new Instruction(0x1B, "RR E", () -> opcode_RR(e)));
        cb_opcodes.add(new Instruction(0x1C, "RR H", () -> opcode_RR(h)));
        cb_opcodes.add(new Instruction(0x1D, "RR L", () -> opcode_RR(l)));
        cb_opcodes.add(new Instruction(0x1E, "RR (HL)", this::opcode_RR_HL));
        cb_opcodes.add(new Instruction(0x1F, "RR A", () -> opcode_RR(a)));
        cb_opcodes.add(new Instruction(0x20, "SLA B", () -> opcode_SLA(b)));
        cb_opcodes.add(new Instruction(0x21, "SLA C", () -> opcode_SLA(c)));
        cb_opcodes.add(new Instruction(0x22, "SLA D", () -> opcode_SLA(d)));
        cb_opcodes.add(new Instruction(0x23, "SLA E", () -> opcode_SLA(e)));
        cb_opcodes.add(new Instruction(0x24, "SLA H", () -> opcode_SLA(h)));
        cb_opcodes.add(new Instruction(0x25, "SLA L", () -> opcode_SLA(l)));
        cb_opcodes.add(new Instruction(0x26, "SLA (HL)", this::opcode_SLA_HL));
        cb_opcodes.add(new Instruction(0x27, "SLA A", () -> opcode_SLA(a)));
        cb_opcodes.add(new Instruction(0x28, "SRA B", () -> opcode_SRA(b)));
        cb_opcodes.add(new Instruction(0x29, "SRA C", () -> opcode_SRA(c)));
        cb_opcodes.add(new Instruction(0x2A, "SRA D", () -> opcode_SRA(d)));
        cb_opcodes.add(new Instruction(0x2B, "SRA E", () -> opcode_SRA(e)));
        cb_opcodes.add(new Instruction(0x2C, "SRA H", () -> opcode_SRA(h)));
        cb_opcodes.add(new Instruction(0x2D, "SRA L", () -> opcode_SRA(l)));
        cb_opcodes.add(new Instruction(0x2E, "SRA (HL)", this::opcode_SRA_HL));
        cb_opcodes.add(new Instruction(0x2F, "SRA A", () -> opcode_SRA(a)));
        cb_opcodes.add(new Instruction(0x30, "SWAP B", () -> opcode_SWAP(b)));
        cb_opcodes.add(new Instruction(0x31, "SWAP C", () -> opcode_SWAP(c)));
        cb_opcodes.add(new Instruction(0x32, "SWAP D", () -> opcode_SWAP(d)));
        cb_opcodes.add(new Instruction(0x33, "SWAP E", () -> opcode_SWAP(e)));
        cb_opcodes.add(new Instruction(0x34, "SWAP H", () -> opcode_SWAP(h)));
        cb_opcodes.add(new Instruction(0x35, "SWAP L", () -> opcode_SWAP(l)));
        cb_opcodes.add(new Instruction(0x36, "SWAP (HL)", this::opcode_SWAP_HL));
        cb_opcodes.add(new Instruction(0x37, "SWAP A", () -> opcode_SWAP(a)));
        cb_opcodes.add(new Instruction(0x38, "SRL B", () -> opcode_SRL(b)));
        cb_opcodes.add(new Instruction(0x39, "SRL C", () -> opcode_SRL(c)));
        cb_opcodes.add(new Instruction(0x3A, "SRL D", () -> opcode_SRL(d)));
        cb_opcodes.add(new Instruction(0x3B, "SRL E", () -> opcode_SRL(e)));
        cb_opcodes.add(new Instruction(0x3C, "SRL H", () -> opcode_SRL(h)));
        cb_opcodes.add(new Instruction(0x3D, "SRL L", () -> opcode_SRL(l)));
        cb_opcodes.add(new Instruction(0x3E, "SRL (HL)", this::opcode_SRL_HL));
        cb_opcodes.add(new Instruction(0x3F, "SRL A", () -> opcode_SRL(a)));
        cb_opcodes.add(new Instruction(0x40, "BIT 0,B", () -> opcode_BIT(b)));
        cb_opcodes.add(new Instruction(0x41, "BIT 0,C", () -> opcode_BIT(c)));
        cb_opcodes.add(new Instruction(0x42, "BIT 0,D", () -> opcode_BIT(d)));
        cb_opcodes.add(new Instruction(0x43, "BIT 0,E", () -> opcode_BIT(e)));
        cb_opcodes.add(new Instruction(0x44, "BIT 0,H", () -> opcode_BIT(h)));
        cb_opcodes.add(new Instruction(0x45, "BIT 0,L", () -> opcode_BIT(l)));
        cb_opcodes.add(new Instruction(0x46, "BIT 0,(HL)", this::opcode_BIT_HL));
        cb_opcodes.add(new Instruction(0x47, "BIT 0,A", () -> opcode_BIT(a)));
        cb_opcodes.add(new Instruction(0x48, "BIT 1,B", () -> opcode_BIT(b)));
        cb_opcodes.add(new Instruction(0x49, "BIT 1,C", () -> opcode_BIT(c)));
        cb_opcodes.add(new Instruction(0x4A, "BIT 1,D", () -> opcode_BIT(d)));
        cb_opcodes.add(new Instruction(0x4B, "BIT 1,E", () -> opcode_BIT(e)));
        cb_opcodes.add(new Instruction(0x4C, "BIT 1,H", () -> opcode_BIT(h)));
        cb_opcodes.add(new Instruction(0x4D, "BIT 1,L", () -> opcode_BIT(l)));
        cb_opcodes.add(new Instruction(0x4E, "BIT 1,(HL)", this::opcode_BIT_HL));
        cb_opcodes.add(new Instruction(0x4F, "BIT 1,A", () -> opcode_BIT(a)));
        cb_opcodes.add(new Instruction(0x50, "BIT 2,B", () -> opcode_BIT(b)));
        cb_opcodes.add(new Instruction(0x51, "BIT 2,C", () -> opcode_BIT(c)));
        cb_opcodes.add(new Instruction(0x52, "BIT 2,D", () -> opcode_BIT(d)));
        cb_opcodes.add(new Instruction(0x53, "BIT 2,E", () -> opcode_BIT(e)));
        cb_opcodes.add(new Instruction(0x54, "BIT 2,H", () -> opcode_BIT(h)));
        cb_opcodes.add(new Instruction(0x55, "BIT 2,L", () -> opcode_BIT(l)));
        cb_opcodes.add(new Instruction(0x56, "BIT 2,(HL)", this::opcode_BIT_HL));
        cb_opcodes.add(new Instruction(0x57, "BIT 2,A", () -> opcode_BIT(a)));
        cb_opcodes.add(new Instruction(0x58, "BIT 3,B", () -> opcode_BIT(b)));
        cb_opcodes.add(new Instruction(0x59, "BIT 3,C", () -> opcode_BIT(c)));
        cb_opcodes.add(new Instruction(0x5A, "BIT 3,D", () -> opcode_BIT(d)));
        cb_opcodes.add(new Instruction(0x5B, "BIT 3,E", () -> opcode_BIT(e)));
        cb_opcodes.add(new Instruction(0x5C, "BIT 3,H", () -> opcode_BIT(h)));
        cb_opcodes.add(new Instruction(0x5D, "BIT 3,L", () -> opcode_BIT(l)));
        cb_opcodes.add(new Instruction(0x5E, "BIT 3,(HL)", this::opcode_BIT_HL));
        cb_opcodes.add(new Instruction(0x5F, "BIT 3,A", () -> opcode_BIT(a)));
        cb_opcodes.add(new Instruction(0x60, "BIT 4,B", () -> opcode_BIT(b)));
        cb_opcodes.add(new Instruction(0x61, "BIT 4,C", () -> opcode_BIT(c)));
        cb_opcodes.add(new Instruction(0x62, "BIT 4,D", () -> opcode_BIT(d)));
        cb_opcodes.add(new Instruction(0x63, "BIT 4,E", () -> opcode_BIT(e)));
        cb_opcodes.add(new Instruction(0x64, "BIT 4,H", () -> opcode_BIT(h)));
        cb_opcodes.add(new Instruction(0x65, "BIT 4,L", () -> opcode_BIT(l)));
        cb_opcodes.add(new Instruction(0x66, "BIT 4,(HL)", this::opcode_BIT_HL));
        cb_opcodes.add(new Instruction(0x67, "BIT 4,A", () -> opcode_BIT(a)));
        cb_opcodes.add(new Instruction(0x68, "BIT 5,B", () -> opcode_BIT(b)));
        cb_opcodes.add(new Instruction(0x69, "BIT 5,C", () -> opcode_BIT(c)));
        cb_opcodes.add(new Instruction(0x6A, "BIT 5,D", () -> opcode_BIT(d)));
        cb_opcodes.add(new Instruction(0x6B, "BIT 5,E", () -> opcode_BIT(e)));
        cb_opcodes.add(new Instruction(0x6C, "BIT 5,H", () -> opcode_BIT(h)));
        cb_opcodes.add(new Instruction(0x6D, "BIT 5,L", () -> opcode_BIT(l)));
        cb_opcodes.add(new Instruction(0x6E, "BIT 5,(HL)", this::opcode_BIT_HL));
        cb_opcodes.add(new Instruction(0x6F, "BIT 5,A", () -> opcode_BIT(a)));
        cb_opcodes.add(new Instruction(0x70, "BIT 6,B", () -> opcode_BIT(b)));
        cb_opcodes.add(new Instruction(0x71, "BIT 6,C", () -> opcode_BIT(c)));
        cb_opcodes.add(new Instruction(0x72, "BIT 6,D", () -> opcode_BIT(d)));
        cb_opcodes.add(new Instruction(0x73, "BIT 6,E", () -> opcode_BIT(e)));
        cb_opcodes.add(new Instruction(0x74, "BIT 6,H", () -> opcode_BIT(h)));
        cb_opcodes.add(new Instruction(0x75, "BIT 6,L", () -> opcode_BIT(l)));
        cb_opcodes.add(new Instruction(0x76, "BIT 6,(HL)", this::opcode_BIT_HL));
        cb_opcodes.add(new Instruction(0x77, "BIT 6,A", () -> opcode_BIT(a)));
        cb_opcodes.add(new Instruction(0x78, "BIT 7,B", () -> opcode_BIT(b)));
        cb_opcodes.add(new Instruction(0x79, "BIT 7,C", () -> opcode_BIT(c)));
        cb_opcodes.add(new Instruction(0x7A, "BIT 7,D", () -> opcode_BIT(d)));
        cb_opcodes.add(new Instruction(0x7B, "BIT 7,E", () -> opcode_BIT(e)));
        cb_opcodes.add(new Instruction(0x7C, "BIT 7,H", () -> opcode_BIT(h)));
        cb_opcodes.add(new Instruction(0x7D, "BIT 7,L", () -> opcode_BIT(l)));
        cb_opcodes.add(new Instruction(0x7E, "BIT 7,(HL)", this::opcode_BIT_HL));
        cb_opcodes.add(new Instruction(0x7F, "BIT 7,A", () -> opcode_BIT(a)));
        cb_opcodes.add(new Instruction(0x80, "RES 0,B", () -> opcode_RES(b)));
        cb_opcodes.add(new Instruction(0x81, "RES 0,C", () -> opcode_RES(c)));
        cb_opcodes.add(new Instruction(0x82, "RES 0,D", () -> opcode_RES(d)));
        cb_opcodes.add(new Instruction(0x83, "RES 0,E", () -> opcode_RES(e)));
        cb_opcodes.add(new Instruction(0x84, "RES 0,H", () -> opcode_RES(h)));
        cb_opcodes.add(new Instruction(0x85, "RES 0,L", () -> opcode_RES(l)));
        cb_opcodes.add(new Instruction(0x86, "RES 0,(HL)", this::opcode_RES_HL));
        cb_opcodes.add(new Instruction(0x87, "RES 0,A", () -> opcode_RES(a)));
        cb_opcodes.add(new Instruction(0x88, "RES 1,B", () -> opcode_RES(b)));
        cb_opcodes.add(new Instruction(0x89, "RES 1,C", () -> opcode_RES(c)));
        cb_opcodes.add(new Instruction(0x8A, "RES 1,D", () -> opcode_RES(d)));
        cb_opcodes.add(new Instruction(0x8B, "RES 1,E", () -> opcode_RES(e)));
        cb_opcodes.add(new Instruction(0x8C, "RES 1,H", () -> opcode_RES(h)));
        cb_opcodes.add(new Instruction(0x8D, "RES 1,L", () -> opcode_RES(l)));
        cb_opcodes.add(new Instruction(0x8E, "RES 1,(HL)", this::opcode_RES_HL));
        cb_opcodes.add(new Instruction(0x8F, "RES 1,A", () -> opcode_RES(a)));
        cb_opcodes.add(new Instruction(0x90, "RES 2,B", () -> opcode_RES(b)));
        cb_opcodes.add(new Instruction(0x91, "RES 2,C", () -> opcode_RES(c)));
        cb_opcodes.add(new Instruction(0x92, "RES 2,D", () -> opcode_RES(d)));
        cb_opcodes.add(new Instruction(0x93, "RES 2,E", () -> opcode_RES(e)));
        cb_opcodes.add(new Instruction(0x94, "RES 2,H", () -> opcode_RES(h)));
        cb_opcodes.add(new Instruction(0x95, "RES 2,L", () -> opcode_RES(l)));
        cb_opcodes.add(new Instruction(0x96, "RES 2,(HL)", this::opcode_RES_HL));
        cb_opcodes.add(new Instruction(0x97, "RES 2,A", () -> opcode_RES(a)));
        cb_opcodes.add(new Instruction(0x98, "RES 3,B", () -> opcode_RES(b)));
        cb_opcodes.add(new Instruction(0x99, "RES 3,C", () -> opcode_RES(c)));
        cb_opcodes.add(new Instruction(0x9A, "RES 3,D", () -> opcode_RES(d)));
        cb_opcodes.add(new Instruction(0x9B, "RES 3,E", () -> opcode_RES(e)));
        cb_opcodes.add(new Instruction(0x9C, "RES 3,H", () -> opcode_RES(h)));
        cb_opcodes.add(new Instruction(0x9D, "RES 3,L", () -> opcode_RES(l)));
        cb_opcodes.add(new Instruction(0x9E, "RES 3,(HL)", this::opcode_RES_HL));
        cb_opcodes.add(new Instruction(0x9F, "RES 3,A", () -> opcode_RES(a)));
        cb_opcodes.add(new Instruction(0xA0, "RES 4,B", () -> opcode_RES(b)));
        cb_opcodes.add(new Instruction(0xA1, "RES 4,C", () -> opcode_RES(c)));
        cb_opcodes.add(new Instruction(0xA2, "RES 4,D", () -> opcode_RES(d)));
        cb_opcodes.add(new Instruction(0xA3, "RES 4,E", () -> opcode_RES(e)));
        cb_opcodes.add(new Instruction(0xA4, "RES 4,H", () -> opcode_RES(h)));
        cb_opcodes.add(new Instruction(0xA5, "RES 4,L", () -> opcode_RES(l)));
        cb_opcodes.add(new Instruction(0xA6, "RES 4,(HL)", this::opcode_RES_HL));
        cb_opcodes.add(new Instruction(0xA7, "RES 4,A", () -> opcode_RES(a)));
        cb_opcodes.add(new Instruction(0xA8, "RES 5,B", () -> opcode_RES(b)));
        cb_opcodes.add(new Instruction(0xA9, "RES 5,C", () -> opcode_RES(c)));
        cb_opcodes.add(new Instruction(0xAA, "RES 5,D", () -> opcode_RES(d)));
        cb_opcodes.add(new Instruction(0xAB, "RES 5,E", () -> opcode_RES(e)));
        cb_opcodes.add(new Instruction(0xAC, "RES 5,H", () -> opcode_RES(h)));
        cb_opcodes.add(new Instruction(0xAD, "RES 5,L", () -> opcode_RES(l)));
        cb_opcodes.add(new Instruction(0xAE, "RES 5,(HL)", this::opcode_RES_HL));
        cb_opcodes.add(new Instruction(0xAF, "RES 5,A", () -> opcode_RES(a)));
        cb_opcodes.add(new Instruction(0xB0, "RES 6,B", () -> opcode_RES(b)));
        cb_opcodes.add(new Instruction(0xB1, "RES 6,C", () -> opcode_RES(c)));
        cb_opcodes.add(new Instruction(0xB2, "RES 6,D", () -> opcode_RES(d)));
        cb_opcodes.add(new Instruction(0xB3, "RES 6,E", () -> opcode_RES(e)));
        cb_opcodes.add(new Instruction(0xB4, "RES 6,H", () -> opcode_RES(h)));
        cb_opcodes.add(new Instruction(0xB5, "RES 6,L", () -> opcode_RES(l)));
        cb_opcodes.add(new Instruction(0xB6, "RES 6,(HL)", this::opcode_RES_HL));
        cb_opcodes.add(new Instruction(0xB7, "RES 6,A", () -> opcode_RES(a)));
        cb_opcodes.add(new Instruction(0xB8, "RES 7,B", () -> opcode_RES(b)));
        cb_opcodes.add(new Instruction(0xB9, "RES 7,C", () -> opcode_RES(c)));
        cb_opcodes.add(new Instruction(0xBA, "RES 7,D", () -> opcode_RES(d)));
        cb_opcodes.add(new Instruction(0xBB, "RES 7,E", () -> opcode_RES(e)));
        cb_opcodes.add(new Instruction(0xBC, "RES 7,H", () -> opcode_RES(h)));
        cb_opcodes.add(new Instruction(0xBD, "RES 7,L", () -> opcode_RES(l)));
        cb_opcodes.add(new Instruction(0xBE, "RES 7,(HL)", this::opcode_RES_HL));
        cb_opcodes.add(new Instruction(0xBF, "RES 7,A", () -> opcode_RES(a)));
        cb_opcodes.add(new Instruction(0xC0, "SET 0,B", () -> opcode_SET(b)));
        cb_opcodes.add(new Instruction(0xC1, "SET 0,C", () -> opcode_SET(c)));
        cb_opcodes.add(new Instruction(0xC2, "SET 0,D", () -> opcode_SET(d)));
        cb_opcodes.add(new Instruction(0xC3, "SET 0,E", () -> opcode_SET(e)));
        cb_opcodes.add(new Instruction(0xC4, "SET 0,H", () -> opcode_SET(h)));
        cb_opcodes.add(new Instruction(0xC5, "SET 0,L", () -> opcode_SET(l)));
        cb_opcodes.add(new Instruction(0xC6, "SET 0,(HL)", this::opcode_SET_HL));
        cb_opcodes.add(new Instruction(0xC7, "SET 0,A", () -> opcode_SET(a)));
        cb_opcodes.add(new Instruction(0xC8, "SET 1,B", () -> opcode_SET(b)));
        cb_opcodes.add(new Instruction(0xC9, "SET 1,C", () -> opcode_SET(c)));
        cb_opcodes.add(new Instruction(0xCA, "SET 1,D", () -> opcode_SET(d)));
        cb_opcodes.add(new Instruction(0xCB, "SET 1,E", () -> opcode_SET(e)));
        cb_opcodes.add(new Instruction(0xCC, "SET 1,H", () -> opcode_SET(h)));
        cb_opcodes.add(new Instruction(0xCD, "SET 1,L", () -> opcode_SET(l)));
        cb_opcodes.add(new Instruction(0xCE, "SET 1,(HL)", this::opcode_SET_HL));
        cb_opcodes.add(new Instruction(0xCF, "SET 1,A", () -> opcode_SET(a)));
        cb_opcodes.add(new Instruction(0xD0, "SET 2,B", () -> opcode_SET(b)));
        cb_opcodes.add(new Instruction(0xD1, "SET 2,C", () -> opcode_SET(c)));
        cb_opcodes.add(new Instruction(0xD2, "SET 2,D", () -> opcode_SET(d)));
        cb_opcodes.add(new Instruction(0xD3, "SET 2,E", () -> opcode_SET(e)));
        cb_opcodes.add(new Instruction(0xD4, "SET 2,H", () -> opcode_SET(h)));
        cb_opcodes.add(new Instruction(0xD5, "SET 2,L", () -> opcode_SET(l)));
        cb_opcodes.add(new Instruction(0xD6, "SET 2,(HL)", this::opcode_SET_HL));
        cb_opcodes.add(new Instruction(0xD7, "SET 2,A", () -> opcode_SET(a)));
        cb_opcodes.add(new Instruction(0xD8, "SET 3,B", () -> opcode_SET(b)));
        cb_opcodes.add(new Instruction(0xD9, "SET 3,C", () -> opcode_SET(c)));
        cb_opcodes.add(new Instruction(0xDA, "SET 3,D", () -> opcode_SET(d)));
        cb_opcodes.add(new Instruction(0xDB, "SET 3,E", () -> opcode_SET(e)));
        cb_opcodes.add(new Instruction(0xDC, "SET 3,H", () -> opcode_SET(h)));
        cb_opcodes.add(new Instruction(0xDD, "SET 3,L", () -> opcode_SET(l)));
        cb_opcodes.add(new Instruction(0xDE, "SET 3,(HL)", this::opcode_SET_HL));
        cb_opcodes.add(new Instruction(0xDF, "SET 3,A", () -> opcode_SET(a)));
        cb_opcodes.add(new Instruction(0xE0, "SET 4,B", () -> opcode_SET(b)));
        cb_opcodes.add(new Instruction(0xE1, "SET 4,B", () -> opcode_SET(c)));
        cb_opcodes.add(new Instruction(0xE2, "SET 4,D", () -> opcode_SET(d)));
        cb_opcodes.add(new Instruction(0xE3, "SET 4,E", () -> opcode_SET(e)));
        cb_opcodes.add(new Instruction(0xE4, "SET 4,H", () -> opcode_SET(h)));
        cb_opcodes.add(new Instruction(0xE5, "SET 4,L", () -> opcode_SET(l)));
        cb_opcodes.add(new Instruction(0xE6, "SET 4,(HL)", this::opcode_SET_HL));
        cb_opcodes.add(new Instruction(0xE7, "SET 4,A", () -> opcode_SET(a)));
        cb_opcodes.add(new Instruction(0xE8, "SET 5,B", () -> opcode_SET(b)));
        cb_opcodes.add(new Instruction(0xE9, "SET 5,C", () -> opcode_SET(c)));
        cb_opcodes.add(new Instruction(0xEA, "SET 5,D", () -> opcode_SET(d)));
        cb_opcodes.add(new Instruction(0xEB, "SET 5,E", () -> opcode_SET(e)));
        cb_opcodes.add(new Instruction(0xEC, "SET 5,H", () -> opcode_SET(h)));
        cb_opcodes.add(new Instruction(0xED, "SET 5,L", () -> opcode_SET(l)));
        cb_opcodes.add(new Instruction(0xEE, "SET 5,(HL)", this::opcode_SET_HL));
        cb_opcodes.add(new Instruction(0xEF, "SET 5,A", () -> opcode_SET(a)));
        cb_opcodes.add(new Instruction(0xF0, "SET 6,B", () -> opcode_SET(b)));
        cb_opcodes.add(new Instruction(0xF1, "SET 6,C", () -> opcode_SET(c)));
        cb_opcodes.add(new Instruction(0xF2, "SET 6,D", () -> opcode_SET(d)));
        cb_opcodes.add(new Instruction(0xF3, "SET 6,E", () -> opcode_SET(e)));
        cb_opcodes.add(new Instruction(0xF4, "SET 6,H", () -> opcode_SET(h)));
        cb_opcodes.add(new Instruction(0xF5, "SET 6,L", () -> opcode_SET(l)));
        cb_opcodes.add(new Instruction(0xF6, "SET 6,(HL)", this::opcode_SET_HL));
        cb_opcodes.add(new Instruction(0xF7, "SET 6,A", () -> opcode_SET(a)));
        cb_opcodes.add(new Instruction(0xF8, "SET 7,B", () -> opcode_SET(b)));
        cb_opcodes.add(new Instruction(0xF9, "SET 7,C", () -> opcode_SET(c)));
        cb_opcodes.add(new Instruction(0xFA, "SET 7,D", () -> opcode_SET(d)));
        cb_opcodes.add(new Instruction(0xFB, "SET 7,E", () -> opcode_SET(e)));
        cb_opcodes.add(new Instruction(0xFC, "SET 7,H", () -> opcode_SET(h)));
        cb_opcodes.add(new Instruction(0xFD, "SET 7,L", () -> opcode_SET(l)));
        cb_opcodes.add(new Instruction(0xFE, "SET 7,(HL)", this::opcode_SET_HL));
        cb_opcodes.add(new Instruction(0xFF, "SET 7,A", () -> opcode_SET(a)));
    }

    public void clock() {
        if (halted) {
            cycle++;
            return;
        }

        if (remaining_cycle_until_op == 0) {
            int opcode = read8(pc.read());
            //System.out.println(Integer.toHexString(pc.read()));
            pc.inc();
            Instruction inst = opcodes.get(opcode);
            remaining_cycle_until_op = inst.operate()/4;
            if (enable_interrupt_in_opcode > 0) {
                enable_interrupt_in_opcode--;
            } else if (enable_interrupt_in_opcode == 0){
                IME = true;
                enable_interrupt_in_opcode = -1;
            }

            if (disable_interrupt_in_opcode > 0) {
                disable_interrupt_in_opcode--;
            } else if (disable_interrupt_in_opcode == 0) {
                IME = false;
                disable_interrupt_in_opcode = -1;
            }
        } else {
            remaining_cycle_until_op--;
        }

        cycle++;
    }

    public void reset() {
        af.write(0x01B0);
        bc.write(0x0013);
        de.write(0x00D8);
        hl.write(0x014D);
        sp.write(0xFFFE);
        pc.write(0x0000);
        cycle = 0;
        remaining_cycle_until_op = 0;
        disable_interrupt_in_opcode = -1;
        enable_interrupt_in_opcode = -1;
    }

    private int read8(int addr) {
        return memory.readByte(addr & 0xFFFF);
    }

    private void write8(int addr, int data) {
        memory.writeByte(addr & 0xFFFF, data & 0xFF);
    }

    private int read16(int addr) {
        return read8(addr & 0xFFFF) | (read8(addr + 1) << 8);
    }

    private void write16(int addr, int data) {
        addr &= 0xFFFF;
        write8(addr, lsb(data));
        write8(addr + 1, msb(data));
    }

    private void pushStack(int data) {
        sp.dec();
        write8(sp.read(), msb(data));
        sp.dec();
        write8(sp.read(), lsb(data));
    }

    private int popStack() {
        int data = read8(sp.read()) | (read8(sp.read() + 1) << 8);
        sp.inc();
        sp.inc();
        return data;
    }

    private void setFlag(Flags flag, boolean state) {
        if (state)
            f.write(f.read() | flag.getMask());
        else
            f.write(f.read() & ~flag.getMask());
    }

    private boolean hasFlag(Flags flag) {
        return (f.read() & flag.getMask()) == flag.getMask();
    }

    private int prefix() {
        int opcode = read8(pc.read());
        pc.inc();
        return 4 + cb_opcodes.get(opcode).operate();
    }

    public int opcode_LD(Register8 reg) {
        reg.write(read8(pc.read()));
        pc.inc();
        return 8;
    }

    public int opcode_LD(Register16 reg) {
        reg.write(read16(pc.read()));
        pc.inc();
        pc.inc();
        return 12;
    }

    public int opcode_LD(Register8 reg, Register8 reg1) {
        reg.write(reg1.read());
        return 4;
    }

    public int opcode_LD_from_HL_addr(Register8 reg) {
        reg.write(read8(hl.read()));
        return 8;
    }

    public int opcode_LD_to_HL_addr(Register8 reg) {
        write8(hl.read(), reg.read());
        return 8;
    }

    public int opcode_LD_to_HL_addr() {
        write8(hl.read(), read8(pc.read()));
        pc.inc();
        return 12;
    }

    public int opcode_LD_to_A_from_addr(Register16 reg) {
        a.write(read8(reg.read()));
        return 8;
    }

    public int opcode_LD_to_A_REL() {
        a.write(read8(read16(pc.read())));
        pc.inc();
        pc.inc();
        return 8;
    }

    public int opcode_LD_to_addr_from_A(Register16 reg) {
        write8(reg.read(), a.read());
        return 8;
    }

    public int opcode_LD_to_addr_from_A() {
        write8(read16(pc.read()), a.read());
        pc.inc();
        pc.inc();
        return 16;
    }

    public int opcode_LD_A_C() {
        a.write(read8(0xFF00 | c.read()));
        return 8;
    }

    public int opcode_LD_C_A() {
        write8(0xFF00 | c.read(), a.read());
        return 8;
    }

    public int opcode_LD_A_HLD() {
        a.write(read8(hl.read()));
        hl.dec();
        return 8;
    }

    public int opcode_LD_HLD_A() {
        write8(hl.read(), a.read());
        hl.dec();
        return 8;
    }

    public int opcode_LD_A_HLI() {
        a.write(read8(hl.read()));
        hl.inc();
        return 8;
    }

    public int opcode_LD_HLI_A() {
        write8(hl.read(), a.read());
        hl.inc();
        return 8;
    }

    public int opcode_LD_from_A_off() {
        write8(0xFF00 | read8(pc.read()), a.read());
        pc.inc();
        return 12;
    }

    public int opcode_LD_from_off_A() {
        a.write(read8(0xFF00 | read8(pc.read())));
        pc.inc();
        return 12;
    }

    public int opcode_LD_SP_HL() {
        sp.write(hl.read());
        return 8;
    }

    public int opcode_LD_HL_SP_off() {
        hl.write(sp.read() + signed8(read8(pc.read())));
        pc.inc();
        return 12;
    }

    public int opcode_LD_from_SP() {
        write16(read16(pc.read()), sp.read());
        pc.inc();
        pc.inc();
        return 20;
    }

    public int opcode_PUSH(Register16 reg) {
        pushStack(reg.read());
        return 16;
    }

    public int opcode_POP(Register16 reg) {
        reg.write(popStack());
        return 12;
    }

    public int opcode_ADD(Register8 reg) {
        int val = (a.read() + reg.read());

        int carry = a.read() ^ reg.read() ^ val;

        setFlag(Flags.ZERO, (val & 0xFF) == 0x00);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, (carry & 0x10) == 0x10);
        setFlag(Flags.CARRY, (carry & 0x100) == 0x100);

        a.write(val & 0xFF);
        return 4;
    }

    public int opcode_ADD() {
        int read = read8(pc.read());
        int val = (a.read() + read);
        int carry = a.read() ^ read ^ val;

        setFlag(Flags.ZERO, (val & 0xFF) == 0x00);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, (carry & 0x10) == 0x10);
        setFlag(Flags.CARRY, (carry & 0x100) == 0x100);

        a.write(val & 0xFF);
        pc.inc();
        return 8;
    }

    public int opcode_ADD_HL() {
        int read = read8(hl.read());
        int val = (a.read() + read);
        int carry = a.read() ^ read ^ val;

        setFlag(Flags.ZERO, (val & 0xFF) == 0x00);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, (carry & 0x10) == 0x10);
        setFlag(Flags.CARRY, (carry & 0x100) == 0x100);

        a.write(val & 0xFF);
        return 8;
    }

    public int opcode_ADC(Register8 reg) {
        int val = (a.read() + reg.read()) + (hasFlag(Flags.CARRY) ? 1 : 0);

        setFlag(Flags.ZERO, (val & 0xFF) == 0x00);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, (((a.read() & 0xF) + (reg.read() & 0xF) + (hasFlag(Flags.CARRY) ? 1 : 0)) & 0xF0) == 0x00);
        setFlag(Flags.CARRY, (val & 0x100) == 0x100);

        a.write(val & 0xFF);
        return 4;
    }

    public int opcode_ADC() {
        int read = read8(pc.read());
        int val = (a.read() + read) + (hasFlag(Flags.CARRY) ? 1 : 0);

        setFlag(Flags.ZERO, (val & 0xFF) == 0x00);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, (((a.read() & 0xF) + (read & 0xF) + (hasFlag(Flags.CARRY) ? 1 : 0)) & 0xF0) != 0x00);
        setFlag(Flags.CARRY, (val & 0x100) == 0x100);

        a.write(val & 0xFF);
        pc.inc();
        return 8;
    }

    public int opcode_ADC_HL() {
        int read = read8(hl.read());
        int val = (a.read() + read) + (hasFlag(Flags.CARRY) ? 1 : 0);

        setFlag(Flags.ZERO, (val & 0xFF) == 0x00);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, (((a.read() & 0xF) + (read & 0xF) + (hasFlag(Flags.CARRY) ? 1 : 0)) & 0xF0) == 0x00);
        setFlag(Flags.CARRY, (val & 0x100) == 0x100);

        a.write(val & 0xFF);
        return 8;
    }

    public int opcode_SUB(Register8 reg) {
        int val = (a.read() - reg.read());
        int carry = a.read() ^ reg.read() ^ val;

        setFlag(Flags.ZERO, (val & 0xFF) == 0x00);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, (carry & 0x10) == 0x10);
        setFlag(Flags.CARRY, (carry & 0x100) == 0x100);

        a.write(val & 0xFF);
        return 4;
    }

    public int opcode_SUB() {
        int read = read8(pc.read());
        int val = (a.read() - read);
        int carry = a.read() ^ read ^ val;

        setFlag(Flags.ZERO, (val & 0xFF) == 0x00);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, (carry & 0x10) == 0x10);
        setFlag(Flags.CARRY, (carry & 0x100) == 0x100);

        a.write(val & 0xFF);
        pc.inc();
        return 8;
    }

    public int opcode_SUB_HL() {
        int read = read8(hl.read());
        int val = (a.read() - read);
        int carry = a.read() ^ read ^ val;

        setFlag(Flags.ZERO, (val & 0xFF) == 0x00);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, (carry & 0x10) == 0x10);
        setFlag(Flags.CARRY, (carry & 0x100) == 0x100);

        a.write(val & 0xFF);
        return 8;
    }

    public int opcode_SBC(Register8 reg) {
        int val = (a.read() - (reg.read() + (hasFlag(Flags.CARRY) ? 1 : 0)));

        setFlag(Flags.ZERO, (val & 0xFF) == 0x00);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, (a.read() & 0xF) - (reg.read() & 0xF) - (hasFlag(Flags.CARRY) ? 1 : 0) < 0);
        setFlag(Flags.CARRY, a.read() < reg.read());

        a.write(val & 0xFF);
        return 4;
    }

    public int opcode_SBC() {
        int read = read8(pc.read());
        int val = (a.read() - (read + (hasFlag(Flags.CARRY) ? 1 : 0)));

        setFlag(Flags.ZERO, (val & 0xFF) == 0x00);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, (a.read() & 0xF) - (read & 0xF) - (hasFlag(Flags.CARRY) ? 1 : 0) < 0);
        setFlag(Flags.CARRY, a.read() < read);

        a.write(val & 0xFF);
        pc.inc();
        return 8;
    }

    public int opcode_SBC_HL() {
        int read = read8(hl.read());
        int val = (a.read() - (read + (hasFlag(Flags.CARRY) ? 1 : 0)));

        setFlag(Flags.ZERO, (val & 0xFF) == 0x00);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, (a.read() & 0xF) - (read & 0xF) - (hasFlag(Flags.CARRY) ? 1 : 0) < 0);
        setFlag(Flags.CARRY, a.read() < read);

        a.write(val & 0xFF);
        return 8;
    }

    public int opcode_AND(Register8 reg) {
        int val = (a.read() & reg.read());

        setFlag(Flags.ZERO, (val & 0xFF) == 0x00);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, true);
        setFlag(Flags.CARRY, false);

        a.write(val & 0xFF);
        return 4;
    }

    public int opcode_AND() {
        int val = (a.read() & read8(pc.read()));

        setFlag(Flags.ZERO, (val & 0xFF) == 0x00);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, true);
        setFlag(Flags.CARRY, false);

        a.write(val & 0xFF);
        pc.inc();
        return 8;
    }

    public int opcode_AND_HL() {
        int val = (a.read() & read8(pc.read()));
        pc.inc();

        setFlag(Flags.ZERO, (val & 0xFF) == 0x00);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, true);
        setFlag(Flags.CARRY, false);

        a.write(val & 0xFF);
        return 8;
    }

    public int opcode_OR(Register8 reg) {
        int val = (a.read() | reg.read());

        setFlag(Flags.ZERO, (val & 0xFF) == 0x00);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, false);
        setFlag(Flags.CARRY, false);

        a.write(val & 0xFF);
        return 4;
    }

    public int opcode_OR() {
        int val = (a.read() | read8(pc.read()));

        setFlag(Flags.ZERO, (val & 0xFF) == 0x00);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, false);
        setFlag(Flags.CARRY, false);

        a.write(val & 0xFF);
        pc.inc();
        return 8;
    }

    public int opcode_OR_HL() {
        int val = (a.read() | read8(hl.read()));

        setFlag(Flags.ZERO, (val & 0xFF) == 0x00);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, false);
        setFlag(Flags.CARRY, false);

        a.write(val & 0xFF);
        return 8;
    }

    public int opcode_XOR(Register8 reg) {
        int val = (a.read() ^ reg.read());

        setFlag(Flags.ZERO, (val & 0xFF) == 0x00);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, false);
        setFlag(Flags.CARRY, false);

        a.write(val & 0xFF);
        return 4;
    }

    public int opcode_XOR() {
        int val = (a.read() ^ read8(pc.read()));

        setFlag(Flags.ZERO, (val & 0xFF) == 0x00);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, false);
        setFlag(Flags.CARRY, false);

        a.write(val & 0xFF);
        pc.inc();
        return 8;
    }

    public int opcode_XOR_HL() {
        int val = (a.read() ^ read8(hl.read()));

        setFlag(Flags.ZERO, (val & 0xFF) == 0x00);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, false);
        setFlag(Flags.CARRY, false);

        a.write(val & 0xFF);
        return 8;
    }

    public int opcode_CP(Register8 reg) {
        int val = (a.read() - reg.read());

        setFlag(Flags.ZERO, (val & 0xFF) == 0x00);
        setFlag(Flags.SUBTRACT, true);
        setFlag(Flags.HALF_CARRY, (val & 0xF) > (a.read() & 0xF));
        setFlag(Flags.CARRY, a.read() < reg.read());

        return 4;
    }

    public int opcode_CP() {
        int read = read8(pc.read());
        int val = (a.read() - read);

        setFlag(Flags.ZERO, (val & 0xFF) == 0x00);
        setFlag(Flags.SUBTRACT, true);
        setFlag(Flags.HALF_CARRY, (val & 0xF) > (a.read() & 0xF));
        setFlag(Flags.CARRY, a.read() < read);

        pc.inc();
        return 8;
    }

    public int opcode_CP_HL() {
        int read = read8(hl.read());
        int val = (a.read() - read);

        setFlag(Flags.ZERO, (val & 0xFF) == 0x00);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, (val & 0xF) > (a.read() & 0xF));
        setFlag(Flags.CARRY, a.read() < read);

        return 8;
    }

    public int opcode_INC(Register8 reg) {
        setFlag(Flags.ZERO, reg.read() == 0xFF);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, (reg.read() & 0xF) == 0xF);

        reg.inc();
        return 4;
    }

    public int opcode_INC_HL() {
        int val = read8(hl.read());
        setFlag(Flags.ZERO, val == 0xFF);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, (val & 0xF) == 0xF);

        write8(hl.read(), (val + 1) & 0xFF);
        return 12;
    }

    public int opcode_DEC(Register8 reg) {
        setFlag(Flags.ZERO, reg.read() == 0x01);
        setFlag(Flags.SUBTRACT, false);
        reg.dec();
        setFlag(Flags.HALF_CARRY, (reg.read() & 0xF) == 0xF);

        return 4;
    }

    public int opcode_DEC_HL() {
        int val = read8(hl.read());
        setFlag(Flags.ZERO, val == 0x01);
        setFlag(Flags.SUBTRACT, true);
        setFlag(Flags.HALF_CARRY, ((val - 1) & 0xF) == 0xF);

        write8(hl.read(), (val - 1) & 0xFF);
        return 12;
    }

    public int opcode_ADD_HL(Register16 reg) {
        int val = hl.read() + reg.read();
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, ((hl.read() ^ reg.read() ^ (val & 0xFFFF)) & 0x1000) == 0x1000);
        setFlag(Flags.CARRY, (val & 0x10000) == 0x10000);

        hl.write(val);
        return 8;
    }

    public int opcode_ADD_SP() {
        int offset = signed8(read8(pc.read()));
        int val = sp.read() + offset;
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, ((hl.read() ^ offset ^ (val & 0xFFFF)) & 0x1000) == 0x1000);
        setFlag(Flags.CARRY, (val & 0x10000) == 0x10000);

        sp.write(val);
        pc.inc();
        return 8;
    }

    public int opcode_INC(Register16 reg) {
        reg.inc();
        return 8;
    }

    public int opcode_DEC(Register16 reg) {
        reg.dec();
        return 8;
    }

    public int opcode_SWAP(Register8 reg) {
        reg.write(((reg.read() >> 4) & 0xF) | (reg.read() << 4));
        setFlag(Flags.ZERO, reg.read() == 0x00);
        return 8;
    }

    public int opcode_SWAP_HL() {
        int data = read8(hl.read());
        write8(hl.read(), (data >> 4) | (data << 4));
        setFlag(Flags.ZERO, data == 0x00);
        return 16;
    }

    public int opcode_DAA() {
        if (!hasFlag(Flags.SUBTRACT)) {
            if (hasFlag(Flags.HALF_CARRY) || ((a.read() & 0xF) > 9))
                a.write(a.read() + 0x06);
            if (hasFlag(Flags.CARRY) || (a.read() > 0x9F))
                a.write(a.read() + 0x60);
        } else {
            if (hasFlag(Flags.HALF_CARRY))
                a.write((a.read() - 0x6) & 0xFF);
            if (hasFlag(Flags.CARRY))
                a.write(a.read() - 0x60);
        }

        setFlag(Flags.HALF_CARRY, false);
        setFlag(Flags.ZERO, false);

        if ((a.read() & 0x100) == 0x100)
            setFlag(Flags.CARRY, true);
        a.write(a.read() & 0xFF);
        setFlag(Flags.ZERO, a.read() == 0x00);
        return 4;
    }

    public int opcode_CPL() {
        a.write(~a.read());
        setFlag(Flags.SUBTRACT, true);
        setFlag(Flags.HALF_CARRY, true);

        return 4;
    }

    public int opcode_CCF() {
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, false);
        setFlag(Flags.CARRY, !hasFlag(Flags.CARRY));

        return 4;
    }

    public int opcode_SCF() {
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, false);
        setFlag(Flags.CARRY, true);

        return 4;
    }

    public int opcode_NOP() {
        return 4;
    }

    public int opcode_HALT() {
        halted = true;
        return 4;
    }

    public int opcode_STOP() {
        halted = true;
        pc.inc();
        return 4;
    }

    public int opcode_DI() {
        disable_interrupt_in_opcode = 1;
        return 4;
    }

    public int opcode_EI() {
        enable_interrupt_in_opcode = 1;
        return 4;
    }

    public int opcode_RLCA() {
        setFlag(Flags.CARRY, (a.read() & 0x80) == 0x80);
        a.write((a.read() << 1) | ((a.read() >> 7) & 0x01));
        setFlag(Flags.HALF_CARRY, false);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.ZERO, a.read() == 0x00);
        return 4;
    }

    public int opcode_RLA() {
        int tmp = a.read();
        a.write((a.read() << 1) | (hasFlag(Flags.CARRY) ? 0x01 : 0x00));

        setFlag(Flags.HALF_CARRY, false);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.ZERO, a.read() == 0x00);
        setFlag(Flags.CARRY, (tmp & 0x80) == 0x80);
        return 4;
    }

    public int opcode_RRCA() {
        setFlag(Flags.CARRY, (a.read() & 0x01) == 0x01);
        a.write((a.read() >> 1) | ((a.read() & 0x01) << 7));
        setFlag(Flags.HALF_CARRY, false);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.ZERO, a.read() == 0x00);
        return 4;
    }

    public int opcode_RRA() {
        int tmp = a.read();
        a.write((a.read() >> 1) | (hasFlag(Flags.CARRY) ? 0x80 : 0x00));

        setFlag(Flags.HALF_CARRY, false);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.ZERO, a.read() == 0x00);
        setFlag(Flags.CARRY, (tmp & 0x01) == 0x01);
        return 4;
    }

    public int opcode_RLC(Register8 reg) {
        setFlag(Flags.CARRY, (reg.read() & 0x80) == 0x80);
        setFlag(Flags.HALF_CARRY, false);
        setFlag(Flags.SUBTRACT, false);
        reg.write((reg.read() << 1) | ((reg.read() >> 7) & 0x01));
        setFlag(Flags.ZERO, reg.read() == 0x00);
        return 8;
    }

    public int opcode_RLC_HL() {
        int tmp = read8(hl.read());
        write8(hl.read(),(tmp << 1) | ((tmp >> 7) & 0x01));
        setFlag(Flags.HALF_CARRY, false);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.ZERO, tmp == 0x00);
        setFlag(Flags.CARRY, (tmp & 0x80) == 0x80);
        return 16;
    }

    public int opcode_RL(Register8 reg) {
        int tmp = reg.read();
        reg.write((reg.read() << 1) | (hasFlag(Flags.CARRY) ? 0x01 : 0x00));

        setFlag(Flags.HALF_CARRY, false);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.ZERO, reg.read() == 0x00);
        setFlag(Flags.CARRY, (tmp & 0x80) == 0x80);
        return 8;
    }

    public int opcode_RL_HL() {
        int tmp = read8(hl.read());
        write8(hl.read(), (tmp << 1) | (hasFlag(Flags.CARRY) ? 0x01 : 0x00));

        setFlag(Flags.HALF_CARRY, false);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.ZERO, read8(hl.read()) == 0x00);
        setFlag(Flags.CARRY, (tmp & 0x80) == 0x80);
        return 16;
    }

    public int opcode_RRC(Register8 reg) {
        setFlag(Flags.CARRY, (reg.read() & 0x01) == 0x01);
        reg.write((reg.read() >> 1) | ((reg.read() & 0x01) << 7));
        setFlag(Flags.HALF_CARRY, false);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.ZERO, reg.read() == 0x00);
        return 8;
    }

    public int opcode_RRC_HL() {
        int tmp = read8(hl.read());
        write8(hl.read(),(tmp >> 1) | ((tmp & 0x01) << 7));

        setFlag(Flags.HALF_CARRY, false);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.ZERO, tmp == 0x00);
        setFlag(Flags.CARRY, (tmp & 0x01) == 0x01);
        return 16;
    }

    public int opcode_RR(Register8 reg) {
        int tmp = reg.read();
        reg.write((reg.read() >> 1) | (hasFlag(Flags.CARRY) ? 0x80 : 0x00));

        setFlag(Flags.HALF_CARRY, false);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.ZERO, reg.read() == 0x00);
        setFlag(Flags.CARRY, (tmp & 0x80) == 0x80);
        return 8;
    }

    public int opcode_RR_HL() {
        int tmp = read8(hl.read());
        write8(hl.read(), (tmp >> 1) | (hasFlag(Flags.CARRY) ? 0x80 : 0x00));

        setFlag(Flags.HALF_CARRY, false);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.ZERO, read8(hl.read()) == 0x00);
        setFlag(Flags.CARRY, (tmp & 0x01) == 0x01);
        return 16;
    }

    public int opcode_SLA(Register8 reg) {
        setFlag(Flags.CARRY, (reg.read() & 0x80) == 0x80);
        reg.write(reg.read() << 1);
        setFlag(Flags.HALF_CARRY, false);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.ZERO, reg.read() == 0x00);
        return 8;
    }

    public int opcode_SLA_HL() {
        int tmp = read8(hl.read());
        setFlag(Flags.CARRY, (tmp & 0x80) == 0x80);
        write8(hl.read(), tmp << 1);
        setFlag(Flags.HALF_CARRY, false);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.ZERO, (tmp << 1) == 0x00);
        return 16;
    }

    public int opcode_SRA(Register8 reg) {
        setFlag(Flags.CARRY, (reg.read() & 0x01) == 0x01);
        reg.write((reg.read() >> 1) | (reg.read() & 0x80));
        setFlag(Flags.HALF_CARRY, false);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.ZERO, reg.read() == 0x00);
        return 8;
    }

    public int opcode_SRA_HL() {
        int tmp = read8(hl.read());
        setFlag(Flags.CARRY, (tmp & 0x01) == 0x01);
        write8(hl.read(), (tmp >> 1) | (tmp & 0x80));
        setFlag(Flags.HALF_CARRY, false);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.ZERO, ((tmp >> 1) | (tmp & 0x80)) == 0x00);
        return 16;
    }

    public int opcode_SRL(Register8 reg) {
        setFlag(Flags.CARRY, (reg.read() & 0x01) == 0x01);
        reg.write((reg.read() >> 1));
        setFlag(Flags.HALF_CARRY, false);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.ZERO, reg.read() == 0x00);
        return 8;
    }

    public int opcode_SRL_HL() {
        int tmp = read8(hl.read());
        setFlag(Flags.CARRY, (tmp & 0x01) == 0x01);
        write8(hl.read(), (tmp >> 1));
        setFlag(Flags.HALF_CARRY, false);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.ZERO, (tmp >> 1) == 0x00);
        return 16;
    }

    public int opcode_BIT(Register8 reg) {
        int mask = (0x01 << read8(pc.read())) & 0x07;

        setFlag(Flags.HALF_CARRY, true);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.ZERO, (reg.read() & mask) == mask);
        pc.inc();

        return 8;
    }

    public int opcode_BIT_HL() {
        int mask = (0x01 << read8(pc.read())) & 0x07;

        setFlag(Flags.HALF_CARRY, true);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.ZERO, (read8(hl.read()) & mask) == mask);
        pc.inc();

        return 16;
    }

    public int opcode_SET(Register8 reg) {
        int mask = (0x01 << read8(pc.read())) & 0x07;
        reg.write(reg.read() | mask);
        pc.inc();
        return 8;
    }

    public int opcode_SET_HL() {
        int mask = (0x01 << read8(pc.read())) & 0x07;
        write8(hl.read(), read8(hl.read()) | mask);
        pc.inc();
        return 16;
    }

    public int opcode_RES(Register8 reg) {
        int mask = (0x01 << read8(pc.read())) & 0x07;
        reg.write(reg.read() & ~mask);
        pc.inc();
        return 8;
    }

    public int opcode_RES_HL() {
        int mask = (0x01 << read8(pc.read())) & 0x07;
        write8(hl.read(), read8(hl.read()) & ~mask);
        pc.inc();
        return 16;
    }

    public int opcode_JP() {
        pc.write(read16(pc.read()));
        return 12;
    }

    public int opcode_JP_NZ() {
        if (!hasFlag(Flags.ZERO)) {
            pc.write(read16(pc.read()));
        } else {
            pc.inc();
            pc.inc();
        }
        return 16;
    }

    public int opcode_JP_Z() {
        if (hasFlag(Flags.ZERO)) {
            pc.write(read16(pc.read()));
        } else {
            pc.inc();
            pc.inc();
        }
        return 16;
    }

    public int opcode_JP_NC() {
        if (!hasFlag(Flags.CARRY)) {
            pc.write(read16(pc.read()));
        } else {
            pc.inc();
            pc.inc();
        }
        return 16;
    }

    public int opcode_JP_C() {
        if (hasFlag(Flags.CARRY)) {
            pc.write(read16(pc.read()));
        } else {
            pc.inc();
            pc.inc();
        }
        return 16;
    }

    public int opcode_JP_HL() {
        pc.write(hl.read());
        return 4;
    }

    public int opcode_JR() {
        int offset = signed8(read8(pc.read()));
        pc.inc();
        pc.write(pc.read() + offset);
        return 8;
    }

    public int opcode_JR_NZ() {
        int offset = signed8(read8(pc.read()));
        pc.inc();
        if (!hasFlag(Flags.ZERO))
            pc.write(pc.read() + offset);
        return 8;
    }

    public int opcode_JR_Z() {
        int offset = signed8(read8(pc.read()));
        pc.inc();
        if (hasFlag(Flags.ZERO))
            pc.write(pc.read() + offset);
        return 8;
    }

    public int opcode_JR_NC() {
        int offset = signed8(read8(pc.read()));
        pc.inc();
        if (!hasFlag(Flags.CARRY))
            pc.write(pc.read() + offset);
        return 8;
    }

    public int opcode_JR_C() {
        int offset = signed8(read8(pc.read()));
        pc.inc();
        if (hasFlag(Flags.CARRY))
            pc.write(pc.read() + offset);
        return 8;
    }

    public int opcode_CALL() {
        int addr = read16(pc.read());
        pc.inc();
        pc.inc();
        pushStack(pc.read());
        pc.write(addr);
        return 12;
    }

    public int opcode_CALL_NZ() {
        int addr = read16(pc.read());
        pc.inc();
        pc.inc();
        if (!hasFlag(Flags.ZERO)) {
            pushStack(pc.read());
            pc.write(addr);
        }
        return 12;
    }

    public int opcode_CALL_Z() {
        int addr = read16(pc.read());
        pc.inc();
        pc.inc();
        if (hasFlag(Flags.ZERO)) {
            pushStack(pc.read());
            pc.write(addr);
        }
        return 12;
    }

    public int opcode_CALL_NC() {
        int addr = read16(pc.read());
        pc.inc();
        pc.inc();
        if (!hasFlag(Flags.CARRY)) {
            pushStack(pc.read());
            pc.write(addr);
        }
        return 12;
    }

    public int opcode_CALL_C() {
        int addr = read16(pc.read());
        pc.inc();
        pc.inc();
        if (hasFlag(Flags.CARRY)) {
            pushStack(pc.read());
            pc.write(addr);
        }
        return 12;
    }

    public int opcode_RST(int offset) {
        pushStack(pc.read());
        pc.write(offset);
        return 32;
    }

    public int opcode_RET() {
         pc.write(popStack());
         return 8;
    }

    public int opcode_RET_NZ() {
        if (!hasFlag(Flags.ZERO))
            pc.write(popStack());
        return 8;
    }

    public int opcode_RET_Z() {
        if (hasFlag(Flags.ZERO))
            pc.write(popStack());
        return 8;
    }

    public int opcode_RET_NC() {
        if (!hasFlag(Flags.CARRY))
            pc.write(popStack());
        return 8;
    }

    public int opcode_RET_C() {
        if (hasFlag(Flags.CARRY))
            pc.write(popStack());
        return 8;
    }

    public int opcode_RETI() {
        pc.write(popStack());
        IME = true;
        return 8;
    }

    public static class Instruction {

        private final String name;
        private final int opcode;
        private final Supplier<Integer> fct_operate;

        public Instruction(int opcode, String name, Supplier<Integer> fct_operate) {
            this.name = name;
            this.opcode = opcode;
            this.fct_operate = fct_operate;
        }

        int operate() {
            return fct_operate.get();
        }

        public String getName() {
            return name;
        }

        public int getOpcode() {
            return opcode;
        }

        @Override
        public String toString() {
            return "[$" + Integer.toHexString(opcode) + "] " + name;
        }
    }
}
