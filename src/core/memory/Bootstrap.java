package core.memory;

import core.GameBoy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Bootstrap {

    private static int[] DMG_BOOTSTRAP;
    private static int[] CGB_BOOTSTRAP;

    private final GameBoy gameboy;

    public Bootstrap(GameBoy gameboy) {
        this.gameboy = gameboy;
    }

    public void loadBootstrap(GameBoy.Mode mode, String file) {
        byte[] bytes = new byte[0];
        try { bytes = Files.readAllBytes(Paths.get(file));
        } catch (IOException e) { e.printStackTrace(); }
        if (mode == GameBoy.Mode.DMG) {
            DMG_BOOTSTRAP = new int[bytes.length];
            for (int i = 0; i < bytes.length; i++)
                DMG_BOOTSTRAP[i] = ((int) bytes[i]) & 0xFF;
        } else if (mode == GameBoy.Mode.CGB) {
            CGB_BOOTSTRAP = new int[bytes.length];
            for (int i = 0; i < bytes.length; i++)
                CGB_BOOTSTRAP[i] = ((int) bytes[i]) & 0xFF;
        }
    }

    public int readByte(int addr) {
        switch (gameboy.mode) {
            case DMG -> { if (addr <= 0xFF) return DMG_BOOTSTRAP[addr]; }
            case CGB -> { if (addr <= 0xFF || (addr >= 0x200 && addr <= 0x8FF)) return CGB_BOOTSTRAP[addr % 0x8FF]; }
        }
        return -1;
    }

    public String getSector(int addr) {
        switch (gameboy.mode) {
            case DMG -> { if (addr <= 0xFF) return "BOOT"; }
            case CGB -> { if (addr <= 0xFF || (addr >= 0x200 && addr <= 0x8FF)) return "BOOT"; }
        }
        return "ROM0";
    }
}
