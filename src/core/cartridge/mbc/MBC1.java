package core.cartridge.mbc;

import debug.Logger;

public class MBC1 extends MemoryBankController {

    private boolean ram_enabled = false;
    private int selected_rom_bank = 1;
    private int selected_ram_bank = 0;
    private boolean isRomMode = true;

    public MBC1(int nb_ROM_bank, int nb_RAM_bank) {
        super(nb_ROM_bank, nb_RAM_bank);
    }

    @Override
    public void write(int addr, int data) {
        if (addr <= 0x1FFF) //RAM Enable
            ram_enabled = (data & 0x0A) == 0x0A;
        else if (addr <= 0x3FFF) {//ROM Bank select
            selected_rom_bank = (data == 0) ? 0x01 : data & 0x1F;
            Logger.log(Logger.Type.INFO, "selected ROM bank " + selected_rom_bank);
        }
        else if (addr <= 0x5FFF) {
            if (isRomMode) {
                selected_rom_bank &= 0x1F;
                selected_rom_bank |= (data & 0x03) << 5;
                if ((selected_rom_bank & 0x0F) == 0x00)
                    selected_rom_bank++;
            } else {
                selected_ram_bank = data & 0x1F;
            }
            Logger.log(Logger.Type.INFO, "selected ROM bank " + selected_rom_bank);
        } else if (addr <= 0x7FFF) {
            isRomMode = data == 0x00;
            if (isRomMode)
                selected_ram_bank = 0;
            else
                selected_rom_bank &= 0x3;
        }
    }

    @Override
    public int mapRAMAddr(int addr) {
        if (!ram_enabled || nb_RAM_bank == 0)
            return -1;
        if (selected_ram_bank == 0x00)
            return addr & 0x1FFF;
        return (addr & 0x1FFF) + (0x2000 * selected_ram_bank);
    }

    @Override
    public int mapROMAddr(int addr) {
        if (addr <= 0x3FFF)
            return addr;
        else if (addr <= 0x7FFF)
            return (addr & 0x3FFF) + (0x4000 * selected_rom_bank);
        return addr & 0x3FFF;
    }
}
