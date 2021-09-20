import core.GameBoy;
import gui.Window;

public class Main {


    public static void main(String[] args) throws Exception {
        String rom = "OK-06-ld r,r.gb";
        GameBoy gb = new GameBoy();
        gb.insertCartridge("C:\\Users\\alban\\Documents\\Developpement\\GBemu\\roms\\" + rom);

        Window window = new Window(gb);
        window.init();
        window.run();
        window.destroy();
    }
}