import core.GameBoy;
import core.MMU;
import core.cpu.LR35902;
import core.ppu.PPU;
import gui.CPULayer;
import gui.Window;

public class Main {


    public static void main(String[] args) throws Exception {
        String rom = "OK-06-ld r,r.gb";
        GameBoy gb = new GameBoy();
        gb.insertCartridge("E:\\Developpement\\Projets\\Java\\GBemu\\roms\\" + rom);

        Window window = new Window(gb);
        window.init();
        window.run();
        window.destroy();
    }
}