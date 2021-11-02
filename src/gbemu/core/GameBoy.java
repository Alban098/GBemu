package gbemu.core;

import audio.AudioEngine;
import console.Console;
import console.Type;
import gbemu.core.apu.APU;
import gbemu.core.cartridge.mbc.MBC3;
import gbemu.extension.cheats.CheatManager;
import gbemu.core.cpu.LR35902;
import gbemu.core.input.InputManager;
import gbemu.core.input.InputState;
import gbemu.core.input.Button;
import gbemu.core.memory.MMU;
import gbemu.core.ppu.PPU;
import gbemu.core.ppu.helper.ColorShade;
import gbemu.settings.Setting;
import gbemu.extension.debug.Debugger;
import gbemu.extension.debug.DebuggerMode;
import gbemu.settings.SettingsContainer;

import java.awt.*;

public class GameBoy {

    private boolean hasCartridge = false;
    public Mode mode = Mode.DMG;
    private long mcycles = 0;

    private final MMU memory;
    private final LR35902 cpu;
    private final PPU ppu;
    private final APU apu;
    private final Timer timer;
    private final InputManager inputManager;
    private final Debugger debugger;
    private final CheatManager cheatManager;
    private final SettingsContainer settingsContainer;

    private GameBoyState currentState;
    private AudioEngine audioEngine;
    private boolean half_exec_step = false;
    private int speed_factor = 1;

    public GameBoy(String configFile) {
        debugger = new Debugger(this);
        memory = new MMU(this);
        cpu = new LR35902(this);
        ppu = new PPU(this);
        apu = new APU(this);
        timer = new Timer(this);
        inputManager = new InputManager(this);
        currentState = GameBoyState.RUNNING;
        cheatManager = new CheatManager(this);
        settingsContainer = new SettingsContainer(this, configFile);
    }

    public void loadSettings() {
        settingsContainer.loadFile();
    }

    public void setAudioEngine(AudioEngine audioEngine) {
        this.audioEngine = audioEngine;
    }

    public Debugger getDebugger() {
        return debugger;
    }

    public void insertCartridge(String file) throws Exception {
        memory.loadCart(file);
        hasCartridge = true;
        reset();
        debugger.init();
    }

