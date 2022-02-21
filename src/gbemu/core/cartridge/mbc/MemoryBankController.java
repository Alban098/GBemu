package gbemu.core.cartridge.mbc;

import gbemu.core.GameBoy;

public abstract class MemoryBankController {

    protected final int nb_rom_bank;
    protected final int nb_ram_bank;
    protected boolean battery = false;

    protected final GameBoy gameboy;

    public MemoryBankController(GameBoy gameboy, int nb_rom_bank, int nb_ram_bank) {
        this.nb_rom_bank = nb_rom_bank;
        this.nb_ram_bank = nb_ram_bank;
        this.gameboy = gameboy;
    }

    public abstract void write(int addr, int data);
    public abstract int mapRAMAddr(int addr);
    public abstract int mapROMAddr(int addr);

    public boolean hasBattery() {
        return battery;
    }

    public boolean hasRam() {
        return nb_ram_bank > 0;
    }

    public void clock() {
        //Do Nothing by default
    }

    public boolean hasTimer() {
        return false;
    }

    public abstract int getRomBank();

    public abstract int getRamBank();
}
