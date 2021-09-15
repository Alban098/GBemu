package core.cpu;

import core.Memory;
import core.cpu.register.Register16;
import core.cpu.register.Register8;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static core.BitUtils.lsb;
import static core.BitUtils.msb;
import static core.BitUtils.signed8;

public class LR35902 {

    public static final boolean TRACE = false;

    BufferedWriter bw;
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
    private boolean enableIRQ = true;
    private int timerCycles;
    private int timerTotalCycles = 0xFF;
    private int divider;

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
        opcodes.add(new Instruction(0x00, "NOP", this::nop));
        opcodes.add(new Instruction(0x01, "LD BC,#16", () -> ld_rr_nn(bc)));
        opcodes.add(new Instruction(0x02, "LD (BC),A", () -> ld_rr_A(bc)));
        opcodes.add(new Instruction(0x03, "INC BC", () -> inc_rr(bc)));
        opcodes.add(new Instruction(0x04, "INC B", () -> inc_r(b)));
        opcodes.add(new Instruction(0x05, "DEC B", () -> dec_r(b)));
        opcodes.add(new Instruction(0x06, "LD B,#8", () -> ld_r_n(b)));
        opcodes.add(new Instruction(0x07, "RLCA", this::rlca));
        opcodes.add(new Instruction(0x08, "LD (#16),SP", this::ld_nn_SP));
        opcodes.add(new Instruction(0x09, "ADD HL,BC", () -> add_HL_rr(bc)));
        opcodes.add(new Instruction(0x0A, "LD A,(BC)", () -> ld_A_rr(bc)));
        opcodes.add(new Instruction(0x0B, "DEC BC", () -> dec_rr(bc)));
        opcodes.add(new Instruction(0x0C, "INC C", () -> inc_r(c)));
        opcodes.add(new Instruction(0x0D, "DEC C", () -> dec_r(c)));
        opcodes.add(new Instruction(0x0E, "LD C,#8", () -> ld_r_n(c)));
        opcodes.add(new Instruction(0x0F, "RRCA", this::rrca));
        opcodes.add(new Instruction(0x10, "STOP", this::stop));
        opcodes.add(new Instruction(0x11, "LD DE,d16", () -> ld_rr_nn(de)));
        opcodes.add(new Instruction(0x12, "LD (DE),A", () -> ld_rr_A(de)));
        opcodes.add(new Instruction(0x13, "INC DE", () -> inc_rr(de)));
        opcodes.add(new Instruction(0x14, "INC D", () -> inc_r(d)));
        opcodes.add(new Instruction(0x15, "DEC D", () -> dec_r(d)));
        opcodes.add(new Instruction(0x16, "LD D,d8", () -> ld_r_n(d)));
        opcodes.add(new Instruction(0x17, "RLA", this::rla));
        opcodes.add(new Instruction(0x18, "JR r8", this::jr_n));
        opcodes.add(new Instruction(0x19, "ADD HL,DE", () -> add_HL_rr(de)));
        opcodes.add(new Instruction(0x1A, "LD A,(DE)", () -> ld_A_rr(de)));
        opcodes.add(new Instruction(0x1B, "DEC DE", () -> dec_rr(de)));
        opcodes.add(new Instruction(0x1C, "INC E", () -> inc_r(e)));
        opcodes.add(new Instruction(0x1D, "DEC E", () -> dec_r(e)));
        opcodes.add(new Instruction(0x1E, "LD E,d8", () -> ld_r_n(e)));
        opcodes.add(new Instruction(0x1F, "RRA", this::rra));
        opcodes.add(new Instruction(0x20, "JR NZ,r8", this::jr_nz_n));
        opcodes.add(new Instruction(0x21, "LD HL,d16", () -> ld_rr_nn(hl)));
        opcodes.add(new Instruction(0x22, "LD (HL+),A", this::ldd_HLI_A));
        opcodes.add(new Instruction(0x23, "INC HL", () -> inc_rr(hl)));
        opcodes.add(new Instruction(0x24, "INC H", () -> inc_r(h)));
        opcodes.add(new Instruction(0x25, "DEC H", () -> dec_r(h)));
        opcodes.add(new Instruction(0x26, "LD H,d8", () -> ld_r_n(h)));
        opcodes.add(new Instruction(0x27, "DAA", this::daa));
        opcodes.add(new Instruction(0x28, "JR Z,r8", this::jr_z_n));
        opcodes.add(new Instruction(0x29, "ADD HL,HL", () -> add_HL_rr(hl)));
        opcodes.add(new Instruction(0x2A, "LD A,(HL+)", this::ldd_A_HLI));
        opcodes.add(new Instruction(0x2B, "DEC HL", () -> dec_rr(hl)));
        opcodes.add(new Instruction(0x2C, "INC L", () -> inc_r(l)));
        opcodes.add(new Instruction(0x2D, "DEC L", () -> dec_r(l)));
        opcodes.add(new Instruction(0x2E, "LD L,d8", () -> ld_r_n(l)));
        opcodes.add(new Instruction(0x2F, "CPL", this::cpl));
        opcodes.add(new Instruction(0x30, "JR NC,r8", this::jr_nc_n));
        opcodes.add(new Instruction(0x31, "LD SP,d16", () -> ld_rr_nn(sp)));
        opcodes.add(new Instruction(0x32, "LD (HL-),A", this::ldd_HLD_A));
        opcodes.add(new Instruction(0x33, "INC SP", () -> inc_rr(sp)));
        opcodes.add(new Instruction(0x34, "INC (HL)", this::inc_HL));
        opcodes.add(new Instruction(0x35, "DEC (HL)", this::dec_HL));
        opcodes.add(new Instruction(0x36, "LD (HL),d8", this::ld_HL_n));
        opcodes.add(new Instruction(0x37, "SCF", this::scf));
        opcodes.add(new Instruction(0x38, "JR C,r8", this::jr_c_n));
        opcodes.add(new Instruction(0x39, "ADD HL,SP", () -> add_HL_rr(sp)));
        opcodes.add(new Instruction(0x3A, "LD A,(HL-)", this::ldd_A_HLD));
        opcodes.add(new Instruction(0x3B, "DEC SP", () -> dec_rr(sp)));
        opcodes.add(new Instruction(0x3C, "INC A", () -> inc_r(a)));
        opcodes.add(new Instruction(0x3D, "DEC A", () -> dec_r(a)));
        opcodes.add(new Instruction(0x3E, "LD A,d8", () -> ld_r_n(a)));
        opcodes.add(new Instruction(0x3F, "CCF", this::ccf));
        opcodes.add(new Instruction(0x40, "LD B,B", () -> ld_r_r(b, b)));
        opcodes.add(new Instruction(0x41, "LD B,C", () -> ld_r_r(b, c)));
        opcodes.add(new Instruction(0x42, "LD B,D", () -> ld_r_r(b, d)));
        opcodes.add(new Instruction(0x43, "LD B,E", () -> ld_r_r(b, e)));
        opcodes.add(new Instruction(0x44, "LD B,H", () -> ld_r_r(b, h)));
        opcodes.add(new Instruction(0x45, "LD B,L", () -> ld_r_r(b, l)));
        opcodes.add(new Instruction(0x46, "LD B,(HL)", () -> ld_r_HL(b)));
        opcodes.add(new Instruction(0x47, "LD B,A", () -> ld_r_r(b, a)));
        opcodes.add(new Instruction(0x48, "LD C,B", () -> ld_r_r(c, b)));
        opcodes.add(new Instruction(0x49, "LD C,C", () -> ld_r_r(c, c)));
        opcodes.add(new Instruction(0x4A, "LD C,D", () -> ld_r_r(c, d)));
        opcodes.add(new Instruction(0x4B, "LD C,E", () -> ld_r_r(c, e)));
        opcodes.add(new Instruction(0x4C, "LD C,H", () -> ld_r_r(c, h)));
        opcodes.add(new Instruction(0x4D, "LD C,L", () -> ld_r_r(c, l)));
        opcodes.add(new Instruction(0x4E, "LD C,(HL)", () -> ld_r_HL(c)));
        opcodes.add(new Instruction(0x4F, "LD C,A", () -> ld_r_r(c, a)));
        opcodes.add(new Instruction(0x50, "LD D,B", () -> ld_r_r(d, b)));
        opcodes.add(new Instruction(0x51, "LD D,C", () -> ld_r_r(d, c)));
        opcodes.add(new Instruction(0x52, "LD D,D", () -> ld_r_r(d, d)));
        opcodes.add(new Instruction(0x53, "LD D,E", () -> ld_r_r(d, e)));
        opcodes.add(new Instruction(0x54, "LD D,H", () -> ld_r_r(d, h)));
        opcodes.add(new Instruction(0x55, "LD D,L", () -> ld_r_r(d, l)));
        opcodes.add(new Instruction(0x56, "LD D,(HL)", () -> ld_r_HL(d)));
        opcodes.add(new Instruction(0x57, "LD D,A", () -> ld_r_r(d, a)));
        opcodes.add(new Instruction(0x58, "LD E,B", () -> ld_r_r(e, b)));
        opcodes.add(new Instruction(0x59, "LD E,C", () -> ld_r_r(e, c)));
        opcodes.add(new Instruction(0x5A, "LD E,D", () -> ld_r_r(e, d)));
        opcodes.add(new Instruction(0x5B, "LD E,E", () -> ld_r_r(e, e)));
        opcodes.add(new Instruction(0x5C, "LD E,H", () -> ld_r_r(e, h)));
        opcodes.add(new Instruction(0x5D, "LD E,L", () -> ld_r_r(e, l)));
        opcodes.add(new Instruction(0x5E, "LD E,(HL)", () -> ld_r_HL(e)));
        opcodes.add(new Instruction(0x5F, "LD E,A", () -> ld_r_r(e, a)));
        opcodes.add(new Instruction(0x60, "LD H,B", () -> ld_r_r(h, b)));
        opcodes.add(new Instruction(0x61, "LD H,C", () -> ld_r_r(h, c)));
        opcodes.add(new Instruction(0x62, "LD H,D", () -> ld_r_r(h, d)));
        opcodes.add(new Instruction(0x63, "LD H,E", () -> ld_r_r(h, e)));
        opcodes.add(new Instruction(0x64, "LD H,H", () -> ld_r_r(h, h)));
        opcodes.add(new Instruction(0x65, "LD H,L", () -> ld_r_r(h, l)));
        opcodes.add(new Instruction(0x66, "LD H,(HL)", () -> ld_r_HL(h)));
        opcodes.add(new Instruction(0x67, "LD H,A", () -> ld_r_r(h, a)));
        opcodes.add(new Instruction(0x68, "LD L,B", () -> ld_r_r(l, b)));
        opcodes.add(new Instruction(0x69, "LD L,C", () -> ld_r_r(l, c)));
        opcodes.add(new Instruction(0x6A, "LD L,D", () -> ld_r_r(l, d)));
        opcodes.add(new Instruction(0x6B, "LD L,E", () -> ld_r_r(l, e)));
        opcodes.add(new Instruction(0x6C, "LD L,H", () -> ld_r_r(l, h)));
        opcodes.add(new Instruction(0x6D, "LD L,L", () -> ld_r_r(l, l)));
        opcodes.add(new Instruction(0x6E, "LD L,(HL)", () -> ld_r_HL(l)));
        opcodes.add(new Instruction(0x6F, "LD L,A", () -> ld_r_r(l, a)));
        opcodes.add(new Instruction(0x70, "LD (HL),B", () -> ld_HL_r(b)));
        opcodes.add(new Instruction(0x71, "LD (HL),C", () -> ld_HL_r(c)));
        opcodes.add(new Instruction(0x72, "LD (HL),D", () -> ld_HL_r(d)));
        opcodes.add(new Instruction(0x73, "LD (HL),E", () -> ld_HL_r(e)));
        opcodes.add(new Instruction(0x74, "LD (HL),H", () -> ld_HL_r(h)));
        opcodes.add(new Instruction(0x75, "LD (HL),L", () -> ld_HL_r(l)));
        opcodes.add(new Instruction(0x76, "HALT", this::halt));
        opcodes.add(new Instruction(0x77, "LD (HL),A", () -> ld_HL_r(a)));
        opcodes.add(new Instruction(0x78, "LD A,B", () -> ld_r_r(a, b)));
        opcodes.add(new Instruction(0x79, "LD A,C", () -> ld_r_r(a, c)));
        opcodes.add(new Instruction(0x7A, "LD A,D", () -> ld_r_r(a, d)));
        opcodes.add(new Instruction(0x7B, "LD A,E", () -> ld_r_r(a, e)));
        opcodes.add(new Instruction(0x7C, "LD A,H", () -> ld_r_r(a, h)));
        opcodes.add(new Instruction(0x7D, "LD A,L", () -> ld_r_r(a, l)));
        opcodes.add(new Instruction(0x7E, "LD A,(HL)", () -> ld_r_HL(a)));
        opcodes.add(new Instruction(0x7F, "LD A,A", () -> ld_r_r(a, a)));
        opcodes.add(new Instruction(0x80, "ADD A,B", () -> add_A_r(b)));
        opcodes.add(new Instruction(0x81, "ADD A,C", () -> add_A_r(c)));
        opcodes.add(new Instruction(0x82, "ADD A,D", () -> add_A_r(d)));
        opcodes.add(new Instruction(0x83, "ADD A,E", () -> add_A_r(e)));
        opcodes.add(new Instruction(0x84, "ADD A,H", () -> add_A_r(h)));
        opcodes.add(new Instruction(0x85, "ADD A,L", () -> add_A_r(l)));
        opcodes.add(new Instruction(0x86, "ADD A,(HL)", this::add_A_HL));
        opcodes.add(new Instruction(0x87, "ADD A,A", () -> add_A_r(b)));
        opcodes.add(new Instruction(0x88, "ADC A,B", () -> adc_A_r(b)));
        opcodes.add(new Instruction(0x89, "ADC A,C", () -> adc_A_r(c)));
        opcodes.add(new Instruction(0x8A, "ADC A,D", () -> adc_A_r(d)));
        opcodes.add(new Instruction(0x8B, "ADC A,E", () -> adc_A_r(e)));
        opcodes.add(new Instruction(0x8C, "ADC A,H", () -> adc_A_r(h)));
        opcodes.add(new Instruction(0x8D, "ADC A,L", () -> adc_A_r(l)));
        opcodes.add(new Instruction(0x8E, "ADC A,(HL)", this::adc_A_HL));
        opcodes.add(new Instruction(0x8F, "ADC A,A", () -> adc_A_r(a)));
        opcodes.add(new Instruction(0x90, "SUB B", () -> sub_r(b)));
        opcodes.add(new Instruction(0x91, "SUB C", () -> sub_r(c)));
        opcodes.add(new Instruction(0x92, "SUB D", () -> sub_r(d)));
        opcodes.add(new Instruction(0x93, "SUB E", () -> sub_r(e)));
        opcodes.add(new Instruction(0x94, "SUB H", () -> sub_r(h)));
        opcodes.add(new Instruction(0x95, "SUB L", () -> sub_r(l)));
        opcodes.add(new Instruction(0x96, "SUB (HL)", this::sub_HL));
        opcodes.add(new Instruction(0x97, "SUB A", () -> sub_r(a)));
        opcodes.add(new Instruction(0x98, "SBC A,B", () -> sbc_A_r(b)));
        opcodes.add(new Instruction(0x99, "SBC A,C", () -> sbc_A_r(c)));
        opcodes.add(new Instruction(0x9A, "SBC A,D", () -> sbc_A_r(d)));
        opcodes.add(new Instruction(0x9B, "SBC A,E", () -> sbc_A_r(e)));
        opcodes.add(new Instruction(0x9C, "SBC A,H", () -> sbc_A_r(h)));
        opcodes.add(new Instruction(0x9D, "SBC A,L", () -> sbc_A_r(l)));
        opcodes.add(new Instruction(0x9E, "SBC A,(HL)", this::sbc_A_HL));
        opcodes.add(new Instruction(0x9F, "SBC A,A", () -> sbc_A_r(a)));
        opcodes.add(new Instruction(0xA0, "AND B", () -> and_r(b)));
        opcodes.add(new Instruction(0xA1, "AND C", () -> and_r(c)));
        opcodes.add(new Instruction(0xA2, "AND D", () -> and_r(d)));
        opcodes.add(new Instruction(0xA3, "AND E", () -> and_r(e)));
        opcodes.add(new Instruction(0xA4, "AND H", () -> and_r(h)));
        opcodes.add(new Instruction(0xA5, "AND L", () -> and_r(l)));
        opcodes.add(new Instruction(0xA6, "AND (HL)", this::and_hl));
        opcodes.add(new Instruction(0xA7, "AND A", () -> and_r(a)));
        opcodes.add(new Instruction(0xA8, "XOR B", () -> xor_r(b)));
        opcodes.add(new Instruction(0xA9, "XOR C", () -> xor_r(c)));
        opcodes.add(new Instruction(0xAA, "XOR D", () -> xor_r(d)));
        opcodes.add(new Instruction(0xAB, "XOR E", () -> xor_r(e)));
        opcodes.add(new Instruction(0xAC, "XOR H", () -> xor_r(h)));
        opcodes.add(new Instruction(0xAD, "XOR L", () -> xor_r(l)));
        opcodes.add(new Instruction(0xAE, "XOR (HL)", this::xor_hl));
        opcodes.add(new Instruction(0xAF, "XOR A", () -> xor_r(a)));
        opcodes.add(new Instruction(0xB0, "OR B", () -> or_r(b)));
        opcodes.add(new Instruction(0xB1, "OR C", () -> or_r(c)));
        opcodes.add(new Instruction(0xB2, "OR D", () -> or_r(d)));
        opcodes.add(new Instruction(0xB3, "OR E", () -> or_r(e)));
        opcodes.add(new Instruction(0xB4, "OR H", () -> or_r(h)));
        opcodes.add(new Instruction(0xB5, "OR L", () -> or_r(l)));
        opcodes.add(new Instruction(0xB6, "OR (HL)", this::or_hl));
        opcodes.add(new Instruction(0xB7, "OR A", () -> or_r(a)));
        opcodes.add(new Instruction(0xB8, "CP B", () -> cp_r(b)));
        opcodes.add(new Instruction(0xB9, "CP C", () -> cp_r(c)));
        opcodes.add(new Instruction(0xBA, "CP D", () -> cp_r(d)));
        opcodes.add(new Instruction(0xBB, "CP E", () -> cp_r(e)));
        opcodes.add(new Instruction(0xBC, "CP H", () -> cp_r(h)));
        opcodes.add(new Instruction(0xBD, "CP L", () -> cp_r(l)));
        opcodes.add(new Instruction(0xBE, "CP (HL)", this::cp_HL));
        opcodes.add(new Instruction(0xBF, "CP A", () -> cp_r(a)));
        opcodes.add(new Instruction(0xC0, "RET NZ", this::ret_nz));
        opcodes.add(new Instruction(0xC1, "POP BC", () -> pop_rr(bc)));
        opcodes.add(new Instruction(0xC2, "JP NZ,a16", this::jp_nz_nn));
        opcodes.add(new Instruction(0xC3, "JP a16", this::jp_nn));
        opcodes.add(new Instruction(0xC4, "CALL NZ,a16", this::call_nz_nn));
        opcodes.add(new Instruction(0xC5, "PUSH BC", () -> push_rr(bc)));
        opcodes.add(new Instruction(0xC6, "ADD A,d8", this::add_A_n));
        opcodes.add(new Instruction(0xC7, "RST 00H", this::rst_n));
        opcodes.add(new Instruction(0xC8, "RET Z", this::ret_z));
        opcodes.add(new Instruction(0xC9, "RET", this::ret));
        opcodes.add(new Instruction(0xCA, "JP Z,a16", this::jp_z_nn));
        opcodes.add(new Instruction(0xCB, "PREFIX CB", this::prefix));
        opcodes.add(new Instruction(0xCC, "CALL Z,a16", this::call_z_nn));
        opcodes.add(new Instruction(0xCD, "CALL a16", this::call_nn));
        opcodes.add(new Instruction(0xCE, "ADC A,d8", this::adc_A_n));
        opcodes.add(new Instruction(0xCF, "RST 08H", this::rst_n));
        opcodes.add(new Instruction(0xD0, "RET NC", this::ret_nc));
        opcodes.add(new Instruction(0xD1, "POP DE", () -> pop_rr(de)));
        opcodes.add(new Instruction(0xD2, "JP NC,a16", this::jp_nc_nn));
        opcodes.add(new Instruction(0xD3, "", this::nop));
        opcodes.add(new Instruction(0xD4, "CALL NC,a16", this::call_nc_nn));
        opcodes.add(new Instruction(0xD5, "PUSH DE", () -> push_rr(de)));
        opcodes.add(new Instruction(0xD6, "SUB d8", this::sub_n));
        opcodes.add(new Instruction(0xD7, "RST 10H", this::rst_n));
        opcodes.add(new Instruction(0xD8, "RET C", this::ret_c));
        opcodes.add(new Instruction(0xD9, "RETI", this::reti));
        opcodes.add(new Instruction(0xDA, "JP C,a16", this::jp_c_nn));
        opcodes.add(new Instruction(0xDB, "", this::nop));
        opcodes.add(new Instruction(0xDC, "CALL C,a16", this::call_c_nn));
        opcodes.add(new Instruction(0xDD, "", this::nop));
        opcodes.add(new Instruction(0xDE, "SBC A,d8", this::sbc_A_n));
        opcodes.add(new Instruction(0xDF, "RST 18H", this::rst_n));
        opcodes.add(new Instruction(0xE0, "LDH (a8),A", this::ldh_n_A));
        opcodes.add(new Instruction(0xE1, "POP HL", () -> pop_rr(hl)));
        opcodes.add(new Instruction(0xE2, "LD (C),A", this::ld_C_A));
        opcodes.add(new Instruction(0xE3, "", this::nop));
        opcodes.add(new Instruction(0xE4, "", this::nop));
        opcodes.add(new Instruction(0xE5, "PUSH HL", () -> push_rr(hl)));
        opcodes.add(new Instruction(0xE6, "AND d8", this::and_n));
        opcodes.add(new Instruction(0xE7, "RST 20H", this::rst_n));
        opcodes.add(new Instruction(0xE8, "ADD SP,r8", this::add_SP_n));
        opcodes.add(new Instruction(0xE9, "JP (HL)", this::jp_HL));
        opcodes.add(new Instruction(0xEA, "LD (a16),A", this::ld_nn_A));
        opcodes.add(new Instruction(0xEB, "", this::nop));
        opcodes.add(new Instruction(0xEC, "", this::nop));
        opcodes.add(new Instruction(0xED, "", this::nop));
        opcodes.add(new Instruction(0xEE, "XOR d8", this::xor_n));
        opcodes.add(new Instruction(0xEF, "RST 28H", this::rst_n));
        opcodes.add(new Instruction(0xF0, "LDH A,(a8)", this::ldh_A_n));
        opcodes.add(new Instruction(0xF1, "POP AF", () -> pop_rr(af)));
        opcodes.add(new Instruction(0xF2, "LD A,(C)", this::ld_A_C));
        opcodes.add(new Instruction(0xF3, "DI", this::di));
        opcodes.add(new Instruction(0xF4, "", this::nop));
        opcodes.add(new Instruction(0xF5, "PUSH AF", () -> push_rr(af)));
        opcodes.add(new Instruction(0xF6, "OR d8", this::or_n));
        opcodes.add(new Instruction(0xF7, "RST 30H", this::rst_n));
        opcodes.add(new Instruction(0xF8, "LD HL,SP+r8", this::ldhl_SP_n));
        opcodes.add(new Instruction(0xF9, "LD SP,HL", this::ld_SP_HL));
        opcodes.add(new Instruction(0xFA, "LD A,(a16)", this::ld_A_nn));
        opcodes.add(new Instruction(0xFB, "EI", this::ei));
        opcodes.add(new Instruction(0xFC, "", this::nop));
        opcodes.add(new Instruction(0xFD, "", this::nop));
        opcodes.add(new Instruction(0xFE, "CP d8", this::cp_n));
        opcodes.add(new Instruction(0xFF, "RST 38H", this::rst_n));

