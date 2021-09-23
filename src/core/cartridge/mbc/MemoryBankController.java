package core.cartridge.mbc;

public abstract class MemoryBankController {

    protected int nb_ROM_bank;
    protected int nb_RAM_bank;

    public MemoryBankController(int nb_ROM_bank, int nb_RAM_bank) {
        this.nb_ROM_bank = nb_ROM_bank;
        this.nb_RAM_bank = nb_RAM_bank;
    }

    public abstract void write(int addr, int data);
    public abstract int mapRAMAddr(int addr);
    public abstract int mapROMAddr(int addr);
}
