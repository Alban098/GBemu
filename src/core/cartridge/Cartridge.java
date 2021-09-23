package core.cartridge;

import core.cartridge.mbc.MBC1;
import core.cartridge.mbc.MemoryBankController;
import core.cartridge.mbc.NoMBC;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Cartridge {

    private final int[] rom;
    private final int[] ram;

    private final MemoryBankController mbc;

    public Cartridge(String file) throws Exception {
        Path path = Paths.get(file);

        byte[] bytes = new byte[0];
        try { bytes = Files.readAllBytes(path); } catch (IOException e) { e.printStackTrace(); }

        int type = bytes[0x147];
        int nb_rom_bank = bytes[0x148] << 2;
        int nb_ram_bank;
        switch (bytes[0x149]) {
            case 0x02 -> nb_ram_bank = 1;
            case 0x03 -> nb_ram_bank = 4;
            case 0x04 -> nb_ram_bank = 16;
            case 0x05 -> nb_ram_bank = 8;
            default -> nb_ram_bank = 0;
        }

        switch(type) {
            case 0x00 -> mbc = new NoMBC(2, 0);
            case 0x01 -> mbc = new MBC1(nb_rom_bank, nb_ram_bank);
            default -> throw new Exception("MBC not implemented yet");
        }

        rom = new int[bytes.length];
        if (nb_ram_bank > 0)
            ram = new int[0x2000 * nb_ram_bank];
        else
            ram = null;

        for (int i = 0; i < bytes.length; i++) {
            rom[i] = ((int)bytes[i]) & 0xFF;
        }
    }

    public void write(int addr, int data) {
        addr &= 0xFFFF;
        data &= 0xFF;
        if (addr >= 0x8000 && addr <= 0x9FFF) {
            int mapped = mbc.mapRAMAddr(addr);
            if (mapped >= 0x00)
                ram[mapped] = data;
        } else {
            mbc.write(addr, data);
        }
    }

    public int read(int addr) {
        if (addr >= 0x8000 && addr <= 0x9FFF) {
            int mapped = mbc.mapRAMAddr(addr);
            if (mapped >= 0x00)
                return ram[mapped];
            return 0x00;
        }
        int mappedAddr = mbc.mapROMAddr(addr);
        if (mappedAddr >= 0x00)
            return rom[addr & 0x7FFF];
        return 0x00;
    }
}
