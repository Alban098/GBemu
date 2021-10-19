package core.cartridge.mbc;

public abstract class MemoryBankController {

    protected final int nb_ROM_bank;
    protected final int nb_RAM_bank;
    protected boolean battery = false;

    public MemoryBankController(int nb_ROM_bank, int nb_RAM_bank) {
        this.nb_ROM_bank = nb_ROM_bank;
        this.nb_RAM_bank = nb_RAM_bank;
    }

    public abstract void write(int addr, int data);
    public abstract int mapRAMAddr(int addr);
    public abstract int mapROMAddr(int addr);

    public boolean hasBattery() {
        return battery;
    }

    public boolean hasRam() {
        return nb_RAM_bank > 0;
    }

    public void clock() {
        //Do Nothing by default
    }

    public boolean hasTimer() {
        return false;
    }
}
