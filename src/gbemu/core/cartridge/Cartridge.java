package gbemu.core.cartridge;

import console.Console;
import console.LogLevel;
import gbemu.core.GameBoy;
import gbemu.core.cartridge.mbc.*;

import java.io.IOException;
import java.nio.file.*;

public class Cartridge {

    private final String file;
    private String game_id;
    private final MemoryBankController mbc;

    private final int[] rom;
    private final int[] ram;


    public Cartridge(String file, GameBoy gameboy) throws Exception {
        Path path = Paths.get(file);

        byte[] bytes = new byte[0];
        try {
            bytes = Files.readAllBytes(path);
        } catch (IOException e) {
            e.printStackTrace();
            Console.getInstance().log(LogLevel.ERROR, "Error when loading ROM : " + e.getMessage());
        }

        this.file = file;
        this.game_id = "";
        for (int i = 0x134; i < 0x143; i++)
            game_id += String.valueOf(bytes[i]);

        int level = bytes[0x147];

        gameboy.mode = GameBoy.Mode.DMG;
        if ((bytes[0x143] & 0xFF) == 0x80 || (bytes[0x143] & 0xFF) == 0xC0)
            gameboy.mode = GameBoy.Mode.CGB;

        int nb_rom_bank = 2 << bytes[0x148];
        int nb_ram_bank;
        switch (bytes[0x149]) {
            case 0x02 -> nb_ram_bank = 1;
            case 0x03 -> nb_ram_bank = 4;
            case 0x04 -> nb_ram_bank = 16;
            case 0x05 -> nb_ram_bank = 8;
            default -> nb_ram_bank = 0;
        }

        switch(level) {
            case 0x00 -> mbc = new NoMBC(gameboy, 2, 0);
            case 0x01, 0x02 -> mbc = new MBC1(gameboy, nb_rom_bank, nb_ram_bank, false);
            case 0x03 -> mbc = new MBC1(gameboy, nb_rom_bank, nb_ram_bank, true);
            case 0x05 -> mbc = new MBC2(gameboy, nb_rom_bank, nb_ram_bank, false);
            case 0x06 -> mbc = new MBC2(gameboy, nb_rom_bank, nb_ram_bank, true);
            case 0x08, 0x09 -> mbc = new NoMBC(gameboy, nb_rom_bank, nb_ram_bank);
            case 0x0F, 0x10 -> mbc = new MBC3(gameboy, nb_rom_bank, nb_ram_bank, true, true);
            case 0x11, 0x12 -> mbc = new MBC3(gameboy, nb_rom_bank, nb_ram_bank, false, false);
            case 0x13 -> mbc = new MBC3(gameboy, nb_rom_bank, nb_ram_bank, true, false);
            case 0x19, 0x1A, 0x1C, 0x1D -> mbc = new MBC5(gameboy, nb_rom_bank, nb_ram_bank, false);
            case 0x1B, 0x1E -> mbc = new MBC5(gameboy, nb_rom_bank, nb_ram_bank, true);
            default -> throw new Exception("MBC not implemented yet");
        }

        rom = new int[bytes.length];
        if (nb_ram_bank > 0)
            ram = new int[0x2000 * nb_ram_bank];
        else
            ram = null;

        for (int i = 0; i < bytes.length; i++)
            rom[i] = ((int)bytes[i]) & 0xFF;
        load();
    }

    public String getGameId() {
        return game_id;
    }

    public void write(int addr, int data) {
        addr &= 0xFFFF;
        data &= 0xFF;
        if (addr >= 0xA000 && addr <= 0xBFFF) {
            int mapped = mbc.mapRAMAddr(addr);
            if (mapped >= 0x00)
                ram[mapped] = data;
            else if (mbc instanceof MBC3 && mbc.hasTimer())
                ((MBC3)mbc).writeTimer(data);
        } else if (addr <= 0x7FFF){
            mbc.write(addr, data);
        }
    }

    public int read(int addr) {
        if (addr >= 0xA000 && addr <= 0xBFFF) {
            int mapped = mbc.mapRAMAddr(addr);
            if (mapped >= 0x00)
                return ram[mapped];
            else if (mbc.hasTimer())
                return ((MBC3)mbc).readTimer();
            return 0x00;
        } else if (addr <= 0x7FFF) {
            int mappedAddr = mbc.mapROMAddr(addr);
            if (mappedAddr >= 0x00)
                return rom[mappedAddr];
        }
        return 0x00;
    }

    public void save() {
        if (mbc.hasBattery() && mbc.hasRam()) {
            byte[] ram_export;
            Path path = Paths.get(file.replace(".gb", ".sav"));
            if (mbc instanceof MBC3 && mbc.hasTimer()) {
                ram_export = new byte[ram.length + ((MBC3) mbc).dumpRTC().length];
                for (int i = 0; i < ((MBC3) mbc).dumpRTC().length; i++)
                    ram_export[i + ram.length] = (byte) (((MBC3) mbc).dumpRTC()[i] & 0xFF);
            } else {
                ram_export = new byte[ram.length];
            }

            for (int i = 0; i < ram.length; i++)
                ram_export[i] = (byte) (ram[i] & 0xFF);
            try {
                Files.write(path, ram_export, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            } catch (IOException e) {
                Console.getInstance().log(LogLevel.ERROR, e.getMessage());
            }
        }
    }

    public void load() {
        if (mbc.hasBattery() && mbc.hasRam()) {
            Path path = Paths.get(file.replace(".gb", ".sav"));
            byte[] bytes;
            try {
                bytes = Files.readAllBytes(path);
                for (int i = 0; i < ram.length && i < bytes.length; i++)
                    ram[i] = ((int)bytes[i]) & 0xFF;
                if (mbc instanceof MBC3 && mbc.hasTimer()) {
                    ((MBC3)mbc).restoreRTC(ram.length, bytes);
                }
            } catch (IOException e) {
                Console.getInstance().log(LogLevel.ERROR, e.getMessage());
            }
        }
    }

    public void clock() {
        mbc.clock();
    }

    public int getROMBank() {
        return mbc.getRomBank();
    }

    public int getRAMBank() {
        return mbc.getRamBank();
    }
}