    public void reset() {
        memory.reset();
        cpu.reset();
        ppu.reset();
        apu.reset();
        memory.writeRaw(MMU.P1, 0xCF);
        memory.writeRaw(MMU.SB, 0x00);
        memory.writeRaw(MMU.SC, mode == Mode.CGB ? 0x7F : 0x7E);
        memory.writeRaw(MMU.DIV, 0xAB);
        memory.writeRaw(MMU.TIMA, 0x00);
        memory.writeRaw(MMU.TMA, 0x00);
        memory.writeRaw(MMU.TAC, 0xF8);
        memory.writeRaw(MMU.IF, 0xE1);
        memory.writeRaw(MMU.NR10, 0x80);
        memory.writeRaw(MMU.NR11, 0xBF);
        memory.writeRaw(MMU.NR12, 0x03);
        memory.writeRaw(MMU.NR13, 0xFF);
        memory.writeRaw(MMU.NR14, 0xBF);
        memory.writeRaw(MMU.NR21, 0x3F);
        memory.writeRaw(MMU.NR22, 0x00);
        memory.writeRaw(MMU.NR23, 0xFF);
        memory.writeRaw(MMU.NR24, 0xBF);
        memory.writeRaw(MMU.NR30, 0x7F);
        memory.writeRaw(MMU.NR31, 0xFF);
        memory.writeRaw(MMU.NR32, 0x9F);
        memory.writeRaw(MMU.NR33, 0xFF);
        memory.writeRaw(MMU.NR34, 0xBF);
        memory.writeRaw(MMU.NR41, 0xFF);
        memory.writeRaw(MMU.NR42, 0x00);
        memory.writeRaw(MMU.NR43, 0x00);
        memory.writeRaw(MMU.NR44, 0xBF);
        memory.writeRaw(MMU.NR50, 0x77);
        memory.writeRaw(MMU.NR51, 0xF3);
        memory.writeRaw(MMU.NR52, 0xF1);
        memory.writeRaw(MMU.LCDC, 0x91);
        memory.writeRaw(MMU.STAT, 0x85);
        memory.writeRaw(MMU.SCX, 0x00);
        memory.writeRaw(MMU.SCY, 0x00);
        memory.writeRaw(MMU.LY, 0x00);
        memory.writeRaw(MMU.LYC, 0x00);
        memory.writeRaw(MMU.DMA,  mode == Mode.CGB ? 0x00 : 0xFF);
        memory.writeRaw(MMU.BGP, 0xFC);
        memory.writeRaw(MMU.OBP0, 0xFF);
        memory.writeRaw(MMU.OBP1, 0xFF);
        memory.writeRaw(MMU.WX, 0x00);
        memory.writeRaw(MMU.WY, 0x00);
        memory.writeRaw(MMU.IE, 0x00);
        memory.writeRaw(MMU.CGB_KEY_1, 0xFF);
        memory.writeRaw(MMU.CGB_VRAM_BANK, 0xFF);
        memory.writeRaw(MMU.CGB_HDMA1, 0xFF);
        memory.writeRaw(MMU.CGB_HDMA2, 0xFF);
        memory.writeRaw(MMU.CGB_HDMA3, 0xFF);
        memory.writeRaw(MMU.CGB_HDMA4, 0xFF);
        memory.writeRaw(MMU.CGB_HDMA5, 0xFF);
        memory.writeRaw(MMU.CGB_RP, 0xFF);
        memory.writeRaw(MMU.CGB_BCPS_BCPI, 0xFF);
        memory.writeRaw(MMU.CGB_BCPD_BGPD, 0xFF);
        memory.writeRaw(MMU.CGB_OCPS_OBPI, 0xFF);
        memory.writeRaw(MMU.CGB_OCPD_OBPD, 0xFF);
        memory.writeRaw(MMU.CGB_WRAM_BANK, 0xFF);
        Console.getInstance().log(Type.INFO, "Emulation reset");
        cpu.init();
    }

    public MMU getMemory() {
        return memory;
    }

    public LR35902 getCpu() {
        return cpu;
    }

    public PPU getPpu() {
        return ppu;
    }

    public APU getApu() {
        return apu;
    }

    public void executeInstruction() {
        int opcode_mcycles = Integer.MAX_VALUE;
        while (opcode_mcycles > 0) {
            switch (mode) {
                case CGB -> {
                    if (memory.clock()) {
                        opcode_mcycles = cpu.execute();
                        timer.clock();
                        if (half_exec_step) {
                            ppu.clock();
                            apu.clock();
                            inputManager.clock();
                            cheatManager.clock();
                        }
                        half_exec_step = !half_exec_step;
                    }
                }
                case DMG -> {
                    if (memory.clock()) {
                        opcode_mcycles = cpu.execute();
                        timer.clock();
                        ppu.clock();
                        apu.clock();
                        inputManager.clock();
                        cheatManager.clock();
                    }
                }
            }
            mcycles++;
            if (mcycles >= mode.cpu_cycles_per_second * 10L) {
                memory.saveCartridge();
                mcycles -= mode.cpu_cycles_per_second * 10L;
            }
        }
    }

    public void executeInstructions(int nb_instr, boolean force) {
        for (int i = 0; i < nb_instr && (currentState == GameBoyState.RUNNING || force); i++)
            executeInstruction();
    }

    public String getSerialOutput() {
        return memory.getSerialOutput();
    }

    public void flushSerialOutput() {
        memory.flushSerialOutput();
    }

    public void executeFrames() {
        for (int i = 0; i < speed_factor; i++)
            while(ppu.isFrameIncomplete() && currentState == GameBoyState.RUNNING)
                executeInstruction();
    }

