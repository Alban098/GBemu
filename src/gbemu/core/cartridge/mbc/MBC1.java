package gbemu.core.cartridge.mbc;

import gbemu.core.GameBoy;

public class MBC1 extends MemoryBankController {

    private boolean ram_enabled = false;
    private int selected_rom_bank = 1;
    private int selected_ram_bank = 0;
    private boolean bankingMode = false;

    public MBC1(GameBoy gameboy, int nb_rom_bank, int nb_ram_bank, boolean battery) {
        super(gameboy, nb_rom_bank, nb_ram_bank);
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
        if (addr <= 0x1FFF) {
            ram_enabled = (data & 0x0A) == 0x0A;
        //ROM Bank Number
        } else if (addr <= 0x3FFF) {
            selected_rom_bank = (data == 0) ? 0x01 : data & 0x1F;
        //RAM Bank Number / ROM Bank Number High
        } else if (addr <= 0x5FFF) {
            selected_ram_bank = data & 0x03;
            //If the cart need more than 5bits to address all the ROM Banks
            if (nb_rom_bank > 32)
                selected_rom_bank = (selected_ram_bank & 0x1F) | ((selected_ram_bank & 0x03) << 5);
        //Banking Mode
        } else if (addr <= 0x7FFF) {
            bankingMode = (data & 0x01) == 0x01;
            //if the car has fewer than 32 ROM Bank, this register does nothing
            if (nb_rom_bank < 32)
                selected_rom_bank &= 0x1F;
            else
                selected_rom_bank = (selected_ram_bank & 0x1F) | ((selected_ram_bank & 0x03) << 5);
        }
    }

    @Override
    public int mapRAMAddr(int addr) {
        //If RAM is disabled or there is no RAM Bank, the address is not mapped
        if (!ram_enabled || nb_ram_bank == 0)
            return -1;
        //If the cart is a 'Small ROM' and 'Large RAM', the RAM bank is locked to 0x00 on mode 0 and mapped in mode 1
        if (nb_rom_bank < 32 && nb_ram_bank > 1 && bankingMode)
            return (addr & 0x1FFF) + (0x2000 * selected_ram_bank);
        else
            return addr & 0x1FFF;
    }

    @Override
    public int mapROMAddr(int addr) {
        //The top mapped ROM Bank is not affected by the banking mode
        if (addr >= 0x4000 && addr <= 0x7FFF) {
            return (addr & 0x3FFF) + (0x4000 * selected_rom_bank);
        } else if (addr <= 0x3FFF) {
            //In mode one the low ROM Bank is mapped according to the ROM Bank register, otherwise it's mapped to Bank 0
            if (bankingMode)
                return (addr & 0x3FFF) + (0x4000 * (selected_ram_bank << 5));
            else
                return addr & 0x3FFF;
        }
        return addr & 0x7FFF;
    }

    @Override
    public int getRomBank() {
        return selected_rom_bank;
    }

    @Override
    public int getRamBank() {
        return selected_ram_bank;
    }
}
