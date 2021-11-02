package gbemu.core.cartridge.mbc;

import gbemu.core.GameBoy;

public class MBC5 extends MemoryBankController {

    private boolean ram_enabled = false;
    private int selected_rom_bank = 1;
    private int selected_ram_bank = 0;

    public MBC5(GameBoy gameboy, int nb_ROM_bank, int nb_RAM_bank, boolean battery) {
        super(gameboy, nb_ROM_bank, nb_RAM_bank);
        this.battery = battery;
    }

    /**
     * Writing in range :
     * [0x0000, 0x1FFF] : Enable or disable RAM if present
     * [0x2000, 0x2FFF] : Write the ROM Bank low 8 bits mapped to [0x4000, 0x7FFF]
     * [0x3000, 0x3FFF] : Write the ROM Bank high bitmapped to [0x4000, 0x7FFF]
     * [0x4000, 0x5FFF] : Write the RAM Bank mapped to [0xA000, 0xBFFF]
     * @param addr the address to write as 16bit unsigned int
     * @param data the data to write as 8bit unsigned int
     */
    @Override
    public void write(int addr, int data) {
        //RAM Enable
        if (addr <= 0x1FFF) {
            ram_enabled = (data & 0x0A) == 0x0A;
        //ROM Bank Number
        } else if (addr <= 0x2FFF) {
            selected_rom_bank = data & 0xFF;
            //ROM Bank high bit
        } else if (addr <= 0x3FFF) {
            selected_rom_bank = ((data & 0x01) << 8) | selected_rom_bank;
        } else if (addr <= 0x5FFF) {
            selected_ram_bank = data & 0x0F;
        }
    }

    @Override
    public int mapRAMAddr(int addr) {
        if (!ram_enabled || nb_RAM_bank == 0)
            return -1;
        else return addr & 0x1FFF + (0x2000 * selected_ram_bank);
    }

    @Override
    public int mapROMAddr(int addr) {
        if (addr <= 0x3FFF)
            return addr;
        else if (addr <= 0x7FFF)
            return (addr & 0x3FFF) + (0x4000 * selected_rom_bank);
        return addr & 0x3FFF;
    }

    @Override
    public int getROMBank() {
        return selected_rom_bank;
    }

    @Override
    public int getRAMBank() {
        return selected_ram_bank;
    }
}
