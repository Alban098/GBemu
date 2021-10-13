package core.cartridge.mbc;

import core.cpu.LR35902;

public class MBC3 extends MemoryBankController {

    private boolean ram_enabled = false;
    private int selected_rom_bank = 1;
    private int selected_ram_bank = 0;
    private int latchRegister = 0x00;
    private int rtcMapped = 0;
    private boolean hasTimer = false;
    private boolean rtcLatched = false;
    private final int[] rtc = new int[5];
    private long mcycles = 0;

    public MBC3(int nb_ROM_bank, int nb_RAM_bank, boolean battery, boolean timer) {
        super(nb_ROM_bank, nb_RAM_bank);
        this.battery = battery;
        hasTimer = timer;
    }

    /**
     * Writing in range :
     * [0x0000, 0x1FFF] : Enable or disable RAM if present
     * [0x2000, 0x3FFF] : Write the ROM Bank mapped to [0x4000, 0x7FFF]
     * [0x4000, 0x5FFF] : Write the RAM Bank mapped to [0xA000, 0xBFFF] or the RTC Register address if present
     * [0x6000, 0x7FFF] : Writing 1 latch the RTC Registers
     * @param addr the address to write as 16bit unsigned int
     * @param data the data to write as 8bit unsigned int
     */
    @Override
    public void write(int addr, int data) {
        //RAM Enable
        if (addr <= 0x1FFF) {
            ram_enabled = (data & 0x0A) == 0x0A;
        //ROM Bank Number
        } else if (addr <= 0x3FFF) {
            selected_rom_bank = (data == 0) ? 0x01 : data & 0x7F;
        //RAM Bank Number / RTC Register Select
        } else if (addr <= 0x5FFF) {
            if (data > 0x8 && data < 0xC) {
                rtcMapped = data;
            } else {
                rtcMapped = 0;
                selected_ram_bank = data & 0x03;
            }
        //Latch RTC Timer
        } else if (addr <= 0x7FFF) {
            if (latchRegister == 0x00 && data == 0x01)
                rtcLatched = !rtcLatched;
            latchRegister = data;
        }
    }

    @Override
    public int mapRAMAddr(int addr) {
        if (!ram_enabled || nb_RAM_bank == 0 || rtcMapped != 0)
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
    public boolean hasTimer() {
        return hasTimer;
    }

    @Override
    public void clock() {
        mcycles++;
        if (hasTimer && !rtcLatched) {
            if (mcycles >= LR35902.CPU_CYCLES_PER_SEC) {
                rtc[0]++;
                //If seconds overflow
                if (rtc[0] >= 0x3B) {
                    rtc[0] = 0;
                    rtc[1]++;
                    //If minutes overflow
                    if (rtc[1] >= 0x3B) {
                        rtc[1] = 0;
                        rtc[2]++;
                        //If hours overflow
                        if (rtc[2] >= 0x17) {
                            rtc[2] = 0;
                            rtc[3]++;
                            //If days lower overflow
                            if (rtc[4] >= 0xFF) {
                                rtc[4] = 0;
                                //If days upper has overflowed also
                                if ((rtc[5] & 0x1) == 0)
                                    rtc[5] |= 1;
                                //Set the flags and reset counter
                                else
                                    rtc[5] = (rtc[5] & 0xFE) | 0x80;
                            }
                        }
                    }
                }
            }
        }
    }

    public int readTimer() {
        if (hasTimer && rtcMapped != 0x00)
            return rtc[rtcMapped - 0x8];
        return 0x00;
    }

    public void writeTimer(int data) {
        if (hasTimer && rtcMapped != 0x00)
            rtc[rtcMapped - 0x8] = data;
    }
}
