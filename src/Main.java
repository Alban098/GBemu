import core.GameBoy;
import gui.Window;

public class Main {


    public static void main(String[] args) {
        String rom = "02-interrupts.gb";
        GameBoy gb = new GameBoy();
        gb.insertCartridge("roms\\" + rom);

        Window window = new Window(gb);
        window.init();
        window.run();
        window.destroy();
    }
}