    public void forceFrame() {
        while(ppu.isFrameIncomplete() && currentState == GameBoyState.DEBUG)
            executeInstruction();
    }

    public GameBoyState getState() {
        return currentState;
    }

    public void setState(GameBoyState state) {
        this.currentState = state;
    }

    public float getNextSample() {
        return apu.getNextSample();
    }

    public synchronized void setButtonState(Button button, InputState state) {
        inputManager.setButtonState(button, state);
    }

    public boolean hasCartridge() {
        return hasCartridge;
    }

    public boolean isDebuggerHooked(DebuggerMode mode) {
        return debugger.isHooked(mode);
    }

    public synchronized void propagateSetting(Setting<?> setting) {
        switch (setting.getIdentifier()) {
            case RTC -> MBC3.enableRTC((boolean) setting.getValue());
            case SPEED -> {
                speed_factor = (int) setting.getValue();
                mode.cpu_cycles_per_sample = 4194304f / APU.SAMPLE_RATE * speed_factor;
            }
            case BOOTSTRAP -> cpu.enableBootstrap((boolean) setting.getValue());
            case DMG_BOOTROM -> memory.loadBootstrap(Mode.DMG, (String)setting.getValue());
            case CGB_BOOTROM -> memory.loadBootstrap(Mode.CGB, (String)setting.getValue());
            case CHEAT_DATABASE -> cheatManager.loadCheats((String)setting.getValue());
            case DMG_PALETTE_0 -> ColorShade.WHITE.setColor((Color)setting.getValue());
            case DMG_PALETTE_1 -> ColorShade.LIGHT_GRAY.setColor((Color)setting.getValue());
            case DMG_PALETTE_2 -> ColorShade.DARK_GRAY.setColor((Color)setting.getValue());
            case DMG_PALETTE_3 -> ColorShade.BLACK.setColor((Color)setting.getValue());
            case GAMMA -> ppu.setGamma((float) setting.getValue());
            case SQUARE_1_ENABLED -> apu.enableSquare1((boolean) setting.getValue());
            case SQUARE_2_ENABLED -> apu.enableSquare2((boolean) setting.getValue());
            case WAVE_ENABLED -> apu.enableWave((boolean) setting.getValue());
            case NOISE_ENABLED -> apu.enableNoise((boolean) setting.getValue());
            case VOLUME -> audioEngine.setVolume((float) setting.getValue());
        }
    }

    public String getGameId() {
        return memory.readGameId();
    }

    public CheatManager getCheatManager() {
        return cheatManager;
    }

    public SettingsContainer getSettingsContainer() {
        return settingsContainer;
    }

    public AudioEngine getAudioEngine() {
        return audioEngine;
    }

    public enum Mode {
        DMG(4194304),
        CGB(8388608);

        public final int cpu_cycles_per_second;
        public final int cpu_cycles_per_frame;
        public final int cpu_cycles_per_hblank;
        public final int cpu_cycles_per_vblank_scanline; //divide because VBlank is 10 scanline long
        public final int cpu_cycles_per_oam;
        public final int cpu_cycles_per_transfer;
        public final int cpu_cycles_256HZ = 4194304 / 256;
        public final int cpu_cycles_128HZ = 4194304 / 128;
        public final int cpu_cycles_64HZ = 4194304 / 64;
        public float cpu_cycles_per_sample = 4194304f / APU.SAMPLE_RATE;

        Mode(int cpu_cycles_per_second) {
            this.cpu_cycles_per_second = cpu_cycles_per_second;
            this.cpu_cycles_per_frame = 70224;
            this.cpu_cycles_per_hblank = 208;
            this.cpu_cycles_per_vblank_scanline = 456;
            this.cpu_cycles_per_oam = 80;
            this.cpu_cycles_per_transfer = 168;
        }
    }
}
