package core.cartridge;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class Cartridge {

    private final int[] rom;
    private final int[] ram;

    public Cartridge(String file) {
        rom = new int[0x8000];
        ram = new int[0x2000];
        Path path = Paths.get(file);

        byte[] bytes = new byte[0];
        try { bytes = Files.readAllBytes(path); } catch (IOException e) { e.printStackTrace(); }
        for (int i = 0; i < 0x8000; i++) {
            rom[i] = ((int)bytes[i]) & 0xFF;
        }
    }

    public void write(int addr, int data) {
        //TODO implement real behavior
        rom[addr & 0x7FFF] = data;
    }

    public int read(int addr) {
        //TODO implement real behavior
        return rom[addr & 0x7FFF];
    }

    public void writeSRAM(int addr, int data) {
        ram[addr & 0x1FFF] = data & 0xFF;
    }

    public int readSRAM(int addr) {
        return ram[addr & 0x1FFF];
    }

    public void switchRomBank(int data) {
        //TODO implement
    }
}