        cb_opcodes = new ArrayList<>();
        cb_opcodes.add(new Instruction(0x00, "RLC B", () -> rlc_r(b)));
        cb_opcodes.add(new Instruction(0x01, "RLC C", () -> rlc_r(c)));
        cb_opcodes.add(new Instruction(0x02, "RLC D", () -> rlc_r(d)));
        cb_opcodes.add(new Instruction(0x03, "RLC E", () -> rlc_r(e)));
        cb_opcodes.add(new Instruction(0x04, "RLC H", () -> rlc_r(h)));
        cb_opcodes.add(new Instruction(0x05, "RLC L", () -> rlc_r(l)));
        cb_opcodes.add(new Instruction(0x06, "RLC (HL)", this::rlc_HL));
        cb_opcodes.add(new Instruction(0x07, "RLC A", () -> rlc_r(a)));
        cb_opcodes.add(new Instruction(0x08, "RRC B", () -> rrc_r(b)));
        cb_opcodes.add(new Instruction(0x09, "RRC C", () -> rrc_r(c)));
        cb_opcodes.add(new Instruction(0x0A, "RRC D", () -> rrc_r(d)));
        cb_opcodes.add(new Instruction(0x0B, "RRC E", () -> rrc_r(e)));
        cb_opcodes.add(new Instruction(0x0C, "RRC H", () -> rrc_r(h)));
        cb_opcodes.add(new Instruction(0x0D, "RRC L", () -> rrc_r(l)));
        cb_opcodes.add(new Instruction(0x0E, "RRC (HL)", this::rrc_HL));
        cb_opcodes.add(new Instruction(0x0F, "RRC A", () -> rrc_r(a)));
        cb_opcodes.add(new Instruction(0x10, "RL B", () -> rl_r(b)));
        cb_opcodes.add(new Instruction(0x11, "RL C", () -> rl_r(c)));
        cb_opcodes.add(new Instruction(0x12, "RL D", () -> rl_r(d)));
        cb_opcodes.add(new Instruction(0x13, "RL E", () -> rl_r(e)));
        cb_opcodes.add(new Instruction(0x14, "RL H", () -> rl_r(h)));
        cb_opcodes.add(new Instruction(0x15, "RL L", () -> rl_r(l)));
        cb_opcodes.add(new Instruction(0x16, "RL (HL)", this::rl_HL));
        cb_opcodes.add(new Instruction(0x17, "RL A", () -> rl_r(a)));
        cb_opcodes.add(new Instruction(0x18, "RR B", () -> rr_r(b)));
        cb_opcodes.add(new Instruction(0x19, "RR C", () -> rr_r(c)));
        cb_opcodes.add(new Instruction(0x1A, "RR D", () -> rr_r(d)));
        cb_opcodes.add(new Instruction(0x1B, "RR E", () -> rr_r(e)));
        cb_opcodes.add(new Instruction(0x1C, "RR H", () -> rr_r(h)));
        cb_opcodes.add(new Instruction(0x1D, "RR L", () -> rr_r(l)));
        cb_opcodes.add(new Instruction(0x1E, "RR (HL)", this::rr_HL));
        cb_opcodes.add(new Instruction(0x1F, "RR A", () -> rr_r(a)));
        cb_opcodes.add(new Instruction(0x20, "SLA B", () -> sla_r(b)));
        cb_opcodes.add(new Instruction(0x21, "SLA C", () -> sla_r(c)));
        cb_opcodes.add(new Instruction(0x22, "SLA D", () -> sla_r(d)));
        cb_opcodes.add(new Instruction(0x23, "SLA E", () -> sla_r(e)));
        cb_opcodes.add(new Instruction(0x24, "SLA H", () -> sla_r(h)));
        cb_opcodes.add(new Instruction(0x25, "SLA L", () -> sla_r(l)));
        cb_opcodes.add(new Instruction(0x26, "SLA (HL)", this::sla_HL));
        cb_opcodes.add(new Instruction(0x27, "SLA A", () -> sla_r(a)));
        cb_opcodes.add(new Instruction(0x28, "SRA B", () -> sra_r(b)));
        cb_opcodes.add(new Instruction(0x29, "SRA C", () -> sra_r(c)));
        cb_opcodes.add(new Instruction(0x2A, "SRA D", () -> sra_r(d)));
        cb_opcodes.add(new Instruction(0x2B, "SRA E", () -> sra_r(e)));
        cb_opcodes.add(new Instruction(0x2C, "SRA H", () -> sra_r(h)));
        cb_opcodes.add(new Instruction(0x2D, "SRA L", () -> sra_r(l)));
        cb_opcodes.add(new Instruction(0x2E, "SRA (HL)", this::sra_HL));
        cb_opcodes.add(new Instruction(0x2F, "SRA A", () -> sra_r(a)));
        cb_opcodes.add(new Instruction(0x30, "SWAP B", () -> swap_r(b)));
        cb_opcodes.add(new Instruction(0x31, "SWAP C", () -> swap_r(c)));
        cb_opcodes.add(new Instruction(0x32, "SWAP D", () -> swap_r(d)));
        cb_opcodes.add(new Instruction(0x33, "SWAP E", () -> swap_r(e)));
        cb_opcodes.add(new Instruction(0x34, "SWAP H", () -> swap_r(h)));
        cb_opcodes.add(new Instruction(0x35, "SWAP L", () -> swap_r(l)));
        cb_opcodes.add(new Instruction(0x36, "SWAP (HL)", this::swap_HL));
        cb_opcodes.add(new Instruction(0x37, "SWAP A", () -> swap_r(a)));
        cb_opcodes.add(new Instruction(0x38, "SRL B", () -> srl_r(b)));
        cb_opcodes.add(new Instruction(0x39, "SRL C", () -> srl_r(c)));
        cb_opcodes.add(new Instruction(0x3A, "SRL D", () -> srl_r(d)));
        cb_opcodes.add(new Instruction(0x3B, "SRL E", () -> srl_r(e)));
        cb_opcodes.add(new Instruction(0x3C, "SRL H", () -> srl_r(h)));
        cb_opcodes.add(new Instruction(0x3D, "SRL L", () -> srl_r(l)));
        cb_opcodes.add(new Instruction(0x3E, "SRL (HL)", this::srl_HL));
        cb_opcodes.add(new Instruction(0x3F, "SRL A", () -> srl_r(a)));
        cb_opcodes.add(new Instruction(0x40, "BIT 0,B", () -> bit_b_r(b)));
        cb_opcodes.add(new Instruction(0x41, "BIT 0,C", () -> bit_b_r(c)));
        cb_opcodes.add(new Instruction(0x42, "BIT 0,D", () -> bit_b_r(d)));
        cb_opcodes.add(new Instruction(0x43, "BIT 0,E", () -> bit_b_r(e)));
        cb_opcodes.add(new Instruction(0x44, "BIT 0,H", () -> bit_b_r(h)));
        cb_opcodes.add(new Instruction(0x45, "BIT 0,L", () -> bit_b_r(l)));
        cb_opcodes.add(new Instruction(0x46, "BIT 0,(HL)", this::bit_b_HL));
        cb_opcodes.add(new Instruction(0x47, "BIT 0,A", () -> bit_b_r(a)));
        cb_opcodes.add(new Instruction(0x48, "BIT 1,B", () -> bit_b_r(b)));
        cb_opcodes.add(new Instruction(0x49, "BIT 1,C", () -> bit_b_r(c)));
        cb_opcodes.add(new Instruction(0x4A, "BIT 1,D", () -> bit_b_r(d)));
        cb_opcodes.add(new Instruction(0x4B, "BIT 1,E", () -> bit_b_r(e)));
        cb_opcodes.add(new Instruction(0x4C, "BIT 1,H", () -> bit_b_r(h)));
        cb_opcodes.add(new Instruction(0x4D, "BIT 1,L", () -> bit_b_r(l)));
        cb_opcodes.add(new Instruction(0x4E, "BIT 1,(HL)", this::bit_b_HL));
        cb_opcodes.add(new Instruction(0x4F, "BIT 1,A", () -> bit_b_r(a)));
        cb_opcodes.add(new Instruction(0x50, "BIT 2,B", () -> bit_b_r(b)));
        cb_opcodes.add(new Instruction(0x51, "BIT 2,C", () -> bit_b_r(c)));
        cb_opcodes.add(new Instruction(0x52, "BIT 2,D", () -> bit_b_r(d)));
        cb_opcodes.add(new Instruction(0x53, "BIT 2,E", () -> bit_b_r(e)));
        cb_opcodes.add(new Instruction(0x54, "BIT 2,H", () -> bit_b_r(h)));
        cb_opcodes.add(new Instruction(0x55, "BIT 2,L", () -> bit_b_r(l)));
        cb_opcodes.add(new Instruction(0x56, "BIT 2,(HL)", this::bit_b_HL));
        cb_opcodes.add(new Instruction(0x57, "BIT 2,A", () -> bit_b_r(a)));
        cb_opcodes.add(new Instruction(0x58, "BIT 3,B", () -> bit_b_r(b)));
        cb_opcodes.add(new Instruction(0x59, "BIT 3,C", () -> bit_b_r(c)));
        cb_opcodes.add(new Instruction(0x5A, "BIT 3,D", () -> bit_b_r(d)));
        cb_opcodes.add(new Instruction(0x5B, "BIT 3,E", () -> bit_b_r(e)));
        cb_opcodes.add(new Instruction(0x5C, "BIT 3,H", () -> bit_b_r(h)));
        cb_opcodes.add(new Instruction(0x5D, "BIT 3,L", () -> bit_b_r(l)));
        cb_opcodes.add(new Instruction(0x5E, "BIT 3,(HL)", this::bit_b_HL));
        cb_opcodes.add(new Instruction(0x5F, "BIT 3,A", () -> bit_b_r(a)));
        cb_opcodes.add(new Instruction(0x60, "BIT 4,B", () -> bit_b_r(b)));
        cb_opcodes.add(new Instruction(0x61, "BIT 4,C", () -> bit_b_r(c)));
        cb_opcodes.add(new Instruction(0x62, "BIT 4,D", () -> bit_b_r(d)));
        cb_opcodes.add(new Instruction(0x63, "BIT 4,E", () -> bit_b_r(e)));
        cb_opcodes.add(new Instruction(0x64, "BIT 4,H", () -> bit_b_r(h)));
        cb_opcodes.add(new Instruction(0x65, "BIT 4,L", () -> bit_b_r(l)));
        cb_opcodes.add(new Instruction(0x66, "BIT 4,(HL)", this::bit_b_HL));
        cb_opcodes.add(new Instruction(0x67, "BIT 4,A", () -> bit_b_r(a)));
        cb_opcodes.add(new Instruction(0x68, "BIT 5,B", () -> bit_b_r(b)));
        cb_opcodes.add(new Instruction(0x69, "BIT 5,C", () -> bit_b_r(c)));
        cb_opcodes.add(new Instruction(0x6A, "BIT 5,D", () -> bit_b_r(d)));
        cb_opcodes.add(new Instruction(0x6B, "BIT 5,E", () -> bit_b_r(e)));
        cb_opcodes.add(new Instruction(0x6C, "BIT 5,H", () -> bit_b_r(h)));
        cb_opcodes.add(new Instruction(0x6D, "BIT 5,L", () -> bit_b_r(l)));
        cb_opcodes.add(new Instruction(0x6E, "BIT 5,(HL)", this::bit_b_HL));
        cb_opcodes.add(new Instruction(0x6F, "BIT 5,A", () -> bit_b_r(a)));
        cb_opcodes.add(new Instruction(0x70, "BIT 6,B", () -> bit_b_r(b)));
        cb_opcodes.add(new Instruction(0x71, "BIT 6,C", () -> bit_b_r(c)));
        cb_opcodes.add(new Instruction(0x72, "BIT 6,D", () -> bit_b_r(d)));
        cb_opcodes.add(new Instruction(0x73, "BIT 6,E", () -> bit_b_r(e)));
        cb_opcodes.add(new Instruction(0x74, "BIT 6,H", () -> bit_b_r(h)));
        cb_opcodes.add(new Instruction(0x75, "BIT 6,L", () -> bit_b_r(l)));
        cb_opcodes.add(new Instruction(0x76, "BIT 6,(HL)", this::bit_b_HL));
        cb_opcodes.add(new Instruction(0x77, "BIT 6,A", () -> bit_b_r(a)));
        cb_opcodes.add(new Instruction(0x78, "BIT 7,B", () -> bit_b_r(b)));
        cb_opcodes.add(new Instruction(0x79, "BIT 7,C", () -> bit_b_r(c)));
        cb_opcodes.add(new Instruction(0x7A, "BIT 7,D", () -> bit_b_r(d)));
        cb_opcodes.add(new Instruction(0x7B, "BIT 7,E", () -> bit_b_r(e)));
        cb_opcodes.add(new Instruction(0x7C, "BIT 7,H", () -> bit_b_r(h)));
        cb_opcodes.add(new Instruction(0x7D, "BIT 7,L", () -> bit_b_r(l)));
        cb_opcodes.add(new Instruction(0x7E, "BIT 7,(HL)", this::bit_b_HL));
        cb_opcodes.add(new Instruction(0x7F, "BIT 7,A", () -> bit_b_r(a)));
        cb_opcodes.add(new Instruction(0x80, "RES 0,B", () -> res_b_R(b)));
        cb_opcodes.add(new Instruction(0x81, "RES 0,C", () -> res_b_R(c)));
        cb_opcodes.add(new Instruction(0x82, "RES 0,D", () -> res_b_R(d)));
        cb_opcodes.add(new Instruction(0x83, "RES 0,E", () -> res_b_R(e)));
        cb_opcodes.add(new Instruction(0x84, "RES 0,H", () -> res_b_R(h)));
        cb_opcodes.add(new Instruction(0x85, "RES 0,L", () -> res_b_R(l)));
        cb_opcodes.add(new Instruction(0x86, "RES 0,(HL)", this::res_b_HL));
        cb_opcodes.add(new Instruction(0x87, "RES 0,A", () -> res_b_R(a)));
        cb_opcodes.add(new Instruction(0x88, "RES 1,B", () -> res_b_R(b)));
        cb_opcodes.add(new Instruction(0x89, "RES 1,C", () -> res_b_R(c)));
        cb_opcodes.add(new Instruction(0x8A, "RES 1,D", () -> res_b_R(d)));
        cb_opcodes.add(new Instruction(0x8B, "RES 1,E", () -> res_b_R(e)));
        cb_opcodes.add(new Instruction(0x8C, "RES 1,H", () -> res_b_R(h)));
        cb_opcodes.add(new Instruction(0x8D, "RES 1,L", () -> res_b_R(l)));
        cb_opcodes.add(new Instruction(0x8E, "RES 1,(HL)", this::res_b_HL));
        cb_opcodes.add(new Instruction(0x8F, "RES 1,A", () -> res_b_R(a)));
        cb_opcodes.add(new Instruction(0x90, "RES 2,B", () -> res_b_R(b)));
        cb_opcodes.add(new Instruction(0x91, "RES 2,C", () -> res_b_R(c)));
        cb_opcodes.add(new Instruction(0x92, "RES 2,D", () -> res_b_R(d)));
        cb_opcodes.add(new Instruction(0x93, "RES 2,E", () -> res_b_R(e)));
        cb_opcodes.add(new Instruction(0x94, "RES 2,H", () -> res_b_R(h)));
        cb_opcodes.add(new Instruction(0x95, "RES 2,L", () -> res_b_R(l)));
        cb_opcodes.add(new Instruction(0x96, "RES 2,(HL)", this::res_b_HL));
        cb_opcodes.add(new Instruction(0x97, "RES 2,A", () -> res_b_R(a)));
        cb_opcodes.add(new Instruction(0x98, "RES 3,B", () -> res_b_R(b)));
        cb_opcodes.add(new Instruction(0x99, "RES 3,C", () -> res_b_R(c)));
        cb_opcodes.add(new Instruction(0x9A, "RES 3,D", () -> res_b_R(d)));
        cb_opcodes.add(new Instruction(0x9B, "RES 3,E", () -> res_b_R(e)));
        cb_opcodes.add(new Instruction(0x9C, "RES 3,H", () -> res_b_R(h)));
        cb_opcodes.add(new Instruction(0x9D, "RES 3,L", () -> res_b_R(l)));
        cb_opcodes.add(new Instruction(0x9E, "RES 3,(HL)", this::res_b_HL));
        cb_opcodes.add(new Instruction(0x9F, "RES 3,A", () -> res_b_R(a)));
        cb_opcodes.add(new Instruction(0xA0, "RES 4,B", () -> res_b_R(b)));
        cb_opcodes.add(new Instruction(0xA1, "RES 4,C", () -> res_b_R(c)));
        cb_opcodes.add(new Instruction(0xA2, "RES 4,D", () -> res_b_R(d)));
        cb_opcodes.add(new Instruction(0xA3, "RES 4,E", () -> res_b_R(e)));
        cb_opcodes.add(new Instruction(0xA4, "RES 4,H", () -> res_b_R(h)));
        cb_opcodes.add(new Instruction(0xA5, "RES 4,L", () -> res_b_R(l)));
        cb_opcodes.add(new Instruction(0xA6, "RES 4,(HL)", this::res_b_HL));
        cb_opcodes.add(new Instruction(0xA7, "RES 4,A", () -> res_b_R(a)));
        cb_opcodes.add(new Instruction(0xA8, "RES 5,B", () -> res_b_R(b)));
        cb_opcodes.add(new Instruction(0xA9, "RES 5,C", () -> res_b_R(c)));
        cb_opcodes.add(new Instruction(0xAA, "RES 5,D", () -> res_b_R(d)));
        cb_opcodes.add(new Instruction(0xAB, "RES 5,E", () -> res_b_R(e)));
        cb_opcodes.add(new Instruction(0xAC, "RES 5,H", () -> res_b_R(h)));
        cb_opcodes.add(new Instruction(0xAD, "RES 5,L", () -> res_b_R(l)));
        cb_opcodes.add(new Instruction(0xAE, "RES 5,(HL)", this::res_b_HL));
        cb_opcodes.add(new Instruction(0xAF, "RES 5,A", () -> res_b_R(a)));
        cb_opcodes.add(new Instruction(0xB0, "RES 6,B", () -> res_b_R(b)));
        cb_opcodes.add(new Instruction(0xB1, "RES 6,C", () -> res_b_R(c)));
        cb_opcodes.add(new Instruction(0xB2, "RES 6,D", () -> res_b_R(d)));
        cb_opcodes.add(new Instruction(0xB3, "RES 6,E", () -> res_b_R(e)));
        cb_opcodes.add(new Instruction(0xB4, "RES 6,H", () -> res_b_R(h)));
        cb_opcodes.add(new Instruction(0xB5, "RES 6,L", () -> res_b_R(l)));
        cb_opcodes.add(new Instruction(0xB6, "RES 6,(HL)", this::res_b_HL));
        cb_opcodes.add(new Instruction(0xB7, "RES 6,A", () -> res_b_R(a)));
        cb_opcodes.add(new Instruction(0xB8, "RES 7,B", () -> res_b_R(b)));
        cb_opcodes.add(new Instruction(0xB9, "RES 7,C", () -> res_b_R(c)));
        cb_opcodes.add(new Instruction(0xBA, "RES 7,D", () -> res_b_R(d)));
        cb_opcodes.add(new Instruction(0xBB, "RES 7,E", () -> res_b_R(e)));
        cb_opcodes.add(new Instruction(0xBC, "RES 7,H", () -> res_b_R(h)));
        cb_opcodes.add(new Instruction(0xBD, "RES 7,L", () -> res_b_R(l)));
        cb_opcodes.add(new Instruction(0xBE, "RES 7,(HL)", this::res_b_HL));
        cb_opcodes.add(new Instruction(0xBF, "RES 7,A", () -> res_b_R(a)));
        cb_opcodes.add(new Instruction(0xC0, "SET 0,B", () -> set_b_R(b)));
        cb_opcodes.add(new Instruction(0xC1, "SET 0,C", () -> set_b_R(c)));
        cb_opcodes.add(new Instruction(0xC2, "SET 0,D", () -> set_b_R(d)));
        cb_opcodes.add(new Instruction(0xC3, "SET 0,E", () -> set_b_R(e)));
        cb_opcodes.add(new Instruction(0xC4, "SET 0,H", () -> set_b_R(h)));
        cb_opcodes.add(new Instruction(0xC5, "SET 0,L", () -> set_b_R(l)));
        cb_opcodes.add(new Instruction(0xC6, "SET 0,(HL)", this::set_b_HL));
        cb_opcodes.add(new Instruction(0xC7, "SET 0,A", () -> set_b_R(a)));
        cb_opcodes.add(new Instruction(0xC8, "SET 1,B", () -> set_b_R(b)));
        cb_opcodes.add(new Instruction(0xC9, "SET 1,C", () -> set_b_R(c)));
        cb_opcodes.add(new Instruction(0xCA, "SET 1,D", () -> set_b_R(d)));
        cb_opcodes.add(new Instruction(0xCB, "SET 1,E", () -> set_b_R(e)));
        cb_opcodes.add(new Instruction(0xCC, "SET 1,H", () -> set_b_R(h)));
        cb_opcodes.add(new Instruction(0xCD, "SET 1,L", () -> set_b_R(l)));
        cb_opcodes.add(new Instruction(0xCE, "SET 1,(HL)", this::set_b_HL));
        cb_opcodes.add(new Instruction(0xCF, "SET 1,A", () -> set_b_R(a)));
        cb_opcodes.add(new Instruction(0xD0, "SET 2,B", () -> set_b_R(b)));
        cb_opcodes.add(new Instruction(0xD1, "SET 2,C", () -> set_b_R(c)));
        cb_opcodes.add(new Instruction(0xD2, "SET 2,D", () -> set_b_R(d)));
        cb_opcodes.add(new Instruction(0xD3, "SET 2,E", () -> set_b_R(e)));
        cb_opcodes.add(new Instruction(0xD4, "SET 2,H", () -> set_b_R(h)));
        cb_opcodes.add(new Instruction(0xD5, "SET 2,L", () -> set_b_R(l)));
        cb_opcodes.add(new Instruction(0xD6, "SET 2,(HL)", this::set_b_HL));
        cb_opcodes.add(new Instruction(0xD7, "SET 2,A", () -> set_b_R(a)));
        cb_opcodes.add(new Instruction(0xD8, "SET 3,B", () -> set_b_R(b)));
        cb_opcodes.add(new Instruction(0xD9, "SET 3,C", () -> set_b_R(c)));
        cb_opcodes.add(new Instruction(0xDA, "SET 3,D", () -> set_b_R(d)));
        cb_opcodes.add(new Instruction(0xDB, "SET 3,E", () -> set_b_R(e)));
        cb_opcodes.add(new Instruction(0xDC, "SET 3,H", () -> set_b_R(h)));
        cb_opcodes.add(new Instruction(0xDD, "SET 3,L", () -> set_b_R(l)));
        cb_opcodes.add(new Instruction(0xDE, "SET 3,(HL)", this::set_b_HL));
        cb_opcodes.add(new Instruction(0xDF, "SET 3,A", () -> set_b_R(a)));
        cb_opcodes.add(new Instruction(0xE0, "SET 4,B", () -> set_b_R(b)));
        cb_opcodes.add(new Instruction(0xE1, "SET 4,B", () -> set_b_R(c)));
        cb_opcodes.add(new Instruction(0xE2, "SET 4,D", () -> set_b_R(d)));
        cb_opcodes.add(new Instruction(0xE3, "SET 4,E", () -> set_b_R(e)));
        cb_opcodes.add(new Instruction(0xE4, "SET 4,H", () -> set_b_R(h)));
        cb_opcodes.add(new Instruction(0xE5, "SET 4,L", () -> set_b_R(l)));
        cb_opcodes.add(new Instruction(0xE6, "SET 4,(HL)", this::set_b_HL));
        cb_opcodes.add(new Instruction(0xE7, "SET 4,A", () -> set_b_R(a)));
        cb_opcodes.add(new Instruction(0xE8, "SET 5,B", () -> set_b_R(b)));
        cb_opcodes.add(new Instruction(0xE9, "SET 5,C", () -> set_b_R(c)));
        cb_opcodes.add(new Instruction(0xEA, "SET 5,D", () -> set_b_R(d)));
        cb_opcodes.add(new Instruction(0xEB, "SET 5,E", () -> set_b_R(e)));
        cb_opcodes.add(new Instruction(0xEC, "SET 5,H", () -> set_b_R(h)));
        cb_opcodes.add(new Instruction(0xED, "SET 5,L", () -> set_b_R(l)));
        cb_opcodes.add(new Instruction(0xEE, "SET 5,(HL)", this::set_b_HL));
        cb_opcodes.add(new Instruction(0xEF, "SET 5,A", () -> set_b_R(a)));
        cb_opcodes.add(new Instruction(0xF0, "SET 6,B", () -> set_b_R(b)));
        cb_opcodes.add(new Instruction(0xF1, "SET 6,C", () -> set_b_R(c)));
        cb_opcodes.add(new Instruction(0xF2, "SET 6,D", () -> set_b_R(d)));
        cb_opcodes.add(new Instruction(0xF3, "SET 6,E", () -> set_b_R(e)));
        cb_opcodes.add(new Instruction(0xF4, "SET 6,H", () -> set_b_R(h)));
        cb_opcodes.add(new Instruction(0xF5, "SET 6,L", () -> set_b_R(l)));
        cb_opcodes.add(new Instruction(0xF6, "SET 6,(HL)", this::set_b_HL));
        cb_opcodes.add(new Instruction(0xF7, "SET 6,A", () -> set_b_R(a)));
        cb_opcodes.add(new Instruction(0xF8, "SET 7,B", () -> set_b_R(b)));
        cb_opcodes.add(new Instruction(0xF9, "SET 7,C", () -> set_b_R(c)));
        cb_opcodes.add(new Instruction(0xFA, "SET 7,D", () -> set_b_R(d)));
        cb_opcodes.add(new Instruction(0xFB, "SET 7,E", () -> set_b_R(e)));
        cb_opcodes.add(new Instruction(0xFC, "SET 7,H", () -> set_b_R(h)));
        cb_opcodes.add(new Instruction(0xFD, "SET 7,L", () -> set_b_R(l)));
        cb_opcodes.add(new Instruction(0xFE, "SET 7,(HL)", this::set_b_HL));
        cb_opcodes.add(new Instruction(0xFF, "SET 7,A", () -> set_b_R(a)));
        File fout = new File("fout.txt");
        try {
            FileOutputStream fos = new FileOutputStream(fout);
            bw = new BufferedWriter(new OutputStreamWriter(fos));
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    public void clock() {
        if (halted) {
            cycle++;
            return;
        }

        if (remaining_cycle_until_op == 0) {
            int opcode = read8(pc.read());
            System.out.println(Integer.toHexString(pc.read()));
            pc.inc();
            Instruction inst = opcodes.get(opcode);
            remaining_cycle_until_op = inst.operate()/4;
            if (enable_interrupt_in_opcode > 0) {
                enable_interrupt_in_opcode--;
            } else if (enable_interrupt_in_opcode == 0){
                enableIRQ = true;
                enable_interrupt_in_opcode = -1;
            }

            if (disable_interrupt_in_opcode > 0) {
                disable_interrupt_in_opcode--;
            } else if (disable_interrupt_in_opcode == 0) {
                enableIRQ = false;
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
        memory.writeByteRdOnly(addr & 0xFFFF, data & 0xFF);
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

    public int ld_r_n(Register8 reg) {
        reg.write(read8(pc.read()));
        pc.inc();
        return 8;
    }

    public int ld_rr_nn(Register16 reg) {
        reg.write(read16(pc.read()));
        pc.inc();
        pc.inc();
        return 12;
    }

    public int ld_r_r(Register8 reg, Register8 reg1) {
        reg.write(reg1.read());
        return 4;
    }

    public int ld_r_HL(Register8 reg) {
        reg.write(read8(hl.read()));
        return 8;
    }

    public int ld_HL_r(Register8 reg) {
        write8(hl.read(), reg.read());
        return 8;
    }

    public int ld_HL_n() {
        write8(hl.read(), read8(pc.read()));
        pc.inc();
        return 12;
    }

    public int ld_A_rr(Register16 reg) {
        a.write(read8(reg.read()));
        return 8;
    }

    public int ld_A_nn() {
        a.write(read8(read16(pc.read())));
        pc.inc();
        pc.inc();
        return 8;
    }

    public int ld_rr_A(Register16 reg) {
        write8(reg.read(), a.read());
        return 8;
    }

    public int ld_nn_A() {
        write8(read16(pc.read()), a.read());
        pc.inc();
        pc.inc();
        return 16;
    }

    public int ld_A_C() {
        a.write(read8(0xFF00 | c.read()));
        return 8;
    }

    public int ld_C_A() {
        write8(0xFF00 | c.read(), a.read());
        return 8;
    }

    public int ldd_A_HLD() {
        a.write(read8(hl.read()));
        hl.dec();
        return 8;
    }

    public int ldd_HLD_A() {
        write8(hl.read(), a.read());
        hl.dec();
        return 8;
    }

    public int ldd_A_HLI() {
        a.write(read8(hl.read()));
        hl.inc();
        return 8;
    }

    public int ldd_HLI_A() {
        write8(hl.read(), a.read());
        hl.inc();
        return 8;
    }

    public int ldh_n_A() {
        write8(0xFF00 | read8(pc.read()), a.read());
        pc.inc();
        return 12;
    }

    public int ldh_A_n() {
        a.write(read8(0xFF00 | read8(pc.read())));
        pc.inc();
        return 12;
    }

    public int ld_SP_HL() {
        sp.write(hl.read());
        return 8;
    }

    public int ldhl_SP_n() {
        hl.write(sp.read() + signed8(read8(pc.read())));
        pc.inc();
        return 12;
    }

    public int ld_nn_SP() {
        write16(read16(pc.read()), sp.read());
        pc.inc();
        pc.inc();
        return 20;
    }

    public int push_rr(Register16 reg) {
        pushStack(reg.read());
        return 16;
    }

    public int pop_rr(Register16 reg) {
        reg.write(popStack());
        return 12;
    }

    public int add_A_r(Register8 reg) {
        int val = (a.read() + reg.read());

        setFlag(Flags.ZERO, (val & 0xFF) == 0x00);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, (((a.read() & 0xF) + (reg.read() & 0xF)) & 0x10) == 0x10);
        setFlag(Flags.CARRY, (val & 0x100) == 0x100);

        a.write(val & 0xFF);
        return 4;
    }

    public int add_A_n() {
        int read = read8(pc.read());
        int val = (a.read() + read);

        setFlag(Flags.ZERO, (val & 0xFF) == 0x00);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, (((a.read() & 0xF) + (read & 0xF)) & 0x10) == 0x10);
        setFlag(Flags.CARRY, (val & 0x100) == 0x100);

        a.write(val & 0xFF);
        pc.inc();
        return 8;
    }

    public int add_A_HL() {
        int read = read8(hl.read());
        int val = (a.read() + read);

        setFlag(Flags.ZERO, (val & 0xFF) == 0x00);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, (((a.read() & 0xF) + (read & 0xF)) & 0x10) == 0x10);
        setFlag(Flags.CARRY, (val & 0x100) == 0x100);

        a.write(val & 0xFF);
        return 8;
    }

    public int adc_A_r(Register8 reg) {
        int val = (a.read() + reg.read()) + (hasFlag(Flags.CARRY) ? 1 : 0);

        setFlag(Flags.ZERO, (val & 0xFF) == 0x00);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, (((a.read() & 0xF) + (reg.read() & 0xF) + (hasFlag(Flags.CARRY) ? 1 : 0)) & 0x10) == 0x10);
        setFlag(Flags.CARRY, (val & 0x100) == 0x100);

        a.write(val & 0xFF);
        return 4;
    }

