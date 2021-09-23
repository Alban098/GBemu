package core.cartridge.mbc;

public class NoMBC extends MemoryBankController {

    public NoMBC(int nb_ROM_bank, int nb_RAM_bank) {
        super(nb_ROM_bank, nb_RAM_bank);
    }

    @Override
    public void write(int addr, int data) {}

    @Override
    public int mapRAMAddr(int addr) {
        if (nb_RAM_bank > 0)
            return addr & 0x1FFF;
        return -1;
    }

    @Override
    public int mapROMAddr(int addr) {
        return addr;
    }
}
