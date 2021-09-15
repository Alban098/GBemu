package core.cartridge;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Cartridge {

    private final int[] rom;

    public Cartridge(String file) {
        rom = new int[0x8000];
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
}
