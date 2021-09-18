package core.cpu;

import core.Memory;
import core.cpu.register.Register16;
import core.cpu.register.Register8;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
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

    private final Register8 tmp_reg;

    private final Memory memory;
    private BufferedWriter writer;

    private long cycle = 0;
    private long nb_instr = 0;
    private int remaining_cycle_until_op = 0;
    private int IME_delay = -1;

    private boolean halted = false;
    private boolean IME = true;
    private int last_opcode = 0;

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

        tmp_reg = new Register8(0x00);

        this.memory = memory;

        opcodes = new ArrayList<>();
        opcodes.add(new Instruction(0x00, "NOP", this::opcode_0x00_nop));
        opcodes.add(new Instruction(0x01, "LD BC,#16", this::opcode_0x01_fd));
        opcodes.add(new Instruction(0x02, "LD (BC),A", this::opcode_0x02_fd));
        opcodes.add(new Instruction(0x03, "INC BC", this::opcode_0x03_inc));
        opcodes.add(new Instruction(0x04, "INC B", this::opcode_0X04_inc));
        opcodes.add(new Instruction(0x05, "DEC B", this::opcode_0x05_dec));
        opcodes.add(new Instruction(0x06, "LD B,#8", this::opcode_0x06_fd));
        opcodes.add(new Instruction(0x07, "RLCA", this::opcode_0x07_rlca));
        opcodes.add(new Instruction(0x08, "LD (#16),SP", this::opcode_0x08_fd));
        opcodes.add(new Instruction(0x09, "ADD HL,BC", this::opcode_0x09_add));
        opcodes.add(new Instruction(0x0A, "LD A,(BC)", this::opcode_0x0A_fd));
        opcodes.add(new Instruction(0x0B, "DEC BC", this::opcode_0x0B_dec));
        opcodes.add(new Instruction(0x0C, "INC C", this::opcode_0x0C_inc));
        opcodes.add(new Instruction(0x0D, "DEC C", this::opcode_0x0D_dec));
        opcodes.add(new Instruction(0x0E, "LD C,#8", this::opcode_0x0E_fd));
        opcodes.add(new Instruction(0x0F, "RRCA", this::opcode_0x0F_rrca));
        opcodes.add(new Instruction(0x10, "STOP", this::opcode_0x10_stop));
        opcodes.add(new Instruction(0x11, "LD DE,d16", this::opcode_0x11_fd));
        opcodes.add(new Instruction(0x12, "LD (DE),A", this::opcode_0x12_fd));
        opcodes.add(new Instruction(0x13, "INC DE", this::opcode_0x13_inc));
        opcodes.add(new Instruction(0x14, "INC D", this::opcode_0x14_inc));
        opcodes.add(new Instruction(0x15, "DEC D", this::opcode_0x15_dec));
        opcodes.add(new Instruction(0x16, "LD D,d8", this::opcode_0x16_fd));
        opcodes.add(new Instruction(0x17, "RLA", this::opcode_0x17_rla));
        opcodes.add(new Instruction(0x18, "JR r8", this::opcode_0x18_jr));
        opcodes.add(new Instruction(0x19, "ADD HL,DE", this::opcode_0x19_add));
        opcodes.add(new Instruction(0x1A, "LD A,(DE)", this::opcode_0x1A_fd));
        opcodes.add(new Instruction(0x1B, "DEC DE", this::opcode_0x1B_dec));
        opcodes.add(new Instruction(0x1C, "INC E", this::opcode_0x1C_inc));
        opcodes.add(new Instruction(0x1D, "DEC E", this::opcode_0x1D_dec));
        opcodes.add(new Instruction(0x1E, "LD E,d8", this::opcode_0x1E_fd));
        opcodes.add(new Instruction(0x1F, "RRA", this::opcode_0x1F_rra));
        opcodes.add(new Instruction(0x20, "JR NZ,r8", this::opcode_0x20_jr));
        opcodes.add(new Instruction(0x21, "LD HL,d16", this::opcode_0x21_fd));
        opcodes.add(new Instruction(0x22, "LD (HL+),A", this::opcode_0x22_fd));
        opcodes.add(new Instruction(0x23, "INC HL", this::opcode_0x23_inc));
        opcodes.add(new Instruction(0x24, "INC H", this::opcode_0x24_inc));
        opcodes.add(new Instruction(0x25, "DEC H", this::opcode_0x25_dec));
        opcodes.add(new Instruction(0x26, "LD H,d8", this::opcode_0x26_fd));
        opcodes.add(new Instruction(0x27, "DAA", this::opcode_0x27_daa));
        opcodes.add(new Instruction(0x28, "JR Z,r8", this::opcode_0x28_jr));
        opcodes.add(new Instruction(0x29, "ADD HL,HL", this::opcode_0x29_add));
        opcodes.add(new Instruction(0x2A, "LD A,(HL+)", this::opcode_0x2A_fd));
        opcodes.add(new Instruction(0x2B, "DEC HL", this::opcode_0x2B_dec));
        opcodes.add(new Instruction(0x2C, "INC L", this::opcode_0x2C_inc));
        opcodes.add(new Instruction(0x2D, "DEC L", this::opcode_0x2D_dec));
        opcodes.add(new Instruction(0x2E, "LD L,d8", this::opcode_0x2E_fd));
        opcodes.add(new Instruction(0x2F, "CPL", this::opcode_0x2F_cpl));
        opcodes.add(new Instruction(0x30, "JR NC,r8", this::opcode_0x30_jr));
        opcodes.add(new Instruction(0x31, "LD SP,d16", this::opcode_0x31_fd));
        opcodes.add(new Instruction(0x32, "LD (HL-),A", this::opcode_0x32_fd));
        opcodes.add(new Instruction(0x33, "INC SP", this::opcode_0x33_inc));
        opcodes.add(new Instruction(0x34, "INC (HL)", this::opcode_0x34_inc));
        opcodes.add(new Instruction(0x35, "DEC (HL)", this::opcode_0x35_dec));
        opcodes.add(new Instruction(0x36, "LD (HL),d8", this::opcode_0x36_fd));
        opcodes.add(new Instruction(0x37, "SCF", this::opcode_0x37_scf));
        opcodes.add(new Instruction(0x38, "JR C,r8", this::opcode_0x38_jr));
        opcodes.add(new Instruction(0x39, "ADD HL,SP", this::opcode_0x39_add));
        opcodes.add(new Instruction(0x3A, "LD A,(HL-)", this::opcode_0x3A_fd));
        opcodes.add(new Instruction(0x3B, "DEC SP", this::opcode_0x3B_dec));
        opcodes.add(new Instruction(0x3C, "INC A", this::opcode_0x3C_inc));
        opcodes.add(new Instruction(0x3D, "DEC A", this::opcode_0x3D_dec));
        opcodes.add(new Instruction(0x3E, "LD A,d8", this::opcode_0x3E_fd));
        opcodes.add(new Instruction(0x3F, "CCF", this::opcode_0x3F_ccf));
        opcodes.add(new Instruction(0x40, "LD B,B", this::opcode_0x40_fd));
        opcodes.add(new Instruction(0x41, "LD B,C", this::opcode_0x41_fd));
        opcodes.add(new Instruction(0x42, "LD B,D", this::opcode_0x42_fd));
        opcodes.add(new Instruction(0x43, "LD B,E", this::opcode_0x43_fd));
        opcodes.add(new Instruction(0x44, "LD B,H", this::opcode_0x44_fd));
        opcodes.add(new Instruction(0x45, "LD B,L", this::opcode_0x45_fd));
        opcodes.add(new Instruction(0x46, "LD B,(HL)", this::opcode_0x46_fd));
        opcodes.add(new Instruction(0x47, "LD B,A", this::opcode_0x47_fd));
        opcodes.add(new Instruction(0x48, "LD C,B", this::opcode_0x48_fd));
        opcodes.add(new Instruction(0x49, "LD C,C", this::opcode_0x49_fd));
        opcodes.add(new Instruction(0x4A, "LD C,D", this::opcode_0x4A_fd));
        opcodes.add(new Instruction(0x4B, "LD C,E", this::opcode_0x4B_fd));
        opcodes.add(new Instruction(0x4C, "LD C,H", this::opcode_0x4C_fd));
        opcodes.add(new Instruction(0x4D, "LD C,L", this::opcode_0x4D_fd));
        opcodes.add(new Instruction(0x4E, "LD C,(HL)", this::opcode_0x4E_fd));
        opcodes.add(new Instruction(0x4F, "LD C,A", this::opcode_0x4F_fd));
        opcodes.add(new Instruction(0x50, "LD D,B", this::opcode_0x50_fd));
        opcodes.add(new Instruction(0x51, "LD D,C", this::opcode_0x51_fd));
        opcodes.add(new Instruction(0x52, "LD D,D", this::opcode_0x52_fd));
        opcodes.add(new Instruction(0x53, "LD D,E", this::opcode_0x53_fd));
        opcodes.add(new Instruction(0x54, "LD D,H", this::opcode_0x54_fd));
        opcodes.add(new Instruction(0x55, "LD D,L", this::opcode_0x55_fd));
        opcodes.add(new Instruction(0x56, "LD D,(HL)", this::opcode_0x56_fd));
        opcodes.add(new Instruction(0x57, "LD D,A", this::opcode_0x57_fd));
        opcodes.add(new Instruction(0x58, "LD E,B", this::opcode_0x58_fd));
        opcodes.add(new Instruction(0x59, "LD E,C", this::opcode_0x59_fd));
        opcodes.add(new Instruction(0x5A, "LD E,D", this::opcode_0x5A_fd));
        opcodes.add(new Instruction(0x5B, "LD E,E", this::opcode_0x5B_fd));
        opcodes.add(new Instruction(0x5C, "LD E,H", this::opcode_0x5C_fd));
        opcodes.add(new Instruction(0x5D, "LD E,L", this::opcode_0x5D_fd));
        opcodes.add(new Instruction(0x5E, "LD E,(HL)", this::opcode_0x5E_fd));
        opcodes.add(new Instruction(0x5F, "LD E,A", this::opcode_0x5F_fd));
        opcodes.add(new Instruction(0x60, "LD H,B", this::opcode_0x60_fd));
        opcodes.add(new Instruction(0x61, "LD H,C", this::opcode_0x61_fd));
        opcodes.add(new Instruction(0x62, "LD H,D", this::opcode_0x62_fd));
        opcodes.add(new Instruction(0x63, "LD H,E", this::opcode_0x63_fd));
        opcodes.add(new Instruction(0x64, "LD H,H", this::opcode_0x64_fd));
        opcodes.add(new Instruction(0x65, "LD H,L", this::opcode_0x65_fd));
        opcodes.add(new Instruction(0x66, "LD H,(HL)", this::opcode_0x66_fd));
        opcodes.add(new Instruction(0x67, "LD H,A", this::opcode_0x67_fd));
        opcodes.add(new Instruction(0x68, "LD L,B", this::opcode_0x68_fd));
        opcodes.add(new Instruction(0x69, "LD L,C", this::opcode_0x69_fd));
        opcodes.add(new Instruction(0x6A, "LD L,D", this::opcode_0x6A_fd));
        opcodes.add(new Instruction(0x6B, "LD L,E", this::opcode_0x6B_fd));
        opcodes.add(new Instruction(0x6C, "LD L,H", this::opcode_0x6C_fd));
        opcodes.add(new Instruction(0x6D, "LD L,L", this::opcode_0x6D_fd));
        opcodes.add(new Instruction(0x6E, "LD L,(HL)", this::opcode_0x6E_fd));
        opcodes.add(new Instruction(0x6F, "LD L,A", this::opcode_0x6F_fd));
        opcodes.add(new Instruction(0x70, "LD (HL),B", this::opcode_0x70_fd));
        opcodes.add(new Instruction(0x71, "LD (HL),C", this::opcode_0x71_fd));
        opcodes.add(new Instruction(0x72, "LD (HL),D", this::opcode_0x72_fd));
        opcodes.add(new Instruction(0x73, "LD (HL),E", this::opcode_0x73_fd));
        opcodes.add(new Instruction(0x74, "LD (HL),H", this::opcode_0x74_fd));
        opcodes.add(new Instruction(0x75, "LD (HL),L", this::opcode_0x75_fd));
        opcodes.add(new Instruction(0x76, "HALT", this::opcode_0x76_halt));
        opcodes.add(new Instruction(0x77, "LD (HL),A", this::opcode_0x77_fd));
        opcodes.add(new Instruction(0x78, "LD A,B", this::opcode_0x78_fd));
        opcodes.add(new Instruction(0x79, "LD A,C", this::opcode_0x79_fd));
        opcodes.add(new Instruction(0x7A, "LD A,D", this::opcode_0x7A_fd));
        opcodes.add(new Instruction(0x7B, "LD A,E", this::opcode_0x7B_fd));
        opcodes.add(new Instruction(0x7C, "LD A,H", this::opcode_0x7C_fd));
        opcodes.add(new Instruction(0x7D, "LD A,L", this::opcode_0x7D_fd));
        opcodes.add(new Instruction(0x7E, "LD A,(HL)", this::opcode_0x7E_fd));
        opcodes.add(new Instruction(0x7F, "LD A,A", this::opcode_0x7F_fd));
        opcodes.add(new Instruction(0x80, "ADD A,B", this::opcode_0x80_add));
        opcodes.add(new Instruction(0x81, "ADD A,C", this::opcode_0x81_add));
        opcodes.add(new Instruction(0x82, "ADD A,D", this::opcode_0x82_add));
        opcodes.add(new Instruction(0x83, "ADD A,E", this::opcode_0x83_add));
        opcodes.add(new Instruction(0x84, "ADD A,H", this::opcode_0x84_add));
        opcodes.add(new Instruction(0x85, "ADD A,L", this::opcode_0x85_add));
        opcodes.add(new Instruction(0x86, "ADD A,(HL)", this::opcode_0x86_add));
        opcodes.add(new Instruction(0x87, "ADD A,A", this::opcode_0x87_add));
        opcodes.add(new Instruction(0x88, "ADC A,B", this::opcode_0x88_adc));
        opcodes.add(new Instruction(0x89, "ADC A,C", this::opcode_0x89_adc));
        opcodes.add(new Instruction(0x8A, "ADC A,D", this::opcode_0x8A_adc));
        opcodes.add(new Instruction(0x8B, "ADC A,E", this::opcode_0x8B_adc));
        opcodes.add(new Instruction(0x8C, "ADC A,H", this::opcode_0x8C_adc));
        opcodes.add(new Instruction(0x8D, "ADC A,L", this::opcode_0x8D_adc));
        opcodes.add(new Instruction(0x8E, "ADC A,(HL)", this::opcode_0x8E_adc));
        opcodes.add(new Instruction(0x8F, "ADC A,A", this::opcode_0x8F_adc));
        opcodes.add(new Instruction(0x90, "SUB B", this::opcode_0x90_sub));
        opcodes.add(new Instruction(0x91, "SUB C", this::opcode_0x91_sub));
        opcodes.add(new Instruction(0x92, "SUB D", this::opcode_0x92_sub));
        opcodes.add(new Instruction(0x93, "SUB E", this::opcode_0x93_sub));
        opcodes.add(new Instruction(0x94, "SUB H", this::opcode_0x94_sub));
        opcodes.add(new Instruction(0x95, "SUB L", this::opcode_0x95_sub));
        opcodes.add(new Instruction(0x96, "SUB (HL)", this::opcode_0x96_sub));
        opcodes.add(new Instruction(0x97, "SUB A", this::opcode_0x97_sub));
        opcodes.add(new Instruction(0x98, "SBC A,B", this::opcode_0x98_sbc));
        opcodes.add(new Instruction(0x99, "SBC A,C", this::opcode_0x99_sbc));
        opcodes.add(new Instruction(0x9A, "SBC A,D", this::opcode_0x9A_sbc));
        opcodes.add(new Instruction(0x9B, "SBC A,E", this::opcode_0x9B_sbc));
        opcodes.add(new Instruction(0x9C, "SBC A,H", this::opcode_0x9C_sbc));
        opcodes.add(new Instruction(0x9D, "SBC A,L", this::opcode_0x9D_sbc));
        opcodes.add(new Instruction(0x9E, "SBC A,(HL)", this::opcode_0x9E_sbc));
        opcodes.add(new Instruction(0x9F, "SBC A,A", this::opcode_0x9F_sbc));
        opcodes.add(new Instruction(0xA0, "AND B", this::opcode_0xA0_and));
        opcodes.add(new Instruction(0xA1, "AND C", this::opcode_0xA1_and));
        opcodes.add(new Instruction(0xA2, "AND D", this::opcode_0xA2_and));
        opcodes.add(new Instruction(0xA3, "AND E", this::opcode_0xA3_and));
        opcodes.add(new Instruction(0xA4, "AND H", this::opcode_0xA4_and));
        opcodes.add(new Instruction(0xA5, "AND L", this::opcode_0xA5_and));
        opcodes.add(new Instruction(0xA6, "AND (HL)", this::opcode_0xA6_and));
        opcodes.add(new Instruction(0xA7, "AND A", this::opcode_0xA7_and));
        opcodes.add(new Instruction(0xA8, "XOR B", this::opcode_0xA8_xor));
        opcodes.add(new Instruction(0xA9, "XOR C", this::opcode_0xA9_xor));
        opcodes.add(new Instruction(0xAA, "XOR D", this::opcode_0xAA_xor));
        opcodes.add(new Instruction(0xAB, "XOR E", this::opcode_0xAB_xor));
        opcodes.add(new Instruction(0xAC, "XOR H", this::opcode_0xAC_xor));
        opcodes.add(new Instruction(0xAD, "XOR L", this::opcode_0xAD_xor));
        opcodes.add(new Instruction(0xAE, "XOR (HL)", this::opcode_0xAE_xor));
        opcodes.add(new Instruction(0xAF, "XOR A", this::opcode_0xAF_xor));
        opcodes.add(new Instruction(0xB0, "OR B", this::opcode_0xB0_or));
        opcodes.add(new Instruction(0xB1, "OR C", this::opcode_0xB1_or));
        opcodes.add(new Instruction(0xB2, "OR D", this::opcode_0xB2_or));
        opcodes.add(new Instruction(0xB3, "OR E", this::opcode_0xB3_or));
        opcodes.add(new Instruction(0xB4, "OR H", this::opcode_0xB4_or));
        opcodes.add(new Instruction(0xB5, "OR L", this::opcode_0xB5_or));
        opcodes.add(new Instruction(0xB6, "OR (HL)", this::opcode_0xB6_or));
        opcodes.add(new Instruction(0xB7, "OR A", this::opcode_0xB7_or));
        opcodes.add(new Instruction(0xB8, "CP B", this::opcode_0xB8_cp));
        opcodes.add(new Instruction(0xB9, "CP C", this::opcode_0xB9_cp));
        opcodes.add(new Instruction(0xBA, "CP D", this::opcode_0xBA_cp));
        opcodes.add(new Instruction(0xBB, "CP E", this::opcode_0xBB_cp));
        opcodes.add(new Instruction(0xBC, "CP H", this::opcode_0xBC_cp));
        opcodes.add(new Instruction(0xBD, "CP L", this::opcode_0xBD_cp));
        opcodes.add(new Instruction(0xBE, "CP (HL)", this::opcode_0xBE_cp));
        opcodes.add(new Instruction(0xBF, "CP A", this::opcode_0xBF_cp));
        opcodes.add(new Instruction(0xC0, "RET NZ", this::opcode_0xC0_ret));
        opcodes.add(new Instruction(0xC1, "POP BC", this::opcode_0xC1_pop));
        opcodes.add(new Instruction(0xC2, "JP NZ,a16", this::opcode_0xC2_jp));
        opcodes.add(new Instruction(0xC3, "JP a16", this::opcode_0xC3_jp));
        opcodes.add(new Instruction(0xC4, "CALL NZ,a16", this::opcode_0xC4_call));
        opcodes.add(new Instruction(0xC5, "PUSH BC", this::opcode_0xC5_push));
        opcodes.add(new Instruction(0xC6, "ADD A,d8", this::opcode_0xC6_add));
        opcodes.add(new Instruction(0xC7, "RST 00H", this::opcode_0xC7_rst));
        opcodes.add(new Instruction(0xC8, "RET Z", this::opcode_0xC8_ret));
        opcodes.add(new Instruction(0xC9, "RET", this::opcode_0xC9_ret));
        opcodes.add(new Instruction(0xCA, "JP Z,a16", this::opcode_0xCA_jp));
        opcodes.add(new Instruction(0xCB, "PREFIX CB", this::prefix));
        opcodes.add(new Instruction(0xCC, "CALL Z,a16", this::opcode_0xCC_call));
        opcodes.add(new Instruction(0xCD, "CALL a16", this::opcode_0xCD_call));
        opcodes.add(new Instruction(0xCE, "ADC A,d8", this::opcode_0xCE_adc));
        opcodes.add(new Instruction(0xCF, "RST 08H", this::opcode_0xCF_rst));
        opcodes.add(new Instruction(0xD0, "RET NC", this::opcode_0xD0_ret));
        opcodes.add(new Instruction(0xD1, "POP DE", this::opcode_0xD1_pop));
        opcodes.add(new Instruction(0xD2, "JP NC,a16", this::opcode_0xD2_jp));
        opcodes.add(new Instruction(0xD3, "", this::opcode_0x00_nop));
        opcodes.add(new Instruction(0xD4, "CALL NC,a16", this::opcode_0xD4_call));
        opcodes.add(new Instruction(0xD5, "PUSH DE", this::opcode_0xD5_push));
        opcodes.add(new Instruction(0xD6, "SUB d8", this::opcode_0xD6_sub));
        opcodes.add(new Instruction(0xD7, "RST 10H", this::opcode_0xD7_rst));
        opcodes.add(new Instruction(0xD8, "RET C", this::opcode_0xD8_ret));
        opcodes.add(new Instruction(0xD9, "RETI", this::opcode_0xD9_reti));
        opcodes.add(new Instruction(0xDA, "JP C,a16", this::opcode_0xDA_jp));
        opcodes.add(new Instruction(0xDB, "", this::opcode_0x00_nop));
        opcodes.add(new Instruction(0xDC, "CALL C,a16", this::opcode_0xDC_call));
        opcodes.add(new Instruction(0xDD, "", this::opcode_0x00_nop));
        opcodes.add(new Instruction(0xDE, "SBC A,d8", this::opcode_0xDE_sbc));
        opcodes.add(new Instruction(0xDF, "RST 18H", this::opcode_0xDF_rst));
        opcodes.add(new Instruction(0xE0, "LDH (a8),A", this::opcode_0xE0_ldh));
        opcodes.add(new Instruction(0xE1, "POP HL", this::opcode_0xE1_pop));
        opcodes.add(new Instruction(0xE2, "LD (C),A", this::opcode_0xE2_fd));
        opcodes.add(new Instruction(0xE3, "", this::opcode_0x00_nop));
        opcodes.add(new Instruction(0xE4, "", this::opcode_0x00_nop));
        opcodes.add(new Instruction(0xE5, "PUSH HL", this::opcode_0xE5_push));
        opcodes.add(new Instruction(0xE6, "AND d8", this::opcode_0xE6_and));
        opcodes.add(new Instruction(0xE7, "RST 20H", this::opcode_0xE7_rst));
        opcodes.add(new Instruction(0xE8, "ADD SP,r8", this::opcode_0xE8_add));
        opcodes.add(new Instruction(0xE9, "JP (HL)", this::opcode_0xE9_jp));
        opcodes.add(new Instruction(0xEA, "LD (a16),A", this::opcode_0xEA_fd));
        opcodes.add(new Instruction(0xEB, "", this::opcode_0x00_nop));
        opcodes.add(new Instruction(0xEC, "", this::opcode_0x00_nop));
        opcodes.add(new Instruction(0xED, "", this::opcode_0x00_nop));
        opcodes.add(new Instruction(0xEE, "XOR d8", this::opcode_0xEE_xor));
        opcodes.add(new Instruction(0xEF, "RST 28H", this::opcode_0xEF_rst));
        opcodes.add(new Instruction(0xF0, "LDH A,(a8)", this::opcode_0xF0_ldh));
        opcodes.add(new Instruction(0xF1, "POP AF", this::opcode_0xF1_pop));
        opcodes.add(new Instruction(0xF2, "LD A,(C)", this::opcode_0xF2_fd));
        opcodes.add(new Instruction(0xF3, "DI", this::opcode_0xF3_di));
        opcodes.add(new Instruction(0xF4, "", this::opcode_0x00_nop));
        opcodes.add(new Instruction(0xF5, "PUSH AF", this::opcode_0xF5_push));
        opcodes.add(new Instruction(0xF6, "OR d8", this::opcode_0xF6_or));
        opcodes.add(new Instruction(0xF7, "RST 30H", this::opcode_0xF7_rst));
        opcodes.add(new Instruction(0xF8, "LD HL,SP+r8", this::opcode_0xF8_fd));
        opcodes.add(new Instruction(0xF9, "LD SP,HL", this::opcode_0xF9_fd));
        opcodes.add(new Instruction(0xFA, "LD A,(a16)", this::opcode_0xFA_fd));
        opcodes.add(new Instruction(0xFB, "EI", this::opcode_0xFB_ei));
        opcodes.add(new Instruction(0xFC, "", this::opcode_0x00_nop));
        opcodes.add(new Instruction(0xFD, "", this::opcode_0x00_nop));
        opcodes.add(new Instruction(0xFE, "CP d8", this::opcode_0xFE_cp));
        opcodes.add(new Instruction(0xFF, "RST 38H", this::opcode_0xFF_rst));

        cb_opcodes = new ArrayList<>();
        cb_opcodes.add(new Instruction(0x00, "RLC B", this::opcode_0xCB00_rlc));
        cb_opcodes.add(new Instruction(0x01, "RLC C", this::opcode_0xCB01_rlc));
        cb_opcodes.add(new Instruction(0x02, "RLC D", this::opcode_0xCB02_rlc));
        cb_opcodes.add(new Instruction(0x03, "RLC E", this::opcode_0xCB03_rlc));
        cb_opcodes.add(new Instruction(0x04, "RLC H", this::opcode_0xCB04_rlc));
        cb_opcodes.add(new Instruction(0x05, "RLC L", this::opcode_0xCB05_rlc));
        cb_opcodes.add(new Instruction(0x06, "RLC (HL)", this::opcode_0xCB06_rlc));
        cb_opcodes.add(new Instruction(0x07, "RLC A", this::opcode_0xCB07_rlc));
        cb_opcodes.add(new Instruction(0x08, "RRC B", this::opcode_0xCB08_rrc));
        cb_opcodes.add(new Instruction(0x09, "RRC C", this::opcode_0xCB09_rrc));
        cb_opcodes.add(new Instruction(0x0A, "RRC D", this::opcode_0xCB0A_rrc));
        cb_opcodes.add(new Instruction(0x0B, "RRC E", this::opcode_0xCB0B_rrc));
        cb_opcodes.add(new Instruction(0x0C, "RRC H", this::opcode_0xCB0C_rrc));
        cb_opcodes.add(new Instruction(0x0D, "RRC L", this::opcode_0xCB0D_rrc));
        cb_opcodes.add(new Instruction(0x0E, "RRC (HL)", this::opcode_0xCB0E_rrc));
        cb_opcodes.add(new Instruction(0x0F, "RRC A", this::opcode_0xCB0F_rrc));
        cb_opcodes.add(new Instruction(0x10, "RL B", this::opcode_0xCB10_rl));
        cb_opcodes.add(new Instruction(0x11, "RL C", this::opcode_0xCB11_rl));
        cb_opcodes.add(new Instruction(0x12, "RL D", this::opcode_0xCB12_rl));
        cb_opcodes.add(new Instruction(0x13, "RL E", this::opcode_0xCB13_rl));
        cb_opcodes.add(new Instruction(0x14, "RL H", this::opcode_0xCB14_rl));
        cb_opcodes.add(new Instruction(0x15, "RL L", this::opcode_0xCB15_rl));
        cb_opcodes.add(new Instruction(0x16, "RL (HL)", this::opcode_0xCB16_rl));
        cb_opcodes.add(new Instruction(0x17, "RL A", this::opcode_0xCB17_rl));
        cb_opcodes.add(new Instruction(0x18, "RR B", this::opcode_0xCB18_rr));
        cb_opcodes.add(new Instruction(0x19, "RR C", this::opcode_0xCB19_rr));
        cb_opcodes.add(new Instruction(0x1A, "RR D", this::opcode_0xCB1A_rr));
        cb_opcodes.add(new Instruction(0x1B, "RR E", this::opcode_0xCB1B_rr));
        cb_opcodes.add(new Instruction(0x1C, "RR H", this::opcode_0xCB1C_rr));
        cb_opcodes.add(new Instruction(0x1D, "RR L", this::opcode_0xCB1D_rr));
        cb_opcodes.add(new Instruction(0x1E, "RR (HL)", this::opcode_0xCB1E_rr));
        cb_opcodes.add(new Instruction(0x1F, "RR A", this::opcode_0xCB1F_rr));
        cb_opcodes.add(new Instruction(0x20, "SLA B", this::opcode_0xCB20_sla));
        cb_opcodes.add(new Instruction(0x21, "SLA C", this::opcode_0xCB21_sla));
        cb_opcodes.add(new Instruction(0x22, "SLA D", this::opcode_0xCB22_sla));
        cb_opcodes.add(new Instruction(0x23, "SLA E", this::opcode_0xCB23_sla));
        cb_opcodes.add(new Instruction(0x24, "SLA H", this::opcode_0xCB24_sla));
        cb_opcodes.add(new Instruction(0x25, "SLA L", this::opcode_0xCB25_sla));
        cb_opcodes.add(new Instruction(0x26, "SLA (HL)", this::opcode_0xCB26_sla));
        cb_opcodes.add(new Instruction(0x27, "SLA A", this::opcode_0xCB27_sla));
        cb_opcodes.add(new Instruction(0x28, "SRA B", this::opcode_0xCB28_sra));
        cb_opcodes.add(new Instruction(0x29, "SRA C", this::opcode_0xCB29_sra));
        cb_opcodes.add(new Instruction(0x2A, "SRA D", this::opcode_0xCB2A_sra));
        cb_opcodes.add(new Instruction(0x2B, "SRA E", this::opcode_0xCB2B_sra));
        cb_opcodes.add(new Instruction(0x2C, "SRA H", this::opcode_0xCB2C_sra));
        cb_opcodes.add(new Instruction(0x2D, "SRA L", this::opcode_0xCB2D_sra));
        cb_opcodes.add(new Instruction(0x2E, "SRA (HL)", this::opcode_0xCB2E_sra));
        cb_opcodes.add(new Instruction(0x2F, "SRA A", this::opcode_0xCB2F_sra));
        cb_opcodes.add(new Instruction(0x30, "SWAP B", this::opcode_0xCB30_swap));
        cb_opcodes.add(new Instruction(0x31, "SWAP C", this::opcode_0xCB31_swap));
        cb_opcodes.add(new Instruction(0x32, "SWAP D", this::opcode_0xCB32_swap));
        cb_opcodes.add(new Instruction(0x33, "SWAP E", this::opcode_0xCB33_swap));
        cb_opcodes.add(new Instruction(0x34, "SWAP H", this::opcode_0xCB34_swap));
        cb_opcodes.add(new Instruction(0x35, "SWAP L", this::opcode_0xCB35_swap));
        cb_opcodes.add(new Instruction(0x36, "SWAP (HL)", this::opcode_0xCB36_swap));
        cb_opcodes.add(new Instruction(0x37, "SWAP A", this::opcode_0xCB37_swap));
        cb_opcodes.add(new Instruction(0x38, "SRL B", this::opcode_0xCB38_srl));
        cb_opcodes.add(new Instruction(0x39, "SRL C", this::opcode_0xCB39_srl));
        cb_opcodes.add(new Instruction(0x3A, "SRL D", this::opcode_0xCB3A_srl));
        cb_opcodes.add(new Instruction(0x3B, "SRL E", this::opcode_0xCB3B_srl));
        cb_opcodes.add(new Instruction(0x3C, "SRL H", this::opcode_0xCB3C_srl));
        cb_opcodes.add(new Instruction(0x3D, "SRL L", this::opcode_0xCB3D_srl));
        cb_opcodes.add(new Instruction(0x3E, "SRL (HL)", this::opcode_0xCB3E_srl));
        cb_opcodes.add(new Instruction(0x3F, "SRL A", this::opcode_0xCB3F_srl));
        cb_opcodes.add(new Instruction(0x40, "BIT 0,B", this::opcode_0xCB40_bit));
        cb_opcodes.add(new Instruction(0x41, "BIT 0,C", this::opcode_0xCB41_bit));
        cb_opcodes.add(new Instruction(0x42, "BIT 0,D", this::opcode_0xCB42_bit));
        cb_opcodes.add(new Instruction(0x43, "BIT 0,E", this::opcode_0xCB43_bit));
        cb_opcodes.add(new Instruction(0x44, "BIT 0,H", this::opcode_0xCB44_bit));
        cb_opcodes.add(new Instruction(0x45, "BIT 0,L", this::opcode_0xCB45_bit));
        cb_opcodes.add(new Instruction(0x46, "BIT 0,(HL)", this::opcode_0xCB46_bit));
        cb_opcodes.add(new Instruction(0x47, "BIT 0,A", this::opcode_0xCB47_bit));
        cb_opcodes.add(new Instruction(0x48, "BIT 1,B", this::opcode_0xCB48_bit));
        cb_opcodes.add(new Instruction(0x49, "BIT 1,C", this::opcode_0xCB49_bit));
        cb_opcodes.add(new Instruction(0x4A, "BIT 1,D", this::opcode_0xCB4A_bit));
        cb_opcodes.add(new Instruction(0x4B, "BIT 1,E", this::opcode_0xCB4B_bit));
        cb_opcodes.add(new Instruction(0x4C, "BIT 1,H", this::opcode_0xCB4C_bit));
        cb_opcodes.add(new Instruction(0x4D, "BIT 1,L", this::opcode_0xCB4D_bit));
        cb_opcodes.add(new Instruction(0x4E, "BIT 1,(HL)", this::opcode_0xCB4E_bit));
        cb_opcodes.add(new Instruction(0x4F, "BIT 1,A", this::opcode_0xCB4F_bit));
        cb_opcodes.add(new Instruction(0x50, "BIT 2,B", this::opcode_0xCB50_bit));
        cb_opcodes.add(new Instruction(0x51, "BIT 2,C", this::opcode_0xCB51_bit));
        cb_opcodes.add(new Instruction(0x52, "BIT 2,D", this::opcode_0xCB52_bit));
        cb_opcodes.add(new Instruction(0x53, "BIT 2,E", this::opcode_0xCB53_bit));
        cb_opcodes.add(new Instruction(0x54, "BIT 2,H", this::opcode_0xCB54_bit));
        cb_opcodes.add(new Instruction(0x55, "BIT 2,L", this::opcode_0xCB55_bit));
        cb_opcodes.add(new Instruction(0x56, "BIT 2,(HL)", this::opcode_0xCB56_bit));
        cb_opcodes.add(new Instruction(0x57, "BIT 2,A", this::opcode_0xCB57_bit));
        cb_opcodes.add(new Instruction(0x58, "BIT 3,B", this::opcode_0xCB58_bit));
        cb_opcodes.add(new Instruction(0x59, "BIT 3,C", this::opcode_0xCB59_bit));
        cb_opcodes.add(new Instruction(0x5A, "BIT 3,D", this::opcode_0xCB5A_bit));
        cb_opcodes.add(new Instruction(0x5B, "BIT 3,E", this::opcode_0xCB5B_bit));
        cb_opcodes.add(new Instruction(0x5C, "BIT 3,H", this::opcode_0xCB5C_bit));
        cb_opcodes.add(new Instruction(0x5D, "BIT 3,L", this::opcode_0xCB5D_bit));
        cb_opcodes.add(new Instruction(0x5E, "BIT 3,(HL)", this::opcode_0xCB5E_bit));
        cb_opcodes.add(new Instruction(0x5F, "BIT 3,A", this::opcode_0xCB5F_bit));
        cb_opcodes.add(new Instruction(0x60, "BIT 4,B", this::opcode_0xCB60_bit));
        cb_opcodes.add(new Instruction(0x61, "BIT 4,C", this::opcode_0xCB61_bit));
        cb_opcodes.add(new Instruction(0x62, "BIT 4,D", this::opcode_0xCB62_bit));
        cb_opcodes.add(new Instruction(0x63, "BIT 4,E", this::opcode_0xCB63_bit));
        cb_opcodes.add(new Instruction(0x64, "BIT 4,H", this::opcode_0xCB64_bit));
        cb_opcodes.add(new Instruction(0x65, "BIT 4,L", this::opcode_0xCB65_bit));
        cb_opcodes.add(new Instruction(0x66, "BIT 4,(HL)", this::opcode_0xCB66_bit));
        cb_opcodes.add(new Instruction(0x67, "BIT 4,A", this::opcode_0xCB67_bit));
        cb_opcodes.add(new Instruction(0x68, "BIT 5,B", this::opcode_0xCB68_bit));
        cb_opcodes.add(new Instruction(0x69, "BIT 5,C", this::opcode_0xCB69_bit));
        cb_opcodes.add(new Instruction(0x6A, "BIT 5,D", this::opcode_0xCB6A_bit));
        cb_opcodes.add(new Instruction(0x6B, "BIT 5,E", this::opcode_0xCB6B_bit));
        cb_opcodes.add(new Instruction(0x6C, "BIT 5,H", this::opcode_0xCB6C_bit));
        cb_opcodes.add(new Instruction(0x6D, "BIT 5,L", this::opcode_0xCB6D_bit));
        cb_opcodes.add(new Instruction(0x6E, "BIT 5,(HL)", this::opcode_0xCB6E_bit));
        cb_opcodes.add(new Instruction(0x6F, "BIT 5,A", this::opcode_0xCB6F_bit));
        cb_opcodes.add(new Instruction(0x70, "BIT 6,B", this::opcode_0xCB70_bit));
        cb_opcodes.add(new Instruction(0x71, "BIT 6,C", this::opcode_0xCB71_bit));
        cb_opcodes.add(new Instruction(0x72, "BIT 6,D", this::opcode_0xCB72_bit));
        cb_opcodes.add(new Instruction(0x73, "BIT 6,E", this::opcode_0xCB73_bit));
        cb_opcodes.add(new Instruction(0x74, "BIT 6,H", this::opcode_0xCB74_bit));
        cb_opcodes.add(new Instruction(0x75, "BIT 6,L", this::opcode_0xCB75_bit));
        cb_opcodes.add(new Instruction(0x76, "BIT 6,(HL)", this::opcode_0xCB76_bit));
        cb_opcodes.add(new Instruction(0x77, "BIT 6,A", this::opcode_0xCB77_bit));
        cb_opcodes.add(new Instruction(0x78, "BIT 7,B", this::opcode_0xCB78_bit));
        cb_opcodes.add(new Instruction(0x79, "BIT 7,C", this::opcode_0xCB79_bit));
        cb_opcodes.add(new Instruction(0x7A, "BIT 7,D", this::opcode_0xCB7A_bit));
        cb_opcodes.add(new Instruction(0x7B, "BIT 7,E", this::opcode_0xCB7B_bit));
        cb_opcodes.add(new Instruction(0x7C, "BIT 7,H", this::opcode_0xCB7C_bit));
        cb_opcodes.add(new Instruction(0x7D, "BIT 7,L", this::opcode_0xCB7D_bit));
        cb_opcodes.add(new Instruction(0x7E, "BIT 7,(HL)", this::opcode_0xCB7E_bit));
        cb_opcodes.add(new Instruction(0x7F, "BIT 7,A", this::opcode_0xCB7F_bit));
        cb_opcodes.add(new Instruction(0x80, "RES 0,B", this::opcode_0xCB80_res));
        cb_opcodes.add(new Instruction(0x81, "RES 0,C", this::opcode_0xCB81_res));
        cb_opcodes.add(new Instruction(0x82, "RES 0,D", this::opcode_0xCB82_res));
        cb_opcodes.add(new Instruction(0x83, "RES 0,E", this::opcode_0xCB83_res));
        cb_opcodes.add(new Instruction(0x84, "RES 0,H", this::opcode_0xCB84_res));
        cb_opcodes.add(new Instruction(0x85, "RES 0,L", this::opcode_0xCB85_res));
        cb_opcodes.add(new Instruction(0x86, "RES 0,(HL)", this::opcode_0xCB86_res));
        cb_opcodes.add(new Instruction(0x87, "RES 0,A", this::opcode_0xCB87_res));
        cb_opcodes.add(new Instruction(0x88, "RES 1,B", this::opcode_0xCB88_res));
        cb_opcodes.add(new Instruction(0x89, "RES 1,C", this::opcode_0xCB89_res));
        cb_opcodes.add(new Instruction(0x8A, "RES 1,D", this::opcode_0xCB8A_res));
        cb_opcodes.add(new Instruction(0x8B, "RES 1,E", this::opcode_0xCB8B_res));
        cb_opcodes.add(new Instruction(0x8C, "RES 1,H", this::opcode_0xCB8C_res));
        cb_opcodes.add(new Instruction(0x8D, "RES 1,L", this::opcode_0xCB8D_res));
        cb_opcodes.add(new Instruction(0x8E, "RES 1,(HL)", this::opcode_0xCB8E_res));
        cb_opcodes.add(new Instruction(0x8F, "RES 1,A", this::opcode_0xCB8F_res));
        cb_opcodes.add(new Instruction(0x90, "RES 2,B", this::opcode_0xCB90_res));
        cb_opcodes.add(new Instruction(0x91, "RES 2,C", this::opcode_0xCB91_res));
        cb_opcodes.add(new Instruction(0x92, "RES 2,D", this::opcode_0xCB92_res));
        cb_opcodes.add(new Instruction(0x93, "RES 2,E", this::opcode_0xCB93_res));
        cb_opcodes.add(new Instruction(0x94, "RES 2,H", this::opcode_0xCB94_res));
        cb_opcodes.add(new Instruction(0x95, "RES 2,L", this::opcode_0xCB95_res));
        cb_opcodes.add(new Instruction(0x96, "RES 2,(HL)", this::opcode_0xCB96_res));
        cb_opcodes.add(new Instruction(0x97, "RES 2,A", this::opcode_0xCB97_res));
        cb_opcodes.add(new Instruction(0x98, "RES 3,B", this::opcode_0xCB98_res));
        cb_opcodes.add(new Instruction(0x99, "RES 3,C", this::opcode_0xCB99_res));
        cb_opcodes.add(new Instruction(0x9A, "RES 3,D", this::opcode_0xCB9A_res));
        cb_opcodes.add(new Instruction(0x9B, "RES 3,E", this::opcode_0xCB9B_res));
        cb_opcodes.add(new Instruction(0x9C, "RES 3,H", this::opcode_0xCB9C_res));
        cb_opcodes.add(new Instruction(0x9D, "RES 3,L", this::opcode_0xCB9D_res));
        cb_opcodes.add(new Instruction(0x9E, "RES 3,(HL)", this::opcode_0xCB9E_res));
        cb_opcodes.add(new Instruction(0x9F, "RES 3,A", this::opcode_0xCB9F_res));
        cb_opcodes.add(new Instruction(0xA0, "RES 4,B", this::opcode_0xCBA0_res));
        cb_opcodes.add(new Instruction(0xA1, "RES 4,C", this::opcode_0xCBA1_res));
        cb_opcodes.add(new Instruction(0xA2, "RES 4,D", this::opcode_0xCBA2_res));
        cb_opcodes.add(new Instruction(0xA3, "RES 4,E", this::opcode_0xCBA3_res));
        cb_opcodes.add(new Instruction(0xA4, "RES 4,H", this::opcode_0xCBA4_res));
        cb_opcodes.add(new Instruction(0xA5, "RES 4,L", this::opcode_0xCBA5_res));
        cb_opcodes.add(new Instruction(0xA6, "RES 4,(HL)", this::opcode_0xCBA6_res));
        cb_opcodes.add(new Instruction(0xA7, "RES 4,A", this::opcode_0xCBA7_res));
        cb_opcodes.add(new Instruction(0xA8, "RES 5,B", this::opcode_0xCBA8_res));
        cb_opcodes.add(new Instruction(0xA9, "RES 5,C", this::opcode_0xCBA9_res));
        cb_opcodes.add(new Instruction(0xAA, "RES 5,D", this::opcode_0xCBAA_res));
        cb_opcodes.add(new Instruction(0xAB, "RES 5,E", this::opcode_0xCBAB_res));
        cb_opcodes.add(new Instruction(0xAC, "RES 5,H", this::opcode_0xCBAC_res));
        cb_opcodes.add(new Instruction(0xAD, "RES 5,L", this::opcode_0xCBAD_res));
        cb_opcodes.add(new Instruction(0xAE, "RES 5,(HL)", this::opcode_0xCBAE_res));
        cb_opcodes.add(new Instruction(0xAF, "RES 5,A", this::opcode_0xCBAF_res));
        cb_opcodes.add(new Instruction(0xB0, "RES 6,B", this::opcode_0xCBB0_res));
        cb_opcodes.add(new Instruction(0xB1, "RES 6,C", this::opcode_0xCBB1_res));
        cb_opcodes.add(new Instruction(0xB2, "RES 6,D", this::opcode_0xCBB2_res));
        cb_opcodes.add(new Instruction(0xB3, "RES 6,E", this::opcode_0xCBB3_res));
        cb_opcodes.add(new Instruction(0xB4, "RES 6,H", this::opcode_0xCBB4_res));
        cb_opcodes.add(new Instruction(0xB5, "RES 6,L", this::opcode_0xCBB5_res));
        cb_opcodes.add(new Instruction(0xB6, "RES 6,(HL)", this::opcode_0xCBB6_res));
        cb_opcodes.add(new Instruction(0xB7, "RES 6,A", this::opcode_0xCBB7_res));
        cb_opcodes.add(new Instruction(0xB8, "RES 7,B", this::opcode_0xCBB8_res));
        cb_opcodes.add(new Instruction(0xB9, "RES 7,C", this::opcode_0xCBB9_res));
        cb_opcodes.add(new Instruction(0xBA, "RES 7,D", this::opcode_0xCBBA_res));
        cb_opcodes.add(new Instruction(0xBB, "RES 7,E", this::opcode_0xCBBB_res));
        cb_opcodes.add(new Instruction(0xBC, "RES 7,H", this::opcode_0xCBBC_res));
        cb_opcodes.add(new Instruction(0xBD, "RES 7,L", this::opcode_0xCBBD_res));
        cb_opcodes.add(new Instruction(0xBE, "RES 7,(HL)", this::opcode_0xCBBE_res));
        cb_opcodes.add(new Instruction(0xBF, "RES 7,A", this::opcode_0xCBBF_res));
        cb_opcodes.add(new Instruction(0xC0, "SET 0,B", this::opcode_0xCBC0_set_set));
        cb_opcodes.add(new Instruction(0xC1, "SET 0,C", this::opcode_0xCBC1_set));
        cb_opcodes.add(new Instruction(0xC2, "SET 0,D", this::opcode_0xCBC2_set));
        cb_opcodes.add(new Instruction(0xC3, "SET 0,E", this::opcode_0xCBC3_set));
        cb_opcodes.add(new Instruction(0xC4, "SET 0,H", this::opcode_0xCBC4_set));
        cb_opcodes.add(new Instruction(0xC5, "SET 0,L", this::opcode_0xCBC5_set));
        cb_opcodes.add(new Instruction(0xC6, "SET 0,(HL)", this::opcode_0xCBC6_set));
        cb_opcodes.add(new Instruction(0xC7, "SET 0,A", this::opcode_0xCBC7_set));
        cb_opcodes.add(new Instruction(0xC8, "SET 1,B", this::opcode_0xCBC8_set));
        cb_opcodes.add(new Instruction(0xC9, "SET 1,C", this::opcode_0xCBC9_set));
        cb_opcodes.add(new Instruction(0xCA, "SET 1,D", this::opcode_0xCBCA_set));
        cb_opcodes.add(new Instruction(0xCB, "SET 1,E", this::opcode_0xCBCB_set));
        cb_opcodes.add(new Instruction(0xCC, "SET 1,H", this::opcode_0xCBCC_set));
        cb_opcodes.add(new Instruction(0xCD, "SET 1,L", this::opcode_0xCBCD_set));
        cb_opcodes.add(new Instruction(0xCE, "SET 1,(HL)", this::opcode_0xCBCE_set));
        cb_opcodes.add(new Instruction(0xCF, "SET 1,A", this::opcode_0xCBCF_set));
        cb_opcodes.add(new Instruction(0xD0, "SET 2,B", this::opcode_0xCBD0_set));
        cb_opcodes.add(new Instruction(0xD1, "SET 2,C", this::opcode_0xCBD1_set));
        cb_opcodes.add(new Instruction(0xD2, "SET 2,D", this::opcode_0xCBD2_set));
        cb_opcodes.add(new Instruction(0xD3, "SET 2,E", this::opcode_0xCBD3_set));
        cb_opcodes.add(new Instruction(0xD4, "SET 2,H", this::opcode_0xCBD4_set));
        cb_opcodes.add(new Instruction(0xD5, "SET 2,L", this::opcode_0xCBD5_set));
        cb_opcodes.add(new Instruction(0xD6, "SET 2,(HL)", this::opcode_0xCBD6_set));
        cb_opcodes.add(new Instruction(0xD7, "SET 2,A", this::opcode_0xCBD7_set));
        cb_opcodes.add(new Instruction(0xD8, "SET 3,B", this::opcode_0xCBD8_set));
        cb_opcodes.add(new Instruction(0xD9, "SET 3,C", this::opcode_0xCBD9_set));
        cb_opcodes.add(new Instruction(0xDA, "SET 3,D", this::opcode_0xCBDA_set));
        cb_opcodes.add(new Instruction(0xDB, "SET 3,E", this::opcode_0xCBDB_set));
        cb_opcodes.add(new Instruction(0xDC, "SET 3,H", this::opcode_0xCBDC_set));
        cb_opcodes.add(new Instruction(0xDD, "SET 3,L", this::opcode_0xCBDD_set));
        cb_opcodes.add(new Instruction(0xDE, "SET 3,(HL)", this::opcode_0xCBDE_set));
        cb_opcodes.add(new Instruction(0xDF, "SET 3,A", this::opcode_0xCBDF_set));
        cb_opcodes.add(new Instruction(0xE0, "SET 4,B", this::opcode_0xCBE0_set));
        cb_opcodes.add(new Instruction(0xE1, "SET 4,B", this::opcode_0xCBE1_set));
        cb_opcodes.add(new Instruction(0xE2, "SET 4,D", this::opcode_0xCBE2_set));
        cb_opcodes.add(new Instruction(0xE3, "SET 4,E", this::opcode_0xCBE3_set));
        cb_opcodes.add(new Instruction(0xE4, "SET 4,H", this::opcode_0xCBE4_set));
        cb_opcodes.add(new Instruction(0xE5, "SET 4,L", this::opcode_0xCBE5_set));
        cb_opcodes.add(new Instruction(0xE6, "SET 4,(HL)", this::opcode_0xCBE6_set));
        cb_opcodes.add(new Instruction(0xE7, "SET 4,A", this::opcode_0xCBE7_set));
        cb_opcodes.add(new Instruction(0xE8, "SET 5,B", this::opcode_0xCBE8_set));
        cb_opcodes.add(new Instruction(0xE9, "SET 5,C", this::opcode_0xCBE9_set));
        cb_opcodes.add(new Instruction(0xEA, "SET 5,D", this::opcode_0xCBEA_set));
        cb_opcodes.add(new Instruction(0xEB, "SET 5,E", this::opcode_0xCBEB_set));
        cb_opcodes.add(new Instruction(0xEC, "SET 5,H", this::opcode_0xCBEC_set));
        cb_opcodes.add(new Instruction(0xED, "SET 5,L", this::opcode_0xCBED_set));
        cb_opcodes.add(new Instruction(0xEE, "SET 5,(HL)", this::opcode_0xCBEE_set));
        cb_opcodes.add(new Instruction(0xEF, "SET 5,A", this::opcode_0xCBEF_set));
        cb_opcodes.add(new Instruction(0xF0, "SET 6,B", this::opcode_0xCBF0_set));
        cb_opcodes.add(new Instruction(0xF1, "SET 6,C", this::opcode_0xCBF1_set));
        cb_opcodes.add(new Instruction(0xF2, "SET 6,D", this::opcode_0xCBF2_set));
        cb_opcodes.add(new Instruction(0xF3, "SET 6,E", this::opcode_0xCBF3_set));
        cb_opcodes.add(new Instruction(0xF4, "SET 6,H", this::opcode_0xCBF4_set));
        cb_opcodes.add(new Instruction(0xF5, "SET 6,L", this::opcode_0xCBF5_set));
        cb_opcodes.add(new Instruction(0xF6, "SET 6,(HL)", this::opcode_0xCBF6_set));
        cb_opcodes.add(new Instruction(0xF7, "SET 6,A", this::opcode_0xCBF7_set));
        cb_opcodes.add(new Instruction(0xF8, "SET 7,B", this::opcode_0xCBF8_set));
        cb_opcodes.add(new Instruction(0xF9, "SET 7,C", this::opcode_0xCBF9_set));
        cb_opcodes.add(new Instruction(0xFA, "SET 7,D", this::opcode_0xCBFA_set));
        cb_opcodes.add(new Instruction(0xFB, "SET 7,E", this::opcode_0xCBFB_set));
        cb_opcodes.add(new Instruction(0xFC, "SET 7,H", this::opcode_0xCBFC_set));
        cb_opcodes.add(new Instruction(0xFD, "SET 7,L", this::opcode_0xCBFD_set));
        cb_opcodes.add(new Instruction(0xFE, "SET 7,(HL)", this::opcode_0xCBFE_set));
        cb_opcodes.add(new Instruction(0xFF, "SET 7,A", this::opcode_0xCBFF_set));
        try {
            writer = new BufferedWriter(new FileWriter("out_new"));
        } catch (IOException ex) {
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
            try {
                if (opcode != last_opcode || nb_instr <= 60000)
                writer.write(Integer.toHexString(pc.read()) + "\n");
            } catch (IOException ex) {}
            //System.out.println(Integer.toHexString(pc.read()));
            if (pc.read() == 0xC3FC)
                pc.read(); //to trigger debug point
            if (nb_instr == 59867)
                pc.read(); //to trigger debug point
            pc.inc();
            Instruction inst = opcodes.get(opcode);
            nb_instr++;
            remaining_cycle_until_op = inst.operate()/4;
            if (IME_delay > 0) {
                IME_delay--;
            } else if (IME_delay == 0){
                IME = true;
                IME_delay = -1;
            }
            last_opcode = opcode;
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
        IME_delay = -1;
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

    //===============REG UTILS===============//

    public void inc_reg8(Register8 reg) {
        reg.inc();
        setFlag(Flags.ZERO, reg.read() == 0x00);
        setFlag(Flags.SUBSTRACT, false);
        setFlag(Flags.HALF_CARRY, (reg.read() & 0x0F) == 0x00);
    }

    public void dec_reg8(Register8 reg) {
        reg.dec();
        setFlag(Flags.ZERO, reg.read() == 0x00);
        setFlag(Flags.SUBSTRACT, true);
        setFlag(Flags.HALF_CARRY, (reg.read() & 0x0F) == 0x00);
    }

    public void add_reg16(Register16 reg, int data) {
        data &= 0xFFFF;
        int result = reg.read() + data;

        setFlag(Flags.HALF_CARRY, ((reg.read() ^ data ^ (result & 0xFFFF)) & 0x1000) == 0x1000);
        setFlag(Flags.CARRY, (result & 0x10000) == 0x10000);
        setFlag(Flags.SUBSTRACT, false);

        reg.write(result & 0xFFFF);
    }

    public void add_reg8(Register8 reg, int data) {
        data &= 0xFF;
        int result = reg.read() + data;

        setFlag(Flags.HALF_CARRY, ((reg.read() & 0xF) + (data & 0xF)) > 0xF);
        setFlag(Flags.CARRY, (result & 0x100) == 0x100);
        setFlag(Flags.SUBSTRACT, false);
        setFlag(Flags.ZERO, result == 0x00);
        reg.write(result & 0xFF);
    }

    public void adc_reg8(Register8 reg, int data) {
        data &= 0xFF;
        int result = reg.read() + data + (hasFlag(Flags.CARRY) ? 1 : 0);

        setFlag(Flags.HALF_CARRY, ((reg.read() & 0xF) + (data & 0xF) + (hasFlag(Flags.CARRY) ? 1 : 0)) > 0xF);
        setFlag(Flags.CARRY, (result & 0x100) == 0x100);
        setFlag(Flags.SUBSTRACT, false);
        setFlag(Flags.ZERO, result == 0x00);
        reg.write(result & 0xFF);
    }

    public void sub_reg8(Register8 reg, int data) {
        data &= 0xFF;
        int result = (reg.read() - data) & 0xFF;

        setFlag(Flags.HALF_CARRY, (reg.read() & 0xF) - (data & 0xF) < 0);
        setFlag(Flags.CARRY, reg.read() < data);
        setFlag(Flags.SUBSTRACT, true);
        setFlag(Flags.ZERO, result == 0x0);

        reg.write(result);
    }

    public void sbc_reg8(Register8 reg, int data) {
        data &= 0xFF;
        int result = (reg.read() - data - (hasFlag(Flags.CARRY) ? 1 : 0));

        setFlag(Flags.ZERO, result == 0x0);
        setFlag(Flags.SUBSTRACT, true);
        setFlag(Flags.HALF_CARRY, ((reg.read() & 0xF) - (data & 0xF) - (hasFlag(Flags.CARRY) ? 1 : 0) < 0));
        setFlag(Flags.CARRY, result < 0);

        reg.write(result & 0xFF);
    }

    public void and_reg8(Register8 reg, int data) {
        data &= 0xFF;
        reg.write(a.read() & data);

        setFlag(Flags.ZERO, reg.read() == 0x00);
        setFlag(Flags.SUBSTRACT, false);
        setFlag(Flags.HALF_CARRY, true);
        setFlag(Flags.CARRY, false);
    }

    public void xor_reg8(Register8 reg, int data) {
        data &= 0xFF;
        reg.write(a.read() ^ data);

        setFlag(Flags.ZERO, reg.read() == 0x00);
        setFlag(Flags.SUBSTRACT, false);
        setFlag(Flags.HALF_CARRY, false);
        setFlag(Flags.CARRY, false);
    }

    public void or_reg8(Register8 reg, int data) {
        data &= 0xFF;
        reg.write(a.read() | data);

        setFlag(Flags.ZERO, reg.read() == 0x00);
        setFlag(Flags.SUBSTRACT, false);
        setFlag(Flags.HALF_CARRY, false);
        setFlag(Flags.CARRY, false);
    }

    public void cp_reg8(Register8 reg, int data) {
        data &= 0xFF;
        setFlag(Flags.ZERO, reg.read() == data);
        setFlag(Flags.SUBSTRACT, true);
        setFlag(Flags.HALF_CARRY, (reg.read() & 0xF) - (data & 0xF) < 0);
        setFlag(Flags.CARRY, reg.read() < data);
    }

    public void rlc_reg8(Register8 reg) {
        int result = (reg.read() << 1) | ((reg.read() >> 7) & 0x01);
        result &= 0xFF;

        setFlag(Flags.ZERO, result == 0x00);
        setFlag(Flags.SUBSTRACT, false);
        setFlag(Flags.HALF_CARRY, false);
        setFlag(Flags.CARRY, ((reg.read() >> 7) & 0x01) != 0x0);

        reg.write(result);
    }

    public void rrc_reg8(Register8 reg) {
        int result = ((reg.read() & 0x01) << 7) | (reg.read() >> 1);
        result &= 0xFF;

        setFlag(Flags.ZERO, result == 0x00);
        setFlag(Flags.SUBSTRACT, false);
        setFlag(Flags.HALF_CARRY, false);
        setFlag(Flags.CARRY, (reg.read() & 0x01) != 0x0);

        reg.write(result);
    }

    public void rl_reg8(Register8 reg) {
        int result = (reg.read() << 1) | (hasFlag(Flags.CARRY) ? 1 : 0);
        result &= 0xFF;

        setFlag(Flags.ZERO, result == 0x00);
        setFlag(Flags.SUBSTRACT, false);
        setFlag(Flags.HALF_CARRY, false);
        setFlag(Flags.CARRY, ((reg.read() >> 7) & 0x01) != 0x0);

        reg.write(result);
    }

    public void rr_reg8(Register8 reg) {
        int result = (hasFlag(Flags.CARRY) ? 0x80 : 0x00) | (reg.read() >> 1);
        result &= 0xFF;

        setFlag(Flags.ZERO, result == 0x00);
        setFlag(Flags.SUBSTRACT, false);
        setFlag(Flags.HALF_CARRY, false);
        setFlag(Flags.CARRY, (reg.read() & 0x01) != 0x0);

        reg.write(result);
    }

    public void sla_reg8(Register8 reg) {
        int result = reg.read() << 1;
        result &= 0xFF;

        setFlag(Flags.ZERO, result == 0x00);
        setFlag(Flags.SUBSTRACT, false);
        setFlag(Flags.HALF_CARRY, false);
        setFlag(Flags.CARRY, ((reg.read() >> 7) & 0x01) != 0x0);

        reg.write(result);
    }

    public void sra_reg8(Register8 reg) {
        int result = (reg.read() & 0x80) | (reg.read() >> 1);
        result &= 0xFF;

        setFlag(Flags.ZERO, result == 0x00);
        setFlag(Flags.SUBSTRACT, false);
        setFlag(Flags.HALF_CARRY, false);
        setFlag(Flags.CARRY, (reg.read() & 0x01) != 0x0);

        reg.write(result);
    }

    public void srl_reg8(Register8 reg) {
        int result = reg.read() >> 1;
        result &= 0xFF;

        setFlag(Flags.ZERO, result == 0x00);
        setFlag(Flags.SUBSTRACT, false);
        setFlag(Flags.HALF_CARRY, false);
        setFlag(Flags.CARRY, (reg.read() & 0x01) != 0x0);

        reg.write(result);
    }

    public void swap_reg8(Register8 reg) {
        reg.write(((reg.read() & 0x0F) << 4) |((reg.read() & 0xF0) >> 4));

        setFlag(Flags.ZERO, reg.read() == 0x00);
        setFlag(Flags.SUBSTRACT, false);
        setFlag(Flags.HALF_CARRY, false);
        setFlag(Flags.CARRY, false);
    }

    public void set_reg8(Register8 reg, int bit) {
        reg.write(reg.read() | ((0x01) << (bit & 0x07)));
    }

    public void res_reg8(Register8 reg, int bit) {
        reg.write(reg.read() & ~((0x01) << (bit & 0x07)));
    }

    public void bit_reg8(int bit, Register8 reg) {
        setFlag(Flags.ZERO, ((reg.read() >> (bit & 0x07)) & 0x01) == 0x01);
        setFlag(Flags.SUBSTRACT, false);
        setFlag(Flags.HALF_CARRY, true);
    }

    //=================OPCODE==================//
    public int opcode_0x03_inc() {
        //INC BC
        bc.inc();
        return 8;
    }

    public int opcode_0X04_inc() {
        //INC B
        inc_reg8(b);
        return 4;
    }

    public int opcode_0x0C_inc() {
        //INC C
        inc_reg8(c);
        return 4;
    }

    public int opcode_0x13_inc() {
        //INC DE
        de.inc();
        return 8;
    }

    public int opcode_0x14_inc() {
        //INC D
        inc_reg8(d);
        return 4;
    }

    public int opcode_0x1C_inc() {
        //INC E
        inc_reg8(e);
        return 4;
    }

    public int opcode_0x23_inc() {
        //INC HL
        hl.inc();
        return 8;
    }

    public int opcode_0x24_inc() {
        //INC H
        inc_reg8(h);
        return 4;
    }

    public int opcode_0x2C_inc() {
        //INC L
        inc_reg8(l);
        return 4;
    }

    public int opcode_0x33_inc() {
        //INC SP
        sp.inc();
        return 8;
    }

    public int opcode_0x34_inc() {
        //INC (HL)
        tmp_reg.write(read8(read16(hl.read())));
        inc_reg8(tmp_reg);
        write8(hl.read(), tmp_reg.read());
        return 12;
    }

    public int opcode_0x3C_inc() {
        //INC A
        inc_reg8(a);
        return 4;
    }

    public int opcode_0x05_dec() {
        //DEC B
        dec_reg8(b);
        return 4;
    }

    public int opcode_0x0B_dec() {
        //DEC BC
        bc.dec();
        return 8;
    }

    public int opcode_0x0D_dec() {
        //DEC C
        dec_reg8(c);
        return 4;
    }

    public int opcode_0x15_dec() {
        //DEC D
        dec_reg8(d);
        return 4;
    }

    public int opcode_0x1B_dec() {
        //DEC DE
        de.dec();
        return 8;
    }

    public int opcode_0x1D_dec() {
        //DEC E
        dec_reg8(e);
        return 4;
    }

    public int opcode_0x25_dec() {
        //DEC H
        dec_reg8(h);
        return 4;
    }

    public int opcode_0x2B_dec() {
        //DEC HL
        hl.dec();
        return 8;
    }

    public int opcode_0x2D_dec() {
        //DEC L
        dec_reg8(l);
        return 4;
    }

    public int opcode_0x35_dec() {
        //DEC (HL)
        tmp_reg.write(read8(read16(hl.read())));
        dec_reg8(tmp_reg);
        write8(hl.read(), tmp_reg.read());
        return 12;
    }

    public int opcode_0x3B_dec() {
        //DEC SP
        sp.dec();
        return 8;
    }

    public int opcode_0x3D_dec() {
        //DEC A
        dec_reg8(a);
        return 4;
    }

    public int opcode_0x09_add() {
        //ADD HL, BC
        add_reg16(hl, bc.read());
        return 8;
    }

    public int opcode_0x19_add() {
        //ADD HL, DE
        add_reg16(hl, de.read());
        return 8;
    }

    public int opcode_0x29_add() {
        //ADD HL, HL
        add_reg16(hl, hl.read());
        return 8;
    }

    public int opcode_0x39_add() {
        //ADD HL, SP
        add_reg16(hl, sp.read());
        return 8;
    }

    public int opcode_0x80_add() {
        //ADD A, B
        add_reg8(a, b.read());
        return 4;
    }

    public int opcode_0x81_add() {
        //ADD A, C
        add_reg8(a, c.read());
        return 4;
    }

    public int opcode_0x82_add() {
        //ADD A, D
        add_reg8(a, d.read());
        return 4;
    }

    public int opcode_0x83_add() {
        //ADD A, E
        add_reg8(a, e.read());
        return 4;
    }

    public int opcode_0x84_add() {
        //ADD A, H
        add_reg8(a, h.read());
        return 4;
    }

    public int opcode_0x85_add() {
        //ADD A, L
        add_reg8(a, l.read());
        return 4;
    }

    public int opcode_0x86_add() {
        //ADD A, (HL)
        add_reg8(a, read8(hl.read()));
        return 8;
    }

    public int opcode_0x87_add() {
        //ADD A, A
        add_reg8(a, a.read());
        return 4;
    }

    public int opcode_0xC6_add() {
        //ADD A, d8
        add_reg8(a, read8(pc.read()));
        pc.inc();
        return 8;
    }

    public int opcode_0xE8_add() {
        //ADD SP, r8
        int data = signed8(read8(pc.read()));
        pc.inc();
        int result = sp.read() + data;

        setFlag(Flags.ZERO, false);
        setFlag(Flags.SUBSTRACT, false);
        setFlag(Flags.HALF_CARRY, ((sp.read() ^ data ^ (result & 0xFFFF)) & 0x10) == 0x10);
        setFlag(Flags.CARRY, ((sp.read() ^ data ^ (result & 0xFFFF)) & 0x100) == 0x100);

        sp.write(result & 0xFFFF);
        return 16;
    }

    public int opcode_0x27_daa() {
        //DAA
        int result = a.read();
        if (hasFlag(Flags.SUBSTRACT)) {
            if (hasFlag(Flags.HALF_CARRY))
                result -= 6;
            if (hasFlag(Flags.CARRY))
                result -= 0x60;
        } else {
            if (hasFlag(Flags.HALF_CARRY) || (a.read() & 0xF) > 0x9)
                result += 6;
            if (hasFlag(Flags.CARRY) || result > 0x9F)
                result += 60;
        }
        a.write(result & 0xFF);
        setFlag(Flags.ZERO, a.read() == 0x00);
        setFlag(Flags.HALF_CARRY, false);
        return 4;
    }

    public int opcode_0x2F_cpl() {
        //CPL
        a.write(~a.read());
        setFlag(Flags.SUBSTRACT, true);
        setFlag(Flags.HALF_CARRY, true);
        return 4;
    }

    public int opcode_0x37_scf() {
        //SCF
        setFlag(Flags.SUBSTRACT, false);
        setFlag(Flags.HALF_CARRY, false);
        setFlag(Flags.CARRY, true);
        return 4;
    }

    public int opcode_0x3F_ccf() {
        //CCF
        setFlag(Flags.SUBSTRACT, false);
        setFlag(Flags.HALF_CARRY, false);
        setFlag(Flags.CARRY, !hasFlag(Flags.CARRY));
        return 4;
    }

    public int opcode_0x88_adc() {
        //ADC A, B
        adc_reg8(a, b.read());
        return 4;
    }

    public int opcode_0x89_adc() {
        //ADC A, C
        adc_reg8(a, c.read());
        return 4;
    }

    public int opcode_0x8A_adc() {
        //ADC A, D
        adc_reg8(a, d.read());
        return 4;
    }

    public int opcode_0x8B_adc() {
        //ADC A, E
        adc_reg8(a, e.read());
        return 4;
    }

    public int opcode_0x8C_adc() {
        //ADC A, H
        adc_reg8(a, h.read());
        return 4;
    }

    public int opcode_0x8D_adc() {
        //ADC A, L
        adc_reg8(a, l.read());
        return 4;
    }

    public int opcode_0x8E_adc() {
        //ADD A, (HL)
        adc_reg8(a, read8(hl.read()));
        return 8;
    }

    public int opcode_0x8F_adc() {
        //ADD A, A
        adc_reg8(a, a.read());
        return 4;
    }

    public int opcode_0xCE_adc() {
        //ADC A, d8
        adc_reg8(a, read8(pc.read()));
        pc.inc();
        return 8;
    }

    public int opcode_0x90_sub() {
        //SUB B
        sub_reg8(a, b.read());
        return 4;
    }

    public int opcode_0x91_sub() {
        //SUB C
        sub_reg8(a, c.read());
        return 4;
    }

    public int opcode_0x92_sub() {
        //SUB D
        sub_reg8(a, d.read());
        return 4;
    }

    public int opcode_0x93_sub() {
        //SUB E
        sub_reg8(a, e.read());
        return 4;
    }

    public int opcode_0x94_sub() {
        //SUB H
        sub_reg8(a, h.read());
        return 4;
    }

    public int opcode_0x95_sub() {
        //SUB L
        sub_reg8(a, l.read());
        return 4;
    }

    public int opcode_0x96_sub() {
        //SUB (HL)
        sub_reg8(a, read8(hl.read()));
        return 8;
    }

    public int opcode_0x97_sub() {
        //SUB A
        sub_reg8(a, a.read());
        return 4;
    }

    public int opcode_0xD6_sub() {
        //SUB d8
        sub_reg8(a, read8(pc.read()));
        pc.inc();
        return 8;
    }

    public int opcode_0x98_sbc() {
        //SBC A, B
        sbc_reg8(a, b.read());
        return 4;
    }

    public int opcode_0x99_sbc() {
        //SBC A,C
        sbc_reg8(a, c.read());
        return 4;
    }

    public int opcode_0x9A_sbc() {
        //SBC A, D
        sbc_reg8(a, d.read());
        return 4;
    }

    public int opcode_0x9B_sbc() {
        //SBC A, E
        sbc_reg8(a, e.read());
        return 4;
    }

    public int opcode_0x9C_sbc() {
        //SBC A, H
        sbc_reg8(a, h.read());
        return 4;
    }

    public int opcode_0x9D_sbc() {
        //SBC A, L
        sbc_reg8(a, l.read());
        return 4;
    }

    public int opcode_0x9E_sbc() {
        //SBC A, (HL)
        sbc_reg8(a, read8(hl.read()));
        return 8;
    }

    public int opcode_0x9F_sbc() {
        //SBC A, A
        sbc_reg8(a, a.read());
        return 4;
    }

    public int opcode_0xDE_sbc() {
        //SBC A, d8
        sbc_reg8(a, read8(pc.read()));
        pc.inc();
        return 8;
    }

    public int opcode_0xA0_and() {
        //AND B
        and_reg8(a, b.read());
        return 4;
    }

    public int opcode_0xA1_and() {
        //AND C
        and_reg8(a, c.read());
        return 4;
    }

    public int opcode_0xA2_and() {
        //AND D
        and_reg8(a, d.read());
        return 4;
    }

    public int opcode_0xA3_and() {
        //AND E
        and_reg8(a, e.read());
        return 4;
    }

    public int opcode_0xA4_and() {
        //AND H
        and_reg8(a, h.read());
        return 4;
    }

    public int opcode_0xA5_and() {
        //AND L
        and_reg8(a, l.read());
        return 4;
    }

    public int opcode_0xA6_and() {
        //AND (HL)
        and_reg8(a, read8(hl.read()));
        return 8;
    }

    public int opcode_0xA7_and() {
        //AND A
        and_reg8(a, a.read());
        return 4;
    }

    public int opcode_0xE6_and() {
        //AND d8
        and_reg8(a, read8(pc.read()));
        pc.inc();
        return 8;
    }

    public int opcode_0xA8_xor() {
        //XOR B
        xor_reg8(a, b.read());
        return 4;
    }

    public int opcode_0xA9_xor() {
        //XOR C
        xor_reg8(a, c.read());
        return 4;
    }

    public int opcode_0xAA_xor() {
        //XOR D
        xor_reg8(a, d.read());
        return 4;
    }

    public int opcode_0xAB_xor() {
        //XOR E
        xor_reg8(a, e.read());
        return 4;
    }

    public int opcode_0xAC_xor() {
        //XOR H
        xor_reg8(a, h.read());
        return 4;
    }

    public int opcode_0xAD_xor() {
        //XOR L
        xor_reg8(a, l.read());
        return 4;
    }

    public int opcode_0xAE_xor() {
        //XOR (HL)
        xor_reg8(a, read8(hl.read()));
        return 8;
    }

    public int opcode_0xAF_xor() {
        //XOR A
        xor_reg8(a, a.read());
        return 4;
    }

    public int opcode_0xEE_xor() {
        //XOR d8
        xor_reg8(a, read8(pc.read()));
        pc.inc();
        return 8;
    }

    public int opcode_0xB0_or() {
        //OR B
        or_reg8(a, b.read());
        return 4;
    }

    public int opcode_0xB1_or() {
        //OR C
        or_reg8(a, c.read());
        return 4;
    }

    public int opcode_0xB2_or() {
        //OR D
        or_reg8(a, d.read());
        return 4;
    }

    public int opcode_0xB3_or() {
        //OR E
        or_reg8(a, e.read());
        return 4;
    }

    public int opcode_0xB4_or() {
        //OR H
        or_reg8(a, h.read());
        return 4;
    }

    public int opcode_0xB5_or() {
        //OR L
        or_reg8(a, l.read());
        return 4;
    }

    public int opcode_0xB6_or() {
        //OR (HL)
        or_reg8(a, read8(hl.read()));
        return 8;
    }

    public int opcode_0xB7_or() {
        //OR A
        or_reg8(a, a.read());
        return 4;
    }

    public int opcode_0xF6_or() {
        //OR d8
        or_reg8(a, read8(pc.read()));
        pc.inc();
        return 8;
    }

    public int opcode_0xB8_cp() {
        // CP B
        cp_reg8(a, b.read());
        return 4;
    }

    public int opcode_0xB9_cp() {
        // CP C
        cp_reg8(a, c.read());
        return 4;
    }

    public int opcode_0xBA_cp() {
        // CP D
        cp_reg8(a, d.read());
        return 4;
    }

    public int opcode_0xBB_cp() {
        // CP E
        cp_reg8(a, e.read());
        return 4;
    }

    public int opcode_0xBC_cp() {
        // CP H
        cp_reg8(a, h.read());
        return 4;
    }

    public int opcode_0xBD_cp() {
        // CP L
        cp_reg8(a, l.read());
        return 4;
    }

    public int opcode_0xBE_cp() {
        // CP (HL)
        cp_reg8(a, read8(hl.read()));
        return 8;
    }

    public int opcode_0xBF_cp() {
        // CP A
        cp_reg8(a, a.read());
        return 4;
    }

    public int opcode_0xFE_cp() {
        // CP d8
        cp_reg8(a, read8(pc.read()));
        pc.inc();
        return 8;
    }

    public int opcode_0x18_jr() {
        //JR r8
        pc.write(pc.read() + signed8(read8(pc.read())));
        pc.inc();
        return 12;
    }

    public int opcode_0x20_jr() {
        //JR NZ r8
        if (!hasFlag(Flags.ZERO)) {
            pc.write(pc.read() + signed8(read8(pc.read())));
            pc.inc();
            return 12;
        }
        pc.inc();
        return 8;
    }

    public int opcode_0x28_jr() {
        //JR Z r8
        if (hasFlag(Flags.ZERO)) {
            pc.write(pc.read() + signed8(read8(pc.read())));
            pc.inc();
            return 12;
        }
        pc.inc();
        return 8;
    }

    public int opcode_0x30_jr() {
        //JR NC r8
        if (!hasFlag(Flags.CARRY)) {
            pc.write(pc.read() + signed8(read8(pc.read())));
            pc.inc();
            return 12;
        }
        pc.inc();
        return 8;
    }

    public int opcode_0x38_jr() {
        //JR C r8
        if (hasFlag(Flags.CARRY)) {
            pc.write(pc.read() + signed8(read8(pc.read())));
            pc.inc();
            return 12;
        }
        pc.inc();
        return 8;
    }

    public int opcode_0xC0_ret() {
        //RET NZ
        if (!hasFlag(Flags.ZERO)) {
            pc.write(popStack());
            return 20;
        }
        return 8;
    }

    public int opcode_0xC8_ret() {
        //RET Z
        if (hasFlag(Flags.ZERO)) {
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
        if (!hasFlag(Flags.CARRY)) {
            pc.write(popStack());
            return 20;
        }
        return 8;
    }

    public int opcode_0xD8_ret() {
        //RET C
        if (hasFlag(Flags.CARRY)) {
            pc.write(popStack());
            return 20;
        }
        return 8;
    }

    public int opcode_0xC2_jp() {
        //JP NZ a16
        if (!hasFlag(Flags.ZERO)) {
            pc.write(read16(pc.read()));
            return 16;
        }
        pc.inc();
        pc.inc();
        return 12;
    }

    public int opcode_0xC3_jp() {
        //JP a16
        pc.write(read16(pc.read()));
        return 16;
    }

    public int opcode_0xCA_jp() {
        //JP Z a16
        if (hasFlag(Flags.ZERO)) {
            pc.write(read16(pc.read()));
            return 16;
        }
        pc.inc();
        pc.inc();
        return 12;
    }

    public int opcode_0xD2_jp() {
        //JP NC a16
        if (!hasFlag(Flags.CARRY)) {
            pc.write(read16(pc.read()));
            return 16;
        }
        pc.inc();
        pc.inc();
        return 12;
    }

    public int opcode_0xDA_jp() {
        //JP C a16
        if (!hasFlag(Flags.CARRY)) {
            pc.write(read16(pc.read()));
            return 16;
        }
        pc.inc();
        pc.inc();
        return 12;
    }

    public int opcode_0xE9_jp() {
        //JP (HL)
        pc.write(hl.read());
        return 4;
    }

    public int opcode_0xC4_call() {
        //CALL NZ a16
        pc.inc();
        pc.inc();
        if (!hasFlag(Flags.ZERO)) {
            pushStack(pc.read());
            pc.write(read16(pc.read() - 2));
            return 24;
        }
        return 12;
    }

    public int opcode_0xCC_call() {
        //CALL Z a16
        pc.inc();
        pc.inc();
        if (hasFlag(Flags.ZERO)) {
            pushStack(pc.read());
            pc.write(read16(pc.read() - 2));
            return 24;
        }
        return 12;
    }

    public int opcode_0xCD_call() {
        //CALL a16
        pc.inc();
        pc.inc();
        pushStack(pc.read());
        pc.write(read16(pc.read() - 2));
        return 24;
    }

    public int opcode_0xD4_call() {
        //CALL NC a16
        pc.inc();
        pc.inc();
        if (!hasFlag(Flags.CARRY)) {
            pushStack(pc.read());
            pc.write(read16(pc.read() - 2));
            return 24;
        }
        return 12;
    }

    public int opcode_0xDC_call() {
        //CALL C a16
        pc.inc();
        pc.inc();
        if (hasFlag(Flags.CARRY)) {
            pushStack(pc.read());
            pc.write(read16(pc.read() - 2));
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
        IME = true;
        return 16;
    }

    public int opcode_0x00_nop() {
        //NOP
        return 4;
    }

    public int opcode_0x07_rlca() {
        //RLCA
        rlc_reg8(a);
        setFlag(Flags.ZERO, false);
        return 4;
    }

    public int opcode_0x0F_rrca() {
        //RRCA
        rrc_reg8(a);
        setFlag(Flags.ZERO, false);
        return 4;
    }

    public int opcode_0x10_stop() {
        //STOP 0
        pc.inc();
        halted = true;
        return 4;
    }

    public int opcode_0x17_rla() {
        //RLA
        rl_reg8(a);
        setFlag(Flags.ZERO, false);
        return 4;
    }

    public int opcode_0x1F_rra() {
        //RRA
        rr_reg8(a);
        setFlag(Flags.ZERO, false);
        return 4;
    }

    public int opcode_0x76_halt() {
        //HALT
        halted = true;
        return 4;
    }

    public int opcode_0xF3_di() {
        //DI
        IME = false;
        return 4;
    }

    public int opcode_0xFB_ei() {
        //EI
        IME_delay = 2;
        return 4;
    }

    public int opcode_0x01_fd() {
        //LD BC,d16
        bc.write(read16(pc.read()));
        pc.inc();
        pc.inc();
        return 12;
    }

    public int opcode_0x02_fd() {
        //LD (BC),A
        write8(bc.read(), a.read());
        return 8;
    }

    public int opcode_0x06_fd() {
        //LD B,d8
        b.write(read8(pc.read()));
        pc.inc();
        return 8;
    }

    public int opcode_0x08_fd() {
        //LD (a16),SP
        write8(read16(pc.read()), sp.read() & 0x00FF);
        pc.inc();
        write8(read16(pc.read()), (sp.read() & 0xFF00) >> 8);
        pc.inc();
        return 20;
    }

    public int opcode_0x0A_fd() {
        //LD A,(BC)
        a.write(read8(bc.read()));
        return 8;
    }

    public int opcode_0x0E_fd() {
        //LD C,d8
        c.write(read8(pc.read()));
        pc.inc();
        return 8;
    }

    public int opcode_0x11_fd() {
        //LD DE,d16
        de.write(read16(pc.read()));
        pc.inc();
        pc.inc();
        return 12;
    }

    public int opcode_0x12_fd() {
        //LD (DE),A
        write8(de.read(), a.read());
        return 8;
    }

    public int opcode_0x16_fd() {
        //LD D,d8
        d.write(read8(pc.read()));
        pc.inc();
        return 8;
    }

    public int opcode_0x1A_fd() {
        //LD A,(DE)
        a.write(read8(de.read()));
        return 8;
    }

    public int opcode_0x1E_fd() {
        //LD E,d8
        e.write(read8(pc.read()));
        pc.inc();
        return 8;
    }

    public int opcode_0x21_fd() {
        //LD HL,d16
        hl.write(read16(pc.read()));
        pc.inc();
        pc.inc();
        return 12;
    }

    public int opcode_0x22_fd() {
        //LD (HL+),A
        write8(hl.read(), a.read());
        hl.inc();
        return 8;
    }

    public int opcode_0x26_fd() {
        //LD H,d8
        h.write(read8(pc.read()));
        pc.inc();
        return 8;
    }

    public int opcode_0x2A_fd() {
        //LD A,(HL+)
        a.write(read8(hl.read()));
        hl.inc();
        return 8;
    }

    public int opcode_0x2E_fd() {
        //LD L,d8
        l.write(read8(pc.read()));
        pc.inc();
        return 8;
    }

    public int opcode_0x31_fd() {
        //LD SP,d16
        sp.write(read16(pc.read()));
        pc.inc();
        pc.inc();
        return 12;
    }

    public int opcode_0x32_fd() {
        //LD (HL-),A
        write8(hl.read(), a.read());
        hl.dec();
        return 8;
    }

    public int opcode_0x36_fd() {
        //LD (HL),d8
        write8(hl.read(), read8(pc.read()));
        pc.inc();
        return 12;
    }

    public int opcode_0x3A_fd() {
        //LD A,(HL-)
        a.write(read8(hl.read()));
        hl.dec();
        return 8;
    }

    public int opcode_0x3E_fd() {
        //LD A,d8
        a.write(read8(pc.read()));
        pc.inc();
        return 8;
    }

    public int opcode_0x40_fd() {
        //LD B,B
        b.write(b.read());
        return 4;
    }

    public int opcode_0x41_fd() {
        //LD B,C
        b.write(c.read());
        return 4;
    }

    public int opcode_0x42_fd() {
        //LD B,D
        b.write(d.read());
        return 4;
    }

    public int opcode_0x43_fd() {
        //LD B,E
        b.write(e.read());
        return 4;
    }

    public int opcode_0x44_fd() {
        //LD B,H
        b.write(h.read());
        return 4;
    }

    public int opcode_0x45_fd() {
        //LD B,L
        b.write(l.read());
        return 4;
    }

    public int opcode_0x46_fd() {
        //LD B,(HL)
        b.write(read8(hl.read()));
        return 8;
    }

    public int opcode_0x47_fd() {
        //LD B,A
        b.write(a.read());
        return 4;
    }

    public int opcode_0x48_fd() {
        //LD C,B
        c.write(b.read());
        return 4;
    }

    public int opcode_0x49_fd() {
        //LD C,C
        c.write(c.read());
        return 4;
    }

    public int opcode_0x4A_fd() {
        //LD C,D
        c.write(d.read());
        return 4;
    }

    public int opcode_0x4B_fd() {
        //LD C,E
        c.write(e.read());
        return 4;
    }

    public int opcode_0x4C_fd() {
        //LD C,H
        c.write(h.read());
        return 4;
    }

    public int opcode_0x4D_fd() {
        //LD C,L
        c.write(l.read());
        return 4;
    }

    public int opcode_0x4E_fd() {
        //LD C,(HL)
        c.write(read8(hl.read()));
        return 8;
    }

    public int opcode_0x4F_fd() {
        //LD C,A
        c.write(a.read());
        return 4;
    }

    public int opcode_0x50_fd() {
        //LD D,B
        d.write(b.read());
        return 4;
    }

    public int opcode_0x51_fd() {
        //LD D,C
        d.write(c.read());
        return 4;
    }

    public int opcode_0x52_fd() {
        //LD D,D
        d.write(d.read());
        return 4;
    }

    public int opcode_0x53_fd() {
        //LD D,E
        d.write(e.read());
        return 4;
    }

    public int opcode_0x54_fd() {
        //LD D,H
        d.write(h.read());
        return 4;
    }

    public int opcode_0x55_fd() {
        //LD D,L
        d.write(l.read());
        return 4;
    }

    public int opcode_0x56_fd() {
        //LD D,(HL)
        d.write(read8(hl.read()));
        return 8;
    }

    public int opcode_0x57_fd() {
        //LD D,A
        d.write(a.read());
        return 4;
    }

    public int opcode_0x58_fd() {
        //LD E,B
        e.write(b.read());
        return 4;
    }

    public int opcode_0x59_fd() {
        //LD E,C
        e.write(c.read());
        return 4;
    }

    public int opcode_0x5A_fd() {
        //LD E,D
        e.write(d.read());
        return 4;
    }

    public int opcode_0x5B_fd() {
        //LD E,E
        e.write(e.read());
        return 4;
    }

    public int opcode_0x5C_fd() {
        //LD E,H
        e.write(h.read());
        return 4;
    }

    public int opcode_0x5D_fd() {
        //LD E,L
        e.write(l.read());
        return 4;
    }

    public int opcode_0x5E_fd() {
        //LD E,(HL)
        e.write(read8(hl.read()));
        return 8;
    }

    public int opcode_0x5F_fd() {
        //LD E,A
        e.write(a.read());
        return 4;
    }

    public int opcode_0x60_fd() {
        //LD H,B
        h.write(b.read());
        return 4;
    }

    public int opcode_0x61_fd() {
        //LD H,C
        h.write(c.read());
        return 4;
    }

    public int opcode_0x62_fd() {
        //LD H,D
        h.write(d.read());
        return 4;
    }

    public int opcode_0x63_fd() {
        //LD H,E
        h.write(e.read());
        return 4;
    }

    public int opcode_0x64_fd() {
        //LD H,H
        h.write(h.read());
        return 4;
    }

    public int opcode_0x65_fd() {
        //LD H,L
        h.write(l.read());
        return 4;
    }

    public int opcode_0x66_fd() {
        //LD H,(HL)
        h.write(read8(hl.read()));
        return 8;
    }

    public int opcode_0x67_fd() {
        //LD H,A
        h.write(a.read());
        return 4;
    }

    public int opcode_0x68_fd() {
        //LD L,B
        l.write(b.read());
        return 4;
    }

    public int opcode_0x69_fd() {
        //LD L,C
        l.write(c.read());
        return 4;
    }

    public int opcode_0x6A_fd() {
        //LD L,D
        l.write(d.read());
        return 4;
    }

    public int opcode_0x6B_fd() {
        //LD L,E
        l.write(e.read());
        return 4;
    }

    public int opcode_0x6C_fd() {
        //LD L,H
        l.write(h.read());
        return 4;
    }

    public int opcode_0x6D_fd() {
        //LD L,L
        l.write(l.read());
        return 4;
    }

    public int opcode_0x6E_fd() {
        //LD L,(HL)
        l.write(read8(hl.read()));
        return 8;
    }

    public int opcode_0x6F_fd() {
        //LD L,A
        l.write(a.read());
        return 4;
    }

    public int opcode_0x70_fd() {
        //LD (HL),B
        write8(hl.read(), b.read());
        return 8;
    }

    public int opcode_0x71_fd() {
        //LD (HL),C
        write8(hl.read(), c.read());
        return 8;
    }

    public int opcode_0x72_fd() {
        //LD (HL),D
        write8(hl.read(), d.read());
        return 8;
    }

    public int opcode_0x73_fd() {
        //LD (HL),E
        write8(hl.read(), e.read());
        return 8;
    }

    public int opcode_0x74_fd() {
        //LD (HL),H
        write8(hl.read(), h.read());
        return 8;
    }

    public int opcode_0x75_fd() {
        //LD (HL),L
        write8(hl.read(), l.read());
        return 8;
    }

    public int opcode_0x77_fd() {
        //LD (HL),A
        write8(hl.read(), a.read());
        return 8;
    }

    public int opcode_0x78_fd() {
        //LD A,B
        a.write(b.read());
        return 4;
    }

    public int opcode_0x79_fd() {
        //LD A,C
        a.write(c.read());
        return 4;
    }

    public int opcode_0x7A_fd() {
        //LD A,D
        a.write(d.read());
        return 4;
    }

    public int opcode_0x7B_fd() {
        //LD A,E
        a.write(e.read());
        return 4;
    }

    public int opcode_0x7C_fd() {
        //LD A,H
        a.write(h.read());
        return 4;
    }

    public int opcode_0x7D_fd() {
        //LD A,L
        a.write(l.read());
        return 4;
    }

    public int opcode_0x7E_fd() {
        //LD A,(HL)
       a.write(read8(hl.read()));
        return 8;
    }

    public int opcode_0x7F_fd() {
        //LD A,A
        a.write(a.read());
        return 4;
    }

    public int opcode_0xE2_fd() {
        //LD (C),A
        write8(0xFF00 + c.read(), a.read());
        return 8;
    }

    public int opcode_0xEA_fd() {
        //LD (a16),A
        write8(read16(pc.read()), a.read());
        pc.inc();
        pc.inc();
        return 16;
    }

    public int opcode_0xF2_fd() {
        //LD A,(C)
        a.write(read8(0xFF00 + c.read()));
        return 8;
    }

    public int opcode_0xF8_fd() {
        //LD HL,SP+r8
        int signedValue = signed8(read8(pc.read()));
        pc.inc();
        int result = sp.read() + signedValue;

        setFlag(Flags.ZERO, false);
        setFlag(Flags.SUBSTRACT, false);
        setFlag(Flags.HALF_CARRY, ((sp.read() ^ signedValue ^ (result & 0xFFFF)) & 0x10) == 0x10);
        setFlag(Flags.CARRY, ((sp.read() ^ signedValue ^ (result & 0xFFFF)) & 0x100) == 0x100);

        hl.write(result & 0xFFFF);
        return 12;
    }

    public int opcode_0xF9_fd() {
        //LD SP,HL
        sp.write(hl.read());
        return 8;
    }

    public int opcode_0xFA_fd() {
        //LD A,(a16)
        a.write(read8(read16(pc.read())));
        pc.inc();
        pc.inc();
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
        write8(0xFF00 + read8(pc.read()), a.read());
        pc.inc();
        return 12;
    }

    public int opcode_0xF0_ldh() {
        //LDH A,(a8)
        a.write(read8(0xFF00 + read8(pc.read())));
        pc.inc();
        return 12;
    }

    public int opcode_0xCB00_rlc() {
        //RLC B
        rlc_reg8(b);
        return 8;
    }

    public int opcode_0xCB01_rlc() {
        //RLC C
        rlc_reg8(c);
        return 8;
    }

    public int opcode_0xCB02_rlc() {
        //RLC D
        rlc_reg8(d);
        return 8;
    }

    public int opcode_0xCB03_rlc() {
        //RLC E
        rlc_reg8(e);
        return 8;
    }

    public int opcode_0xCB04_rlc() {
        //RLC H
        rlc_reg8(h);
        return 8;
    }

    public int opcode_0xCB05_rlc() {
        //RLC L
        rlc_reg8(l);
        return 8;
    }

    public int opcode_0xCB06_rlc() {
        //RLC (HL)
        tmp_reg.write(read8(hl.read()));
        rlc_reg8(tmp_reg);
        write8(hl.read(), tmp_reg.read());
        return 16;
    }

    public int opcode_0xCB07_rlc() {
        //RLC A
        rlc_reg8(a);
        return 8;
    }

    public int opcode_0xCB08_rrc() {
        //RRC B
        rrc_reg8(b);
        return 8;
    }

    public int opcode_0xCB09_rrc() {
        //RRC C
        rrc_reg8(c);
        return 8;
    }

    public int opcode_0xCB0A_rrc() {
        //RRC D
        rrc_reg8(d);
        return 8;
    }

    public int opcode_0xCB0B_rrc() {
        //RRC E
        rrc_reg8(e);
        return 8;
    }

    public int opcode_0xCB0C_rrc() {
        //RRC H
        rrc_reg8(h);
        return 8;
    }

    public int opcode_0xCB0D_rrc() {
        //RRC L
        rrc_reg8(l);
        return 8;
    }

    public int opcode_0xCB0E_rrc() {
        //RRC (HL)
        tmp_reg.write(read8(hl.read()));
        rrc_reg8(tmp_reg);
        write8(hl.read(), tmp_reg.read());
        return 16;
    }

    public int opcode_0xCB0F_rrc() {
        //RRC A
        rrc_reg8(a);
        return 8;
    }

    public int opcode_0xCB10_rl() {
        //RL B
        rl_reg8(b);
        return 8;
    }

    public int opcode_0xCB11_rl() {
        //RL C
        rl_reg8(c);
        return 8;
    }

    public int opcode_0xCB12_rl() {
        //RL D
        rl_reg8(d);
        return 8;
    }

    public int opcode_0xCB13_rl() {
        //RL E
        rl_reg8(e);
        return 8;
    }

    public int opcode_0xCB14_rl() {
        //RL H
        rl_reg8(h);
        return 8;
    }

    public int opcode_0xCB15_rl() {
        //RL L
        rl_reg8(l);
        return 8;
    }

    public int opcode_0xCB16_rl() {
        //RL (HL)
        tmp_reg.write(read8(hl.read()));
        rl_reg8(tmp_reg);
        write8(hl.read(), tmp_reg.read());        
        return 16;
    }

    public int opcode_0xCB17_rl() {
        //RL A
        rl_reg8(a);
        return 8;
    }

    public int opcode_0xCB18_rr() {
        //RR B
        rr_reg8(b);
        return 8;
    }

    public int opcode_0xCB19_rr() {
        //RR C
        rr_reg8(c);
        return 8;
    }

    public int opcode_0xCB1A_rr() {
        //RR D
        rr_reg8(d);
        return 8;
    }

    public int opcode_0xCB1B_rr() {
        //RR E
        rr_reg8(e);
        return 8;
    }

    public int opcode_0xCB1C_rr() {
        //RR H
        rr_reg8(h);
        return 8;
    }

    public int opcode_0xCB1D_rr() {
        //RR L
        rr_reg8(l);
        return 8;
    }

    public int opcode_0xCB1E_rr() {
        //RR (HL)
        tmp_reg.write(read8(hl.read()));
        rr_reg8(tmp_reg);
        write8(hl.read(), tmp_reg.read());
        return 16;
    }

    public int opcode_0xCB1F_rr() {
        //RR A
        rr_reg8(a);
        return 8;
    }

    public int opcode_0xCB20_sla() {
        //SLA B
        sla_reg8(b);
        return 8;
    }

    public int opcode_0xCB21_sla() {
        //SLA C
        sla_reg8(c);
        return 8;
    }

    public int opcode_0xCB22_sla() {
        //SLA D
        sla_reg8(d);
        return 8;
    }

    public int opcode_0xCB23_sla() {
        //SLA E
        sla_reg8(e);
        return 8;
    }

    public int opcode_0xCB24_sla() {
        //SLA H
        sla_reg8(h);
        return 8;
    }

    public int opcode_0xCB25_sla() {
        //SLA L
        sla_reg8(l);
        return 8;
    }

    public int opcode_0xCB26_sla() {
        //SLA (HL)
        tmp_reg.write(read8(hl.read()));
        sla_reg8(tmp_reg);
        write8(hl.read(), tmp_reg.read());
        return 16;
    }

    public int opcode_0xCB27_sla() {
        //SLA A
        sla_reg8(a);
        return 8;
    }

    public int opcode_0xCB28_sra() {
        //SRA B
        sra_reg8(b);
        return 8;
    }

    public int opcode_0xCB29_sra() {
        //SRA C
        sra_reg8(c);
        return 8;
    }

    public int opcode_0xCB2A_sra() {
        //SRA D
        sra_reg8(d);
        return 8;
    }

    public int opcode_0xCB2B_sra() {
        //SRA E
        sra_reg8(e);
        return 8;
    }

    public int opcode_0xCB2C_sra() {
        //SRA H
        sra_reg8(h);
        return 8;
    }

    public int opcode_0xCB2D_sra() {
        //SRA L
        sra_reg8(l);
        return 8;
    }

    public int opcode_0xCB2E_sra() {
        //SRA (HL)
        tmp_reg.write(read8(hl.read()));
        sra_reg8(tmp_reg);
        write8(hl.read(), tmp_reg.read());
        return 16;
    }

    public int opcode_0xCB2F_sra() {
        //SRA A
        sra_reg8(a);
        return 8;
    }

    public int opcode_0xCB30_swap() {
        //SWAP B
        swap_reg8(b);
        return 8;
    }

    public int opcode_0xCB31_swap() {
        //SWAP C
        swap_reg8(c);
        return 8;
    }

    public int opcode_0xCB32_swap() {
        //SWAP D
        swap_reg8(d);
        return 8;
    }

    public int opcode_0xCB33_swap() {
        //SWAP E
        swap_reg8(e);
        return 8;
    }

    public int opcode_0xCB34_swap() {
        //SWAP H
        swap_reg8(h);
        return 8;
    }

    public int opcode_0xCB35_swap() {
        //SWAP L
        swap_reg8(l);
        return 8;
    }

    public int opcode_0xCB36_swap() {
        //SWAP (HL)
        tmp_reg.write(read8(hl.read()));
        swap_reg8(tmp_reg);
        write8(hl.read(), tmp_reg.read());
        return 16;
    }

    public int opcode_0xCB37_swap() {
        //SWAP A
        swap_reg8(a);
        return 8;
    }

    public int opcode_0xCB38_srl() {
        //SRL B
        srl_reg8(b);
        return 8;
    }

    public int opcode_0xCB39_srl() {
        //SRL C
        srl_reg8(c);
        return 8;
    }

    public int opcode_0xCB3A_srl() {
        //SRL D
        srl_reg8(d);
        return 8;
    }

    public int opcode_0xCB3B_srl() {
        //SRL E
        srl_reg8(e);
        return 8;
    }

    public int opcode_0xCB3C_srl() {
        //SRL H
        srl_reg8(h);
        return 8;
    }

    public int opcode_0xCB3D_srl() {
        //SRL L
        srl_reg8(l);
        return 8;
    }

    public int opcode_0xCB3E_srl() {
        //SRL (HL)
        tmp_reg.write(read8(hl.read()));
        srl_reg8(tmp_reg);
        write8(hl.read(), tmp_reg.read());
        return 16;
    }

    public int opcode_0xCB3F_srl() {
        //SRL A
        srl_reg8(a);
        return 8;
    }

    public int opcode_0xCB40_bit() {
        //BIT 0,B
        bit_reg8(0, b);
        return 8;
    }

    public int opcode_0xCB41_bit() {
        //BIT 0,C
        bit_reg8(0, c);
        return 8;
    }

    public int opcode_0xCB42_bit() {
        //BIT 0,D
        bit_reg8(0, d);
        return 8;
    }

    public int opcode_0xCB43_bit() {
        //BIT 0,E
        bit_reg8(0, e);
        return 8;
    }

    public int opcode_0xCB44_bit() {
        //BIT 0,H
        bit_reg8(0, h);
        return 8;
    }

    public int opcode_0xCB45_bit() {
        //BIT 0,L
        bit_reg8(0, l);
        return 8;
    }

    public int opcode_0xCB46_bit() {
        //BIT 0,(HL)
        tmp_reg.write(read8(hl.read()));
        bit_reg8(0, tmp_reg);
        return 12;
    }

    public int opcode_0xCB47_bit() {
        //BIT 0,A
        bit_reg8(0, a);
        return 8;
    }

    public int opcode_0xCB48_bit() {
        //BIT 1,B
        bit_reg8(1, b);
        return 8;
    }

    public int opcode_0xCB49_bit() {
        //BIT 1,C
        bit_reg8(1, c);
        return 8;
    }

    public int opcode_0xCB4A_bit() {
        //BIT 1,D
        bit_reg8(1, d);
        return 8;
    }

    public int opcode_0xCB4B_bit() {
        //BIT 1,E
        bit_reg8(1, e);
        return 8;
    }

    public int opcode_0xCB4C_bit() {
        //BIT 1,H
        bit_reg8(1, h);
        return 8;
    }

    public int opcode_0xCB4D_bit() {
        //BIT 1,L
        bit_reg8(1, l);
        return 8;
    }

    public int opcode_0xCB4E_bit() {
        //BIT 1,(HL)
        tmp_reg.write(read8(hl.read()));
        bit_reg8(1, tmp_reg);
        return 12;
    }

    public int opcode_0xCB4F_bit() {
        //BIT 1,A
        bit_reg8(1, a);
        return 8;
    }

    public int opcode_0xCB50_bit() {
        //BIT 2,B
        bit_reg8(2, b);
        return 8;
    }

    public int opcode_0xCB51_bit() {
        //BIT 2,C
        bit_reg8(2, c);
        return 8;
    }

    public int opcode_0xCB52_bit() {
        //BIT 2,D
        bit_reg8(2, d);
        return 8;
    }

    public int opcode_0xCB53_bit() {
        //BIT 2,E
        bit_reg8(2, e);
        return 8;
    }

    public int opcode_0xCB54_bit() {
        //BIT 2,H
        bit_reg8(2, h);
        return 8;
    }

    public int opcode_0xCB55_bit() {
        //BIT 2,L
        bit_reg8(2, l);
        return 8;
    }

    public int opcode_0xCB56_bit() {
        //BIT 2,(HL)
        tmp_reg.write(read8(hl.read()));
        bit_reg8(2, tmp_reg);
        return 12;
    }

    public int opcode_0xCB57_bit() {
        //BIT 2,A
        bit_reg8(2, a);
        return 8;
    }

    public int opcode_0xCB58_bit() {
        //BIT 3,B
        bit_reg8(3, b);
        return 8;
    }

    public int opcode_0xCB59_bit() {
        //BIT 3,C
        bit_reg8(3, c);
        return 8;
    }

    public int opcode_0xCB5A_bit() {
        //BIT 3,D
        bit_reg8(3, d);
        return 8;
    }

    public int opcode_0xCB5B_bit() {
        //BIT 3,E
        bit_reg8(3, e);
        return 8;
    }

    public int opcode_0xCB5C_bit() {
        //BIT 3,H
        bit_reg8(3, h);
        return 8;
    }

    public int opcode_0xCB5D_bit() {
        //BIT 3,L
        bit_reg8(3, l);
        return 8;
    }

    public int opcode_0xCB5E_bit() {
        //BIT 3,(HL)
        tmp_reg.write(read8(hl.read()));
        bit_reg8(3, tmp_reg);
        return 12;
    }

    public int opcode_0xCB5F_bit() {
        //BIT 3,A
        bit_reg8(3, a);
        return 8;
    }

    public int opcode_0xCB60_bit() {
        //BIT 4,B
        bit_reg8(4, b);
        return 8;
    }

    public int opcode_0xCB61_bit() {
        //BIT 4,C
        bit_reg8(4, c);
        return 8;
    }

    public int opcode_0xCB62_bit() {
        //BIT 4,D
        bit_reg8(4, d);
        return 8;
    }

    public int opcode_0xCB63_bit() {
        //BIT 4,E
        bit_reg8(4, e);
        return 8;
    }

    public int opcode_0xCB64_bit() {
        //BIT 4,H
        bit_reg8(4, h);
        return 8;
    }

    public int opcode_0xCB65_bit() {
        //BIT 4,L
        bit_reg8(4, l);
        return 8;
    }

    public int opcode_0xCB66_bit() {
        //BIT 4,(HL)
        tmp_reg.write(read8(hl.read()));
        bit_reg8(4, tmp_reg);
        return 12;
    }

    public int opcode_0xCB67_bit() {
        //BIT 4,A
        bit_reg8(4, a);
        return 8;
    }

    public int opcode_0xCB68_bit() {
        //BIT 5,B
        bit_reg8(5, b);
        return 8;
    }

    public int opcode_0xCB69_bit() {
        //BIT 5,C
        bit_reg8(5, c);
        return 8;
    }

    public int opcode_0xCB6A_bit() {
        //BIT 5,D
        bit_reg8(5, d);
        return 8;
    }

    public int opcode_0xCB6B_bit() {
        //BIT 5,E
        bit_reg8(5, e);
        return 8;
    }

    public int opcode_0xCB6C_bit() {
        //BIT 5,H
        bit_reg8(5, h);
        return 8;
    }

    public int opcode_0xCB6D_bit() {
        //BIT 5,L
        bit_reg8(5, l);
        return 8;
    }

    public int opcode_0xCB6E_bit() {
        //BIT 5,(HL)
        tmp_reg.write(read8(hl.read()));
        bit_reg8(5, tmp_reg);
        return 12;
    }

    public int opcode_0xCB6F_bit() {
        //BIT 5,A
        bit_reg8(5, a);
        return 8;
    }

    public int opcode_0xCB70_bit() {
        //BIT 6,B
        bit_reg8(6, b);
        return 8;
    }

    public int opcode_0xCB71_bit() {
        //BIT 6,C
        bit_reg8(6, c);
        return 8;
    }

    public int opcode_0xCB72_bit() {
        //BIT 6,D
        bit_reg8(6, d);
        return 8;
    }

    public int opcode_0xCB73_bit() {
        //BIT 6,E
        bit_reg8(6, e);
        return 8;
    }

    public int opcode_0xCB74_bit() {
        //BIT 6,H
        bit_reg8(6, h);
        return 8;
    }

    public int opcode_0xCB75_bit() {
        //BIT 6,L
        bit_reg8(6, l);
        return 8;
    }

    public int opcode_0xCB76_bit() {
        //BIT 6,(HL)
        tmp_reg.write(read8(hl.read()));
        bit_reg8(6, tmp_reg);
        return 12;
    }

    public int opcode_0xCB77_bit() {
        //BIT 6,A
        bit_reg8(6, a);
        return 8;
    }

    public int opcode_0xCB78_bit() {
        //BIT 7,B
        bit_reg8(7, b);
        return 8;
    }

    public int opcode_0xCB79_bit() {
        //BIT 7,C
        bit_reg8(7, c);
        return 8;
    }

    public int opcode_0xCB7A_bit() {
        //BIT 7,D
        bit_reg8(7, d);
        return 8;
    }

    public int opcode_0xCB7B_bit() {
        //BIT 7,E
        bit_reg8(7, e);
        return 8;
    }

    public int opcode_0xCB7C_bit() {
        //BIT 7,H
        bit_reg8(7, h);
        return 8;
    }

    public int opcode_0xCB7D_bit() {
        //BIT 7,L
        bit_reg8(7, l);
        return 8;
    }

    public int opcode_0xCB7E_bit() {
        //BIT 7,(HL)
        tmp_reg.write(read8(hl.read()));
        bit_reg8(7, tmp_reg);
        return 12;
    }

    public int opcode_0xCB7F_bit() {
        //BIT 7,A
        bit_reg8(7, a);
        return 8;
    }

    public int opcode_0xCB80_res() {
        //RES 0,B
        res_reg8(b, 0);
        return 8;
    }

    public int opcode_0xCB81_res() {
        //RES 0,C
        res_reg8(c, 0);
        return 8;
    }

    public int opcode_0xCB82_res() {
        //RES 0,D
        res_reg8(d, 0);
        return 8;
    }

    public int opcode_0xCB83_res() {
        //RES 0,E
        res_reg8(e, 0);
        return 8;
    }

    public int opcode_0xCB84_res() {
        //RES 0,H
        res_reg8(h, 0);
        return 8;
    }

    public int opcode_0xCB85_res() {
        //RES 0,L
        res_reg8(l, 0);
        return 8;
    }

    public int opcode_0xCB86_res() {
        //RES 0,(HL)
        tmp_reg.write(read8(hl.read()));
        res_reg8(tmp_reg, 0);
        write8(hl.read(), tmp_reg.read());
        return 16;
    }

    public int opcode_0xCB87_res() {
        //RES 0,A
        res_reg8(a, 0);
        return 8;
    }

    public int opcode_0xCB88_res() {
        //RES 1,B
        res_reg8(b, 1);
        return 8;
    }

    public int opcode_0xCB89_res() {
        //RES 1,C
        res_reg8(c, 1);
        return 8;
    }

    public int opcode_0xCB8A_res() {
        //RES 1,D
        res_reg8(d, 1);
        return 8;
    }

    public int opcode_0xCB8B_res() {
        //RES 1,E
        res_reg8(e, 1);
        return 8;
    }

    public int opcode_0xCB8C_res() {
        //RES 1,H
        res_reg8(h, 1);
        return 8;
    }

    public int opcode_0xCB8D_res() {
        //RES 1,L
        res_reg8(l, 1);
        return 8;
    }

    public int opcode_0xCB8E_res() {
        //RES 1,(HL)
        tmp_reg.write(read8(hl.read()));
        res_reg8(tmp_reg, 1);
        write8(hl.read(), tmp_reg.read());
        return 16;
    }

    public int opcode_0xCB8F_res() {
        //RES 1,A
        res_reg8(a, 1);
        return 8;
    }

    public int opcode_0xCB90_res() {
        //RES 2,B
        res_reg8(b, 2);
        return 8;
    }

    public int opcode_0xCB91_res() {
        //RES 2,C
        res_reg8(c, 2);
        return 8;
    }

    public int opcode_0xCB92_res() {
        //RES 2,D
        res_reg8(d, 2);
        return 8;
    }

    public int opcode_0xCB93_res() {
        //RES 2,E
        res_reg8(e, 2);
        return 8;
    }

    public int opcode_0xCB94_res() {
        //RES 2,H
        res_reg8(h, 2);
        return 8;
    }

    public int opcode_0xCB95_res() {
        //RES 2,L
        res_reg8(l, 2);
        return 8;
    }

    public int opcode_0xCB96_res() {
        //RES 2,(HL)
        tmp_reg.write(read8(hl.read()));
        res_reg8(tmp_reg, 2);
        write8(hl.read(), tmp_reg.read());
        return 16;
    }

    public int opcode_0xCB97_res() {
        //RES 2,A
        res_reg8(a, 2);
        return 8;
    }

    public int opcode_0xCB98_res() {
        //RES 3,B
        res_reg8(b, 3);
        return 8;
    }

    public int opcode_0xCB99_res() {
        //RES 3,C
        res_reg8(c, 3);
        return 8;
    }

    public int opcode_0xCB9A_res() {
        //RES 3,D
        res_reg8(d, 3);
        return 8;
    }

    public int opcode_0xCB9B_res() {
        //RES 3,E
        res_reg8(e, 3);
        return 8;
    }

    public int opcode_0xCB9C_res() {
        //RES 3,H
        res_reg8(h, 3);
        return 8;
    }

    public int opcode_0xCB9D_res() {
        //RES 3,L
        res_reg8(l, 3);
        return 8;
    }

    public int opcode_0xCB9E_res() {
        //RES 3,(HL)
        tmp_reg.write(read8(hl.read()));
        res_reg8(tmp_reg, 3);
        write8(hl.read(), tmp_reg.read());
        return 16;
    }

    public int opcode_0xCB9F_res() {
        //RES 3,A
        res_reg8(a, 3);
        return 8;
    }

    public int opcode_0xCBA0_res() {
        //RES 4,B
        res_reg8(b, 4);
        return 8;
    }

    public int opcode_0xCBA1_res() {
        //RES 4,C
        res_reg8(c, 4);
        return 8;
    }

    public int opcode_0xCBA2_res() {
        //RES 4,D
        res_reg8(d, 4);
        return 8;
    }

    public int opcode_0xCBA3_res() {
        //RES 4,E
        res_reg8(e, 4);
        return 8;
    }

    public int opcode_0xCBA4_res() {
        //RES 4,H
        res_reg8(h, 4);
        return 8;
    }

    public int opcode_0xCBA5_res() {
        //RES 4,L
        res_reg8(l, 4);
        return 8;
    }

    public int opcode_0xCBA6_res() {
        //RES 4,(HL)
        tmp_reg.write(read8(hl.read()));
        res_reg8(tmp_reg, 4);
        write8(hl.read(), tmp_reg.read());
        return 16;
    }

    public int opcode_0xCBA7_res() {
        //RES 4,A
        res_reg8(a, 4);
        return 8;
    }

    public int opcode_0xCBA8_res() {
        //RES 5,B
        res_reg8(b, 5);
        return 8;
    }

    public int opcode_0xCBA9_res() {
        //RES 5,C
        res_reg8(c, 5);
        return 8;
    }

    public int opcode_0xCBAA_res() {
        //RES 5,D
        res_reg8(d, 5);
        return 8;
    }

    public int opcode_0xCBAB_res() {
        //RES 5,E
        res_reg8(e, 5);
        return 8;
    }

    public int opcode_0xCBAC_res() {
        //RES 5,H
        res_reg8(h, 5);
        return 8;
    }

    public int opcode_0xCBAD_res() {
        //RES 5,L
        res_reg8(l, 5);
        return 8;
    }

    public int opcode_0xCBAE_res() {
        //RES 5,(HL)
        tmp_reg.write(read8(hl.read()));
        res_reg8(tmp_reg, 5);
        write8(hl.read(), tmp_reg.read());
        return 16;
    }

    public int opcode_0xCBAF_res() {
        //RES 5,A
        res_reg8(a, 5);
        return 8;
    }

    public int opcode_0xCBB0_res() {
        //RES 6,B
        res_reg8(b, 6);
        return 8;
    }

    public int opcode_0xCBB1_res() {
        //RES 6,C
        res_reg8(c, 6);
        return 8;
    }

    public int opcode_0xCBB2_res() {
        //RES 6,D
        res_reg8(d, 6);
        return 8;
    }

    public int opcode_0xCBB3_res() {
        //RES 6,E
        res_reg8(e, 6);
        return 8;
    }

    public int opcode_0xCBB4_res() {
        //RES 6,H
        res_reg8(h, 6);
        return 8;
    }

    public int opcode_0xCBB5_res() {
        //RES 6,L
        res_reg8(l, 6);
        return 8;
    }

    public int opcode_0xCBB6_res() {
        //RES 6,(HL)
        tmp_reg.write(read8(hl.read()));
        res_reg8(tmp_reg, 6);
        write8(hl.read(), tmp_reg.read());
        return 16;
    }

    public int opcode_0xCBB7_res() {
        //RES 6,A
        res_reg8(a, 6);
        return 8;
    }

    public int opcode_0xCBB8_res() {
        //RES 7,B
        res_reg8(b, 7);
        return 8;
    }

    public int opcode_0xCBB9_res() {
        //RES 7,C
        res_reg8(c, 7);
        return 8;
    }

    public int opcode_0xCBBA_res() {
        //RES 7,D
        res_reg8(d, 7);
        return 8;
    }

    public int opcode_0xCBBB_res() {
        //RES 7,E
        res_reg8(e, 7);
        return 8;
    }

    public int opcode_0xCBBC_res() {
        //RES 7,H
        res_reg8(h, 7);
        return 8;
    }

    public int opcode_0xCBBD_res() {
        //RES 7,L
        res_reg8(l, 7);
        return 8;
    }

    public int opcode_0xCBBE_res() {
        //RES 7,(HL)
        tmp_reg.write(read8(hl.read()));
        res_reg8(tmp_reg, 7);
        write8(hl.read(), tmp_reg.read());
        return 16;
    }

    public int opcode_0xCBBF_res() {
        //RES 7,A
        res_reg8(a, 7);
        return 8;
    }

    public int opcode_0xCBC0_set_set() {
        //SET 0,B
        set_reg8(b, 0);
        return 8;
    }

    public int opcode_0xCBC1_set() {
        //SET 0,C
        set_reg8(c, 0);
        return 8;
    }

    public int opcode_0xCBC2_set() {
        //SET 0,D
        set_reg8(d, 0);
        return 8;
    }

    public int opcode_0xCBC3_set() {
        //SET 0,E
        set_reg8(e, 0);
        return 8;
    }

    public int opcode_0xCBC4_set() {
        //SET 0,H
        set_reg8(h, 0);
        return 8;
    }

    public int opcode_0xCBC5_set() {
        //SET 0,L
        set_reg8(l, 0);
        return 8;
    }

    public int opcode_0xCBC6_set() {
        //SET 0,(HL)
        tmp_reg.write(read8(hl.read()));
        set_reg8(tmp_reg, 0);
        write8(hl.read(), tmp_reg.read());
        return 16;
    }

    public int opcode_0xCBC7_set() {
        //SET 0,A
        set_reg8(a, 0);
        return 8;
    }

    public int opcode_0xCBC8_set() {
        //SET 1,B
        set_reg8(b, 1);
        return 8;
    }

    public int opcode_0xCBC9_set() {
        //SET 1,C
        set_reg8(c, 1);
        return 8;
    }

    public int opcode_0xCBCA_set() {
        //SET 1,D
        set_reg8(d, 1);
        return 8;
    }

    public int opcode_0xCBCB_set() {
        //SET 1,E
        set_reg8(e, 1);
        return 8;
    }

    public int opcode_0xCBCC_set() {
        //SET 1,H
        set_reg8(h, 1);
        return 8;
    }

    public int opcode_0xCBCD_set() {
        //SET 1,L
        set_reg8(l, 1);
        return 8;
    }

    public int opcode_0xCBCE_set() {
        //SET 1,(HL)
        tmp_reg.write(read8(hl.read()));
        set_reg8(tmp_reg, 1);
        write8(hl.read(), tmp_reg.read());
        return 16;
    }

    public int opcode_0xCBCF_set() {
        //SET 1,A
        set_reg8(a, 1);
        return 8;
    }

    public int opcode_0xCBD0_set() {
        //SET 2,B
        set_reg8(b, 2);
        return 8;
    }

    public int opcode_0xCBD1_set() {
        //SET 2,C
        set_reg8(c, 2);
        return 8;
    }

    public int opcode_0xCBD2_set() {
        //SET 2,D
        set_reg8(d, 2);
        return 8;
    }

    public int opcode_0xCBD3_set() {
        //SET 2,E
        set_reg8(e, 2);
        return 8;
    }

    public int opcode_0xCBD4_set() {
        //SET 2,H
        set_reg8(h, 2);
        return 8;
    }

    public int opcode_0xCBD5_set() {
        //SET 2,L
        set_reg8(l, 2);
        return 8;
    }

    public int opcode_0xCBD6_set() {
        //SET 2,(HL)
        tmp_reg.write(read8(hl.read()));
        set_reg8(tmp_reg, 2);
        write8(hl.read(), tmp_reg.read());
        return 16;
    }

    public int opcode_0xCBD7_set() {
        //SET 2,A
        set_reg8(a, 2);
        return 8;
    }

    public int opcode_0xCBD8_set() {
        //SET 3,B
        set_reg8(b, 3);
        return 8;
    }

    public int opcode_0xCBD9_set() {
        //SET 3,C
        set_reg8(c, 3);
        return 8;
    }

    public int opcode_0xCBDA_set() {
        //SET 3,D
        set_reg8(d, 3);
        return 8;
    }

    public int opcode_0xCBDB_set() {
        //SET 3,E
        set_reg8(e, 3);
        return 8;
    }

    public int opcode_0xCBDC_set() {
        //SET 3,H
        set_reg8(h, 3);
        return 8;
    }

    public int opcode_0xCBDD_set() {
        //SET 3,L
        set_reg8(l, 3);
        return 8;
    }

    public int opcode_0xCBDE_set() {
        //SET 3,(HL)
        tmp_reg.write(read8(hl.read()));
        set_reg8(tmp_reg, 3);
        write8(hl.read(), tmp_reg.read());
        return 16;
    }

    public int opcode_0xCBDF_set() {
        //SET 3,A
        set_reg8(a, 3);
        return 8;
    }

    public int opcode_0xCBE0_set() {
        //SET 4,B
        set_reg8(b, 4);
        return 8;
    }

    public int opcode_0xCBE1_set() {
        //SET 4,C
        set_reg8(c, 4);
        return 8;
    }

    public int opcode_0xCBE2_set() {
        //SET 4,D
        set_reg8(d, 4);
        return 8;
    }

    public int opcode_0xCBE3_set() {
        //SET 4,E
        set_reg8(e, 4);
        return 8;
    }

    public int opcode_0xCBE4_set() {
        //SET 4,H
        set_reg8(h, 4);
        return 8;
    }

    public int opcode_0xCBE5_set() {
        //SET 4,L
        set_reg8(l, 4);
        return 8;
    }

    public int opcode_0xCBE6_set() {
        //SET 4,(HL)
        tmp_reg.write(read8(hl.read()));
        set_reg8(tmp_reg, 4);
        write8(hl.read(), tmp_reg.read());
        return 16;
    }

    public int opcode_0xCBE7_set() {
        //SET 4,A
        set_reg8(a, 4);
        return 8;
    }

    public int opcode_0xCBE8_set() {
        //SET 5,B
        set_reg8(b, 5);
        return 8;
    }

    public int opcode_0xCBE9_set() {
        //SET 5,C
        set_reg8(c, 5);
        return 8;
    }

    public int opcode_0xCBEA_set() {
        //SET 5,D
        set_reg8(d, 5);
        return 8;
    }

    public int opcode_0xCBEB_set() {
        //SET 5,E
        set_reg8(e, 5);
        return 8;
    }

    public int opcode_0xCBEC_set() {
        //SET 5,H
        set_reg8(h, 5);
        return 8;
    }

    public int opcode_0xCBED_set() {
        //SET 5,L
        set_reg8(l, 5);
        return 8;
    }

    public int opcode_0xCBEE_set() {
        //SET 5,(HL)
        tmp_reg.write(read8(hl.read()));
        set_reg8(tmp_reg, 5);
        write8(hl.read(), tmp_reg.read());
        return 16;
    }

    public int opcode_0xCBEF_set() {
        //SET 5,A
        set_reg8(a, 5);
        return 8;
    }

    public int opcode_0xCBF0_set() {
        //SET 6,B
        set_reg8(b, 6);
        return 8;
    }

    public int opcode_0xCBF1_set() {
        //SET 6,C
        set_reg8(c, 6);
        return 8;
    }

    public int opcode_0xCBF2_set() {
        //SET 6,D
        set_reg8(d, 6);
        return 8;
    }

    public int opcode_0xCBF3_set() {
        //SET 6,E
        set_reg8(e, 6);
        return 8;
    }

    public int opcode_0xCBF4_set() {
        //SET 6,H
        set_reg8(h, 6);
        return 8;
    }

    public int opcode_0xCBF5_set() {
        //SET 6,L
        set_reg8(l, 6);
        return 8;
    }

    public int opcode_0xCBF6_set() {
        //SET 6,(HL)
        tmp_reg.write(read8(hl.read()));
        set_reg8(tmp_reg, 6);
        write8(hl.read(), tmp_reg.read());
        return 16;
    }

    public int opcode_0xCBF7_set() {
        //SET 6,A
        set_reg8(a, 6);
        return 8;
    }

    public int opcode_0xCBF8_set() {
        //SET 7,B
        set_reg8(b, 7);
        return 8;
    }

    public int opcode_0xCBF9_set() {
        //SET 7,C
        set_reg8(c, 7);
        return 8;
    }

    public int opcode_0xCBFA_set() {
        //SET 7,D
        set_reg8(d, 7);
        return 8;
    }

    public int opcode_0xCBFB_set() {
        //SET 7,E
        set_reg8(e, 7);
        return 8;
    }

    public int opcode_0xCBFC_set() {
        //SET 7,H
        set_reg8(h, 7);
        return 8;
    }

    public int opcode_0xCBFD_set() {
        //SET 7,L
        set_reg8(l, 7);
        return 8;
    }

    public int opcode_0xCBFE_set() {
        //SET 7,(HL)
        tmp_reg.write(read8(hl.read()));
        set_reg8(tmp_reg, 7);
        write8(hl.read(), tmp_reg.read());
        return 16;
    }

    public int opcode_0xCBFF_set() {
        //SET 7,A
        set_reg8(a, 7);
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
