package gbemu.core.cpu;

import gbemu.core.cpu.register.RegisterWord;

public class State {

    private final RegisterWord af;
    private final RegisterWord bc;
    private final RegisterWord de;
    private final RegisterWord hl;
    private final RegisterWord sp;
    private final RegisterWord pc;
    private boolean ime;
    private final Instruction instruction;

    public State() {
        af = new RegisterWord(0);
        bc = new RegisterWord(0);
        de = new RegisterWord(0);
        hl = new RegisterWord(0);
        sp = new RegisterWord(0);
        pc = new RegisterWord(0);
        ime = false;
        instruction = new Instruction(0, Instruction.Type.MISC, "NOP", 1, null, this);
    }

    public synchronized void set(RegisterWord af, RegisterWord bc, RegisterWord de, RegisterWord hl, RegisterWord sp, RegisterWord pc, boolean ime, Instruction instruction) {
        this.af.write(af.read());
        this.bc.write(bc.read());
        this.de.write(de.read());
        this.hl.write(hl.read());
        this.sp.write(sp.read());
        this.pc.write(pc.read() - instruction.getLength());
        this.ime = ime;
        this.instruction.copyMeta(instruction);
    }

    public synchronized RegisterWord getAf() {
        return af;
    }

    public synchronized RegisterWord getBc() {
        return bc;
    }

    public synchronized RegisterWord getDe() {
        return de;
    }

    public synchronized RegisterWord getHl() {
        return hl;
    }

    public synchronized RegisterWord getSp() {
        return sp;
    }

    public synchronized RegisterWord getPc() {
        return pc;
    }

    public synchronized boolean getIME() {
        return ime;
    }

    public synchronized Instruction getInstruction() {
        return instruction;
    }

    public synchronized boolean hasFlag(int flag) {
        return (af.getLow().read() & flag) == flag;
    }

}
