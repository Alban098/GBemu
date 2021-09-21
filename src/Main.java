import core.GameBoy;
import gui.Window;

public class Main {


    public static void main(String[] args) {
        String rom = "OK-01-special.gb";
        GameBoy gb = new GameBoy("roms\\DMG_ROM.bin");
        gb.insertCartridge("roms\\" + rom);

        Window window = new Window(gb);
        window.init();
        window.run();
        window.destroy();
    }
}