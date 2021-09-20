package core.cpu;

import core.cpu.register.RegisterWord;

public class State {

    private final RegisterWord af;
    private final RegisterWord bc;
    private final RegisterWord de;
    private final RegisterWord hl;
    private final RegisterWord sp;
    private final RegisterWord pc;
    private final LR35902.Instruction instruction;

    public State() {
        af = new RegisterWord(0);
        bc = new RegisterWord(0);
        de = new RegisterWord(0);
        hl = new RegisterWord(0);
        sp = new RegisterWord(0);
        pc = new RegisterWord(0);
        instruction = new LR35902.Instruction(0, "NOP", 1, null);
    }

    public void set(RegisterWord af, RegisterWord bc, RegisterWord de, RegisterWord hl, RegisterWord sp, RegisterWord pc, LR35902.Instruction instruction) {
        this.af.write(af.read());
        this.bc.write(bc.read());
        this.de.write(de.read());
        this.hl.write(hl.read());
        this.sp.write(sp.read());
        this.pc.write(pc.read() - instruction.getLength());
        this.instruction.copyMeta(instruction);
    }

    public RegisterWord getAf() {
        return af;
    }

    public RegisterWord getBc() {
        return bc;
    }

    public RegisterWord getDe() {
        return de;
    }

    public RegisterWord getHl() {
        return hl;
    }

    public RegisterWord getSp() {
        return sp;
    }

    public RegisterWord getPc() {
        return pc;
    }

    public LR35902.Instruction getInstruction() {
        return instruction;
    }
}
