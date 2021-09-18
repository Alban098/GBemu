import core.Memory;
import core.cpu.LR35902;

public class Main {

    public static void main(String[] args) {
        String rom = "01-special.gb";
        Memory memory = new Memory();
        LR35902 cpu = new LR35902(memory);
        memory.loadCart("E:\\Developpement\\Projets\\Java\\GBemu\\roms\\" + rom);

        while (true)
            cpu.clock();
    }
}