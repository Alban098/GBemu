package gbemu.core.cartridge.mbc;

import gbemu.core.GameBoy;

public class NoMBC extends MemoryBankController {

    public NoMBC(GameBoy gameboy, int nb_rom_bank, int nb_ram_bank) {
        super(gameboy, nb_rom_bank, nb_ram_bank);
    }

    @Override
    public void write(int addr, int data) {}

    @Override
    public int mapRAMAddr(int addr) {
        if (nb_ram_bank > 0)
            return addr & 0x1FFF;
        return -1;
    }

    @Override
    public int mapROMAddr(int addr) {
        return addr;
    }

    @Override
    public int getRomBank() {
        return 1;
    }

    @Override
    public int getRamBank() {
        return 0;
    }
}