    public int adc_A_n() {
        int read = read8(pc.read());
        int val = (a.read() + read) + (hasFlag(Flags.CARRY) ? 1 : 0);

        setFlag(Flags.ZERO, (val & 0xFF) == 0x00);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, (((a.read() & 0xF) + (read & 0xF) + (hasFlag(Flags.CARRY) ? 1 : 0)) & 0x10) == 0x10);
        setFlag(Flags.CARRY, (val & 0x100) == 0x100);

        a.write(val & 0xFF);
        pc.inc();
        return 8;
    }

    public int adc_A_HL() {
        int read = read8(hl.read());
        int val = (a.read() + read) + (hasFlag(Flags.CARRY) ? 1 : 0);

        setFlag(Flags.ZERO, (val & 0xFF) == 0x00);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, (((a.read() & 0xF) + (read & 0xF) + (hasFlag(Flags.CARRY) ? 1 : 0)) & 0x10) == 0x10);
        setFlag(Flags.CARRY, (val & 0x100) == 0x100);

        a.write(val & 0xFF);
        return 8;
    }

    public int sub_r(Register8 reg) {
        int val = (a.read() - reg.read());

        setFlag(Flags.ZERO, (val & 0xFF) == 0x00);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, (a.read() & 0xF) < (reg.read() & 0xF));
        setFlag(Flags.CARRY, a.read() < reg.read());

        a.write(val & 0xFF);
        return 4;
    }

    public int sub_n() {
        int read = read8(pc.read());
        int val = (a.read() - read);

        setFlag(Flags.ZERO, (val & 0xFF) == 0x00);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, (a.read() & 0xF) < (read & 0xF));
        setFlag(Flags.CARRY, a.read() < read);

        a.write(val & 0xFF);
        pc.inc();
        return 8;
    }

    public int sub_HL() {
        int read = read8(hl.read());
        int val = (a.read() - read);

        setFlag(Flags.ZERO, (val & 0xFF) == 0x00);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, (a.read() & 0xF) < (read & 0xF));
        setFlag(Flags.CARRY, a.read() < read);

        a.write(val & 0xFF);
        return 8;
    }

    public int sbc_A_r(Register8 reg) {
        int val = (a.read() - (reg.read() + (hasFlag(Flags.CARRY) ? 1 : 0)));

        setFlag(Flags.ZERO, (val & 0xFF) == 0x00);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, (a.read() & 0xF) < ((reg.read() & 0xF) + (hasFlag(Flags.CARRY) ? 1 : 0)));
        setFlag(Flags.CARRY, a.read() < reg.read());

        a.write(val & 0xFF);
        return 4;
    }

    public int sbc_A_n() {
        int read = read8(pc.read());
        int val = (a.read() - (read + (hasFlag(Flags.CARRY) ? 1 : 0)));

        setFlag(Flags.ZERO, (val & 0xFF) == 0x00);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, (a.read() & 0xF) < ((read & 0xF) + (hasFlag(Flags.CARRY) ? 1 : 0)));
        setFlag(Flags.CARRY, a.read() < read);

        a.write(val & 0xFF);
        pc.inc();
        return 8;
    }

    public int sbc_A_HL() {
        int read = read8(hl.read());
        int val = (a.read() - (read + (hasFlag(Flags.CARRY) ? 1 : 0)));

        setFlag(Flags.ZERO, (val & 0xFF) == 0x00);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, (a.read() & 0xF) < ((read & 0xF) + (hasFlag(Flags.CARRY) ? 1 : 0)));
        setFlag(Flags.CARRY, a.read() < read);

        a.write(val & 0xFF);
        return 8;
    }

    public int and_r(Register8 reg) {
        int val = (a.read() & reg.read());

        setFlag(Flags.ZERO, (val & 0xFF) == 0x00);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, true);
        setFlag(Flags.CARRY, false);

        a.write(val & 0xFF);
        return 4;
    }

    public int and_hl() {
        int val = (a.read() & read8(pc.read()));

        setFlag(Flags.ZERO, (val & 0xFF) == 0x00);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, true);
        setFlag(Flags.CARRY, false);

        a.write(val & 0xFF);
        pc.inc();
        return 8;
    }

    public int and_n() {
        int val = (a.read() & read8(pc.read()));
        pc.inc();

        setFlag(Flags.ZERO, (val & 0xFF) == 0x00);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, true);
        setFlag(Flags.CARRY, false);

        a.write(val & 0xFF);
        return 8;
    }

    public int or_r(Register8 reg) {
        int val = (a.read() | reg.read());

        setFlag(Flags.ZERO, (val & 0xFF) == 0x00);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, false);
        setFlag(Flags.CARRY, false);

        a.write(val & 0xFF);
        return 4;
    }

    public int or_hl() {
        int val = (a.read() | read8(pc.read()));

        setFlag(Flags.ZERO, (val & 0xFF) == 0x00);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, false);
        setFlag(Flags.CARRY, false);

        a.write(val & 0xFF);
        pc.inc();
        return 8;
    }

    public int or_n() {
        int val = (a.read() | read8(hl.read()));

        setFlag(Flags.ZERO, (val & 0xFF) == 0x00);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, false);
        setFlag(Flags.CARRY, false);

        a.write(val & 0xFF);
        return 8;
    }

    public int xor_r(Register8 reg) {
        int val = (a.read() ^ reg.read());

        setFlag(Flags.ZERO, (val & 0xFF) == 0x00);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, false);
        setFlag(Flags.CARRY, false);

        a.write(val & 0xFF);
        return 4;
    }

    public int xor_hl() {
        int val = (a.read() ^ read8(pc.read()));

        setFlag(Flags.ZERO, (val & 0xFF) == 0x00);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, false);
        setFlag(Flags.CARRY, false);

        a.write(val & 0xFF);
        pc.inc();
        return 8;
    }

    public int xor_n() {
        int val = (a.read() ^ read8(hl.read()));

        setFlag(Flags.ZERO, (val & 0xFF) == 0x00);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, false);
        setFlag(Flags.CARRY, false);

        a.write(val & 0xFF);
        return 8;
    }

    public int cp_r(Register8 reg) {
        int val = (a.read() - reg.read());

        setFlag(Flags.ZERO, (val & 0xFF) == 0x00);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, (a.read() & 0xF) < (reg.read() & 0xF));
        setFlag(Flags.CARRY, a.read() < reg.read());

        return 4;
    }

    public int cp_n() {
        int read = read8(pc.read());
        int val = (a.read() - read);

        setFlag(Flags.ZERO, (val & 0xFF) == 0x00);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, (a.read() & 0xF) < (read & 0xF));
        setFlag(Flags.CARRY, a.read() < read);

        pc.inc();
        return 8;
    }

    public int cp_HL() {
        int read = read8(hl.read());
        int val = (a.read() - read);

        setFlag(Flags.ZERO, (val & 0xFF) == 0x00);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, (a.read() & 0xF) < (read & 0xF));
        setFlag(Flags.CARRY, a.read() < read);

        return 8;
    }

    public int inc_r(Register8 reg) {
        setFlag(Flags.ZERO, reg.read() == 0xFF);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, (reg.read() & 0xF) == 0xF);

        reg.inc();
        return 4;
    }

    public int inc_HL() {
        int val = read8(hl.read());
        setFlag(Flags.ZERO, val == 0xFF);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, (val & 0xF) == 0xF);

        write8(hl.read(), (val + 1) & 0xFF);
        return 12;
    }

    public int dec_r(Register8 reg) {
        setFlag(Flags.ZERO, reg.read() == 0x01);
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, (reg.read() & 0xF) == 0xF);

        reg.dec();
        return 4;
    }

    public int dec_HL() {
        int val = read8(hl.read());
        setFlag(Flags.ZERO, val == 0x01);
        setFlag(Flags.SUBTRACT, true);
        setFlag(Flags.HALF_CARRY, (val & 0xF) == 0xF);

        write8(hl.read(), (val - 1) & 0xFF);
        return 12;
    }

    public int add_HL_rr(Register16 reg) {
        int val = hl.read() + reg.read();
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, ((hl.read() & 0x0FFF) + (reg.read() & 0x0FFF) & 0x1000) == 0x1000);
        setFlag(Flags.CARRY, (val & 0x10000) == 0x10000);

        hl.write(val);
        return 8;
    }

    public int add_SP_n() {
        int offset = signed8(read8(pc.read()));
        int val = sp.read() + offset;
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, ((hl.read() & 0x0FFF) + (offset & 0x0FFF) & 0x1000) == 0x1000);
        setFlag(Flags.CARRY, (val & 0x10000) == 0x10000);

        sp.write(val);
        pc.inc();
        return 8;
    }

    public int inc_rr(Register16 reg) {
        reg.inc();
        return 8;
    }

    public int dec_rr(Register16 reg) {
        reg.dec();
        return 8;
    }

    public int swap_r(Register8 reg) {
        reg.write((reg.read() >> 4) | (reg.read() << 4));
        setFlag(Flags.ZERO, reg.read() == 0x00);
        return 8;
    }

    public int swap_HL() {
        int data = read8(hl.read());
        write8(hl.read(), (data >> 4) | (data << 4));
        setFlag(Flags.ZERO, data == 0x00);
        return 16;
    }

    public int daa() {
        int n = 0;
        if (hasFlag(Flags.HALF_CARRY) || (!hasFlag(Flags.SUBTRACT) && ((a.read() & 0x0F) > 0x09)))
            n = 6;
        if (hasFlag(Flags.CARRY) || (!hasFlag(Flags.SUBTRACT) && ((a.read() & 0xFF) > 0x99)))
            n |= 0x60;
        a.write(a.read() + (!hasFlag(Flags.SUBTRACT) ? n : -n ));

        setFlag(Flags.ZERO, a.read() == 0x00);
        setFlag(Flags.HALF_CARRY, false);
        setFlag(Flags.CARRY, n > 6);
        return 4;
    }

    public int cpl() {
        a.write(~a.read());
        setFlag(Flags.SUBTRACT, true);
        setFlag(Flags.HALF_CARRY, true);

        return 4;
    }

    public int ccf() {
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, false);
        setFlag(Flags.CARRY, !hasFlag(Flags.CARRY));

        return 4;
    }

    public int scf() {
        setFlag(Flags.SUBTRACT, false);
        setFlag(Flags.HALF_CARRY, false);
        setFlag(Flags.CARRY, true);

        return 4;
    }

    public int nop() {
        return 4;
    }

    public int halt() {
        halted = true;
        return 4;
    }

    public int stop() {
        halted = true;
        pc.inc();
        return 4;
    }

    public int di() {
        disable_interrupt_in_opcode = 1;
        return 4;
    }

    public int ei() {
        enable_interrupt_in_opcode = 1;
        return 4;
    }

    public int rlca() {
        setFlag(Flags.CARRY, (a.read() & 0x80) == 0x80);
        a.write((a.read() << 1) | ((a.read() >> 7) & 0x01));
        setFlag(Flags.ZERO, a.read() == 0x00);
        return 4;
    }

    public int rla() {
        int tmp = a.read();
        a.write((a.read() << 1) | (hasFlag(Flags.CARRY) ? 0x01 : 0x00));

        setFlag(Flags.ZERO, a.read() == 0x00);
        setFlag(Flags.CARRY, (tmp & 0x80) == 0x80);
        return 4;
    }

    public int rrca() {
        setFlag(Flags.CARRY, (a.read() & 0x01) == 0x01);
        a.write((a.read() >> 1) | ((a.read() & 0x01) << 7));
        setFlag(Flags.ZERO, a.read() == 0x00);
        return 4;
    }

    public int rra() {
        int tmp = a.read();
        a.write((a.read() << 1) | (hasFlag(Flags.CARRY) ? 0x80 : 0x00));

        setFlag(Flags.ZERO, a.read() == 0x00);
        setFlag(Flags.CARRY, (tmp & 0x01) == 0x01);
        return 4;
    }

    public int rlc_r(Register8 reg) {
        setFlag(Flags.CARRY, (reg.read() & 0x80) == 0x80);
        reg.write((reg.read() << 1) | ((reg.read() >> 7) & 0x01));
        setFlag(Flags.ZERO, reg.read() == 0x00);
        return 8;
    }

    public int rlc_HL() {
        int tmp = read8(hl.read());
        write8(hl.read(),(tmp << 1) | ((tmp >> 7) & 0x01));

        setFlag(Flags.ZERO, tmp == 0x00);
        setFlag(Flags.CARRY, (tmp & 0x80) == 0x80);
        return 16;
    }

    public int rl_r(Register8 reg) {
        int tmp = reg.read();
        reg.write((reg.read() << 1) | (hasFlag(Flags.CARRY) ? 0x01 : 0x00));

        setFlag(Flags.ZERO, reg.read() == 0x00);
        setFlag(Flags.CARRY, (tmp & 0x80) == 0x80);
        return 8;
    }

    public int rl_HL() {
        int tmp = read8(hl.read());
        write8(hl.read(), (tmp << 1) | (hasFlag(Flags.CARRY) ? 0x01 : 0x00));

        setFlag(Flags.ZERO, read8(hl.read()) == 0x00);
        setFlag(Flags.CARRY, (tmp & 0x80) == 0x80);
        return 16;
    }

    public int rrc_r(Register8 reg) {
        setFlag(Flags.CARRY, (reg.read() & 0x01) == 0x01);
        reg.write((reg.read() >> 1) | ((reg.read() & 0x01) << 7));
        setFlag(Flags.ZERO, reg.read() == 0x00);
        return 8;
    }

    public int rrc_HL() {
        int tmp = read8(hl.read());
        write8(hl.read(),(tmp >> 1) | ((tmp & 0x01) << 7));

        setFlag(Flags.ZERO, tmp == 0x00);
        setFlag(Flags.CARRY, (tmp & 0x01) == 0x01);
        return 16;
    }

    public int rr_r(Register8 reg) {
        int tmp = reg.read();
        reg.write((reg.read() >> 1) | (hasFlag(Flags.CARRY) ? 0x80 : 0x00));

        setFlag(Flags.ZERO, reg.read() == 0x00);
        setFlag(Flags.CARRY, (tmp & 0x80) == 0x80);
        return 8;
    }

    public int rr_HL() {
        int tmp = read8(hl.read());
        write8(hl.read(), (tmp >> 1) | (hasFlag(Flags.CARRY) ? 0x80 : 0x00));

        setFlag(Flags.ZERO, read8(hl.read()) == 0x00);
        setFlag(Flags.CARRY, (tmp & 0x01) == 0x01);
        return 16;
    }

    public int sla_r(Register8 reg) {
        setFlag(Flags.CARRY, (reg.read() & 0x80) == 0x80);
        reg.write(reg.read() << 1);
        setFlag(Flags.ZERO, reg.read() == 0x00);
        return 8;
    }

    public int sla_HL() {
        int tmp = read8(hl.read());
        setFlag(Flags.CARRY, (tmp & 0x80) == 0x80);
        write8(hl.read(), tmp << 1);
        setFlag(Flags.ZERO, (tmp << 1) == 0x00);
        return 16;
    }

    public int sra_r(Register8 reg) {
        setFlag(Flags.CARRY, (reg.read() & 0x01) == 0x01);
        reg.write((reg.read() >> 1) | (reg.read() & 0x80));
        setFlag(Flags.ZERO, reg.read() == 0x00);
        return 8;
    }

    public int sra_HL() {
        int tmp = read8(hl.read());
        setFlag(Flags.CARRY, (tmp & 0x01) == 0x01);
        write8(hl.read(), (tmp >> 1) | (tmp & 0x80));
        setFlag(Flags.ZERO, ((tmp >> 1) | (tmp & 0x80)) == 0x00);
        return 16;
    }

    public int srl_r(Register8 reg) {
        setFlag(Flags.CARRY, (reg.read() & 0x01) == 0x01);
        reg.write((reg.read() >> 1));
        setFlag(Flags.ZERO, reg.read() == 0x00);
        return 8;
    }

    public int srl_HL() {
        int tmp = read8(hl.read());
        setFlag(Flags.CARRY, (tmp & 0x01) == 0x01);
        write8(hl.read(), (tmp >> 1));
        setFlag(Flags.ZERO, (tmp >> 1) == 0x00);
        return 16;
    }

    public int bit_b_r(Register8 reg) {
        int mask = (0x01 << read8(pc.read())) & 0x07;

        setFlag(Flags.ZERO, (reg.read() & mask) == mask);
        pc.inc();

        return 8;
    }

    public int bit_b_HL() {
        int mask = (0x01 << read8(pc.read())) & 0x07;

        setFlag(Flags.ZERO, (read8(hl.read()) & mask) == mask);
        pc.inc();

        return 16;
    }

    public int set_b_R(Register8 reg) {
        int mask = (0x01 << read8(pc.read())) & 0x07;
        reg.write(reg.read() | mask);
        pc.inc();
        return 8;
    }

    public int set_b_HL() {
        int mask = (0x01 << read8(pc.read())) & 0x07;
        write8(hl.read(), read8(hl.read()) | mask);
        pc.inc();
        return 16;
    }

    public int res_b_R(Register8 reg) {
        int mask = (0x01 << read8(pc.read())) & 0x07;
        reg.write(reg.read() & ~mask);
        pc.inc();
        return 8;
    }

    public int res_b_HL() {
        int mask = (0x01 << read8(pc.read())) & 0x07;
        write8(hl.read(), read8(hl.read()) & ~mask);
        pc.inc();
        return 16;
    }

    public int jp_nn() {
        pc.write(read16(pc.read()));
        return 12;
    }

    public int jp_nz_nn() {
        if (!hasFlag(Flags.ZERO)) {
            pc.write(read16(pc.read()));
        } else {
            pc.inc();
            pc.inc();
        }
        return 16;
    }

    public int jp_z_nn() {
        if (hasFlag(Flags.ZERO)) {
            pc.write(read16(pc.read()));
        } else {
            pc.inc();
            pc.inc();
        }
        return 16;
    }

    public int jp_nc_nn() {
        if (!hasFlag(Flags.CARRY)) {
            pc.write(read16(pc.read()));
        } else {
            pc.inc();
            pc.inc();
        }
        return 16;
    }

    public int jp_c_nn() {
        if (hasFlag(Flags.CARRY)) {
            pc.write(read16(pc.read()));
        } else {
            pc.inc();
            pc.inc();
        }
        return 16;
    }

    public int jp_HL() {
        pc.write(hl.read());
        return 4;
    }

    public int jr_n() {
        int offset = signed8(read8(pc.read()));
        pc.inc();
        pc.write(pc.read() + offset);
        return 8;
    }

    public int jr_nz_n() {
        int offset = signed8(read8(pc.read()));
        pc.inc();
        if (!hasFlag(Flags.ZERO))
            pc.write(pc.read() + offset);
        return 8;
    }

    public int jr_z_n() {
        int offset = signed8(read8(pc.read()));
        pc.inc();
        if (hasFlag(Flags.ZERO))
            pc.write(pc.read() + offset);
        return 8;
    }

    public int jr_nc_n() {
        int offset = signed8(read8(pc.read()));
        pc.inc();
        if (!hasFlag(Flags.CARRY))
            pc.write(pc.read() + offset);
        return 8;
    }

    public int jr_c_n() {
        int offset = signed8(read8(pc.read()));
        pc.inc();
        if (hasFlag(Flags.CARRY))
            pc.write(pc.read() + offset);
        return 8;
    }

    public int call_nn() {
        int addr = read16(pc.read());
        pc.inc();
        pc.inc();
        pushStack(pc.read());
        pc.write(addr);
        return 12;
    }

    public int call_nz_nn() {
        int addr = read16(pc.read());
        pc.inc();
        pc.inc();
        if (!hasFlag(Flags.ZERO)) {
            pushStack(pc.read());
            pc.write(addr);
        }
        return 12;
    }

    public int call_z_nn() {
        int addr = read16(pc.read());
        pc.inc();
        pc.inc();
        if (hasFlag(Flags.ZERO)) {
            pushStack(pc.read());
            pc.write(addr);
        }
        return 12;
    }

    public int call_nc_nn() {
        int addr = read16(pc.read());
        pc.inc();
        pc.inc();
        if (!hasFlag(Flags.CARRY)) {
            pushStack(pc.read());
            pc.write(addr);
        }
        return 12;
    }

    public int call_c_nn() {
        int addr = read16(pc.read());
        pc.inc();
        pc.inc();
        if (hasFlag(Flags.CARRY)) {
            pushStack(pc.read());
            pc.write(addr);
        }
        return 12;
    }

    public int rst_n() {
        int offset = read8(pc.read());
        pc.inc();
        pushStack(pc.read());
        pc.write(offset);
        return 32;
    }

    public int ret() {
         pc.write(popStack());
         return 8;
    }

    public int ret_nz() {
        if (!hasFlag(Flags.ZERO))
            pc.write(popStack());
        return 8;
    }

    public int ret_z() {
        if (hasFlag(Flags.ZERO))
            pc.write(popStack());
        return 8;
    }

    public int ret_nc() {
        if (!hasFlag(Flags.CARRY))
            pc.write(popStack());
        return 8;
    }

    public int ret_c() {
        if (hasFlag(Flags.CARRY))
            pc.write(popStack());
        return 8;
    }

    public int reti() {
        pc.write(popStack());
        enableIRQ = true;
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
