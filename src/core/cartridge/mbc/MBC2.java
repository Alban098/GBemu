package core.cartridge.mbc;

import core.GameBoy;

public class MBC2 extends MemoryBankController {

    private boolean ram_enabled = false;
    private int selected_rom_bank = 1;

    public MBC2(GameBoy gameboy, int nb_ROM_bank, int nb_RAM_bank, boolean battery) {
        super(gameboy, nb_ROM_bank, nb_RAM_bank);
        this.battery = battery;
    }

    /**
     * Writing in range :
     * [0x0000, 0x1FFF] : Enable or disable RAM if present
     * [0x2000, 0x3FFF] : Write the ROM Bank mapped to [0x4000, 0x7FFF]
     * [0x4000, 0x5FFF] : Write the ROM Bank mapped to [0xA000, 0xBFFF] or the ROM Bank high bits
     * [0x6000, 0x7FFF] : Write the banking mode
     * @param addr the address to write as 16bit unsigned int
     * @param data the data to write as 8bit unsigned int
     */
    @Override
    public void write(int addr, int data) {
        // RAM Enable
        if (addr <= 0x3FFF) {
            //RAM Enable if bit 8 is cleared
            if ((addr & 0x10) == 0x00) {
                ram_enabled = (data & 0x0A) == 0x0A;
            //ROM Bank if bit 8 is set
            } else {
                selected_rom_bank = (data == 0) ? 0x01 : data & 0x0F;
            }
        }
    }

    @Override
    public int mapRAMAddr(int addr) {
        //If RAM is disabled or there is no RAM Bank, the address is not mapped
        if (!ram_enabled)
            return -1;
        else
            return addr & 0x01FF;
    }

    @Override
    public int mapROMAddr(int addr) {
        if (addr >= 0x4000 && addr <= 0x7FFF)
            return (addr & 0x3FFF) + (0x4000 * selected_rom_bank);
        else if (addr <= 0x3FFF)
            return addr & 0x3FFF;
        return addr & 0x7FFF;
    }
}
