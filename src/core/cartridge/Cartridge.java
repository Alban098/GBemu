package core.cartridge;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Cartridge {

    private final int[] rom;
    private final int[] ram;

    private int selectedBank = 1;

    public Cartridge(String file) {
        Path path = Paths.get(file);

        byte[] bytes = new byte[0];
        try { bytes = Files.readAllBytes(path); } catch (IOException e) { e.printStackTrace(); }

        rom = new int[bytes.length];
        ram = new int[0x2000];

        for (int i = 0; i < bytes.length; i++) {
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
